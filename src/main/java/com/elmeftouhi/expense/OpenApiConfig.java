package com.elmeftouhi.expense;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI expenseApiOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Expense API")
                        .description("API documentation for expense.wiki backend services")
                        .version("v1")
                        .contact(new Contact().name("Expense API Team"))
                        .license(new License().name("Proprietary")));
    }
}

