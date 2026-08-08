package com.smart.agriculture.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.smart.agriculture.entity.ImageTask;

import java.util.Map;

public interface ImageTaskService extends IService<ImageTask> {

    /** 分页查询影像任务 */
    Page<ImageTask> pageQuery(Map<String, Object> params);

    /** 提交检测任务 */
    ImageTask submitTask(ImageTask task);

    /** 更新任务状态 */
    void updateStatus(Long id, String status, String errorMsg);
}
