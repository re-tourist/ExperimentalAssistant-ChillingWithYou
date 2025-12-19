package com.experimentalassistant.backend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.experimentalassistant.backend.entity.Tag;
import com.experimentalassistant.backend.mapper.TagMapper;
import com.experimentalassistant.backend.service.TagService;
import org.springframework.stereotype.Service;

@Service
public class TagServiceImpl extends ServiceImpl<TagMapper, Tag> implements TagService {
}
