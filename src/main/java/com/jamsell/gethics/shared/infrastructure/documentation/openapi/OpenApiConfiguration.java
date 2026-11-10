package com.jamsell.gethics.shared.infrastructure.documentation.openapi;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {

    @Bean
    public OpenAPI gethicsOpenApi() {
        return new OpenAPI()
            .info(new Info()
                .title("Gethics API")
                .description("API REST para la gestión ganadera de Gethics. Los endpoints /api/v1 aún no exigen token JWT (TS08 pendiente del equipo): hoy el dueño o el veterinario se indican como parámetro o en el cuerpo de la petición.")
                .version("1.0.0"))
            .components(new Components().addSecuritySchemes("bearerAuth", new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")))
            .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
}
