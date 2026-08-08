package com.smart.agriculture.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smart.agriculture.entity.FarmField;
import com.smart.agriculture.mapper.FarmFieldMapper;
import com.smart.agriculture.service.FarmFieldService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class FarmFieldServiceImpl extends ServiceImpl<FarmFieldMapper, FarmField> implements FarmFieldService {

    @Override
    public IPage<FarmField> getFieldPage(Integer pageNum, Integer pageSize, Long userId, String fieldName) {
        LambdaQueryWrapper<FarmField> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(userId != null, FarmField::getUserId, userId);
        wrapper.like(StringUtils.hasText(fieldName), FarmField::getFieldName, fieldName);
        wrapper.orderByDesc(FarmField::getCreateTime);
        return page(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    public List<FarmField> getFieldListByUser(Long userId) {
        return list(new LambdaQueryWrapper<FarmField>().eq(FarmField::getUserId, userId));
    }
}
