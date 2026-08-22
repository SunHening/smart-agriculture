package com.smart.agriculture.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 检测结果实体 — 深度学习模型输出的农田区域检测结果
 */
@Data
@TableName("detection_result")
public class DetectionResult {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 关联影像任务ID */
    private Long imageTaskId;
    /** 关联农田ID */
    private Long fieldId;
    /** 边界多边形 GeoJSON */
    @TableField("boundary_geojson")
    private String boundaryGeoJson;
    /** 检测面积(亩) — 基于像素/坐标计算 */
    private BigDecimal detectedArea;
    /** 置信度 (0~1) */
    private BigDecimal confidence;
    /** 农田覆盖率 (检测区域占影像比例) */
    private BigDecimal coverageRate;
    /** 分割掩码图片路径 */
    private String maskImagePath;
    /** 叠加可视化图片路径 */
    private String overlayImagePath;
    /** 处理耗时(毫秒) */
    private Long processingTimeMs;
    /** 模型名称 */
    private String modelName;
    /** 模型版本 */
    private String modelVersion;
    /** 状态: SUCCESS / FAILED */
    private String status;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
