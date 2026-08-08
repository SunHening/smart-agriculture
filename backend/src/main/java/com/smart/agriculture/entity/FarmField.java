package com.smart.agriculture.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 农田实体 — 支持遥感/无人机影像检测结果
 */
@Data
@TableName("farm_field")
public class FarmField {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    /** 农田名称 */
    private String fieldName;
    /** 面积(亩) — 优先取检测结果，其次人工录入 */
    private BigDecimal area;
    /** 检测面积(亩) — 来自影像自动检测 */
    private BigDecimal detectedArea;
    /** 位置描述 */
    private String location;
    /** 经度 */
    private Double longitude;
    /** 纬度 */
    private Double latitude;
    /** 土壤类型 */
    private String soilType;
    /** 状态: 1正常 0闲置 */
    private Integer status;
    /** 封面图片 */
    private String coverImage;
    /** 边界多边形 GeoJSON */
    @TableField("boundary_geojson")
    private String boundaryGeoJson;
    /** 关联的最近检测结果ID */
    private Long lastDetectionId;
    /** 备注 */
    private String remark;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
