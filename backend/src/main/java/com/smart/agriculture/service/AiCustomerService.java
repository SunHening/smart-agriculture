package com.smart.agriculture.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.smart.agriculture.entity.AiMessage;

import java.util.Map;

/**
 * 智能客服服务接口
 * 预留 AI 服务商接入能力
 */
public interface AiCustomerService extends IService<AiMessage> {

    /** 发送消息并获取AI回复 */
    Map<String, Object> chat(Long userId, String message);

    /** 获取历史消息 */
    Page<AiMessage> getHistory(Map<String, Object> params);

    /** 清除历史 */
    void clearHistory(Long userId);
}
