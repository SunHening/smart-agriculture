package com.smart.agriculture.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smart.agriculture.entity.AiConversation;
import com.smart.agriculture.entity.AiMessage;
import com.smart.agriculture.mapper.AiConversationMapper;
import com.smart.agriculture.mapper.AiMessageMapper;
import com.smart.agriculture.service.AiCustomerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 智能客服服务实现
 * 当前为规则匹配模式，已预留 AI 服务商接入接口
 */
@Service
public class AiCustomerServiceImpl extends ServiceImpl<AiMessageMapper, AiMessage> implements AiCustomerService {

    @Autowired
    private AiConversationMapper conversationMapper;

    // ========== AI服务商预留接口 ==========
    // TODO: 替换为真实AI服务商调用
    // 可选接入:
    //   1. DeepSeek API  - AiProvider.deepseek()
    //   2. OpenAI API    - AiProvider.openai()
    //   3. 本地模型       - AiProvider.local()

    @Override
    public Map<String, Object> chat(Long userId, String message) {
        // 获取或创建会话
        AiConversation conversation = getOrCreateConversation(userId);

        // 保存用户消息
        AiMessage userMsg = new AiMessage();
        userMsg.setConversationId(conversation.getId());
        userMsg.setSender("USER");
        userMsg.setContent(message);
        userMsg.setMessageType("TEXT");
        userMsg.setProvider("default");
        userMsg.setCreateTime(LocalDateTime.now());
        save(userMsg);

        // 生成AI回复（当前为规则匹配，预留AI服务商接入）
        String reply = generateReply(message);

        // 保存AI回复
        AiMessage aiMsg = new AiMessage();
        aiMsg.setConversationId(conversation.getId());
        aiMsg.setSender("AI");
        aiMsg.setContent(reply);
        aiMsg.setMessageType("TEXT");
        aiMsg.setProvider("default");
        aiMsg.setCreateTime(LocalDateTime.now());
        save(aiMsg);

        // 更新会话时间
        conversation.setUpdateTime(LocalDateTime.now());
        conversationMapper.updateById(conversation);

        Map<String, Object> result = new HashMap<>();
        result.put("conversationId", conversation.getId());
        result.put("userMessage", userMsg);
        result.put("aiMessage", aiMsg);
        return result;
    }

    @Override
    public Page<AiMessage> getHistory(Map<String, Object> params) {
        Integer pageNum = Integer.parseInt(params.getOrDefault("pageNum", "1").toString());
        Integer pageSize = Integer.parseInt(params.getOrDefault("pageSize", "50").toString());
        Long userId = Long.valueOf(params.get("userId").toString());

        // 获取用户会话
        LambdaQueryWrapper<AiConversation> convWrapper = new LambdaQueryWrapper<>();
        convWrapper.eq(AiConversation::getUserId, userId)
                    .eq(AiConversation::getStatus, "ACTIVE")
                    .orderByDesc(AiConversation::getUpdateTime)
                    .last("LIMIT 1");
        AiConversation conversation = conversationMapper.selectOne(convWrapper);

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
        LambdaQueryWrapper<AiConversation> convWrapper = new LambdaQueryWrapper<>();
        convWrapper.eq(AiConversation::getUserId, userId)
                    .eq(AiConversation::getStatus, "ACTIVE");
        AiConversation conversation = conversationMapper.selectOne(convWrapper);
        if (conversation != null) {
            conversation.setStatus("CLOSED");
            conversation.setUpdateTime(LocalDateTime.now());
            conversationMapper.updateById(conversation);
        }
    }

    // ==================== 私有方法 ====================

    private AiConversation getOrCreateConversation(Long userId) {
        LambdaQueryWrapper<AiConversation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiConversation::getUserId, userId)
               .eq(AiConversation::getStatus, "ACTIVE")
               .orderByDesc(AiConversation::getUpdateTime)
               .last("LIMIT 1");
        AiConversation conversation = conversationMapper.selectOne(wrapper);

        if (conversation == null) {
            conversation = new AiConversation();
            conversation.setUserId(userId);
            conversation.setTitle("智能客服会话");
            conversation.setStatus("ACTIVE");
            conversation.setCreateTime(LocalDateTime.now());
            conversation.setUpdateTime(LocalDateTime.now());
            conversationMapper.insert(conversation);
        }
        return conversation;
    }

    /**
     * 规则匹配回复（临时代替AI模型）
     * TODO: 替换为 AiProvider 调用
     */
    private String generateReply(String message) {
        if (message.contains("面积") || message.contains("检测")) {
            return "您好！本系统支持通过遥感影像和无人机航拍图像自动检测农田面积。您可以在「影像检测」模块上传图片，系统将自动识别农田区域并计算面积。";
        }
        if (message.contains("遥感") || message.contains("卫星")) {
            return "遥感影像可以从Landsat、Sentinel等卫星获取，系统支持GeoTIFF格式的遥感影像。上传后将通过深度学习模型自动提取农田边界并计算面积。";
        }
        if (message.contains("无人机") || message.contains("航拍")) {
            return "无人机航拍图像可以通过DJI等无人机设备获取。上传后系统会利用计算机视觉技术自动识别农田区域，提取边界并精确计算面积。";
        }
        if (message.contains("边界") || message.contains("轮廓")) {
            return "边界提取基于深度学习语义分割模型（如U-Net、DeepLabV3+），从影像中自动识别农田轮廓并输出GeoJSON格式的多边形边界数据。";
        }
        if (message.contains("精度") || message.contains("误差")) {
            return "系统面积检测精度受影像分辨率（GSD）和模型性能影响。建议使用分辨率优于1m/pixel的遥感影像，检测误差通常在5%以内。";
        }
        return "您好！我是智慧农业智能客服，可以为您解答关于农田面积检测、遥感影像处理、无人机航拍分析等方面的问题。请随时提问！";
    }
}
