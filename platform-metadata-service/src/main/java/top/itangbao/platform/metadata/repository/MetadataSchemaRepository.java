package top.itangbao.platform.metadata.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import top.itangbao.platform.metadata.domain.MetadataSchema;

import java.util.List;
import java.util.Optional;

@Repository
public interface MetadataSchemaRepository extends JpaRepository<MetadataSchema, Long> {

    /**
     * 根据模式名称查找元数据模式
     * @param name 模式名称
     * @return 包含元数据模式的 Optional 对象
     */
    Optional<MetadataSchema> findByName(String name);

    /**
     * 根据租户ID查找所有元数据模式
     * @param tenantId 租户ID
     * @return 属于该租户的所有元数据模式列表
     */
    List<MetadataSchema> findByTenantId(String tenantId);

    /**
     * 根据模式名称和租户ID查找元数据模式
     * @param name 模式名称
     * @param tenantId 租户ID
     * @return 包含元数据模式的 Optional 对象
     */
    Optional<MetadataSchema> findByNameAndTenantId(String name, String tenantId);

    /**
     * 根据模式名称判断模式是否存在
     * @param name 模式名称
     * @return 如果存在返回 true，否则返回 false
     */
    boolean existsByName(String name);

    /**
     * 根据模式名称和租户ID判断模式是否存在
     * @param name 模式名称
     * @param tenantId 租户ID
     * @return 如果存在返回 true，否则返回 false
     */
    boolean existsByNameAndTenantId(String name, String tenantId);
}
