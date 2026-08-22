package com.smart.agriculture.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.smart.agriculture.entity.ModelConfig;

public interface ModelConfigService extends IService<ModelConfig> {
    /** 获取当前生效配置 */
    ModelConfig getActive();
}
