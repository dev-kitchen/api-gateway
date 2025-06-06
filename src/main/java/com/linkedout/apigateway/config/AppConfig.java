package com.linkedout.apigateway.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.linkedout.common.exception.ErrorResponseBuilder;
import com.linkedout.common.messaging.ServiceIdentifier;
import com.linkedout.common.messaging.ServiceMessageResponseHandler;
import com.linkedout.common.util.JsonUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;

/**
 * 스프링이 관리하는 빈들을 정의하는 어플리케이션 설정 클래스입니다.
 *
 * <p>이 클래스는 {@code @Configuration} 어노테이션이 적용되어 있어서 스프링 프레임워크가 관리하는 빈 정의들을 포함하고 있음을 나타냅니다.
 */
@Configuration
public class AppConfig {

	@Value("${service.name}")
	private String serviceName;

	@Bean
	public ServiceIdentifier serviceIdentifier() {
		return new ServiceIdentifier(serviceName);
	}

	@Bean
	public JsonUtils jsonUtils(ObjectMapper objectMapper) {
		return new JsonUtils(objectMapper);
	}

	@Bean
	public ErrorResponseBuilder errorResponseBuilder(ObjectMapper objectMapper) {
		return new ErrorResponseBuilder(objectMapper);
	}


	@Bean
	public ServiceMessageResponseHandler serviceMessageResponseHandler(
		ReactiveRedisTemplate<String, String> reactiveRedisTemplate,
		ReactiveRedisConnectionFactory connectionFactory,
		ObjectMapper objectMapper,
		ServiceIdentifier serviceIdentifier) {
		return new ServiceMessageResponseHandler(
			reactiveRedisTemplate, connectionFactory, objectMapper, serviceIdentifier);
	}
}
