package com.linkedout.apigateway.controller;

import com.linkedout.common.messaging.ApiMessageClient;
import com.linkedout.common.messaging.ServiceIdentifier;
import com.linkedout.common.messaging.ServiceMessageResponseHandler;
import com.linkedout.common.model.dto.BaseApiResponse;
import com.linkedout.common.model.dto.HealthResponse;
import com.linkedout.common.model.dto.recipe.RecipeDTO;
import com.linkedout.common.util.JsonUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

	@Operation(
		summary = "특정 레시피 조회",
		description = "특정 ID의 레시피 정보를 조회합니다.",
		responses = {
			@ApiResponse(
				responseCode = "200",
				description = "레시피 조회 성공",
				content = @Content(
					mediaType = "application/json",
					schema = @Schema(implementation = BaseApiResponse.class, subTypes = {RecipeDTO.class})
				)
			),
			@ApiResponse(
				responseCode = "200",
				description = "레시피를 찾을 수 없음",
				content = @Content(
					mediaType = "application/json",
					schema = @Schema(implementation = BaseApiResponse.class, subTypes = {})
				)
			),
			@ApiResponse(
				responseCode = "500",
				description = "서버 에러",
				content = @Content(
					mediaType = "application/json",
					schema = @Schema(implementation = BaseApiResponse.class)
				)
			)
		}
	)
	@GetMapping("/{id}")
	public Mono<ResponseEntity<BaseApiResponse<RecipeDTO>>> getRecipe(
		@Parameter(description = "레시피 ID", required = true)
		@PathVariable String id,
		ServerWebExchange exchange) {
		return sendMessage(exchange);
	}
}
