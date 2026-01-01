package com.experimentalassistant.backend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.experimentalassistant.backend.entity.Domain;
import com.experimentalassistant.backend.mapper.DomainMapper;
import com.experimentalassistant.backend.service.DomainService;
import org.springframework.stereotype.Service;

@Service
public class DomainServiceImpl extends ServiceImpl<DomainMapper, Domain> implements DomainService {
}
