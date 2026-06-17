package com.sanoli.financedash;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;
import com.sanoli.financedash.config.AsaasProperties;
import com.sanoli.financedash.config.AuthProperties;
import com.sanoli.financedash.config.MailProperties;
import com.sanoli.financedash.config.RadarAiProperties;
import com.sanoli.financedash.config.RadarDigestProperties;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties({
        AsaasProperties.class,
        AuthProperties.class,
        MailProperties.class,
        RadarAiProperties.class,
        RadarDigestProperties.class
})
public class FinanceDashApplication {

    public static void main(String[] args) {
        SpringApplication.run(FinanceDashApplication.class, args);
    }
}

