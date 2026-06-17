package com.sanoli.financedash.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class RadarConfig {

    @Bean
    public Clock radarClock(@Value("${app.radar.timezone:America/Sao_Paulo}") String timezone) {
        return Clock.system(ZoneId.of(timezone));
    }
}
