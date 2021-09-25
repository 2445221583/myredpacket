package com.wcr.redpacket.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;
/**
 * 注入配置的线程池
 * */
@Configuration
public class ThreadPollConfig {
    @Bean
    public ThreadPoolTaskExecutor threadPoolExecutor(){
        ThreadPoolTaskExecutor threadPoolExecutor=new ThreadPoolTaskExecutor();
        threadPoolExecutor.setCorePoolSize(4);
        threadPoolExecutor.setMaxPoolSize(8);
        threadPoolExecutor.setKeepAliveSeconds(3000);
        threadPoolExecutor.setQueueCapacity(2000);
        return threadPoolExecutor;
    }
}
