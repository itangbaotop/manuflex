package top.itangbao.platform.data.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import top.itangbao.platform.common.exception.DataValidationException;
import top.itangbao.platform.common.exception.ResourceNotFoundException;
import top.itangbao.platform.data.api.dto.*;
import top.itangbao.platform.data.client.MetadataServiceClient;
import top.itangbao.platform.data.manager.DynamicTableManager;
import top.itangbao.platform.data.service.DynamicDataService;
import top.itangbao.platform.metadata.api.dto.MetadataFieldDTO;
import top.itangbao.platform.metadata.api.dto.MetadataSchemaDTO;
import top.itangbao.platform.common.enums.FieldType;

import java.io.*;
import java.math.BigInteger; // 用于处理插入后返回的ID
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

@Service
public class DynamicDataServiceImpl implements DynamicDataService {

    private static final Logger logger = LoggerFactory.getLogger(DynamicDataServiceImpl.class);

    private final DynamicTableManager dynamicTableManager;
    private final MetadataServiceClient metadataServiceClient;
    private final ObjectMapper objectMapper;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    public DynamicDataServiceImpl(DynamicTableManager dynamicTableManager,
                                  MetadataServiceClient metadataServiceClient,
                                  ObjectMapper objectMapper) {
        this.dynamicTableManager = dynamicTableManager;
        this.metadataServiceClient = metadataServiceClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public void createOrUpdateDynamicTable(Long schemaId) {
        MetadataSchemaDTO schemaDTO = metadataServiceClient.getSchemaById(schemaId)
                .blockOptional() // 阻塞等待结果，生产环境考虑异步处理
                .orElseThrow(() -> new ResourceNotFoundException("Metadata schema not found with ID: " + schemaId));
        dynamicTableManager.createTable(schemaDTO);
        dynamicTableManager.updateTable(schemaDTO); // 尝试更新表结构
    }

    @Override
    public void deleteDynamicTable(Long schemaId) {
        MetadataSchemaDTO schemaDTO = metadataServiceClient.getSchemaById(schemaId)
                .blockOptional()
                .orElseThrow(() -> new ResourceNotFoundException("Metadata schema not found with ID: " + schemaId));
        dynamicTableManager.dropTable(schemaDTO.getTenantId(), schemaDTO.getName());
    }

    @Override
    @Transactional
    public DynamicDataResponse insertDynamicData(DynamicDataRequest request) {
        MetadataSchemaDTO schemaDTO = metadataServiceClient.getSchemaByNameAndTenantId(request.getSchemaName(), request.getTenantId())
                .blockOptional()
                .orElseThrow(() -> new ResourceNotFoundException("Metadata schema not found with name '" + request.getSchemaName() + "' for tenant '" + request.getTenantId() + "'"));

        validateDynamicData(request.getData(), schemaDTO);

        String tableName = dynamicTableManager.buildTableName(request.getTenantId(), request.getSchemaName());

        StringBuilder insertSql = new StringBuilder("INSERT INTO `").append(tableName).append("` (");
        StringBuilder valuesSql = new StringBuilder("VALUES (");

        Map<String, Object> data = request.getData();
        List<String> columnNames = new ArrayList<>();
        List<Object> columnValues = new ArrayList<>();

        columnNames.add("tenant_id");
        columnValues.add(request.getTenantId());

        columnNames.add("created_at");
        columnValues.add(LocalDateTime.now());
        columnNames.add("updated_at");
        columnValues.add(LocalDateTime.now());


        for (MetadataFieldDTO field : schemaDTO.getFields()) {
            if (data.containsKey(field.getFieldName())) {
                columnNames.add(field.getFieldName());
                columnValues.add(convertValueToFieldType(data.get(field.getFieldName()), field.getFieldType()));
            }
        }

        insertSql.append(columnNames.stream().map(col -> "`" + col + "`").collect(Collectors.joining(", ")));
        valuesSql.append(columnNames.stream().map(col -> "?").collect(Collectors.joining(", ")));

        insertSql.append(") ").append(valuesSql).append(")");

        logger.debug("Executing INSERT SQL: {}", insertSql);

        Query query = entityManager.createNativeQuery(insertSql.toString());
        for (int i = 0; i < columnValues.size(); i++) {
            query.setParameter(i + 1, columnValues.get(i));
        }

        query.executeUpdate();

        Object lastInsertIdResult = entityManager.createNativeQuery("SELECT LAST_INSERT_ID()").getSingleResult();
        Long id;
        if (lastInsertIdResult instanceof BigInteger) {
            id = ((BigInteger) lastInsertIdResult).longValue();
        } else if (lastInsertIdResult instanceof Long) {
            id = (Long) lastInsertIdResult;
        } else if (lastInsertIdResult instanceof Integer) {
            id = ((Integer) lastInsertIdResult).longValue();
        } else {
            throw new IllegalStateException("Unexpected type for LAST_INSERT_ID(): " + lastInsertIdResult.getClass().getName());
        }


        return DynamicDataResponse.builder()
                .id(id)
                .tenantId(request.getTenantId())
                .schemaName(request.getSchemaName())
                .data(data)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Override
    public DynamicDataResponse getDynamicDataById(String tenantId, String schemaName, Long id) {
        MetadataSchemaDTO schemaDTO = metadataServiceClient.getSchemaByNameAndTenantId(schemaName, tenantId)
                .blockOptional()
                .orElseThrow(() -> new ResourceNotFoundException("Metadata schema not found with name '" + schemaName + "' for tenant '" + tenantId + "'"));

        String tableName = dynamicTableManager.buildTableName(tenantId, schemaName);

        String selectSql = "SELECT * FROM `" + tableName + "` WHERE id = ?1 AND tenant_id = ?2";
        List<Object[]> resultList = entityManager.createNativeQuery(selectSql)
                .setParameter(1, id)
                .setParameter(2, tenantId)
                .getResultList();

        if (resultList.isEmpty()) {
            throw new ResourceNotFoundException("Dynamic data not found with ID: " + id + " in schema '" + schemaName + "' for tenant '" + tenantId + "'");
        }

        Object[] row = resultList.get(0);
        Map<String, Object> dataMap = new HashMap<>();
        LocalDateTime createdAt = null;
        LocalDateTime updatedAt = null;

        List<String> columnNames = getColumnNames(tableName);

        for (int i = 0; i < columnNames.size(); i++) {
            String colName = columnNames.get(i);
            Object colValue = row[i];

            if ("id".equals(colName) && colValue instanceof Number) {
                id = ((Number) colValue).longValue();
            } else if ("tenant_id".equals(colName)) {
                // 忽略这些内部字段，或在DTO中单独处理
            } else if ("created_at".equals(colName) && colValue instanceof java.sql.Timestamp) {
                createdAt = ((java.sql.Timestamp) colValue).toLocalDateTime();
            } else if ("updated_at".equals(colName) && colValue instanceof java.sql.Timestamp) {
                updatedAt = ((java.sql.Timestamp) colValue).toLocalDateTime();
            } else {
                dataMap.put(colName, colValue);
            }
        }

        return DynamicDataResponse.builder()
                .id(id)
                .tenantId(tenantId)
                .schemaName(schemaName)
                .data(dataMap)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .build();
    }


    @Override
    public PageResponseDTO<DynamicDataResponse> getAllDynamicData(String tenantId, String schemaName, PageRequestDTO pageRequest, FilterRequestDTO filterRequest) {
        MetadataSchemaDTO schemaDTO = metadataServiceClient.getSchemaByNameAndTenantId(schemaName, tenantId)
                .blockOptional()
                .orElseThrow(() -> new ResourceNotFoundException("Metadata schema not found with name '" + schemaName + "' for tenant '" + tenantId + "'"));

        String tableName = dynamicTableManager.buildTableName(tenantId, schemaName);
        List<String> columnNames = getColumnNames(tableName);

        StringBuilder whereClause = new StringBuilder(" WHERE tenant_id = ? ");
        List<Object> queryParams = new ArrayList<>();
        queryParams.add(tenantId);

        if (filterRequest != null && filterRequest.getFilters() != null && !filterRequest.getFilters().isEmpty()) {
            for (Map.Entry<String, String> entry : filterRequest.getFilters().entrySet()) {
                String filterKeyWithOperator = entry.getKey();
                String filterValue = entry.getValue();

                String[] parts = filterKeyWithOperator.split("\\.");
                String fieldName = parts[0];
                String operator = parts.length > 1 ? parts[1].toLowerCase() : "eq";

                if (!columnNames.contains(fieldName)) {
                    logger.warn("Filter field '{}' does not exist in table '{}'. Skipping.", fieldName, tableName);
                    continue;
                }

                whereClause.append(" AND `").append(fieldName).append("` ");
                switch (operator) {
                    case "eq":
                        whereClause.append("= ?");
                        queryParams.add(filterValue);
                        break;
                    case "ne":
                        whereClause.append("!= ?");
                        queryParams.add(filterValue);
                        break;
                    case "gt":
                        whereClause.append("> ?");
                        queryParams.add(filterValue);
                        break;
                    case "lt":
                        whereClause.append("< ?");
                        queryParams.add(filterValue);
                        break;
                    case "ge":
                        whereClause.append(">= ?");
                        queryParams.add(filterValue);
                        break;
                    case "le":
                        whereClause.append("<= ?");
                        queryParams.add(filterValue);
                        break;
                    case "like":
                        whereClause.append("LIKE ?");
                        queryParams.add("%" + filterValue + "%");
                        break;
                    case "in":
                        String[] inValues = filterValue.split(",");
                        whereClause.append("IN (");
                        for (int i = 0; i < inValues.length; i++) {
                            whereClause.append("?");
                            if (i < inValues.length - 1) {
                                whereClause.append(",");
                            }
                            queryParams.add(inValues[i]);
                        }
                        whereClause.append(")");
                        break;
                    default:
                        logger.warn("Unsupported filter operator '{}' for field '{}'. Skipping.", operator, fieldName);
                        whereClause.append("= ?");
                        queryParams.add(filterValue);
                        break;
                }
            }
        }

        StringBuilder orderByClause = new StringBuilder();
        if (pageRequest != null && pageRequest.getSortBy() != null && !pageRequest.getSortBy().isEmpty()) {
            if (columnNames.contains(pageRequest.getSortBy())) {
                orderByClause.append(" ORDER BY `").append(pageRequest.getSortBy()).append("` ");
                if ("desc".equalsIgnoreCase(pageRequest.getSortOrder())) {
                    orderByClause.append("DESC");
                } else {
                    orderByClause.append("ASC");
                }
            }
        } else {
            orderByClause.append(" ORDER BY `id` ASC");
        }

        String countSql = "SELECT COUNT(*) FROM `" + tableName + "`" + whereClause.toString();
        Query countQuery = entityManager.createNativeQuery(countSql);
        for (int i = 0; i < queryParams.size(); i++) {
            countQuery.setParameter(i + 1, queryParams.get(i));
        }
        long totalElements = ((Number) countQuery.getSingleResult()).longValue();

        StringBuilder selectSql = new StringBuilder("SELECT * FROM `" + tableName + "`")
                .append(whereClause)
                .append(orderByClause);

        if (pageRequest != null) {
            selectSql.append(" LIMIT ? OFFSET ?");
        }

        Query dataQuery = entityManager.createNativeQuery(selectSql.toString());
        int paramIndex = 1;
        for (Object param : queryParams) {
            dataQuery.setParameter(paramIndex++, param);
        }

        if (pageRequest != null) {
            dataQuery.setParameter(paramIndex++, pageRequest.getSize());
            dataQuery.setParameter(paramIndex++, (long) pageRequest.getPage() * pageRequest.getSize());
        }

        List<Object[]> resultList = dataQuery.getResultList();

        List<DynamicDataResponse> content = resultList.stream().map(row -> {
            Map<String, Object> dataMap = new HashMap<>();
            Long id = null;
            LocalDateTime createdAt = null;
            LocalDateTime updatedAt = null;

            for (int i = 0; i < columnNames.size(); i++) {
                String colName = columnNames.get(i);
                Object colValue = row[i];

                if ("id".equals(colName) && colValue instanceof Number) {
                    id = ((Number) colValue).longValue();
                } else if ("tenant_id".equals(colName)) {
                    // 忽略
                } else if ("created_at".equals(colName) && colValue instanceof java.sql.Timestamp) {
                    createdAt = ((java.sql.Timestamp) colValue).toLocalDateTime();
                } else if ("updated_at".equals(colName) && colValue instanceof java.sql.Timestamp) {
                    updatedAt = ((java.sql.Timestamp) colValue).toLocalDateTime();
                } else {
                    dataMap.put(colName, colValue);
                }
            }
            return DynamicDataResponse.builder()
                    .id(id)
                    .tenantId(tenantId)
                    .schemaName(schemaName)
                    .data(dataMap)
                    .createdAt(createdAt)
                    .updatedAt(updatedAt)
                    .build();
        }).collect(Collectors.toList());

        int totalPages = (int) Math.ceil((double) totalElements / (pageRequest != null ? pageRequest.getSize() : 1));

        return PageResponseDTO.<DynamicDataResponse>builder()
                .content(content)
                .page(pageRequest != null ? pageRequest.getPage() : 0)
                .size(pageRequest != null ? pageRequest.getSize() : content.size())
                .totalElements(totalElements)
                .totalPages(totalPages)
                .first(pageRequest != null && pageRequest.getPage() == 0)
                .last(pageRequest != null && pageRequest.getPage() == totalPages - 1)
                .build();
    }

    @Override
    @Transactional
    public DynamicDataResponse updateDynamicData(String tenantId, String schemaName, Long id, Map<String, Object> updates) {
        MetadataSchemaDTO schemaDTO = metadataServiceClient.getSchemaByNameAndTenantId(schemaName, tenantId)
                .blockOptional()
                .orElseThrow(() -> new ResourceNotFoundException("Metadata schema not found with name '" + schemaName + "' for tenant '" + tenantId + "'"));

        validateDynamicData(updates, schemaDTO); // 使用 updates 进行校验

        String tableName = dynamicTableManager.buildTableName(tenantId, schemaName);

        String checkSql = "SELECT COUNT(*) FROM `" + tableName + "` WHERE id = ?1 AND tenant_id = ?2";
        Number count = (Number) entityManager.createNativeQuery(checkSql)
                .setParameter(1, id)
                .setParameter(2, tenantId)
                .getSingleResult();
        if (count.intValue() == 0) {
            throw new ResourceNotFoundException("Dynamic data not found with ID: " + id + " in schema '" + schemaName + "' for tenant '" + tenantId + "'");
        }

        StringBuilder updateSql = new StringBuilder("UPDATE `").append(tableName).append("` SET ");
        List<String> setClauses = new ArrayList<>();
        List<Object> params = new ArrayList<>();

        setClauses.add("`updated_at` = ?");
        params.add(LocalDateTime.now());

        for (Map.Entry<String, Object> entry : updates.entrySet()) {
            String fieldName = entry.getKey();
            Object fieldValue = entry.getValue();

            // 确保更新的字段在 schema 中存在
            MetadataFieldDTO field = schemaDTO.getFields().stream()
                    .filter(f -> f.getFieldName().equals(fieldName))
                    .findFirst()
                    .orElse(null);

            if (field == null) {
                logger.warn("Attempted to update non-existent field '{}' in schema '{}'. Skipping.", fieldName, schemaName);
                continue;
            }

            setClauses.add("`" + fieldName + "` = ?");
            params.add(convertValueToFieldType(fieldValue, field.getFieldType()));
        }

        if (setClauses.isEmpty()) {
            throw new IllegalArgumentException("No fields provided for update.");
        }

        updateSql.append(String.join(", ", setClauses));
        updateSql.append(" WHERE `id` = ? AND `tenant_id` = ?");

        Query query = entityManager.createNativeQuery(updateSql.toString());
        for (int i = 0; i < params.size(); i++) {
            query.setParameter(i + 1, params.get(i));
        }
        query.setParameter(params.size() + 1, id);
        query.setParameter(params.size() + 2, tenantId);

        query.executeUpdate();

        return getDynamicDataById(tenantId, schemaName, id);
    }

    @Override
    @Transactional
    public void deleteDynamicData(String tenantId, String schemaName, Long id) {
        String tableName = dynamicTableManager.buildTableName(tenantId, schemaName);

        String deleteSql = "DELETE FROM `" + tableName + "` WHERE id = ?1 AND tenant_id = ?2";
        int deletedCount = entityManager.createNativeQuery(deleteSql)
                .setParameter(1, id)
                .setParameter(2, tenantId)
                .executeUpdate();

        if (deletedCount == 0) {
            throw new ResourceNotFoundException("Dynamic data not found with ID: " + id + " in schema '" + schemaName + "' for tenant '" + tenantId + "'");
        }
    }


    @Override
    @Transactional
    public DataImportResponse importData(String tenantId, String schemaName, MultipartFile file) throws IOException {
        MetadataSchemaDTO schemaDTO = metadataServiceClient.getSchemaByNameAndTenantId(schemaName, tenantId)
                .blockOptional()
                .orElseThrow(() -> new ResourceNotFoundException("Metadata schema not found with name '" + schemaName + "' for tenant '" + tenantId + "'"));

        int totalRecords = 0;
        int successCount = 0;
        int failedCount = 0;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                return DataImportResponse.builder().totalRecords(0).successCount(0).failedCount(0).message("Empty file").build();
            }
            List<String> headers = Arrays.asList(headerLine.split(","));

            String line;
            while ((line = reader.readLine()) != null) {
                totalRecords++;
                Map<String, Object> recordData = new HashMap<>();
                List<String> values = Arrays.asList(line.split(","));

                if (headers.size() != values.size()) {
                    failedCount++;
                    logger.error("Failed to import record: Column count mismatch for line: {}", line);
                    continue;
                }

                for (int i = 0; i < headers.size(); i++) {
                    recordData.put(headers.get(i), values.get(i));
                }

                try {
                    validateDynamicData(recordData, schemaDTO);

                    DynamicDataRequest request = DynamicDataRequest.builder()
                            .tenantId(tenantId)
                            .schemaName(schemaName)
                            .data(recordData)
                            .build();
                    insertDynamicData(request); // insertDynamicData 内部会再次校验，这里可以考虑优化，避免重复校验
                    successCount++;
                } catch (Exception e) {
                    failedCount++;
                    logger.error("Failed to import record: {} - {}", recordData, e.getMessage());
                }
            }
        }

        return DataImportResponse.builder()
                .totalRecords(totalRecords)
                .successCount(successCount)
                .failedCount(failedCount)
                .message(failedCount == 0 ? "Data imported successfully." : "Data imported with some failures.")
                .build();
    }

    @Override
    public byte[] exportData(String tenantId, String schemaName, FilterRequestDTO filterRequest, String format) throws IOException {
        MetadataSchemaDTO schemaDTO = metadataServiceClient.getSchemaByNameAndTenantId(schemaName, tenantId)
                .blockOptional()
                .orElseThrow(() -> new ResourceNotFoundException("Metadata schema not found with name '" + schemaName + "' for tenant '" + tenantId + "'"));

        PageRequestDTO pageRequest = PageRequestDTO.builder().page(0).size(Integer.MAX_VALUE).build();
        PageResponseDTO<DynamicDataResponse> dataPage = getAllDynamicData(tenantId, schemaName, pageRequest, filterRequest);
        List<DynamicDataResponse> records = dataPage.getContent();

        if ("csv".equalsIgnoreCase(format)) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            try (PrintWriter writer = new PrintWriter(bos)) {

                List<String> finalHeaderNames = new ArrayList<>();
                finalHeaderNames.add("id");
                finalHeaderNames.add("tenant_id");
                finalHeaderNames.add("created_at");
                finalHeaderNames.add("updated_at");
                schemaDTO.getFields().stream()
                        .map(MetadataFieldDTO::getFieldName)
                        .forEach(finalHeaderNames::add);

                writer.println(String.join(",", finalHeaderNames));

                for (DynamicDataResponse record : records) {
                    List<String> values = new ArrayList<>();
                    Map<String, Object> recordData = record.getData();

                    for (String header : finalHeaderNames) {
                        switch (header) {
                            case "id":
                                values.add(String.valueOf(record.getId()));
                                break;
                            case "tenant_id":
                                values.add(record.getTenantId());
                                break;
                            case "created_at":
                                values.add(record.getCreatedAt() != null ? record.getCreatedAt().toString() : "");
                                break;
                            case "updated_at":
                                values.add(record.getUpdatedAt() != null ? record.getUpdatedAt().toString() : "");
                                break;
                            default:
                                Object value = recordData.get(header);
                                values.add(value != null ? String.valueOf(value) : "");
                                break;
                        }
                    }
                    writer.println(String.join(",", values));
                }
            }
            return bos.toByteArray();
        } else if ("excel".equalsIgnoreCase(format)) {
            throw new UnsupportedOperationException("Excel export is not yet supported.");
        } else {
            throw new IllegalArgumentException("Unsupported export format: " + format);
        }
    }

    // 辅助方法：获取表的列名
    private List<String> getColumnNames(String tableName) {
        try {
            List<?> results = entityManager.createNativeQuery(
                            "SELECT column_name FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = ?1 ORDER BY ORDINAL_POSITION")
                    .setParameter(1, tableName)
                    .getResultList();

            return results.stream()
                    .map(item -> {
                        if (item instanceof String) {
                            return (String) item;
                        } else if (item instanceof Object[]) {
                            return (String) ((Object[]) item)[0];
                        } else {
                            logger.warn("Unexpected column name type: {}", item.getClass().getName());
                            return item.toString();
                        }
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Error getting column names for table '{}': {}", tableName, e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 动态数据校验方法
     * 根据 MetadataSchemaDTO 中的字段定义对数据进行校验
     * @param data 要校验的数据
     * @param schemaDTO 模式定义
     * @throws DataValidationException 如果校验失败
     */
    private void validateDynamicData(Map<String, Object> data, MetadataSchemaDTO schemaDTO) {
        for (MetadataFieldDTO field : schemaDTO.getFields()) {
            String fieldName = field.getFieldName();
            Object fieldValue = data.get(fieldName);

            // 1. 必填字段校验
            if (field.getRequired() != null && field.getRequired()) {
                if (fieldValue == null || String.valueOf(fieldValue).trim().isEmpty()) {
                    throw new DataValidationException("Field '" + fieldName + "' is required.");
                }
            }

            // 如果字段值为空且不是必填，则跳过后续校验
            if (fieldValue == null || String.valueOf(fieldValue).trim().isEmpty()) {
                continue;
            }

            // 2. 类型校验和转换
            try {
                switch (field.getFieldType()) {
                    case INTEGER:
                        if (!(fieldValue instanceof Integer || fieldValue instanceof Long)) {
                            // 尝试从 String 转换
                            try {
                                Long.parseLong(String.valueOf(fieldValue));
                            } catch (NumberFormatException e) {
                                throw new DataValidationException("Field '" + fieldName + "' must be an integer.");
                            }
                        }
                        break;
                    case NUMBER:
                        if (!(fieldValue instanceof Double || fieldValue instanceof Float || fieldValue instanceof Integer || fieldValue instanceof Long)) {
                            // 尝试从 String 转换
                            try {
                                Double.parseDouble(String.valueOf(fieldValue));
                            } catch (NumberFormatException e) {
                                throw new DataValidationException("Field '" + fieldName + "' must be a number.");
                            }
                        }
                        break;
                    case BOOLEAN:
                        if (!(fieldValue instanceof Boolean)) {
                            // 尝试从 String 转换
                            String strValue = String.valueOf(fieldValue).toLowerCase();
                            if (!("true".equals(strValue) || "false".equals(strValue))) {
                                throw new DataValidationException("Field '" + fieldName + "' must be a boolean (true/false).");
                            }
                        }
                        break;
                    case DATE:
                        // 假设日期格式为 "YYYY-MM-DD"
                        try {
                            LocalDate.parse(String.valueOf(fieldValue));
                        } catch (DateTimeParseException e) {
                            throw new DataValidationException("Field '" + fieldName + "' must be a valid date (YYYY-MM-DD).");
                        }
                        break;
                    case DATETIME:
                        // 假设日期时间格式为 "YYYY-MM-DDTHH:MM:SS"
                        try {
                            LocalDateTime.parse(String.valueOf(fieldValue));
                        } catch (DateTimeParseException e) {
                            throw new DataValidationException("Field '" + fieldName + "' must be a valid datetime (YYYY-MM-DDTHH:MM:SS).");
                        }
                        break;
                    case ENUM:
                        if (field.getOptions() != null && !field.getOptions().isEmpty()) {
                            try {
                                // 使用 ObjectMapper 解析 JSON 数组字符串
                                List<String> options = objectMapper.readValue(field.getOptions(), new TypeReference<List<String>>() {});
                                if (!options.contains(String.valueOf(fieldValue))) {
                                    throw new DataValidationException("Field '" + fieldName + "' must be one of " + field.getOptions() + ".");
                                }
                            } catch (JsonProcessingException e) {
                                logger.warn("Error parsing enum options JSON for field '{}': {}", fieldName, e.getMessage());
                                throw new DataValidationException("Field '" + fieldName + "' has invalid enum options format or is not a valid JSON array string.");
                            } catch (Exception e) {
                                logger.warn("Error processing enum options for field '{}': {}", fieldName, e.getMessage());
                                throw new DataValidationException("Field '" + fieldName + "' has an issue with enum options processing.");
                            }
                        }
                        break;
                    case STRING:
                    case TEXT:
                    case FILE:
                    case REFERENCE:
                        // 对于这些类型，主要进行格式或长度校验，这里先不做复杂校验
                        break;
                }
            } catch (DataValidationException e) {
                throw e; // 重新抛出自定义校验异常
            } catch (Exception e) {
                // 捕获其他类型转换异常
                throw new DataValidationException("Field '" + fieldName + "' has an invalid value or format: " + e.getMessage());
            }

            // 3. 正则表达式校验 (如果定义了 validationRule)
            if (field.getValidationRule() != null && !field.getValidationRule().isEmpty()) {
                try {
                    if (!Pattern.matches(field.getValidationRule(), String.valueOf(fieldValue))) {
                        throw new DataValidationException("Field '" + fieldName + "' does not match validation rule: " + field.getValidationRule());
                    }
                } catch (PatternSyntaxException e) {
                    logger.error("Invalid regex pattern for field '{}': {}", fieldName, field.getValidationRule());
                    // 即使正则表达式本身有问题，也不应该阻止流程，但应该记录
                }
            }
        }
    }

    /**
     * 将值转换为目标字段类型 (用于插入/更新 SQL 参数)
     * @param value 原始值
     * @param fieldType 目标字段类型
     * @return 转换后的值
     */
    private Object convertValueToFieldType(Object value, FieldType fieldType) {
        if (value == null) {
            return null;
        }
        String strValue = String.valueOf(value);
        try {
            switch (fieldType) {
                case INTEGER:
                    return Long.parseLong(strValue);
                case NUMBER:
                    return Double.parseDouble(strValue);
                case BOOLEAN:
                    return Boolean.parseBoolean(strValue);
                case DATE:
                    return LocalDate.parse(strValue);
                case DATETIME:
                    return LocalDateTime.parse(strValue);
                case STRING:
                case TEXT:
                case ENUM:
                case FILE:
                case REFERENCE:
                default:
                    return value; // 其他类型直接返回原始值
            }
        } catch (NumberFormatException | DateTimeParseException e) {
            logger.warn("Value '{}' cannot be converted to type {}. Keeping original type.", strValue, fieldType);
            return value; // 转换失败时返回原始值，后续可能在数据库层面报错
        }
    }
}
