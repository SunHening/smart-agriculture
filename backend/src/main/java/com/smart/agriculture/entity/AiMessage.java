package com.smart.agriculture.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 智能客服消息实体
 */
@Data
@TableName("ai_message")
public class AiMessage {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 关联会话ID */
    private Long conversationId;
    /** 发送者: USER / AI */
    private String sender;
    /** 消息内容 */
    private String content;
    /** 消息类型: TEXT / IMAGE */
    private String messageType;
    /** AI服务提供商标识（预留接口: deepseek / openai / local） */
    private String provider;
    /** AI模型名称（预留） */
    private String modelName;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
