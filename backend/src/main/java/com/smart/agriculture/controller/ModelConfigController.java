package com.smart.agriculture.controller;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.smart.agriculture.common.Result;
import com.smart.agriculture.entity.ModelConfig;
import com.smart.agriculture.service.ModelConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 检测模型/参数配置控制器
 */
@RestController
@RequestMapping("/api/model-config")
public class ModelConfigController {

    @Autowired
    private ModelConfigService modelConfigService;

    @GetMapping("/list")
    public Result<List<ModelConfig>> list() {
        return Result.success(modelConfigService.list());
    }

    @GetMapping("/active")
    public Result<ModelConfig> active() {
        return Result.success(modelConfigService.getActive());
    }

    @PostMapping
    public Result<?> add(@RequestBody ModelConfig config) {
        clearActiveIfNeeded(config);
        modelConfigService.save(config);
        return Result.success();
    }

    @PutMapping
    public Result<?> update(@RequestBody ModelConfig config) {
        clearActiveIfNeeded(config);
        modelConfigService.updateById(config);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        modelConfigService.removeById(id);
        return Result.success();
    }

    /** 当设置为生效配置时，取消其它配置的生效状态 */
    private void clearActiveIfNeeded(ModelConfig config) {
        if (config.getIsActive() != null && config.getIsActive() == 1) {
            modelConfigService.update(new LambdaUpdateWrapper<ModelConfig>()
                    .ne(config.getId() != null, ModelConfig::getId, config.getId())
                    .set(ModelConfig::getIsActive, 0));
        }
    }
}
