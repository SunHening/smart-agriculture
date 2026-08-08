package com.smart.agriculture.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * DeepSeek AI 服务商接入（预留实现示例）
 * 使用时配置 API Key 即可启用
 */
@Slf4j
@Component("deepseekProvider")
public class DeepSeekAiProvider implements AiProvider {

    // TODO: 配置 API Key
    // @Value("${ai.deepseek.api-key:}")
    // private String apiKey;

    // TODO: 配置 API 端点
    // private static final String API_URL = "https://api.deepseek.com/v1/chat/completions";

    @Override
    public String chat(String message, String context, String modelName) {
        // TODO: 调用 DeepSeek API
        // RestTemplate restTemplate = new RestTemplate();
        // HttpHeaders headers = new HttpHeaders();
        // headers.set("Authorization", "Bearer " + apiKey);
        // ... 构建请求体，发送请求，解析响应
        log.info("[DeepSeek] 收到消息: {} (待接入API)", message);
        return "DeepSeek AI 服务尚未配置 API Key，当前使用默认规则匹配。";
    }

    @Override
    public String getProviderName() {
        return "deepseek";
    }
}
