package com.smart.agriculture.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smart.agriculture.common.Result;
import com.smart.agriculture.entity.DetectionResult;
import com.smart.agriculture.entity.ImageTask;
import com.smart.agriculture.service.DetectionResultService;
import com.smart.agriculture.service.ImageTaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * SAM-Geo 地图检测控制器
 * 调用 Python 微服务进行农田边界分割
 */
@RestController
@RequestMapping("/api/sam-geo")
public class SamGeoController {

    @Autowired
    private ImageTaskService imageTaskService;

    @Autowired
    private DetectionResultService detectionResultService;

    // Python 微服务地址（默认本地 5000 端口）
    @Value("${sam-geo.service-url:http://localhost:5000}")
    private String samGeoServiceUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * 地图框选检测
     * 前端发送 bbox 区域 → 调用 SAM-Geo → 返回边界 + 保存结果
     */
    @PostMapping("/detect")
    public Result<?> detect(@RequestBody Map<String, Object> params) {
        try {
            // Step 1: 创建 ImageTask 记录
            ImageTask task = new ImageTask();
            task.setUserId(Long.valueOf(params.get("userId").toString()));
            task.setImageType("REMOTE_SENSING");
            task.setOriginalFilename("tianditu_" + System.currentTimeMillis() + ".png");
            task.setStatus("PROCESSING");
            task.setCreateTime(LocalDateTime.now());
            task.setUpdateTime(LocalDateTime.now());
            imageTaskService.save(task);

            // Step 2: 调用 Python SAM-Geo 微服务
            Map<String, Object> pythonReq = new HashMap<>();
            pythonReq.put("bbox", params.get("bbox"));
            pythonReq.put("zoom", params.getOrDefault("zoom", 17));
            pythonReq.put("method", "auto");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(pythonReq, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    samGeoServiceUrl + "/detect", request, Map.class);

            Map<String, Object> pyResult = response.getBody();
            if (pyResult == null || !Boolean.TRUE.equals(pyResult.get("success"))) {
                task.setStatus("FAILED");
                task.setErrorMsg(pyResult != null ? (String) pyResult.get("error") : "SAM-Geo 服务异常");
                imageTaskService.updateById(task);
                return Result.error("检测失败: " + task.getErrorMsg());
            }

            // Step 3: 保存检测结果
            List<Map<String, Object>> boundaries = (List<Map<String, Object>>) pyResult.get("boundaries");
            double totalArea = ((Number) pyResult.get("total_area_mu")).doubleValue();

            for (Map<String, Object> boundary : boundaries) {
                DetectionResult dr = new DetectionResult();
                dr.setImageTaskId(task.getId());
                dr.setFieldId(params.get("fieldId") != null
                        ? Long.valueOf(params.get("fieldId").toString()) : null);
                dr.setBoundaryGeoJson(new ObjectMapper().writeValueAsString(boundary.get("geojson")));
                dr.setDetectedArea(BigDecimal.valueOf(((Number) boundary.get("area_mu")).doubleValue()));
                dr.setConfidence(BigDecimal.valueOf(((Number) boundary.get("confidence")).doubleValue()));
                dr.setModelName("SAM-Geo");
                dr.setModelVersion("v1.0.0");
                dr.setProcessingTimeMs(((Number) pyResult.get("processing_time_ms")).longValue());
                dr.setStatus("SUCCESS");
                dr.setCreateTime(LocalDateTime.now());
                detectionResultService.save(dr);
            }

            // Step 4: 更新 ImageTask
            task.setStatus("COMPLETED");
            task.setImageWidth(256);
            task.setImageHeight(256);
            task.setUpdateTime(LocalDateTime.now());
            imageTaskService.updateById(task);

            Map<String, Object> result = new HashMap<>();
            result.put("imageTaskId", task.getId());
            result.put("boundaries", boundaries);
            result.put("totalAreaMu", totalArea);
            result.put("processingTimeMs", pyResult.get("processing_time_ms"));
            result.put("overlayImage", pyResult.get("image_base64"));

            return Result.success(result);

        } catch (Exception e) {
            return Result.error("SAM-Geo 检测异常: " + e.getMessage());
        }
    }

    /**
     * 地图截图检测（前端传 base64 图片，绕过天地图服务端下载限制）
     */
    @PostMapping("/detect-image")
    public Result<?> detectImage(@RequestBody Map<String, Object> params) {
        try {
            Long userId = Long.valueOf(params.get("userId").toString());
            Long fieldId = params.get("fieldId") != null ? Long.valueOf(params.get("fieldId").toString()) : null;
            String imageType = params.get("imageType") != null ? params.get("imageType").toString() : "REMOTE_SENSING";
            String filename = params.get("originalFilename") != null
                    ? params.get("originalFilename").toString()
                    : "image_" + System.currentTimeMillis() + ".png";

            // Step 1: 创建 ImageTask
            ImageTask task = new ImageTask();
            task.setUserId(userId);
            task.setFieldId(fieldId);
            task.setImageType(imageType);
            task.setOriginalFilename(filename);
            task.setGsd(params.get("gsd") != null ? new BigDecimal(params.get("gsd").toString()) : null);
            task.setStatus("PROCESSING");
            task.setCreateTime(LocalDateTime.now());
            task.setUpdateTime(LocalDateTime.now());
            imageTaskService.save(task);

            // Step 2: 转发到 Python /detect-image
            Map<String, Object> pythonReq = new HashMap<>();
            pythonReq.put("image_base64", params.get("imageBase64"));
            pythonReq.put("gsd", params.getOrDefault("gsd", 1.2));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(pythonReq, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    samGeoServiceUrl + "/detect-image", request, Map.class);

            Map<String, Object> pyResult = response.getBody();
            if (pyResult == null || !Boolean.TRUE.equals(pyResult.get("success"))) {
                task.setStatus("FAILED");
                task.setErrorMsg(pyResult != null ? (String) pyResult.get("error") : "Python 服务异常");
                imageTaskService.updateById(task);
                return Result.error("检测失败: " + task.getErrorMsg());
            }

            // Step 3: 保存检测结果
            List<Map<String, Object>> boundaries = (List<Map<String, Object>>) pyResult.get("boundaries");
            double totalArea = ((Number) pyResult.get("total_area_mu")).doubleValue();

            for (Map<String, Object> b : boundaries) {
                DetectionResult dr = new DetectionResult();
                dr.setImageTaskId(task.getId());
                dr.setFieldId(fieldId);
                dr.setBoundaryGeoJson(new ObjectMapper().writeValueAsString(b.get("geojson")));
                dr.setDetectedArea(BigDecimal.valueOf(((Number) b.get("area_mu")).doubleValue()));
                dr.setConfidence(BigDecimal.valueOf(((Number) b.get("confidence")).doubleValue()));
                dr.setModelName("SAM-Geo");
                dr.setModelVersion("v1.0.0");
                dr.setProcessingTimeMs(((Number) pyResult.get("processing_time_ms")).longValue());
                dr.setStatus("SUCCESS");
                dr.setCreateTime(LocalDateTime.now());
                detectionResultService.save(dr);
            }

            // Step 4: 更新 ImageTask
            task.setStatus("COMPLETED");
            task.setUpdateTime(LocalDateTime.now());
            imageTaskService.updateById(task);

            Map<String, Object> result = new HashMap<>();
            result.put("imageTaskId", task.getId());
            result.put("boundaries", boundaries);
            result.put("totalAreaMu", totalArea);
            result.put("fieldCount", pyResult.get("field_count"));
            result.put("processingTimeMs", pyResult.get("processing_time_ms"));
            result.put("overlayBase64", pyResult.get("overlay_base64"));

            return Result.success(result);

        } catch (Exception e) {
            return Result.error("检测异常: " + e.getMessage());
        }
    }

    /** 健康检查 */
    @GetMapping("/health")
    public Result<?> health() {
        try {
            ResponseEntity<Map> resp = restTemplate.getForEntity(samGeoServiceUrl + "/health", Map.class);
            return Result.success(resp.getBody());
        } catch (Exception e) {
            return Result.error("SAM-Geo 服务未连接: " + e.getMessage());
        }
    }
}
