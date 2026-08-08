package com.smart.agriculture.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.smart.agriculture.common.Result;
import com.smart.agriculture.entity.User;
import com.smart.agriculture.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("/info")
    public Result<User> getUserInfo() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return Result.success(userService.getUserByUsername(username));
    }

    @PutMapping("/update")
    public Result<?> updateUser(@RequestBody User user) {
        userService.updateById(user);
        return Result.success();
    }

    @PutMapping("/password")
    public Result<?> updatePassword(@RequestBody Map<String, String> params) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userService.getUserByUsername(username);
        userService.updatePassword(user.getId(), params.get("oldPassword"), params.get("newPassword"));
        return Result.success();
    }

    @GetMapping("/page")
    public Result<IPage<User>> getUserPage(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String role) {
        return Result.success(userService.getUserPage(pageNum, pageSize, username, role));
    }

    @PutMapping("/status/{id}")
    public Result<?> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        User user = new User();
        user.setId(id);
        user.setStatus(status);
        userService.updateById(user);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<?> deleteUser(@PathVariable Long id) {
        userService.removeById(id);
        return Result.success();
    }
}
