package com.experimentalassistant.backend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.experimentalassistant.backend.entity.MetricDef;
import com.experimentalassistant.backend.mapper.MetricDefMapper;
import com.experimentalassistant.backend.service.MetricDefService;
import org.springframework.stereotype.Service;

@Service
public class MetricDefServiceImpl extends ServiceImpl<MetricDefMapper, MetricDef> implements MetricDefService {
}
