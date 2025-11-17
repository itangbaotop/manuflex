package top.itangbao.platform.metadata.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import top.itangbao.platform.metadata.domain.MetadataField;
import top.itangbao.platform.metadata.domain.MetadataSchema;

import java.util.List;
import java.util.Optional;

@Repository
public interface MetadataFieldRepository extends JpaRepository<MetadataField, Long> {

    /**
     * 根据所属模式查找所有字段
     * @param schema 所属元数据模式
     * @return 属于该模式的所有字段列表
     */
    List<MetadataField> findBySchema(MetadataSchema schema);

    /**
     * 根据字段名称和所属模式查找字段
     * @param fieldName 字段名称
     * @param schema 所属元数据模式
     * @return 包含元数据字段的 Optional 对象
     */
    Optional<MetadataField> findByFieldNameAndSchema(String fieldName, MetadataSchema schema);

    /**
     * 根据字段名称和所属模式判断字段是否存在
     * @param fieldName 字段名称
     * @param schema 所属元数据模式
     * @return 如果存在返回 true，否则返回 false
     */
    boolean existsByFieldNameAndSchema(String fieldName, MetadataSchema schema);
}
