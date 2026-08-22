package com.smart.agriculture.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.smart.agriculture.entity.KnowledgeBase;

import java.util.List;

public interface KnowledgeBaseService extends IService<KnowledgeBase> {
    /** 按关键词检索知识库 */
    List<KnowledgeBase> search(String keyword);
}
