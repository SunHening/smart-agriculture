package com.smart.agriculture.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smart.agriculture.entity.AiConfig;
import com.smart.agriculture.mapper.AiConfigMapper;
import com.smart.agriculture.service.AiConfigService;
import org.springframework.stereotype.Service;

@Service
public class AiConfigServiceImpl extends ServiceImpl<AiConfigMapper, AiConfig> implements AiConfigService {

    @Override
    public AiConfig getActive() {
        return getOne(new LambdaQueryWrapper<AiConfig>()
                .eq(AiConfig::getIsActive, 1)
                .eq(AiConfig::getStatus, 1)
                .last("LIMIT 1"));
    }
}
