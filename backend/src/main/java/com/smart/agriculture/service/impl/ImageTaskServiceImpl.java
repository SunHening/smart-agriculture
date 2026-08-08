package com.smart.agriculture.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smart.agriculture.entity.ImageTask;
import com.smart.agriculture.mapper.ImageTaskMapper;
import com.smart.agriculture.service.ImageTaskService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Map;

@Service
public class ImageTaskServiceImpl extends ServiceImpl<ImageTaskMapper, ImageTask> implements ImageTaskService {

    @Override
    public Page<ImageTask> pageQuery(Map<String, Object> params) {
        Integer pageNum = Integer.parseInt(params.getOrDefault("pageNum", "1").toString());
        Integer pageSize = Integer.parseInt(params.getOrDefault("pageSize", "10").toString());
        Long userId = params.get("userId") != null ? Long.valueOf(params.get("userId").toString()) : null;
        String imageType = (String) params.get("imageType");
        String status = (String) params.get("status");

        LambdaQueryWrapper<ImageTask> wrapper = new LambdaQueryWrapper<>();
        if (userId != null) {
            wrapper.eq(ImageTask::getUserId, userId);
        }
        if (StringUtils.hasText(imageType)) {
            wrapper.eq(ImageTask::getImageType, imageType);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(ImageTask::getStatus, status);
        }
        wrapper.orderByDesc(ImageTask::getCreateTime);

        return page(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    public ImageTask submitTask(ImageTask task) {
        task.setStatus("PENDING");
        task.setCreateTime(LocalDateTime.now());
        task.setUpdateTime(LocalDateTime.now());
        save(task);
        // TODO: 触发异步检测任务 → 调用 Python 微服务
        return task;
    }

    @Override
    public void updateStatus(Long id, String status, String errorMsg) {
        ImageTask task = new ImageTask();
        task.setId(id);
        task.setStatus(status);
        task.setErrorMsg(errorMsg);
        task.setUpdateTime(LocalDateTime.now());
        updateById(task);
    }
}
