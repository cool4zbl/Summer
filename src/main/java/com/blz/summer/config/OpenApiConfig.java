package com.blz.summer.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springdoc.core.models.GroupedOpenApi;

@OpenAPIDefinition(
        info = @Info(
                title = "Summer API",
                version = "v1",
                description = "API for managing like counters",
                contact = @Contact(name = "Support", email = "binliu.zhang@gmail.com"),
                license = @License(name = "Apache 2.0", url = "https://www.apache.org/licenses/LICENSE-2.0")
        ),
        servers = {
                @Server(url = "http://localhost:8080", description = "Local Dev")
        }
)
@Configuration
public class OpenApiConfig {
    // Group endpoints under a named group for versioning
    @Bean
    public GroupedOpenApi v1Api() {
        return GroupedOpenApi.builder()
                .group("v1")
                .pathsToMatch("/v1/**")
                .build();
    }
}
