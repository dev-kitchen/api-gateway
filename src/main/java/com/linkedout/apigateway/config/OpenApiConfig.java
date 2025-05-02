package com.linkedout.apigateway.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

  @Bean
  public OpenAPI openAPI() {
    return new OpenAPI()
        .info(
            new Info()
                .title("WebFlux API 문서")
                .description("Spring WebFlux로 만든 API 문서입니다")
                .version("v1.0.0")
                .contact(
                    new Contact()
                        .name("개발자 이름")
                        .email("email@example.com")
                        .url("https://github.com/yourusername"))
                .license(new License().name("Apache 2.0").url("http://springdoc.org")));
  }
}
