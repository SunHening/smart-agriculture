package com.smart.agriculture.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smart.agriculture.entity.DetectionResult;
import com.smart.agriculture.entity.ImageTask;
import com.smart.agriculture.mapper.DetectionResultMapper;
import com.smart.agriculture.service.DetectionResultService;
import com.smart.agriculture.service.ImageTaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

@Service
public class DetectionResultServiceImpl extends ServiceImpl<DetectionResultMapper, DetectionResult> implements DetectionResultService {

    @Autowired
    private ImageTaskService imageTaskService;

    @Override
    public Page<DetectionResult> pageQuery(Map<String, Object> params) {
        Integer pageNum = Integer.parseInt(params.getOrDefault("pageNum", "1").toString());
        Integer pageSize = Integer.parseInt(params.getOrDefault("pageSize", "10").toString());
        Long fieldId = params.get("fieldId") != null ? Long.valueOf(params.get("fieldId").toString()) : null;

        LambdaQueryWrapper<DetectionResult> wrapper = new LambdaQueryWrapper<>();
        if (fieldId != null) {
            wrapper.eq(DetectionResult::getFieldId, fieldId);
        }
        wrapper.orderByDesc(DetectionResult::getCreateTime);

        return page(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    public DetectionResult getDetail(Long id) {
        return getById(id);
    }

    @Override
    public DetectionResult reDetect(Long id) {
        DetectionResult result = getById(id);
        if (result != null) {
            ImageTask task = imageTaskService.getById(result.getImageTaskId());
            if (task != null) {
                task.setStatus("PENDING");
                task.setUpdateTime(LocalDateTime.now());
                imageTaskService.updateById(task);
                // TODO: 触发异步重新检测
            }
        }
        return result;
    }
}
