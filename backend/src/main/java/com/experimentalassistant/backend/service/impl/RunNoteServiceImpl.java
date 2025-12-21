package com.experimentalassistant.backend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.experimentalassistant.backend.entity.RunNote;
import com.experimentalassistant.backend.mapper.RunNoteMapper;
import com.experimentalassistant.backend.service.RunNoteService;
import org.springframework.stereotype.Service;

@Service
public class RunNoteServiceImpl extends ServiceImpl<RunNoteMapper, RunNote> implements RunNoteService {
}
