package com.sanoli.financedash.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI financeDashOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("FinanceDash API")
                        .version("v1")
                        .description("API do MVP FinanceDash para controle financeiro de freelancers, MEIs e pequenos negócios."));
    }
}

