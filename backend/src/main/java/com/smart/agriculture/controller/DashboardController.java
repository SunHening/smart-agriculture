package com.smart.agriculture.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.agriculture.common.Result;
import com.smart.agriculture.entity.FarmField;
import com.smart.agriculture.entity.ImageTask;
import com.smart.agriculture.service.FarmFieldService;
import com.smart.agriculture.service.ImageTaskService;
import com.smart.agriculture.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据看板控制器 — 全平台汇总 + 实时任务看板
 */
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    @Autowired
    private UserService userService;
    @Autowired
    private FarmFieldService farmFieldService;
    @Autowired
    private ImageTaskService imageTaskService;

    /** 全平台数据汇总 */
    @GetMapping("/overview")
    public Result<Map<String, Object>> getOverview() {
        Map<String, Object> data = new HashMap<>();

        // 基础统计
        data.put("userCount", userService.count());
        data.put("fieldCount", farmFieldService.count());

        // 面积汇总
        List<FarmField> fields = farmFieldService.list();
        BigDecimal totalArea = fields.stream()
                .map(FarmField::getArea)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalDetectedArea = fields.stream()
                .map(FarmField::getDetectedArea)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        data.put("totalArea", totalArea);
        data.put("totalDetectedArea", totalDetectedArea);

        // 检测统计
        long totalDetections = imageTaskService.count();
        data.put("totalDetections", totalDetections);

        long completedDetections = countByStatus("COMPLETED");
        data.put("completedDetections", completedDetections);

        long pendingDetections = countByStatus("PENDING");
        data.put("pendingDetections", pendingDetections);

        long failedDetections = countByStatus("FAILED");
        data.put("failedDetections", failedDetections);

        // 成功率
        if (totalDetections > 0) {
            data.put("successRate", new BigDecimal(completedDetections)
                    .divide(new BigDecimal(totalDetections), 4, BigDecimal.ROUND_HALF_UP)
                    .multiply(new BigDecimal(100))
                    .setScale(1, BigDecimal.ROUND_HALF_UP));
        } else {
            data.put("successRate", BigDecimal.ZERO);
        }

        return Result.success(data);
    }

    /** 实时任务看板：任务状态分布 + 最近任务 */
    @GetMapping("/tasks")
    public Result<Map<String, Object>> getTasks() {
        Map<String, Object> data = new HashMap<>();

        data.put("pending", countByStatus("PENDING"));
        data.put("processing", countByStatus("PROCESSING"));
        data.put("completed", countByStatus("COMPLETED"));
        data.put("failed", countByStatus("FAILED"));
        data.put("total", imageTaskService.count());

        // 最近 10 条任务
        List<ImageTask> recent = imageTaskService.list(new LambdaQueryWrapper<ImageTask>()
                .orderByDesc(ImageTask::getCreateTime)
                .last("LIMIT 10"));
        data.put("recent", recent);

        return Result.success(data);
    }

    private long countByStatus(String status) {
        return imageTaskService.count(
                new LambdaQueryWrapper<ImageTask>().eq(ImageTask::getStatus, status));
    }
}
