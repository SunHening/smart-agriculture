package com.smart.agriculture.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.smart.agriculture.entity.DetectionResult;

import java.util.Map;

public interface DetectionResultService extends IService<DetectionResult> {

    /** 分页查询检测结果 */
    Page<DetectionResult> pageQuery(Map<String, Object> params);

    /** 获取检测详情 */
    DetectionResult getDetail(Long id);

    /** 重新检测 */
    DetectionResult reDetect(Long id);
}
