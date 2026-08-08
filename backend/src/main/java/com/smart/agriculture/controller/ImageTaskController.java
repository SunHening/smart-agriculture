package com.smart.agriculture.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smart.agriculture.common.Result;
import com.smart.agriculture.entity.ImageTask;
import com.smart.agriculture.service.ImageTaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 影像任务控制器 — 遥感/无人机影像上传与检测
 */
@RestController
@RequestMapping("/api/detection")
public class ImageTaskController {

    @Autowired
    private ImageTaskService imageTaskService;

    /** 分页查询检测任务 */
    @GetMapping("/page")
    public Result<Page<ImageTask>> page(@RequestParam Map<String, Object> params) {
        return Result.success(imageTaskService.pageQuery(params));
    }

    /** 获取检测详情 */
    @GetMapping("/{id}")
    public Result<ImageTask> detail(@PathVariable Long id) {
        return Result.success(imageTaskService.getById(id));
    }

    /** 提交检测任务 */
    @PostMapping("/submit")
    public Result<ImageTask> submit(@RequestBody ImageTask task) {
        return Result.success(imageTaskService.submitTask(task));
    }

    /** 重新检测 */
    @PostMapping("/re-detect/{id}")
    public Result<?> reDetect(@PathVariable Long id) {
        ImageTask task = imageTaskService.getById(id);
        if (task != null) {
            task.setStatus("PENDING");
            imageTaskService.updateById(task);
        }
        return Result.success("已重新提交检测");
    }

    /** 删除检测记录 */
    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        imageTaskService.removeById(id);
        return Result.success("删除成功");
    }
}
