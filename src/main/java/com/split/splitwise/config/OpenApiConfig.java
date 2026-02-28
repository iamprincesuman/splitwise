package com.split.splitwise.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Splitwise Clone API")
                        .version("1.0.0")
                        .description("""
                                Production-grade expense sharing backend API.
                                
                                Features:
                                - User management with email validation
                                - Group management with member handling
                                - Expense creation with EQUAL and EXACT split types
                                - Balance calculation per group
                                - Optimized debt simplification algorithm (O(n log n))
                                """)
                        .contact(new Contact()
                                .name("Prince Suman")
                                .url("https://github.com/iamprincesuman"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Local Development")
                ));
    }
}
