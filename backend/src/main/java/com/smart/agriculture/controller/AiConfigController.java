package com.smart.agriculture.controller;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.smart.agriculture.common.Result;
import com.smart.agriculture.entity.AiConfig;
import com.smart.agriculture.service.AiConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * AI 模型/提示词参数配置控制器
 */
@RestController
@RequestMapping("/api/ai-config")
public class AiConfigController {

    @Autowired
    private AiConfigService aiConfigService;

    @GetMapping("/list")
    public Result<List<AiConfig>> list() {
        return Result.success(aiConfigService.list());
    }

    @GetMapping("/active")
    public Result<AiConfig> active() {
        return Result.success(aiConfigService.getActive());
    }

    @PostMapping
    public Result<?> add(@RequestBody AiConfig config) {
        clearActiveIfNeeded(config);
        aiConfigService.save(config);
        return Result.success();
    }

    @PutMapping
    public Result<?> update(@RequestBody AiConfig config) {
        clearActiveIfNeeded(config);
        aiConfigService.updateById(config);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        aiConfigService.removeById(id);
        return Result.success();
    }

    private void clearActiveIfNeeded(AiConfig config) {
        if (config.getIsActive() != null && config.getIsActive() == 1) {
            aiConfigService.update(new LambdaUpdateWrapper<AiConfig>()
                    .ne(config.getId() != null, AiConfig::getId, config.getId())
                    .set(AiConfig::getIsActive, 0));
        }
    }
}
