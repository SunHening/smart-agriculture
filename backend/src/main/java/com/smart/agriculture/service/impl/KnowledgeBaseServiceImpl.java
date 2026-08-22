package com.smart.agriculture.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smart.agriculture.entity.KnowledgeBase;
import com.smart.agriculture.mapper.KnowledgeBaseMapper;
import com.smart.agriculture.service.KnowledgeBaseService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class KnowledgeBaseServiceImpl extends ServiceImpl<KnowledgeBaseMapper, KnowledgeBase> implements KnowledgeBaseService {

    @Override
    public List<KnowledgeBase> search(String keyword) {
        LambdaQueryWrapper<KnowledgeBase> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgeBase::getStatus, 1);
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(KnowledgeBase::getQuestion, keyword)
                    .or().like(KnowledgeBase::getAnswer, keyword)
                    .or().like(KnowledgeBase::getCategory, keyword));
        }
        wrapper.orderByDesc(KnowledgeBase::getCreateTime);
        return list(wrapper);
    }
}
