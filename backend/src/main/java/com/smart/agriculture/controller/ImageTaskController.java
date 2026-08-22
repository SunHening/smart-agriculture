package com.smart.agriculture.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smart.agriculture.common.Result;
import com.smart.agriculture.entity.DetectionResult;
import com.smart.agriculture.entity.ImageTask;
import com.smart.agriculture.service.DetectionResultService;
import com.smart.agriculture.service.FarmFieldService;
import com.smart.agriculture.service.ImageTaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 影像任务控制器 — 遥感/无人机影像上传与检测
 */
@RestController
@RequestMapping("/api/detection")
public class ImageTaskController {

    @Autowired
    private ImageTaskService imageTaskService;

    @Autowired
    private DetectionResultService detectionResultService;

    @Autowired
    private FarmFieldService farmFieldService;

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 分页查询检测任务 */
    @GetMapping("/page")
    public Result<Page<ImageTask>> page(@RequestParam Map<String, Object> params) {
        return Result.success(imageTaskService.pageQuery(params));
    }

    /**
     * 查询用户的检测结果记录（天地图框选 + 影像上传检测）
     * 返回每条 ImageTask 及其聚合后的检测结果（面积/地块数/置信度/边界明细）
     */
    @GetMapping("/records")
    public Result<?> records(@RequestParam Map<String, Object> params) {
        Page<ImageTask> taskPage = imageTaskService.pageQuery(params);

        List<Map<String, Object>> records = new ArrayList<>();
        List<ImageTask> tasks = taskPage.getRecords();

        if (!tasks.isEmpty()) {
            // 批量解析农田名称，避免逐条查询
            Set<Long> fieldIds = tasks.stream()
                    .map(ImageTask::getFieldId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            Map<Long, String> fieldNameMap = new HashMap<>();
            if (!fieldIds.isEmpty()) {
                farmFieldService.listByIds(fieldIds)
                        .forEach(f -> fieldNameMap.put(f.getId(), f.getFieldName()));
            }

            for (ImageTask task : tasks) {
                Map<String, Object> record = new HashMap<>();
                record.put("taskId", task.getId());
                record.put("imageType", task.getImageType());
                record.put("originalFilename", task.getOriginalFilename());
                record.put("gsd", task.getGsd());
                record.put("status", task.getStatus());
                record.put("fieldId", task.getFieldId());
                record.put("fieldName", task.getFieldId() != null ? fieldNameMap.get(task.getFieldId()) : null);
                record.put("createTime", task.getCreateTime() != null ? task.getCreateTime().format(DTF) : null);

                List<DetectionResult> results = detectionResultService.list(
                        new LambdaQueryWrapper<DetectionResult>()
                                .eq(DetectionResult::getImageTaskId, task.getId()));

                double totalArea = results.stream()
                        .map(DetectionResult::getDetectedArea)
                        .filter(Objects::nonNull)
                        .mapToDouble(BigDecimal::doubleValue)
                        .sum();
                double avgConf = results.stream()
                        .map(DetectionResult::getConfidence)
                        .filter(Objects::nonNull)
                        .mapToDouble(BigDecimal::doubleValue)
                        .average().orElse(0);

                record.put("totalAreaMu", BigDecimal.valueOf(totalArea).setScale(4, RoundingMode.HALF_UP));
                record.put("fieldCount", results.size());
                record.put("confidence", BigDecimal.valueOf(avgConf).setScale(4, RoundingMode.HALF_UP));

                List<Map<String, Object>> boundaries = results.stream().map(dr -> {
                    Map<String, Object> b = new HashMap<>();
                    b.put("id", dr.getId());
                    b.put("detectedArea", dr.getDetectedArea());
                    b.put("confidence", dr.getConfidence());
                    b.put("processingTimeMs", dr.getProcessingTimeMs());
                    b.put("modelName", dr.getModelName());
                    b.put("modelVersion", dr.getModelVersion());
                    b.put("geojson", dr.getBoundaryGeoJson());
                    return b;
                }).collect(Collectors.toList());
                record.put("boundaries", boundaries);

                records.add(record);
            }
        }

        Map<String, Object> data = new HashMap<>();
        data.put("records", records);
        data.put("total", taskPage.getTotal());
        data.put("pageNum", taskPage.getCurrent());
        data.put("pageSize", taskPage.getSize());
        return Result.success(data);
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
