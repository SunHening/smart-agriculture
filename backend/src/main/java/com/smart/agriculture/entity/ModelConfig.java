package com.smart.agriculture.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 检测模型/参数配置实体
 */
@Data
@TableName("model_config")
public class ModelConfig {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 配置名称 */
    private String configName;
    /** 模型类型: vit_b / vit_l / vit_h */
    private String modelType;
    /** 权重文件路径（空则自动下载） */
    private String checkpointPath;
    /** 运行设备: cpu / cuda */
    private String device;
    /** 模型参数 JSON */
    private String modelParams;
    /** 默认地面采样距离 GSD (m/px) */
    private BigDecimal gsdDefault;
    /** 最小地块面积（像素） */
    private Integer minAreaThreshold;
    /** 置信度阈值 (0~1) */
    private BigDecimal confidenceThreshold;
    /** 状态: 1启用 0禁用 */
    private Integer status;
    /** 是否当前生效: 1是 0否 */
    private Integer isActive;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
