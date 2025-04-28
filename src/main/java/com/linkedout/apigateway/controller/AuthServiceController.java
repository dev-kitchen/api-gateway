package com.linkedout.apigateway.controller;

import com.linkedout.apigateway.model.ApiResponse;
import com.linkedout.apigateway.service.ResponseHandlerService;
import com.linkedout.apigateway.util.JsonUtils;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;


@RestController
@RequestMapping("/api/auth")
public class AuthServiceController extends BaseServiceController {

	private static final String AUTH_QUEUE = "auth-queue";

	public AuthServiceController(
		RabbitTemplate rabbitTemplate,
		ResponseHandlerService responseHandlerService,
		JsonUtils jsonUtils) {
		super(rabbitTemplate, responseHandlerService, jsonUtils);
	}

	@RequestMapping("/**")
	public Mono<ApiResponse<?>> handleAuthServiceRequest(ServerWebExchange exchange) {
		return processRequest(exchange, AUTH_QUEUE);
	}
}