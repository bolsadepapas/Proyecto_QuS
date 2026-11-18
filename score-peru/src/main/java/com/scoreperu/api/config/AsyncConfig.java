package com.scoreperu.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        // En Java 21, Executors.newVirtualThreadPerTaskExecutor() permite manejar
        // miles de tareas concurrentes con latencia bloqueante de red de forma óptima
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}

