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
import top.itangbao.platform.lims.dto.TestResultRequest;
import top.itangbao.platform.lims.dto.TestResultResponse;
import top.itangbao.platform.lims.service.TestResultService;
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
public class TestResultServiceImpl implements TestResultService {

    private final MetadataServiceFeignClient metadataServiceFeignClient;
    private final DataServiceFeignClient dataServiceFeignClient;

    private final Map<String, Long> limsTestResultSchemaIdCache = new ConcurrentHashMap<>();
    private static final String LIMS_TEST_RESULT_SCHEMA_NAME = "LimsTestResult";

    @Autowired
    public TestResultServiceImpl(MetadataServiceFeignClient metadataServiceFeignClient,
                                 DataServiceFeignClient dataServiceFeignClient) {
        this.metadataServiceFeignClient = metadataServiceFeignClient;
        this.dataServiceFeignClient = dataServiceFeignClient;
    }

    @Override
    public Long registerLimsTestResultMetadataSchema(String tenantId) {
        return limsTestResultSchemaIdCache.computeIfAbsent(tenantId, k -> {
            try {
                MetadataSchemaDTO existingSchema = metadataServiceFeignClient.getSchemaByNameAndTenantId(LIMS_TEST_RESULT_SCHEMA_NAME, tenantId);
                dataServiceFeignClient.createOrUpdateDynamicTable(tenantId, LIMS_TEST_RESULT_SCHEMA_NAME, existingSchema.getId());
                return existingSchema.getId();
            } catch (FeignException.NotFound e) {
                MetadataSchemaCreateRequest schemaCreateRequest = MetadataSchemaCreateRequest.builder()
                        .name(LIMS_TEST_RESULT_SCHEMA_NAME)
                        .description("LIMS Test Result Information Schema")
                        .tenantId(tenantId)
                        .fields(Arrays.asList(
                                MetadataFieldCreateRequest.builder().fieldName("sampleId").fieldType(FieldType.INTEGER).required(true).description("Related Sample ID").build(),
                                MetadataFieldCreateRequest.builder().fieldName("testItemId").fieldType(FieldType.INTEGER).required(true).description("Related Test Item ID").build(),
                                MetadataFieldCreateRequest.builder().fieldName("resultValue").fieldType(FieldType.STRING).required(true).description("Test Result Value").build(),
                                MetadataFieldCreateRequest.builder().fieldName("unit").fieldType(FieldType.STRING).required(false).description("Result Unit").build(),
                                MetadataFieldCreateRequest.builder().fieldName("status").fieldType(FieldType.ENUM).options("[\"PENDING\", \"APPROVED\", \"REJECTED\"]").required(true).defaultValue("PENDING").description("Result Status").build()
                        ))
                        .build();
                MetadataSchemaDTO createdSchema = metadataServiceFeignClient.createSchema(schemaCreateRequest);
                dataServiceFeignClient.createOrUpdateDynamicTable(tenantId, LIMS_TEST_RESULT_SCHEMA_NAME, createdSchema.getId());
                return createdSchema.getId();
            } catch (Exception e) {
                throw new RuntimeException("Failed to register LIMS test result metadata schema: " + e.getMessage(), e);
            }
        });
    }

    @Override
    public TestResultResponse createTestResult(String tenantId, TestResultRequest request) {
        Long schemaId = registerLimsTestResultMetadataSchema(tenantId);

        Map<String, Object> dataFields = new HashMap<>();
        dataFields.put("sampleId", request.getSampleId());
        dataFields.put("testItemId", request.getTestItemId());
        dataFields.put("resultValue", request.getResultValue());
        dataFields.put("unit", request.getUnit());
        dataFields.put("status", request.getStatus());
        if (request.getCustomFields() != null) {
            dataFields.putAll(request.getCustomFields());
        }

        DynamicDataRequest dynamicDataRequest = DynamicDataRequest.builder()
                .tenantId(tenantId)
                .schemaName(LIMS_TEST_RESULT_SCHEMA_NAME)
                .data(dataFields)
                .build();

        DynamicDataResponse dynamicResponse = dataServiceFeignClient.insertDynamicData(tenantId, LIMS_TEST_RESULT_SCHEMA_NAME, dynamicDataRequest);

        return convertToTestResultResponse(dynamicResponse);
    }

    @Override
    public TestResultResponse getTestResultById(String tenantId, Long testResultId) {
        registerLimsTestResultMetadataSchema(tenantId);

        try {
            DynamicDataResponse dynamicResponse = dataServiceFeignClient.getDynamicDataById(tenantId, LIMS_TEST_RESULT_SCHEMA_NAME, testResultId);
            return convertToTestResultResponse(dynamicResponse);
        } catch (FeignException.NotFound e) {
            throw new ResourceNotFoundException("Test result not found with ID: " + testResultId + " for tenant: " + tenantId);
        }
    }

    @Override
    public List<TestResultResponse> getAllTestResults(String tenantId) {
        registerLimsTestResultMetadataSchema(tenantId);

        PageRequestDTO pageRequest = PageRequestDTO.builder().page(0).size(Integer.MAX_VALUE).build();
        FilterRequestDTO filterRequest = FilterRequestDTO.builder().filters(Collections.emptyMap()).build();

        PageResponseDTO<DynamicDataResponse> dynamicResponsesPage = dataServiceFeignClient.getAllDynamicData(tenantId, LIMS_TEST_RESULT_SCHEMA_NAME, pageRequest, filterRequest);
        return dynamicResponsesPage.getContent().stream()
                .map(this::convertToTestResultResponse)
                .collect(Collectors.toList());
    }

    @Override
    public TestResultResponse updateTestResult(String tenantId, Long testResultId, TestResultRequest request) {
        registerLimsTestResultMetadataSchema(tenantId);

        Map<String, Object> updates = new HashMap<>();
        if (request.getSampleId() != null) updates.put("sampleId", request.getSampleId());
        if (request.getTestItemId() != null) updates.put("testItemId", request.getTestItemId());
        if (request.getResultValue() != null) updates.put("resultValue", request.getResultValue());
        if (request.getUnit() != null) updates.put("unit", request.getUnit());
        if (request.getStatus() != null) updates.put("status", request.getStatus());
        if (request.getCustomFields() != null) updates.putAll(request.getCustomFields());

        DynamicDataResponse dynamicResponse = dataServiceFeignClient.updateDynamicData(tenantId, LIMS_TEST_RESULT_SCHEMA_NAME, testResultId, updates);
        return convertToTestResultResponse(dynamicResponse);
    }

    @Override
    public void deleteTestResult(String tenantId, Long testResultId) {
        registerLimsTestResultMetadataSchema(tenantId);

        try {
            dataServiceFeignClient.deleteDynamicData(tenantId, LIMS_TEST_RESULT_SCHEMA_NAME, testResultId);
        } catch (FeignException.NotFound e) {
            throw new ResourceNotFoundException("Test result not found with ID: " + testResultId + " for tenant: " + tenantId);
        }
    }

    // 辅助方法：将 DynamicDataResponse 转换为 TestResultResponse
    private TestResultResponse convertToTestResultResponse(DynamicDataResponse dynamicResponse) {
        Map<String, Object> data = dynamicResponse.getData();
        return TestResultResponse.builder()
                .id(dynamicResponse.getId())
                .tenantId(dynamicResponse.getTenantId())
                .sampleId((Long) data.get("sampleId"))
                .testItemId((Long) data.get("testItemId"))
                .resultValue((String) data.get("resultValue"))
                .unit((String) data.get("unit"))
                .status((String) data.get("status"))
                .createdAt(dynamicResponse.getCreatedAt())
                .updatedAt(dynamicResponse.getUpdatedAt())
                .customFields(new HashMap<>(data))
                .build();
    }
}
