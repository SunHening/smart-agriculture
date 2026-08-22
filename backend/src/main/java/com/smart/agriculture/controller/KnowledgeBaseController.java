package com.smart.agriculture.controller;

import com.smart.agriculture.common.Result;
import com.smart.agriculture.entity.KnowledgeBase;
import com.smart.agriculture.service.KnowledgeBaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 知识库/常见问题控制器
 */
@RestController
@RequestMapping("/api/knowledge")
public class KnowledgeBaseController {

    @Autowired
    private KnowledgeBaseService knowledgeBaseService;

    @GetMapping("/list")
    public Result<List<KnowledgeBase>> list() {
        return Result.success(knowledgeBaseService.list());
    }

    @GetMapping("/search")
    public Result<List<KnowledgeBase>> search(@RequestParam(required = false) String keyword) {
        return Result.success(knowledgeBaseService.search(keyword));
    }

    @PostMapping
    public Result<?> add(@RequestBody KnowledgeBase kb) {
        knowledgeBaseService.save(kb);
        return Result.success();
    }

    @PutMapping
    public Result<?> update(@RequestBody KnowledgeBase kb) {
        knowledgeBaseService.updateById(kb);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        knowledgeBaseService.removeById(id);
        return Result.success();
    }
}
