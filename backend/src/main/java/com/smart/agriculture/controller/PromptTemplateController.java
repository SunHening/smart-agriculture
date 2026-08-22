package com.smart.agriculture.controller;

import com.smart.agriculture.common.Result;
import com.smart.agriculture.entity.PromptTemplate;
import com.smart.agriculture.service.PromptTemplateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 提示词模板控制器
 */
@RestController
@RequestMapping("/api/prompt-template")
public class PromptTemplateController {

    @Autowired
    private PromptTemplateService promptTemplateService;

    @GetMapping("/list")
    public Result<List<PromptTemplate>> list() {
        return Result.success(promptTemplateService.list());
    }

    @PostMapping
    public Result<?> add(@RequestBody PromptTemplate template) {
        promptTemplateService.save(template);
        return Result.success();
    }

    @PutMapping
    public Result<?> update(@RequestBody PromptTemplate template) {
        promptTemplateService.updateById(template);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        promptTemplateService.removeById(id);
        return Result.success();
    }
}
