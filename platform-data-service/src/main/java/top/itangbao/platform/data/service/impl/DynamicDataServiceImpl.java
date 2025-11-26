package top.itangbao.platform.data.service.impl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.itangbao.platform.common.exception.ResourceNotFoundException;
import top.itangbao.platform.data.api.dto.*;
import top.itangbao.platform.data.client.MetadataServiceClient;
import top.itangbao.platform.data.manager.DynamicTableManager;
import top.itangbao.platform.data.service.DynamicDataService;
import top.itangbao.platform.metadata.api.dto.MetadataFieldDTO;
import top.itangbao.platform.metadata.api.dto.MetadataSchemaDTO;

import java.math.BigInteger; // 用于处理插入后返回的ID
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DynamicDataServiceImpl implements DynamicDataService {

    private static final Logger logger = LoggerFactory.getLogger(DynamicDataServiceImpl.class);

    private final DynamicTableManager dynamicTableManager;
    private final MetadataServiceClient metadataServiceClient;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    public DynamicDataServiceImpl(DynamicTableManager dynamicTableManager,
                                  MetadataServiceClient metadataServiceClient) {
        this.dynamicTableManager = dynamicTableManager;
        this.metadataServiceClient = metadataServiceClient;
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

        String tableName = dynamicTableManager.buildTableName(request.getTenantId(), request.getSchemaName());

        // 构建 INSERT 语句
        StringBuilder insertSql = new StringBuilder("INSERT INTO `").append(tableName).append("` (");
        StringBuilder valuesSql = new StringBuilder("VALUES (");

        Map<String, Object> data = request.getData();
        List<String> columnNames = new ArrayList<>();
        List<Object> columnValues = new ArrayList<>();

        // 添加租户ID
        columnNames.add("tenant_id");
        columnValues.add(request.getTenantId());

        // 添加创建时间和更新时间
        columnNames.add("created_at");
        columnValues.add(LocalDateTime.now());
        columnNames.add("updated_at");
        columnValues.add(LocalDateTime.now());


        for (MetadataFieldDTO field : schemaDTO.getFields()) {
            if (data.containsKey(field.getFieldName())) {
                columnNames.add(field.getFieldName());
                columnValues.add(data.get(field.getFieldName()));
            } else if (field.getRequired() && field.getDefaultValue() == null) {
                // 检查必填字段是否缺失
                throw new IllegalArgumentException("Required field '" + field.getFieldName() + "' is missing.");
            }
            // TODO: 这里可以添加更复杂的校验逻辑，根据 field.getValidationRule()
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

        // 获取插入后的自增ID (MySQL 特定)
        // 灵活处理 LAST_INSERT_ID() 的返回类型
        Object lastInsertIdResult = entityManager.createNativeQuery("SELECT LAST_INSERT_ID()").getSingleResult();
        Long id;
        if (lastInsertIdResult instanceof BigInteger) {
            id = ((BigInteger) lastInsertIdResult).longValue();
        } else if (lastInsertIdResult instanceof Long) {
            id = (Long) lastInsertIdResult;
        } else if (lastInsertIdResult instanceof Integer) {
            id = ((Integer) lastInsertIdResult).longValue();
        } else {
            // 如果是其他类型，可以尝试转换为 String 再解析，或者抛出异常
            throw new IllegalStateException("Unexpected type for LAST_INSERT_ID(): " + lastInsertIdResult.getClass().getName());
        }


        return DynamicDataResponse.builder()
                .tenantId(request.getTenantId())
                .id(id)
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

        // 获取列名
        List<String> columnNames = getColumnNames(tableName);

        for (int i = 0; i < columnNames.size(); i++) {
            String colName = columnNames.get(i);
            Object colValue = row[i];

            if ("id".equals(colName) || "tenant_id".equals(colName)) {
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
        queryParams.add(tenantId); // 第一个参数是 tenant_id

        if (filterRequest != null && filterRequest.getFilters() != null && !filterRequest.getFilters().isEmpty()) {
            // 遍历所有过滤器
            for (Map.Entry<String, String> entry : filterRequest.getFilters().entrySet()) {
                String filterKeyWithOperator = entry.getKey(); // 例如 "fieldName.gt"
                String filterValue = entry.getValue();

                String[] parts = filterKeyWithOperator.split("\\.");
                String fieldName = parts[0];
                String operator = parts.length > 1 ? parts[1].toLowerCase() : "eq"; // 默认等于 "eq"

                if (!columnNames.contains(fieldName)) {
                    logger.warn("Filter field '{}' does not exist in table '{}'. Skipping.", fieldName, tableName);
                    continue; // 字段不存在，跳过
                }

                whereClause.append(" AND `").append(fieldName).append("` ");
                switch (operator) {
                    case "eq": // 等于
                        whereClause.append("= ?");
                        queryParams.add(filterValue);
                        break;
                    case "ne": // 不等于
                        whereClause.append("!= ?");
                        queryParams.add(filterValue);
                        break;
                    case "gt": // 大于
                        whereClause.append("> ?");
                        queryParams.add(filterValue);
                        break;
                    case "lt": // 小于
                        whereClause.append("< ?");
                        queryParams.add(filterValue);
                        break;
                    case "ge": // 大于等于
                        whereClause.append(">= ?");
                        queryParams.add(filterValue);
                        break;
                    case "le": // 小于等于
                        whereClause.append("<= ?");
                        queryParams.add(filterValue);
                        break;
                    case "like": // 模糊匹配
                        whereClause.append("LIKE ?");
                        queryParams.add("%" + filterValue + "%"); // 添加 % 进行模糊匹配
                        break;
                    case "in": // 包含 (逗号分隔的值)
                        // 假设 filterValue 是逗号分隔的字符串，例如 "val1,val2,val3"
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
                        // 默认等于
                        whereClause.append("= ?");
                        queryParams.add(filterValue);
                        break;
                }
            }
        }

        // 2. 构建排序子句
        StringBuilder orderByClause = new StringBuilder();
        if (pageRequest != null && pageRequest.getSortBy() != null && !pageRequest.getSortBy().isEmpty()) {
            if (columnNames.contains(pageRequest.getSortBy())) { // 确保排序字段存在
                orderByClause.append(" ORDER BY `").append(pageRequest.getSortBy()).append("` ");
                if ("desc".equalsIgnoreCase(pageRequest.getSortOrder())) {
                    orderByClause.append("DESC");
                } else {
                    orderByClause.append("ASC");
                }
            }
        } else {
            orderByClause.append(" ORDER BY `id` ASC"); // 默认按 ID 排序
        }

        // 3. 查询总记录数
        String countSql = "SELECT COUNT(*) FROM `" + tableName + "`" + whereClause.toString();
        Query countQuery = entityManager.createNativeQuery(countSql);
        for (int i = 0; i < queryParams.size(); i++) {
            countQuery.setParameter(i + 1, queryParams.get(i));
        }
        long totalElements = ((Number) countQuery.getSingleResult()).longValue();

        // 4. 构建分页查询
        StringBuilder selectSql = new StringBuilder("SELECT * FROM `").append(tableName).append("`")
                .append(whereClause)
                .append(orderByClause);

        if (pageRequest != null) {
            selectSql.append(" LIMIT ? OFFSET ?"); // LIMIT size OFFSET (page * size)
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

        String tableName = dynamicTableManager.buildTableName(tenantId, schemaName);

        // 检查数据是否存在
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

        // 添加更新时间
        setClauses.add("`updated_at` = ?");
        params.add(LocalDateTime.now());

        for (Map.Entry<String, Object> entry : updates.entrySet()) {
            // 确保更新的字段在 schema 中存在 (可选，但推荐进行校验)
            boolean fieldExistsInSchema = schemaDTO.getFields().stream()
                    .anyMatch(field -> field.getFieldName().equals(entry.getKey()));
            if (!fieldExistsInSchema) {
                logger.warn("Attempted to update non-existent field '{}' in schema '{}'. Skipping.", entry.getKey(), schemaName);
                continue;
            }

            setClauses.add("`" + entry.getKey() + "` = ?");
            params.add(entry.getValue());
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

        // 返回更新后的数据
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

    // 辅助方法：获取表的列名
    private List<String> getColumnNames(String tableName) {
        try {
            List<?> results = entityManager.createNativeQuery( // 使用 List<?>
                            "SELECT column_name FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = ?1 ORDER BY ORDINAL_POSITION")
                    .setParameter(1, tableName)
                    .getResultList();

            return results.stream()
                    .map(item -> {
                        if (item instanceof String) {
                            return (String) item; // 如果是 String 类型，直接返回
                        } else if (item instanceof Object[]) {
                            // 如果是 Object[] (多列查询，但这里只有一列，所以理论上不会发生)
                            return (String) ((Object[]) item)[0];
                        } else {
                            // 处理其他意外类型
                            logger.warn("Unexpected column name type: {}", item.getClass().getName());
                            return item.toString(); // 尝试转换为字符串
                        }
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Error getting column names for table '{}': {}", tableName, e.getMessage());
            return Collections.emptyList();
        }
    }
}
