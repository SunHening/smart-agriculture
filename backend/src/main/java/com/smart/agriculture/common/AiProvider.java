package com.smart.agriculture.common;

/**
 * AI 服务商接口（预留）
 * 用于未来接入不同 AI 服务商
 */
public interface AiProvider {

    /**
     * 发送消息并获取回复
     * @param message     用户消息
     * @param context     上下文（历史消息）
     * @param modelName   模型名称
     * @return AI 回复文本
     */
    String chat(String message, String context, String modelName);

    /**
     * 服务商名称
     */
    String getProviderName();
}
