package com.smart.agriculture.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smart.agriculture.entity.Dataset;
import com.smart.agriculture.mapper.DatasetMapper;
import com.smart.agriculture.service.DatasetService;
import org.springframework.stereotype.Service;

@Service
public class DatasetServiceImpl extends ServiceImpl<DatasetMapper, Dataset> implements DatasetService {
}
