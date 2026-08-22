package com.smart.agriculture.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.smart.agriculture.entity.AiConfig;

public interface AiConfigService extends IService<AiConfig> {
    /** 获取当前生效配置 */
    AiConfig getActive();
}
