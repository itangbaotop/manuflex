package top.itangbao.platform.lims.service;

import top.itangbao.platform.lims.dto.TestItemRequest;
import top.itangbao.platform.lims.dto.TestItemResponse;

import java.util.List;

public interface TestItemService {
    /**
     * 注册 LIMS 测试项目相关的元数据模式和字段
     * @param tenantId 租户ID
     * @return 注册成功的模式ID
     */
    Long registerLimsTestItemMetadataSchema(String tenantId);

    /**
     * 创建测试项目
     * @param tenantId 租户ID
     * @param request 测试项目请求
     * @return 创建的测试项目响应
     */
    TestItemResponse createTestItem(String tenantId, TestItemRequest request);

    /**
     * 根据 ID 获取测试项目
     * @param tenantId 租户ID
     * @param testItemId 测试项目ID
     * @return 测试项目响应
     */
    TestItemResponse getTestItemById(String tenantId, Long testItemId);

    /**
     * 获取所有测试项目
     * @param tenantId 租户ID
     * @return 测试项目列表
     */
    List<TestItemResponse> getAllTestItems(String tenantId);

    /**
     * 更新测试项目
     * @param tenantId 租户ID
     * @param testItemId 测试项目ID
     * @param request 测试项目请求
     * @return 更新后的测试项目响应
     */
    TestItemResponse updateTestItem(String tenantId, Long testItemId, TestItemRequest request);

    /**
     * 删除测试项目
     * @param tenantId 租户ID
     * @param testItemId 测试项目ID
     */
    void deleteTestItem(String tenantId, Long testItemId);
}
