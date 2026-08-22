package com.smart.agriculture.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 遥感/检测数据集实体
 */
@Data
@TableName("dataset")
public class Dataset {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 数据集名称 */
    private String datasetName;
    /** 数据集类型: REMOTE_SENSING / DRONE */
    private String datasetType;
    /** 描述 */
    private String description;
    /** 影像数量 */
    private Integer imageCount;
    /** 存储路径 */
    private String storagePath;
    /** 状态: 1启用 0禁用 */
    private Integer status;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
