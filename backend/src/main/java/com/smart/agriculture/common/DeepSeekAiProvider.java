package com.smart.agriculture.common;

import com.smart.agriculture.entity.AiConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DeepSeek AI 服务商接入（OpenAI 兼容 API）
 */
@Slf4j
@Component("deepseekProvider")
public class DeepSeekAiProvider implements AiProvider {

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public String chat(String systemPrompt, List<Map<String, String>> messages, AiConfig config) {
        if (config == null || !StringUtils.hasText(config.getApiKey())) {
            return "AI 服务尚未配置 API Key，请在管理端「AI 配置」中填写后重试。";
        }
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + config.getApiKey());

            // 组装 messages: system + 历史消息
            List<Map<String, String>> allMessages = new ArrayList<>();
            Map<String, String> sys = new HashMap<>();
            sys.put("role", "system");
            sys.put("content", systemPrompt);
            allMessages.add(sys);
            if (messages != null) {
                allMessages.addAll(messages);
            }

            Map<String, Object> body = new HashMap<>();
            body.put("model", config.getModelName());
            if (config.getTemperature() != null) {
                body.put("temperature", config.getTemperature().doubleValue());
            }
            if (config.getMaxTokens() != null) {
                body.put("max_tokens", config.getMaxTokens());
            }
            body.put("messages", allMessages);

            String url = config.getBaseUrl() + "/chat/completions";
            ResponseEntity<Map> resp = restTemplate.postForEntity(
                    url, new HttpEntity<>(body, headers), Map.class);

            Map<String, Object> respBody = resp.getBody();
            if (respBody == null) {
                return "AI 服务返回为空。";
            }
            List<Map<String, Object>> choices = (List<Map<String, Object>>) respBody.get("choices");
            if (choices == null || choices.isEmpty()) {
                return "AI 服务未返回有效结果。";
            }
            Map<String, Object> first = choices.get(0);
            Map<String, Object> message = (Map<String, Object>) first.get("message");
            if (message == null) {
                return "AI 服务返回异常。";
            }
            Object content = message.get("content");
            return content != null ? content.toString() : "";
        } catch (Exception e) {
            log.error("[DeepSeek] 调用失败: {}", e.getMessage(), e);
            return "AI 服务调用失败：" + e.getMessage();
        }
    }

    @Override
    public String getProviderName() {
        return "deepseek";
    }
}
