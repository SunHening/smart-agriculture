package com.smart.agriculture.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 影像任务实体 — 遥感/无人机航拍影像
 */
@Data
@TableName("image_task")
public class ImageTask {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    /** 关联农田ID */
    private Long fieldId;
    /** 影像类型: REMOTE_SENSING(遥感) / DRONE(无人机) */
    private String imageType;
    /** 原始文件名 */
    private String originalFilename;
    /** 服务器存储路径 */
    private String imagePath;
    /** 影像分辨率（如 0.5m/pixel） */
    private BigDecimal resolution;
    /** 地面采样距离 GSD (m/pixel) */
    private BigDecimal gsd;
    /** 坐标参考系 CRS (如 EPSG:4326) */
    private String crs;
    /** 影像中心经度 */
    private Double longitude;
    /** 影像中心纬度 */
    private Double latitude;
    /** 影像宽度(像素) */
    private Integer imageWidth;
    /** 影像高度(像素) */
    private Integer imageHeight;
    /** 状态: PENDING / PROCESSING / COMPLETED / FAILED */
    private String status;
    /** 错误信息 */
    private String errorMsg;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
