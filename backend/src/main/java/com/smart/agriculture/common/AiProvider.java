package com.smart.agriculture.common;

import com.smart.agriculture.entity.AiConfig;

import java.util.List;
import java.util.Map;

/**
 * AI 服务商接口
 * 用于接入不同的大模型服务商（DeepSeek / OpenAI 等）
 */
public interface AiProvider {

    /**
     * 调用大模型获取回复
     *
     * @param systemPrompt 系统提示词
     * @param messages     历史消息 [{role, content}]（不包含 system 消息）
     * @param config       AI 配置（baseURL / APIKey / 模型名 / 温度等）
     * @return AI 回复文本
     */
    String chat(String systemPrompt, List<Map<String, String>> messages, AiConfig config);

    /**
     * 服务商名称
     */
    String getProviderName();
}
