package top.itangbao.platform.data.manager;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import top.itangbao.platform.metadata.api.dto.MetadataFieldDTO;
import top.itangbao.platform.metadata.api.dto.MetadataSchemaDTO;


import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Component
public class DynamicTableManager {

    private static final Logger logger = LoggerFactory.getLogger(DynamicTableManager.class);

    @PersistenceContext
    private EntityManager entityManager;

    private Cache<String, List<String>> columnCache;

    @PostConstruct
    public void init() {
        // 初始化缓存：最多 1000 个表的列信息，写入 1 小时后过期
        this.columnCache = Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(1, TimeUnit.HOURS)
                .build();
    }

    public List<String> getColumnNames(String tableName) {
        // 使用缓存，如果没命中则执行原有的 DB 查询逻辑
        return columnCache.get(tableName, key -> queryActualColumnsFromDb(key));
    }

    private List<String> queryActualColumnsFromDb(String tableName) {
        try {
            List<?> results = entityManager.createNativeQuery(
                            "SELECT column_name FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = ?1 ORDER BY ORDINAL_POSITION")
                    .setParameter(1, tableName)
                    .getResultList();
            return results.stream().map(item -> item.toString()).collect(Collectors.toList());
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }


    /**
     * 根据 MetadataSchemaDTO 动态创建数据表
     * 表名将是 `mf_data_<tenantId>_<schemaName>`
     * @param schemaDTO 元数据模式DTO
     */
    @Transactional
    public void createTable(MetadataSchemaDTO schemaDTO) {
        String tableName = buildTableName(schemaDTO.getTenantId(), schemaDTO.getName());

        // 检查表是否已存在
        if (tableExists(tableName)) {
            logger.warn("Table '{}' already exists. Skipping creation.", tableName);
            return;
        }

        StringBuilder ddl = new StringBuilder("CREATE TABLE IF NOT EXISTS `")
                .append(tableName).append("` (\n");

        // 添加默认的 ID 列
        ddl.append("    `id` BIGINT AUTO_INCREMENT PRIMARY KEY,\n");

        // 添加租户ID列 (确保数据隔离)
        ddl.append("    `tenant_id` VARCHAR(50) NOT NULL,\n");

        // 添加创建时间和更新时间
        ddl.append("    `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,\n");
        ddl.append("    `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,\n");

        ddl.append("    `created_by` VARCHAR(100) DEFAULT NULL COMMENT '创建人', ");
        ddl.append("    `dept_id` BIGINT DEFAULT NULL COMMENT '部门ID', ");


        // 根据字段定义添加列
        String columns = schemaDTO.getFields().stream()
                .map(field -> buildColumnDefinition(field))
                .collect(Collectors.joining(",\n"));

        ddl.append(columns);
        ddl.append("\n) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;");

        logger.info("Executing DDL to create table '{}':\n{}", tableName, ddl.toString());
        entityManager.createNativeQuery(ddl.toString()).executeUpdate();

        columnCache.invalidate(tableName);
        logger.info("Table '{}' created successfully.", tableName);
    }

    /**
     * 更新数据表结构 (添加、修改列)
     * 这是一个复杂的操作，需要考虑数据迁移和兼容性
     * 暂时只实现添加新列，修改列类型或删除列需要更复杂的逻辑
     * @param schemaDTO 元数据模式DTO
     */
    @Transactional
    public void updateTable(MetadataSchemaDTO schemaDTO) {
        String tableName = buildTableName(schemaDTO.getTenantId(), schemaDTO.getName());

        if (!tableExists(tableName)) {
            logger.warn("Table '{}' does not exist. Cannot update. Consider creating it first.", tableName);
            createTable(schemaDTO); // 如果不存在，则创建
            return;
        }

        // 遍历模式中的字段，检查哪些是新增的
        for (MetadataFieldDTO field : schemaDTO.getFields()) {
            if (!columnExists(tableName, field.getFieldName())) {
                String ddl = new StringBuilder("ALTER TABLE `")
                        .append(tableName).append("` ADD COLUMN ")
                        .append(buildColumnDefinition(field)).append(";")
                        .toString();
                logger.info("Executing DDL to add column '{}' to table '{}':\n{}", field.getFieldName(), tableName, ddl);
                entityManager.createNativeQuery(ddl).executeUpdate();
                logger.info("Column '{}' added successfully to table '{}'.", field.getFieldName(), tableName);
            }
            // TODO: 处理列修改 (类型、约束等) - 需要更复杂的 ALTER TABLE 语句和数据迁移策略
        }

        columnCache.invalidate(tableName);
    }

    /**
     * 删除数据表
     * @param tenantId 租户ID
     * @param schemaName 模式名称
     */
    @Transactional
    public void dropTable(String tenantId, String schemaName) {
        String tableName = buildTableName(tenantId, schemaName);
        if (tableExists(tableName)) {
            String ddl = "DROP TABLE IF EXISTS `" + tableName + "`;";
            logger.info("Executing DDL to drop table '{}':\n{}", tableName, ddl);
            entityManager.createNativeQuery(ddl).executeUpdate();
            logger.info("Table '{}' dropped successfully.", tableName);
        } else {
            logger.warn("Table '{}' does not exist. Skipping drop.", tableName);
        }

        columnCache.invalidate(tableName);
    }

    /**
     * 辅助方法：构建表名
     * @param tenantId 租户ID
     * @param schemaName 模式名称
     * @return 完整的表名 (例如: mf_data_tenant-001_sampleinfo)
     */
    public String buildTableName(String tenantId, String schemaName) {
        // 将 schemaName 转换为小写并替换特殊字符，以符合数据库命名规范
        String sanitizedSchemaName = schemaName.toLowerCase().replaceAll("[^a-z0-9_]", "_");
        return String.format("mf_data_%s_%s", tenantId, sanitizedSchemaName);
    }

    // --- 内部辅助方法 ---

    /**
     * 检查表是否存在
     * @param tableName 表名
     * @return 如果存在返回 true，否则返回 false
     */
    private boolean tableExists(String tableName) {
        try {
            // 使用位置参数 (?)
            Object result = entityManager.createNativeQuery(
                            "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = ?1") // 使用 ?1
                    .setParameter(1, tableName) // 绑定位置参数 1
                    .getSingleResult();
            return ((Number) result).intValue() > 0;
        } catch (Exception e) {
            logger.error("Error checking table existence for '{}': {}", tableName, e.getMessage());
            return false;
        }
    }

    /**
     * 检查列是否存在
     * @param tableName 表名
     * @param columnName 列名
     * @return 如果存在返回 true，否则返回 false
     */
    private boolean columnExists(String tableName, String columnName) {
        try {
            // 使用位置参数 (?)
            Object result = entityManager.createNativeQuery(
                            "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = ?1 AND column_name = ?2") // 使用 ?1 和 ?2
                    .setParameter(1, tableName) // 绑定位置参数 1
                    .setParameter(2, columnName) // 绑定位置参数 2
                    .getSingleResult();
            return ((Number) result).intValue() > 0;
        } catch (Exception e) {
            logger.error("Error checking column existence for '{}'.'{}': {}", tableName, columnName, e.getMessage());
            return false;
        }
    }

    /**
     * 根据 MetadataFieldDTO 构建 SQL 列定义
     * @param field 字段DTO
     * @return SQL 列定义字符串
     */
    private String buildColumnDefinition(MetadataFieldDTO field) {
        StringBuilder columnDef = new StringBuilder("    `").append(field.getFieldName()).append("` ");

        switch (field.getFieldType()) {
            case STRING:
                columnDef.append("VARCHAR(255)"); // 默认字符串长度
                break;
            case TEXT:
                columnDef.append("TEXT");
                break;
            case NUMBER:
                columnDef.append("DOUBLE"); // 浮点数
                break;
            case INTEGER:
                columnDef.append("BIGINT"); // 大整数
                break;
            case BOOLEAN:
                columnDef.append("BOOLEAN");
                break;
            case DATE:
                columnDef.append("DATE");
                break;
            case DATETIME:
                columnDef.append("DATETIME");
                break;
            case ENUM:
                columnDef.append("VARCHAR(255)"); // 枚举值也存储为字符串
                break;
            case FILE:
                columnDef.append("VARCHAR(500)"); // 文件路径或ID
                break;
            case REFERENCE:
                columnDef.append("BIGINT"); // 引用其他实体ID
                break;
            default:
                columnDef.append("VARCHAR(255)"); // 默认
        }

        if (field.getRequired() != null && field.getRequired()) {
            columnDef.append(" NOT NULL");
        }
        if (field.getDefaultValue() != null && !field.getDefaultValue().isEmpty()) {
            columnDef.append(" DEFAULT '").append(field.getDefaultValue()).append("'");
        }
        // TODO: 添加校验规则 (validationRule) 的处理，可能需要触发器或在应用层实现

        return columnDef.toString();
    }
}
