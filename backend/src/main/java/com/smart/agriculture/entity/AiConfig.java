package com.smart.agriculture.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * AI 模型/提示词参数配置实体
 */
@Data
@TableName("ai_config")
public class AiConfig {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 服务商: deepseek / openai */
    private String provider;
    /** API Key */
    private String apiKey;
    /** API 端点 */
    private String baseUrl;
    /** 模型名称 */
    private String modelName;
    /** 温度 */
    private BigDecimal temperature;
    /** 最大 token */
    private Integer maxTokens;
    /** 系统提示词 */
    private String systemPrompt;
    /** 状态: 1启用 0禁用 */
    private Integer status;
    /** 是否当前生效: 1是 0否 */
    private Integer isActive;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
