package com.linkedout.apigateway.controller;

import com.linkedout.common.messaging.ApiMessageClient;
import com.linkedout.common.messaging.ServiceIdentifier;
import com.linkedout.common.messaging.ServiceMessageResponseHandler;
import com.linkedout.common.model.dto.BaseApiResponse;
import com.linkedout.common.util.JsonUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
@Tag(name = "Auth", description = "인증 서비스 앤드포인트")
@RestController
@RequestMapping("/api/auth")
public class AuthServiceController extends ApiMessageClient {

	public AuthServiceController(
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

	//	@PreAuthorize("hasRole('ADMIN')")
	//	@PreAuthorize("hasRole('USER')")
	@GetMapping("/testToken")
	public Mono<ResponseEntity<BaseApiResponse<String>>> testToken(ServerWebExchange exchange) {
		return sendMessage(exchange);
	}

	@Operation(
		summary = "Auth 서비스 상태 확인",
		description = "Auth 마이크로서비스의 상태를 체크하여 서비스의 정상 동작 여부를 확인합니다.",
		responses = {
			@ApiResponse(
				responseCode = "200",
				description = "서비스가 정상적으로 동작중",
				content =
				@Content(
					mediaType = "application/json",
					schema = @Schema(implementation = BaseApiResponse.class))),
		})
	@GetMapping("/health")
	public Mono<ResponseEntity<BaseApiResponse<String>>> healthCheck(
		ServerWebExchange exchange) {
		return sendMessage(exchange);
	}

}
