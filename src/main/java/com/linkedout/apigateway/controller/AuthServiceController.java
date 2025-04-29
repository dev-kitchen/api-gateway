package com.linkedout.apigateway.controller;

import com.linkedout.common.constant.RabbitMQConstants;
import com.linkedout.common.dto.ApiResponse;
import com.linkedout.apigateway.service.ResponseHandlerService;
import com.linkedout.apigateway.util.JsonUtils;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;


@RestController
@RequestMapping("/api/auth")
public class AuthServiceController extends BaseServiceController {


	public AuthServiceController(
		RabbitTemplate rabbitTemplate,
		ResponseHandlerService responseHandlerService,
		JsonUtils jsonUtils) {
		super(rabbitTemplate, responseHandlerService, jsonUtils);
	}


	/**
	 * 인증 서비스의 상태를 확인하기 위한 헬스 체크 요청을 처리합니다.
	 * 요청을 해당 RabbitMQ 큐로 전송하고 응답을 처리합니다.
	 *
	 * @param exchange HTTP 요청 정보에 접근할 수 있는 현재 요청/응답 컨텍스트
	 * @return 인증 서비스의 상태 정보를 포함한 {@link Mono} API 응답
	 */
	@GetMapping("/health")
	public Mono<ApiResponse<?>> health(ServerWebExchange exchange) {
		return processRequest(exchange, RabbitMQConstants.AUTH_QUEUE);
	}

	@RequestMapping("/**")
	public Mono<ApiResponse<?>> handleAuthServiceRequest(ServerWebExchange exchange) {
		return processRequest(exchange, RabbitMQConstants.AUTH_QUEUE);
	}
}