package com.linkedout.apigateway.controller;

import com.linkedout.common.messaging.ApiMessageClient;
import com.linkedout.common.messaging.ServiceIdentifier;
import com.linkedout.common.messaging.ServiceMessageResponseHandler;
import com.linkedout.common.model.dto.BaseApiResponse;
import com.linkedout.common.model.dto.recipe.RecipeDTO;
import com.linkedout.common.model.dto.recipe.request.RecipeCreateDTO;
import com.linkedout.common.model.schema.RecipeResponseSchema;
import com.linkedout.common.util.JsonUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 레시피 서비스 관련 요청을 처리하는 컨트롤러
 *
 * <p>이 컨트롤러는 /api/recipes/** 경로로 들어오는 모든 요청을 처리하고, RabbitMQ를 통해 레시피 서비스로 메시지를 전송합니다.
 */
@Slf4j
@Tag(name = "Recipe", description = "레시피 서비스 앤드포인트")
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

	@Operation(
		summary = "특정 레시피 조회",
		description = "특정 ID의 레시피 정보를 조회합니다.",
		responses = {
			@ApiResponse(
				responseCode = "200",
				description = "레시피 조회 성공",
				content = @Content(
					mediaType = "application/json",
					schema = @Schema(implementation = RecipeResponseSchema.class)
				)
			),
		}
	)
	@GetMapping("/{id}")
	public Mono<ResponseEntity<BaseApiResponse<RecipeDTO>>> getRecipe(
		@Parameter(description = "레시피 ID", required = true)
		@PathVariable String id,
		ServerWebExchange exchange) {
		return sendMessage(exchange);
	}

	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public Mono<ResponseEntity<BaseApiResponse<RecipeDTO>>> createRecipe(
		ServerWebExchange exchange
	) {
// 멀티폼으로 받아서 처리하려면 여기에서 미리 http 컨텍스트를 수정해야하는데 그게 불가능해서..
		// 지금 구조상 컨텍스트 자체를 핸들링하고있어서, 이러면 post나 put 요청을 위한 메시지클라이언트를 만들어얗..
		return sendMessage(exchange);
	}
}
