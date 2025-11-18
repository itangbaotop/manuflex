package top.itangbao.platform.lims.service;

import top.itangbao.platform.lims.dto.SampleRequest;
import top.itangbao.platform.lims.dto.SampleResponse;

import java.util.List;

public interface SampleService {
    /**
     * 注册 LIMS 相关的元数据模式和字段
     * @param tenantId 租户ID
     * @return 注册成功的模式ID
     */
    Long registerLimsMetadataSchema(String tenantId);

    /**
     * 创建样品
     * @param tenantId 租户ID
     * @param request 样品请求
     * @return 创建的样品响应
     */
    SampleResponse createSample(String tenantId, SampleRequest request);

    /**
     * 根据 ID 获取样品
     * @param tenantId 租户ID
     * @param sampleId 样品ID
     * @return 样品响应
     */
    SampleResponse getSampleById(String tenantId, Long sampleId);

    /**
     * 获取所有样品
     * @param tenantId 租户ID
     * @return 样品列表
     */
    List<SampleResponse> getAllSamples(String tenantId);

    /**
     * 更新样品
     * @param tenantId 租户ID
     * @param sampleId 样品ID
     * @param request 样品请求
     * @return 更新后的样品响应
     */
    SampleResponse updateSample(String tenantId, Long sampleId, SampleRequest request);

    /**
     * 删除样品
     * @param tenantId 租户ID
     * @param sampleId 样品ID
     */
    void deleteSample(String tenantId, Long sampleId);
}
