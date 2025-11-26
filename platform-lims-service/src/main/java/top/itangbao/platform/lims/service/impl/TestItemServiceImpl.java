package top.itangbao.platform.lims.service.impl;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.itangbao.platform.common.enums.FieldType;
import top.itangbao.platform.common.exception.ResourceNotFoundException;
import top.itangbao.platform.data.api.client.DataServiceFeignClient;
import top.itangbao.platform.data.api.dto.DynamicDataRequest;
import top.itangbao.platform.data.api.dto.DynamicDataResponse;
import top.itangbao.platform.data.api.dto.FilterRequestDTO;
import top.itangbao.platform.data.api.dto.PageRequestDTO;
import top.itangbao.platform.data.api.dto.PageResponseDTO;
import top.itangbao.platform.lims.dto.TestItemRequest;
import top.itangbao.platform.lims.dto.TestItemResponse;
import top.itangbao.platform.lims.service.TestItemService;
import top.itangbao.platform.metadata.api.client.MetadataServiceFeignClient;
import top.itangbao.platform.metadata.api.dto.MetadataFieldCreateRequest;
import top.itangbao.platform.metadata.api.dto.MetadataSchemaCreateRequest;
import top.itangbao.platform.metadata.api.dto.MetadataSchemaDTO;
import feign.FeignException;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class TestItemServiceImpl implements TestItemService {

    private final MetadataServiceFeignClient metadataServiceFeignClient;
    private final DataServiceFeignClient dataServiceFeignClient;

    private final Map<String, Long> limsTestItemSchemaIdCache = new ConcurrentHashMap<>();
    private static final String LIMS_TEST_ITEM_SCHEMA_NAME = "LimsTestItem";

    @Autowired
    public TestItemServiceImpl(MetadataServiceFeignClient metadataServiceFeignClient,
                               DataServiceFeignClient dataServiceFeignClient) {
        this.metadataServiceFeignClient = metadataServiceFeignClient;
        this.dataServiceFeignClient = dataServiceFeignClient;
    }

    @Override
    public Long registerLimsTestItemMetadataSchema(String tenantId) {
        return limsTestItemSchemaIdCache.computeIfAbsent(tenantId, k -> {
            try {
                MetadataSchemaDTO existingSchema = metadataServiceFeignClient.getSchemaByNameAndTenantId(LIMS_TEST_ITEM_SCHEMA_NAME, tenantId);
                dataServiceFeignClient.createOrUpdateDynamicTable(tenantId, LIMS_TEST_ITEM_SCHEMA_NAME, existingSchema.getId());
                return existingSchema.getId();
            } catch (FeignException.NotFound e) {
                MetadataSchemaCreateRequest schemaCreateRequest = MetadataSchemaCreateRequest.builder()
                        .name(LIMS_TEST_ITEM_SCHEMA_NAME)
                        .description("LIMS Test Item Information Schema")
                        .tenantId(tenantId)
                        .fields(Arrays.asList(
                                MetadataFieldCreateRequest.builder().fieldName("itemName").fieldType(FieldType.STRING).required(true).description("Test Item Name").build(),
                                MetadataFieldCreateRequest.builder().fieldName("methodReference").fieldType(FieldType.STRING).required(true).description("Method Reference").build(),
                                MetadataFieldCreateRequest.builder().fieldName("price").fieldType(FieldType.NUMBER).required(true).description("Price").build(),
                                MetadataFieldCreateRequest.builder().fieldName("description").fieldType(FieldType.TEXT).required(false).description("Description").build()
                        ))
                        .build();
                MetadataSchemaDTO createdSchema = metadataServiceFeignClient.createSchema(schemaCreateRequest);
                dataServiceFeignClient.createOrUpdateDynamicTable(tenantId, LIMS_TEST_ITEM_SCHEMA_NAME, createdSchema.getId());
                return createdSchema.getId();
            } catch (Exception e) {
                throw new RuntimeException("Failed to register LIMS test item metadata schema: " + e.getMessage(), e);
            }
        });
    }

    @Override
    public TestItemResponse createTestItem(String tenantId, TestItemRequest request) {
        Long schemaId = registerLimsTestItemMetadataSchema(tenantId);

        Map<String, Object> dataFields = new HashMap<>();
        dataFields.put("itemName", request.getItemName());
        dataFields.put("methodReference", request.getMethodReference());
        dataFields.put("price", request.getPrice());
        dataFields.put("description", request.getDescription());
        if (request.getCustomFields() != null) {
            dataFields.putAll(request.getCustomFields());
        }

        DynamicDataRequest dynamicDataRequest = DynamicDataRequest.builder()
                .tenantId(tenantId)
                .schemaName(LIMS_TEST_ITEM_SCHEMA_NAME)
                .data(dataFields)
                .build();

        DynamicDataResponse dynamicResponse = dataServiceFeignClient.insertDynamicData(tenantId, LIMS_TEST_ITEM_SCHEMA_NAME, dynamicDataRequest);

        return convertToTestItemResponse(dynamicResponse);
    }

    @Override
    public TestItemResponse getTestItemById(String tenantId, Long testItemId) {
        registerLimsTestItemMetadataSchema(tenantId);

        try {
            DynamicDataResponse dynamicResponse = dataServiceFeignClient.getDynamicDataById(tenantId, LIMS_TEST_ITEM_SCHEMA_NAME, testItemId);
            return convertToTestItemResponse(dynamicResponse);
        } catch (FeignException.NotFound e) {
            throw new ResourceNotFoundException("Test item not found with ID: " + testItemId + " for tenant: " + tenantId);
        }
    }

    @Override
    public List<TestItemResponse> getAllTestItems(String tenantId) {
        registerLimsTestItemMetadataSchema(tenantId);

        PageRequestDTO pageRequest = PageRequestDTO.builder().page(0).size(Integer.MAX_VALUE).build();
        FilterRequestDTO filterRequest = FilterRequestDTO.builder().filters(Collections.emptyMap()).build();

        PageResponseDTO<DynamicDataResponse> dynamicResponsesPage = dataServiceFeignClient.getAllDynamicData(tenantId, LIMS_TEST_ITEM_SCHEMA_NAME, pageRequest, filterRequest);
        return dynamicResponsesPage.getContent().stream()
                .map(this::convertToTestItemResponse)
                .collect(Collectors.toList());
    }

    @Override
    public TestItemResponse updateTestItem(String tenantId, Long testItemId, TestItemRequest request) {
        registerLimsTestItemMetadataSchema(tenantId);

        Map<String, Object> updates = new HashMap<>();
        if (request.getItemName() != null) updates.put("itemName", request.getItemName());
        if (request.getMethodReference() != null) updates.put("methodReference", request.getMethodReference());
        if (request.getPrice() != null) updates.put("price", request.getPrice());
        if (request.getDescription() != null) updates.put("description", request.getDescription());
        if (request.getCustomFields() != null) updates.putAll(request.getCustomFields());

        DynamicDataResponse dynamicResponse = dataServiceFeignClient.updateDynamicData(tenantId, LIMS_TEST_ITEM_SCHEMA_NAME, testItemId, updates);
        return convertToTestItemResponse(dynamicResponse);
    }

    @Override
    public void deleteTestItem(String tenantId, Long testItemId) {
        registerLimsTestItemMetadataSchema(tenantId);

        try {
            dataServiceFeignClient.deleteDynamicData(tenantId, LIMS_TEST_ITEM_SCHEMA_NAME, testItemId);
        } catch (FeignException.NotFound e) {
            throw new ResourceNotFoundException("Test item not found with ID: " + testItemId + " for tenant: " + tenantId);
        }
    }

    // 辅助方法：将 DynamicDataResponse 转换为 TestItemResponse
    private TestItemResponse convertToTestItemResponse(DynamicDataResponse dynamicResponse) {
        Map<String, Object> data = dynamicResponse.getData();
        return TestItemResponse.builder()
                .id(dynamicResponse.getId())
                .tenantId(dynamicResponse.getTenantId())
                .itemName((String) data.get("itemName"))
                .methodReference((String) data.get("methodReference"))
                .price((Double) data.get("price"))
                .description((String) data.get("description"))
                .createdAt(dynamicResponse.getCreatedAt())
                .updatedAt(dynamicResponse.getUpdatedAt())
                .customFields(new HashMap<>(data))
                .build();
    }
}
