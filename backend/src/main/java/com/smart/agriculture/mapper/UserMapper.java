package com.smart.agriculture.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.smart.agriculture.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
