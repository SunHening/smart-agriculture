package com.smart.agriculture.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smart.agriculture.entity.PromptTemplate;
import com.smart.agriculture.mapper.PromptTemplateMapper;
import com.smart.agriculture.service.PromptTemplateService;
import org.springframework.stereotype.Service;

@Service
public class PromptTemplateServiceImpl extends ServiceImpl<PromptTemplateMapper, PromptTemplate> implements PromptTemplateService {
}
