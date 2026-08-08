package com.smart.agriculture.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.smart.agriculture.common.Result;
import com.smart.agriculture.entity.FarmField;
import com.smart.agriculture.entity.User;
import com.smart.agriculture.service.FarmFieldService;
import com.smart.agriculture.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/field")
public class FarmFieldController {

    @Autowired
    private FarmFieldService farmFieldService;

    @Autowired
    private UserService userService;

    @GetMapping("/page")
    public Result<IPage<FarmField>> getFieldPage(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String fieldName) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userService.getUserByUsername(username);
        Long userId = "ADMIN".equals(user.getRole()) ? null : user.getId();
        return Result.success(farmFieldService.getFieldPage(pageNum, pageSize, userId, fieldName));
    }

    @GetMapping("/list")
    public Result<List<FarmField>> getFieldList() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userService.getUserByUsername(username);
        if ("ADMIN".equals(user.getRole())) {
            return Result.success(farmFieldService.list());
        }
        return Result.success(farmFieldService.getFieldListByUser(user.getId()));
    }

    @PostMapping
    public Result<?> addField(@RequestBody FarmField field) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userService.getUserByUsername(username);
        field.setUserId(user.getId());
        farmFieldService.save(field);
        return Result.success();
    }

    @PutMapping
    public Result<?> updateField(@RequestBody FarmField field) {
        farmFieldService.updateById(field);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<?> deleteField(@PathVariable Long id) {
        farmFieldService.removeById(id);
        return Result.success();
    }

    @GetMapping("/{id}")
    public Result<FarmField> getField(@PathVariable Long id) {
        return Result.success(farmFieldService.getById(id));
    }
}
