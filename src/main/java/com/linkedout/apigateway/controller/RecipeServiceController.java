package com.linkedout.apigateway.controller;

import com.linkedout.common.dto.ApiResponse;
import com.linkedout.apigateway.service.ResponseHandlerService;
import com.linkedout.apigateway.util.JsonUtils;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 레시피 서비스 관련 요청을 처리하는 컨트롤러
 * <p>
 * 이 컨트롤러는 /api/recipes/** 경로로 들어오는 모든 요청을 처리하고,
 * RabbitMQ를 통해 레시피 서비스로 메시지를 전송합니다.
 */
@RestController
@RequestMapping("/api/recipes")
public class RecipeServiceController extends BaseServiceController {

	// todo delete
	private static final String RECIPE_SERVICE_QUEUE = "recipe-queue";

	public RecipeServiceController(
		RabbitTemplate rabbitTemplate,
		ResponseHandlerService responseHandlerService,
		JsonUtils jsonUtils) {
		super(rabbitTemplate, responseHandlerService, jsonUtils);
	}


	@RequestMapping("/**")
	public Mono<ApiResponse<?>> handleRecipeServiceRequest(ServerWebExchange exchange) {
		return processRequest(exchange, RECIPE_SERVICE_QUEUE);
	}
}