package com.smart.agriculture.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 提示词模板实体
 */
@Data
@TableName("prompt_template")
public class PromptTemplate {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 模板名称 */
    private String templateName;
    /** 模板编码 */
    private String templateCode;
    /** 模板内容 */
    private String content;
    /** 描述 */
    private String description;
    /** 状态: 1启用 0禁用 */
    private Integer status;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
