package com.linkedout.apigateway.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.linkedout.common.exception.ErrorResponseBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {
	@Bean
	public ErrorResponseBuilder errorResponseBuilder(ObjectMapper objectMapper) {
		return new ErrorResponseBuilder(objectMapper);
	}
}
