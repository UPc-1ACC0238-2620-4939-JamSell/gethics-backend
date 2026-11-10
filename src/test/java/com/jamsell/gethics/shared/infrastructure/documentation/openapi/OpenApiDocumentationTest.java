package com.jamsell.gethics.shared.infrastructure.documentation.openapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * TS04: la documentacion OpenAPI esta disponible en /docs y TODOS los endpoints tienen resumen, una etiqueta y al
 * menos una respuesta documentada ademas de la generica. Corre sobre H2 en memoria (nunca la base compartida).
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:openapi_it;MODE=PostgreSQL;NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false"
})
@AutoConfigureMockMvc
class OpenApiDocumentationTest {

    private static final List<String> METHODS = List.of("get", "post", "put", "patch", "delete");

    @Autowired
    MockMvc mvc;

    @Test
    void swaggerUiIsServedAtDocs() throws Exception {
        mvc.perform(get("/docs")).andExpect(status().isFound())
                .andExpect(redirectedUrl("/swagger-ui/index.html"));
    }

    @Test
    void everyEndpointIsDocumentedWithSummaryTagAndResponses() throws Exception {
        var body = mvc.perform(get("/v3/api-docs")).andExpect(status().isOk()).andReturn().getResponse()
                .getContentAsString();
        JsonNode paths = new ObjectMapper().readTree(body).get("paths");
        var undocumented = new ArrayList<String>();
        var operations = 0;
        for (var path : paths.properties()) {
            for (var method : METHODS) {
                var operation = path.getValue().get(method);
                if (operation == null) {
                    continue;
                }
                operations++;
                var summary = operation.path("summary").asText("");
                var hasTag = operation.path("tags").size() > 0;
                var responses = operation.path("responses");
                var documentedResponses = responses.size() > 0 && !responses.has("default");
                if (summary.isBlank() || !hasTag || !documentedResponses) {
                    undocumented.add(method.toUpperCase() + " " + path.getKey());
                }
            }
        }
        assertTrue(operations > 0);
        assertEquals(List.of(), undocumented, "Endpoints sin documentar por completo");
    }
}
