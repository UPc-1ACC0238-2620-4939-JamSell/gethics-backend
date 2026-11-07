package com.jamsell.gethics.shared.infrastructure.documentation.openapi;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {

    @Bean
    public OpenAPI gethicsOpenApi() {
        return new OpenAPI()
            .info(new Info()
                .title("Gethics API")
                .description("API REST para la gestión ganadera de Gethics.")
                .version("1.0.0"));
    }
}
