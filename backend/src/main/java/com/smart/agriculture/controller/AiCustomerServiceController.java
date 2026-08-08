package com.smart.agriculture.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smart.agriculture.common.Result;
import com.smart.agriculture.entity.AiMessage;
import com.smart.agriculture.service.AiCustomerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 智能客服控制器
 * 接口已预留 AI 服务商接入能力
 */
@RestController
@RequestMapping("/api/customer-service")
public class AiCustomerServiceController {

    @Autowired
    private AiCustomerService aiCustomerService;

    /**
     * 发送消息
     * 请求参数: { "userId": 1, "message": "如何使用面积检测功能？" }
     * 预留 provider 参数用于切换 AI 服务商
     */
    @PostMapping("/chat")
    public Result<Map<String, Object>> chat(@RequestBody Map<String, Object> params) {
        Long userId = Long.valueOf(params.get("userId").toString());
        String message = (String) params.get("message");
        return Result.success(aiCustomerService.chat(userId, message));
    }

    /** 获取历史消息 */
    @GetMapping("/history")
    public Result<Page<AiMessage>> history(@RequestParam Map<String, Object> params) {
        return Result.success(aiCustomerService.getHistory(params));
    }

    /** 清除历史 */
    @DeleteMapping("/history")
    public Result<?> clearHistory(@RequestParam Long userId) {
        aiCustomerService.clearHistory(userId);
        return Result.success("清除成功");
    }
}
