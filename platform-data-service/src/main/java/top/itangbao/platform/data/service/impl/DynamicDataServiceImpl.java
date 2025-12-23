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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import top.itangbao.platform.common.exception.DataValidationException;
import top.itangbao.platform.common.exception.ResourceNotFoundException;
import top.itangbao.platform.data.api.dto.*;
import top.itangbao.platform.data.client.MetadataServiceClient;
import top.itangbao.platform.data.context.UserContext;
import top.itangbao.platform.data.manager.DynamicTableManager;
import top.itangbao.platform.data.service.DynamicDataService;
import top.itangbao.platform.iam.api.client.IamFeignClient;
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

        String currentUser = UserContext.getUsername();
        Long currentDept = UserContext.getDeptId();

        columnNames.add("created_by");
        columnValues.add(currentUser != null ? currentUser : "system");

        columnNames.add("dept_id");
        columnValues.add(currentDept != null ? currentDept : 0L);


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
                .createdBy(currentUser)
                .deptId(currentDept)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Override
    public DynamicDataResponse getDynamicDataById(String tenantId, String schemaName, Long id) {
        MetadataSchemaDTO schemaDTO = metadataServiceClient.getSchemaByNameAndTenantId(schemaName, tenantId)
                .blockOptional()
                .orElseThrow(() -> new ResourceNotFoundException("Metadata schema not found"));

        String tableName = dynamicTableManager.buildTableName(tenantId, schemaName);
        List<String> columnNames = dynamicTableManager.getColumnNames(tableName);

        String selectSql = "SELECT * FROM `" + tableName + "` WHERE id = ?1 AND tenant_id = ?2";
        List<Object[]> resultList = entityManager.createNativeQuery(selectSql)
                .setParameter(1, id)
                .setParameter(2, tenantId)
                .getResultList();

        if (resultList.isEmpty()) {
            throw new ResourceNotFoundException("Dynamic data not found with ID: " + id);
        }

        return mapRowToResponse(resultList.get(0), columnNames, tenantId, schemaName);
    }


    @Override
    public PageResponseDTO<DynamicDataResponse> getAllDynamicData(String tenantId, String schemaName, PageRequestDTO pageRequest, FilterRequestDTO filterRequest) {
        MetadataSchemaDTO schemaDTO = metadataServiceClient.getSchemaByNameAndTenantId(schemaName, tenantId)
                .blockOptional()
                .orElseThrow(() -> new ResourceNotFoundException("Metadata schema not found with name '" + schemaName + "' for tenant '" + tenantId + "'"));

        String tableName = dynamicTableManager.buildTableName(tenantId, schemaName);
        List<String> columnNames = dynamicTableManager.getColumnNames(tableName);

        StringBuilder whereClause = new StringBuilder(" WHERE tenant_id = ? ");
        List<Object> queryParams = new ArrayList<>();
        queryParams.add(tenantId);

        applyDataPermissionFilter(whereClause, queryParams);

        if (filterRequest != null && filterRequest.getFilters() != null && !filterRequest.getFilters().isEmpty()) {
            for (Map.Entry<String, String> entry : filterRequest.getFilters().entrySet()) {
                String filterKeyWithOperator = entry.getKey();
                String filterValue = entry.getValue();

                String[] parts = filterKeyWithOperator.split("\\.");
                String fieldName = parts[0];

                if (!columnNames.contains(fieldName)) {
                    logger.warn("检测到非法字段访问: {}", fieldName);
                    continue;
                }

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
            } else {
                throw new IllegalArgumentException("排序字段非法");
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

        List<DynamicDataResponse> content = resultList.stream()
                .map(row -> mapRowToResponse(row, columnNames, tenantId, schemaName))
                .collect(Collectors.toList());

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


    /**
     * 生产级动态数据校验
     * 收集所有字段的错误，而不是遇到第一个就抛出
     */
    private void validateDynamicData(Map<String, Object> data, MetadataSchemaDTO schemaDTO) {
        // 1. 错误收集容器：FieldName -> ErrorMessage
        Map<String, String> errors = new HashMap<>();

        for (MetadataFieldDTO field : schemaDTO.getFields()) {
            String fieldName = field.getFieldName();
            Object fieldValue = data.get(fieldName);

            // --- A. 必填性校验 ---
            // 无论值是什么类型，先转为字符串判空（注意：Boolean false 不算空，0 不算空）
            boolean isProvided = fieldValue != null && !String.valueOf(fieldValue).trim().isEmpty();

            if (Boolean.TRUE.equals(field.getRequired())) {
                if (!isProvided) {
                    errors.put(fieldName, "此字段为必填项");
                    continue; // 必填缺失，后续校验无意义，跳过当前字段
                }
            }

            // 如果非必填且未提供值，直接跳过后续校验
            if (!isProvided) {
                continue;
            }

            // --- B. 类型格式校验 ---
            try {
                // 尝试转换，如果转换失败会抛出 IllegalArgumentException
                // 我们复用 convertValueToFieldType 的逻辑来做校验
                convertValueToFieldType(fieldValue, field.getFieldType());
            } catch (IllegalArgumentException e) {
                errors.put(fieldName, e.getMessage()); // 使用转换方法返回的具体错误信息
            }

            // --- C. 枚举值校验 (ENUM) ---
            if (FieldType.ENUM.equals(field.getFieldType())) {
                validateEnum(field, fieldValue, errors);
            }

            // --- D. 正则表达式校验 ---
            if (field.getValidationRule() != null && !field.getValidationRule().isEmpty()) {
                try {
                    if (!Pattern.matches(field.getValidationRule(), String.valueOf(fieldValue))) {
                        // 如果有描述，优先提示描述，否则提示不符合规则
                        String msg = (field.getDescription() != null && !field.getDescription().isEmpty())
                                ? "格式不正确: " + field.getDescription()
                                : "数据格式不符合校验规则";
                        errors.put(fieldName, msg);
                    }
                } catch (PatternSyntaxException e) {
                    logger.error("Schema 定义中字段 '{}' 的正则表达式无效: {}", fieldName, field.getValidationRule());
                    // 正则配置错误是开发者的锅，不应阻断用户，或者可以提示系统错误
                }
            }
        }

        // 如果收集到了错误，统一抛出
        if (!errors.isEmpty()) {
            throw new DataValidationException("数据校验失败，请检查输入项", errors);
        }
    }

    /**
     * 独立的枚举校验逻辑，抽取出来保持代码整洁
     */
    private void validateEnum(MetadataFieldDTO field, Object value, Map<String, String> errors) {
        if (field.getOptions() == null || field.getOptions().isEmpty()) {
            return;
        }
        try {
            List<String> options = objectMapper.readValue(field.getOptions(), new TypeReference<List<String>>() {});
            String strValue = String.valueOf(value);
            if (!options.contains(strValue)) {
                errors.put(field.getFieldName(), "值无效，必须是以下选项之一: " + String.join(", ", options));
            }
        } catch (JsonProcessingException e) {
            logger.error("解析字段 '{}' 的枚举选项 JSON 失败: {}", field.getFieldName(), e.getMessage());
            // 配置错误，记录日志，暂不阻断用户（或根据策略阻断）
        }
    }

    /**
     * 将值转换为目标字段类型
     * 既用于 insert/update 的参数准备，也用于 validate 的格式检查
     * * @throws IllegalArgumentException 如果转换失败
     */
    private Object convertValueToFieldType(Object value, FieldType fieldType) {
        if (value == null) {
            return null;
        }
        String strValue = String.valueOf(value).trim(); // 去除首尾空格

        // 空字符串对于非 String 类型视为空值
        if (strValue.isEmpty() && fieldType != FieldType.STRING && fieldType != FieldType.TEXT) {
            return null;
        }

        try {
            switch (fieldType) {
                case INTEGER:
                    // 允许 "123" 或 123
                    return Long.parseLong(strValue);
                case NUMBER:
                    // 允许 "123.45" 或 123.45
                    return Double.parseDouble(strValue);
                case BOOLEAN:
                    // 严格校验：只允许 "true"/"false" (不区分大小写)
                    if ("true".equalsIgnoreCase(strValue)) return true;
                    if ("false".equalsIgnoreCase(strValue)) return false;
                    throw new IllegalArgumentException("必须是布尔值 (true/false)");
                case DATE:
                    // 格式：YYYY-MM-DD
                    return LocalDate.parse(strValue);
                case DATETIME:
                    // 格式：YYYY-MM-DDTHH:MM:SS (ISO_LOCAL_DATE_TIME)
                    // 前端传递时需要注意格式，或者后端这里做更宽容的解析
                    return LocalDateTime.parse(strValue);
                case STRING:
                case TEXT:
                case ENUM: // ENUM 存储为字符串
                case FILE: // 文件路径/ID 存储为字符串
                    return strValue;
                case REFERENCE:
                    // 引用 ID 通常是 Long
                    return Long.parseLong(strValue);
                default:
                    return strValue;
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("必须是有效的数字格式");
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("日期时间格式无效");
        } catch (Exception e) {
            throw new IllegalArgumentException("数据格式无效: " + e.getMessage());
        }
    }


    private DynamicDataResponse mapRowToResponse(Object[] row, List<String> columnNames, String tenantId, String schemaName) {
        Map<String, Object> dataMap = new HashMap<>();
        Long id = null;
        LocalDateTime createdAt = null;
        LocalDateTime updatedAt = null;
        String createdBy = null;
        Long deptId = null;

        for (int i = 0; i < columnNames.size(); i++) {
            String colName = columnNames.get(i);
            Object colValue = row[i];

            if ("id".equals(colName)) id = ((Number) colValue).longValue();
            else if ("tenant_id".equals(colName)) { /* skip */ }
            else if ("created_at".equals(colName)) createdAt = colValue instanceof java.sql.Timestamp ? ((java.sql.Timestamp) colValue).toLocalDateTime() : null;
            else if ("updated_at".equals(colName)) updatedAt = colValue instanceof java.sql.Timestamp ? ((java.sql.Timestamp) colValue).toLocalDateTime() : null;
            else if ("created_by".equals(colName)) createdBy = (String) colValue;
            else if ("dept_id".equals(colName)) deptId = colValue != null ? ((Number) colValue).longValue() : null;
            else dataMap.put(colName, colValue);
        }
        return DynamicDataResponse.builder()
                .id(id)
                .tenantId(tenantId)
                .schemaName(schemaName)
                .data(dataMap)
                .createdBy(createdBy)
                .deptId(deptId)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .build();
    }

    private void applyDataPermissionFilter(StringBuilder sql, List<Object> params) {
        Set<String> scopes = UserContext.getDataScopes();
        String username = UserContext.getUsername();

        // 1. 如果是超级管理员，或者拥有 "ALL" 权限，直接放行 (什么都不加 = 看全部)
        if (scopes.contains("ALL") || "admin".equals(username)) { // 简单判断 admin
            return;
        }

        // 2. 拼接权限 SQL
        List<String> conditions = new ArrayList<>();

        Set<Long> accessibleDeptIds = UserContext.getAccessibleDeptIds();

        if (scopes.contains("DEPT_AND_CHILD") || scopes.contains("DEPT")) {
            if (!accessibleDeptIds.isEmpty()) {
                // 构建 IN 语句: dept_id IN (?, ?, ?)
                String placeholders = accessibleDeptIds.stream()
                        .map(id -> "?")
                        .collect(Collectors.joining(", "));
                conditions.add("dept_id IN (" + placeholders + ")");
                params.addAll(accessibleDeptIds);
            }
        } else if (username != null) {
            conditions.add("created_by = ?");
            params.add(username);
        }

        // 将条件拼接到 SQL
        if (!conditions.isEmpty()) {
            sql.append(" AND (").append(String.join(" OR ", conditions)).append(") ");
        }
    }
}
