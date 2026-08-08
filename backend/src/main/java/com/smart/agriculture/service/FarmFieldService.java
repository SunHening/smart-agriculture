package com.smart.agriculture.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.smart.agriculture.entity.FarmField;

import java.util.List;

public interface FarmFieldService extends IService<FarmField> {
    IPage<FarmField> getFieldPage(Integer pageNum, Integer pageSize, Long userId, String fieldName);
    List<FarmField> getFieldListByUser(Long userId);
}
