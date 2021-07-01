package com.activity6.www.workflow.config;

import org.activiti.engine.impl.cfg.IdGenerator;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class MyActivitiIDGenerator implements IdGenerator {
    @Override
    public String getNextId() {
        return UUID.randomUUID().toString();
    }
}