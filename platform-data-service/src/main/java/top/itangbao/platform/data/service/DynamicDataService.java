package top.itangbao.platform.data.service;


import top.itangbao.platform.data.api.dto.*;

import java.util.List;
import java.util.Map;

public interface DynamicDataService {
    /**
     * 创建或更新动态数据表 (根据 schemaId)
     * @param schemaId 元数据模式ID
     */
    void createOrUpdateDynamicTable(Long schemaId);

    /**
     * 删除动态数据表 (根据 schemaId)
     * @param schemaId 元数据模式ID
     */
    void deleteDynamicTable(Long schemaId);

    /**
     * 插入动态数据
     * @param request 动态数据请求
     * @return 插入后的数据响应
     */
    DynamicDataResponse insertDynamicData(DynamicDataRequest request);

    /**
     * 根据 ID 查询动态数据
     * @param tenantId 租户ID
     * @param schemaName 模式名称
     * @param id 数据ID
     * @return 动态数据响应
     */
    DynamicDataResponse getDynamicDataById(String tenantId, String schemaName, Long id);

    /**
     * 查询所有动态数据 (分页和过滤)
     * @param tenantId 租户ID
     * @param schemaName 模式名称
     * @param pageRequest 分页请求参数
     * @param filterRequest 过滤请求参数
     * @return 动态数据列表 (分页响应)
     */
    PageResponseDTO<DynamicDataResponse> getAllDynamicData(String tenantId, String schemaName, PageRequestDTO pageRequest, FilterRequestDTO filterRequest);

    /**
     * 更新动态数据
     * @param tenantId 租户ID
     * @param schemaName 模式名称
     * @param id 数据ID
     * @param updates 要更新的字段和值
     * @return 更新后的数据响应
     */
    DynamicDataResponse updateDynamicData(String tenantId, String schemaName, Long id, Map<String, Object> updates);

    /**
     * 删除动态数据
     * @param tenantId 租户ID
     * @param schemaName 模式名称
     * @param id 数据ID
     */
    void deleteDynamicData(String tenantId, String schemaName, Long id);
}
