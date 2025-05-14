package com.linkedout.apigateway.controller;

import com.linkedout.common.dto.BaseApiResponse;
import com.linkedout.common.dto.HealthResponse;
import com.linkedout.common.messaging.ApiMessageClient;
import com.linkedout.common.messaging.ServiceIdentifier;
import com.linkedout.common.messaging.ServiceMessageResponseHandler;
import com.linkedout.common.util.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 레시피 서비스 관련 요청을 처리하는 컨트롤러
 *
 * <p>이 컨트롤러는 /api/recipes/** 경로로 들어오는 모든 요청을 처리하고, RabbitMQ를 통해 레시피 서비스로 메시지를 전송합니다.
 */
@Slf4j
@RestController
@RequestMapping("/api/recipes")
public class RecipeServiceController extends ApiMessageClient {

	public RecipeServiceController(
		RabbitTemplate rabbitTemplate,
		JsonUtils jsonUtils,
		ServiceMessageResponseHandler serviceMessageResponseHandler,
		ServiceIdentifier serviceIdentifier) {
		super(
			rabbitTemplate,
			jsonUtils,
			serviceMessageResponseHandler,
			serviceIdentifier);
	}

	@GetMapping("/health")
	public Mono<ResponseEntity<BaseApiResponse<HealthResponse>>> health(ServerWebExchange exchange) {
		return sendMessage(exchange);
	}

	//	@RequestMapping("/**")
	//	public Mono<ResponseEntity<ApiResponse<?>>> handleRecipeServiceRequest(ServerWebExchange
	// exchange) {
	//		return processRequest(exchange, RECIPE_QUEUE);
	//	}
}
