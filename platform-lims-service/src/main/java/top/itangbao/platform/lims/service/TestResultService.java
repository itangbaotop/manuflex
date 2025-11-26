package top.itangbao.platform.lims.service;

import top.itangbao.platform.lims.dto.TestResultRequest;
import top.itangbao.platform.lims.dto.TestResultResponse;

import java.util.List;

public interface TestResultService {
    /**
     * 注册 LIMS 测试结果相关的元数据模式和字段
     * @param tenantId 租户ID
     * @return 注册成功的模式ID
     */
    Long registerLimsTestResultMetadataSchema(String tenantId);

    /**
     * 创建测试结果
     * @param tenantId 租户ID
     * @param request 测试结果请求
     * @return 创建的测试结果响应
     */
    TestResultResponse createTestResult(String tenantId, TestResultRequest request);

    /**
     * 根据 ID 获取测试结果
     * @param tenantId 租户ID
     * @param testResultId 测试结果ID
     * @return 测试结果响应
     */
    TestResultResponse getTestResultById(String tenantId, Long testResultId);

    /**
     * 获取所有测试结果
     * @param tenantId 租户ID
     * @return 测试结果列表
     */
    List<TestResultResponse> getAllTestResults(String tenantId);

    /**
     * 更新测试结果
     * @param tenantId 租户ID
     * @param testResultId 测试结果ID
     * @param request 测试结果请求
     * @return 更新后的测试结果响应
     */
    TestResultResponse updateTestResult(String tenantId, Long testResultId, TestResultRequest request);

    /**
     * 删除测试结果
     * @param tenantId 租户ID
     * @param testResultId 测试结果ID
     */
    void deleteTestResult(String tenantId, Long testResultId);
}
