package com.linkedout.apigateway.config;

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
  public OpenAPI openAPI() {
    return new OpenAPI()
        .info(
            new Info()
                .title("우당턍탕 API 문서")
                .description("일을합시다 일일일")
                .version("v1.0.0")
                .contact(
                    new Contact()
                        .name("조씨")
                        .email("daechan476@gmail.com")
                        .url("https://github.com/yourusername"))
                .license(new License().name("Apache 2.0").url("http://springdoc.org"))).servers(List.of(
				new Server().url("https://eat.r-e.kr")
					.description("홈 네트워크 접속 URL")
			));
  }
}
