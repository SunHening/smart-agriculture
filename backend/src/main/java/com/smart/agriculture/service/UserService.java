package com.smart.agriculture.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.smart.agriculture.entity.User;

import java.util.Map;

public interface UserService extends IService<User> {
    Map<String, Object> login(String username, String password);
    User register(User user);
    IPage<User> getUserPage(Integer pageNum, Integer pageSize, String username, String role);
    User getUserByUsername(String username);
    void updatePassword(Long userId, String oldPassword, String newPassword);
}
