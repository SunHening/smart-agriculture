package com.smart.agriculture.controller;

import com.smart.agriculture.common.Result;
import com.smart.agriculture.entity.Dataset;
import com.smart.agriculture.service.DatasetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 遥感/检测数据集控制器
 */
@RestController
@RequestMapping("/api/dataset")
public class DatasetController {

    @Autowired
    private DatasetService datasetService;

    @GetMapping("/list")
    public Result<List<Dataset>> list() {
        return Result.success(datasetService.list());
    }

    @PostMapping
    public Result<?> add(@RequestBody Dataset dataset) {
        datasetService.save(dataset);
        return Result.success();
    }

    @PutMapping
    public Result<?> update(@RequestBody Dataset dataset) {
        datasetService.updateById(dataset);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<?> delete(@PathVariable Long id) {
        datasetService.removeById(id);
        return Result.success();
    }
}
