package com.smart.agriculture.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smart.agriculture.entity.ModelConfig;
import com.smart.agriculture.mapper.ModelConfigMapper;
import com.smart.agriculture.service.ModelConfigService;
import org.springframework.stereotype.Service;

@Service
public class ModelConfigServiceImpl extends ServiceImpl<ModelConfigMapper, ModelConfig> implements ModelConfigService {

    @Override
    public ModelConfig getActive() {
        return getOne(new LambdaQueryWrapper<ModelConfig>()
                .eq(ModelConfig::getIsActive, 1)
                .eq(ModelConfig::getStatus, 1)
                .last("LIMIT 1"));
    }
}
