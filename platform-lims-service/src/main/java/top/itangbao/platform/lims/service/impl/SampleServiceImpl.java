package top.itangbao.platform.lims.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.itangbao.platform.common.exception.ResourceNotFoundException;
import top.itangbao.platform.data.api.client.DataServiceFeignClient;
import top.itangbao.platform.data.api.dto.*;
import top.itangbao.platform.lims.dto.SampleRequest;
import top.itangbao.platform.lims.dto.SampleResponse;
import top.itangbao.platform.lims.service.SampleService;
import top.itangbao.platform.metadata.api.client.MetadataServiceFeignClient;
import top.itangbao.platform.metadata.api.dto.MetadataFieldCreateRequest;
import top.itangbao.platform.metadata.api.dto.MetadataSchemaCreateRequest;
import top.itangbao.platform.metadata.api.dto.MetadataSchemaDTO;
import feign.FeignException; // 引入 FeignException
import top.itangbao.platform.metadata.api.enums.FieldType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class SampleServiceImpl implements SampleService {

    private final MetadataServiceFeignClient metadataServiceFeignClient;
    private final DataServiceFeignClient dataServiceFeignClient;

    // 缓存 LIMS 模式的 ID，避免每次都调用 Metadata Service
    private final Map<String, Long> limsSchemaIdCache = new ConcurrentHashMap<>();
    private static final String LIMS_SAMPLE_SCHEMA_NAME = "LimsSample";

    @Autowired
    public SampleServiceImpl(MetadataServiceFeignClient metadataServiceFeignClient,
                             DataServiceFeignClient dataServiceFeignClient) {
        this.metadataServiceFeignClient = metadataServiceFeignClient;
        this.dataServiceFeignClient = dataServiceFeignClient;
    }

    /**
     * 在服务启动时或首次请求时，注册 LIMS 相关的元数据模式和字段
     * 这是一个简化版本，实际可能需要更复杂的模式管理和版本控制
     * @param tenantId 租户ID
     * @return 注册成功的模式ID
     */
    @Override
    public Long registerLimsMetadataSchema(String tenantId) {
        return limsSchemaIdCache.computeIfAbsent(tenantId, k -> {
            try {
                // 尝试获取现有模式
                MetadataSchemaDTO existingSchema = metadataServiceFeignClient.getSchemaByNameAndTenantId(LIMS_SAMPLE_SCHEMA_NAME, tenantId);
                dataServiceFeignClient.createOrUpdateDynamicTable(tenantId, LIMS_SAMPLE_SCHEMA_NAME, existingSchema.getId()); // 确保表存在或更新
                return existingSchema.getId();
            } catch (FeignException.NotFound e) {
                // 如果模式不存在，则创建新模式
                MetadataSchemaCreateRequest schemaCreateRequest = MetadataSchemaCreateRequest.builder()
                        .name(LIMS_SAMPLE_SCHEMA_NAME)
                        .description("LIMS Sample Information Schema")
                        .tenantId(tenantId)
                        .fields(Arrays.asList(
                                MetadataFieldCreateRequest.builder().fieldName("sampleName").fieldType(FieldType.STRING).required(true).description("Sample Name").build(),
                                MetadataFieldCreateRequest.builder().fieldName("batchNumber").fieldType(FieldType.STRING).required(true).description("Batch Number").build(),
                                MetadataFieldCreateRequest.builder().fieldName("collectionDate").fieldType(FieldType.DATE).required(true).description("Collection Date").build(),
                                MetadataFieldCreateRequest.builder().fieldName("status").fieldType(FieldType.ENUM).options("[\"RECEIVED\", \"IN_TEST\", \"COMPLETED\"]").required(true).defaultValue("RECEIVED").description("Sample Status").build()
                                // TODO: 如果有其他 LIMS 固定字段，可以在这里添加
                        ))
                        .build();
                MetadataSchemaDTO createdSchema = metadataServiceFeignClient.createSchema(schemaCreateRequest);
                dataServiceFeignClient.createOrUpdateDynamicTable(tenantId, LIMS_SAMPLE_SCHEMA_NAME, createdSchema.getId()); // 创建动态表
                return createdSchema.getId();
            } catch (Exception e) {
                throw new RuntimeException("Failed to register LIMS metadata schema: " + e.getMessage(), e);
            }
        });
    }

    @Override
    public SampleResponse createSample(String tenantId, SampleRequest request) {
        Long schemaId = registerLimsMetadataSchema(tenantId); // 确保模式和表已注册

        // 构建 Data Service 的 DynamicDataRequest
        Map<String, Object> dataFields = new HashMap<>();
        dataFields.put("sampleName", request.getSampleName());
        dataFields.put("batchNumber", request.getBatchNumber());
        dataFields.put("collectionDate", request.getCollectionDate());
        dataFields.put("status", request.getStatus());
        if (request.getCustomFields() != null) {
            dataFields.putAll(request.getCustomFields());
        }

        DynamicDataRequest dynamicDataRequest = DynamicDataRequest.builder()
                .tenantId(tenantId)
                .schemaName(LIMS_SAMPLE_SCHEMA_NAME)
                .data(dataFields)
                .build();

        DynamicDataResponse dynamicResponse = dataServiceFeignClient.insertDynamicData(tenantId, LIMS_SAMPLE_SCHEMA_NAME, dynamicDataRequest);

        return convertToSampleResponse(dynamicResponse);
    }

    @Override
    public SampleResponse getSampleById(String tenantId, Long sampleId) {
        // 确保模式和表已注册 (虽然这里只是查询，但可以确保前提条件)
        registerLimsMetadataSchema(tenantId);

        try {
            DynamicDataResponse dynamicResponse = dataServiceFeignClient.getDynamicDataById(tenantId, LIMS_SAMPLE_SCHEMA_NAME, sampleId);
            return convertToSampleResponse(dynamicResponse);
        } catch (FeignException.NotFound e) {
            throw new ResourceNotFoundException("Sample not found with ID: " + sampleId + " for tenant: " + tenantId);
        }
    }

    @Override
    public List<SampleResponse> getAllSamples(String tenantId) {
        // 确保模式和表已注册
        registerLimsMetadataSchema(tenantId);

        PageRequestDTO pageRequest = PageRequestDTO.builder().page(0).size(Integer.MAX_VALUE).build(); // 默认获取所有
        FilterRequestDTO filterRequest = FilterRequestDTO.builder().filters(Collections.emptyMap()).build(); // 默认无过滤

        PageResponseDTO<DynamicDataResponse> dynamicResponsesPage = dataServiceFeignClient.getAllDynamicData(
                tenantId,
                LIMS_SAMPLE_SCHEMA_NAME,
                pageRequest,
                filterRequest
        );
        return dynamicResponsesPage.getContent().stream()
                .map(this::convertToSampleResponse)
                .collect(Collectors.toList());
    }

    @Override
    public SampleResponse updateSample(String tenantId, Long sampleId, SampleRequest request) {
        // 确保模式和表已注册
        registerLimsMetadataSchema(tenantId);

        Map<String, Object> updates = new HashMap<>();
        if (request.getSampleName() != null) updates.put("sampleName", request.getSampleName());
        if (request.getBatchNumber() != null) updates.put("batchNumber", request.getBatchNumber());
        if (request.getCollectionDate() != null) updates.put("collectionDate", request.getCollectionDate());
        if (request.getStatus() != null) updates.put("status", request.getStatus());
        if (request.getCustomFields() != null) updates.putAll(request.getCustomFields());

        DynamicDataResponse dynamicResponse = dataServiceFeignClient.updateDynamicData(tenantId, LIMS_SAMPLE_SCHEMA_NAME, sampleId, updates);
        return convertToSampleResponse(dynamicResponse);
    }

    @Override
    public void deleteSample(String tenantId, Long sampleId) {
        // 确保模式和表已注册
        registerLimsMetadataSchema(tenantId);

        try {
            dataServiceFeignClient.deleteDynamicData(tenantId, LIMS_SAMPLE_SCHEMA_NAME, sampleId);
        } catch (FeignException.NotFound e) {
            throw new ResourceNotFoundException("Sample not found with ID: " + sampleId + " for tenant: " + tenantId);
        }
    }

    // 辅助方法：将 DynamicDataResponse 转换为 SampleResponse
    private SampleResponse convertToSampleResponse(DynamicDataResponse dynamicResponse) {
        Map<String, Object> data = dynamicResponse.getData();
        return SampleResponse.builder()
                .id(dynamicResponse.getId())
                .tenantId(dynamicResponse.getTenantId())
                .sampleName((String) data.get("sampleName"))
                .batchNumber((String) data.get("batchNumber"))
                .collectionDate((String) data.get("collectionDate"))
                .status((String) data.get("status"))
                .createdAt(dynamicResponse.getCreatedAt())
                .updatedAt(dynamicResponse.getUpdatedAt())
                .customFields(new HashMap<>(data)) // 将所有剩余的动态字段放入 customFields
                .build();
    }
}
