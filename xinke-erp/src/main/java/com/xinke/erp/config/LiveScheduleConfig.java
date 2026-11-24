package com.xinke.erp.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@ConditionalOnProperty(prefix = "live.scheduling", name = "enabled", havingValue = "true", matchIfMissing = true)
public class LiveScheduleConfig
{
}
