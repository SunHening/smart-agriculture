package com.smart.agriculture.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smart.agriculture.common.AiProvider;
import com.smart.agriculture.entity.AiConfig;
import com.smart.agriculture.entity.AiConversation;
import com.smart.agriculture.entity.AiMessage;
import com.smart.agriculture.mapper.AiConversationMapper;
import com.smart.agriculture.mapper.AiMessageMapper;
import com.smart.agriculture.service.AiConfigService;
import com.smart.agriculture.service.AiCustomerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 大模型交互问答服务实现
 * 接入 DeepSeek 大模型，支持上下文管理
 */
@Service
public class AiCustomerServiceImpl extends ServiceImpl<AiMessageMapper, AiMessage> implements AiCustomerService {

    @Autowired
    private AiConversationMapper conversationMapper;

    @Autowired
    private AiConfigService aiConfigService;

    @Autowired
    @Qualifier("deepseekProvider")
    private AiProvider aiProvider;

    /** 上下文窗口大小（带入模型的历史消息条数） */
    private static final int CONTEXT_SIZE = 10;

    @Override
    public Map<String, Object> chat(Long userId, String message) {
        // 获取或创建会话
        AiConversation conversation = getOrCreateConversation(userId);

        // 读取生效的 AI 配置
        AiConfig config = aiConfigService.getActive();
        String provider = config != null ? config.getProvider() : "deepseek";
        String modelName = config != null ? config.getModelName() : null;

        // 保存用户消息
        AiMessage userMsg = new AiMessage();
        userMsg.setConversationId(conversation.getId());
        userMsg.setSender("USER");
        userMsg.setContent(message);
        userMsg.setMessageType("TEXT");
        userMsg.setProvider(provider);
        userMsg.setModelName(modelName);
        userMsg.setCreateTime(LocalDateTime.now());
        save(userMsg);

        // 组装上下文（近 N 条历史消息）
        List<Map<String, String>> history = buildHistory(conversation.getId());

        // 调用大模型
        String systemPrompt = config != null && config.getSystemPrompt() != null
                ? config.getSystemPrompt() : "你是智慧农业管理系统的智能助手。";
        String reply = aiProvider.chat(systemPrompt, history, config);

        // 保存 AI 回复
        AiMessage aiMsg = new AiMessage();
        aiMsg.setConversationId(conversation.getId());
        aiMsg.setSender("AI");
        aiMsg.setContent(reply);
        aiMsg.setMessageType("TEXT");
        aiMsg.setProvider(provider);
        aiMsg.setModelName(modelName);
        aiMsg.setCreateTime(LocalDateTime.now());
        save(aiMsg);

        // 更新会话时间
        conversation.setUpdateTime(LocalDateTime.now());
        conversationMapper.updateById(conversation);

        Map<String, Object> result = new HashMap<>();
        result.put("conversationId", conversation.getId());
        result.put("userMessage", userMsg);
        result.put("aiMessage", aiMsg);
        result.put("provider", provider);
        result.put("modelName", modelName);
        return result;
    }

    @Override
    public Page<AiMessage> getHistory(Map<String, Object> params) {
        Integer pageNum = Integer.parseInt(params.getOrDefault("pageNum", "1").toString());
        Integer pageSize = Integer.parseInt(params.getOrDefault("pageSize", "50").toString());
        Long userId = Long.valueOf(params.get("userId").toString());

        AiConversation conversation = getActiveConversation(userId);
        if (conversation == null) {
            return new Page<>(pageNum, pageSize);
        }

        LambdaQueryWrapper<AiMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiMessage::getConversationId, conversation.getId())
               .orderByAsc(AiMessage::getCreateTime);

        return page(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    public void clearHistory(Long userId) {
        AiConversation conversation = getActiveConversation(userId);
        if (conversation != null) {
            conversation.setStatus("CLOSED");
            conversation.setUpdateTime(LocalDateTime.now());
            conversationMapper.updateById(conversation);
        }
    }

    // ==================== 私有方法 ====================

    private AiConversation getActiveConversation(Long userId) {
        LambdaQueryWrapper<AiConversation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiConversation::getUserId, userId)
               .eq(AiConversation::getStatus, "ACTIVE")
               .orderByDesc(AiConversation::getUpdateTime)
               .last("LIMIT 1");
        return conversationMapper.selectOne(wrapper);
    }

    private AiConversation getOrCreateConversation(Long userId) {
        AiConversation conversation = getActiveConversation(userId);
        if (conversation == null) {
            conversation = new AiConversation();
            conversation.setUserId(userId);
            conversation.setTitle("AI 问答会话");
            conversation.setStatus("ACTIVE");
            conversation.setCreateTime(LocalDateTime.now());
            conversation.setUpdateTime(LocalDateTime.now());
            conversationMapper.insert(conversation);
        }
        return conversation;
    }

    /** 取近 N 条历史消息作为上下文（升序） */
    private List<Map<String, String>> buildHistory(Long conversationId) {
        List<AiMessage> recent = list(new LambdaQueryWrapper<AiMessage>()
                .eq(AiMessage::getConversationId, conversationId)
                .orderByDesc(AiMessage::getCreateTime)
                .last("LIMIT " + CONTEXT_SIZE));

        List<Map<String, String>> history = new ArrayList<>();
        for (int i = recent.size() - 1; i >= 0; i--) {
            AiMessage msg = recent.get(i);
            Map<String, String> item = new HashMap<>();
            item.put("role", "USER".equals(msg.getSender()) ? "user" : "assistant");
            item.put("content", msg.getContent());
            history.add(item);
        }
        return history;
    }
}
