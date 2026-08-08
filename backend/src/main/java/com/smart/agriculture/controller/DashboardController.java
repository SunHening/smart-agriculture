package com.smart.agriculture.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smart.agriculture.common.Result;
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
import java.util.Map;

/**
 * 数据概览控制器 — 面向农田面积检测场景
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

    @GetMapping("/overview")
    public Result<Map<String, Object>> getOverview() {
        Map<String, Object> data = new HashMap<>();

        // 基础统计
        data.put("userCount", userService.count());
        data.put("fieldCount", farmFieldService.count());

        // 检测统计
        long totalDetections = imageTaskService.count();
        data.put("totalDetections", totalDetections);

        long completedDetections = imageTaskService.count(
                new LambdaQueryWrapper<ImageTask>().eq(ImageTask::getStatus, "COMPLETED"));
        data.put("completedDetections", completedDetections);

        long pendingDetections = imageTaskService.count(
                new LambdaQueryWrapper<ImageTask>().eq(ImageTask::getStatus, "PENDING"));
        data.put("pendingDetections", pendingDetections);

        long failedDetections = imageTaskService.count(
                new LambdaQueryWrapper<ImageTask>().eq(ImageTask::getStatus, "FAILED"));
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
}
