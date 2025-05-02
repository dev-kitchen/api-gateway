package com.linkedout.apigateway.controller;

import com.linkedout.common.constant.RabbitMQConstants;
import com.linkedout.common.dto.BaseApiResponse;
import com.linkedout.apigateway.service.MessageResponseHandlerService;
import com.linkedout.apigateway.util.JsonUtils;
import com.linkedout.common.dto.HealthResponse;
import com.linkedout.common.dto.auth.oauth.google.GoogleOAuthRequest;
import com.linkedout.common.dto.auth.oauth.google.GoogleOAuthResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;


@RestController
@RequestMapping("/api/auth")
public class AuthServiceController extends BaseServiceController {


	public AuthServiceController(
		RabbitTemplate rabbitTemplate,
		MessageResponseHandlerService messageResponseHandlerService,
		JsonUtils jsonUtils) {
		super(rabbitTemplate, messageResponseHandlerService, jsonUtils);
	}


	@Operation(
		summary = "인증 서비스 상태 확인",
		description = "인증 마이크로서비스의 상태를 체크하여 서비스의 정상 동작 여부를 확인합니다.",
		responses = {
			@ApiResponse(
				responseCode = "200",
				description = "서비스가 정상적으로 동작중",
				content = @Content(
					mediaType = "application/json",
					schema = @Schema(implementation = BaseApiResponse.class)
				)
			),
			@ApiResponse(
				responseCode = "503",
				description = "서비스 이용 불가",
				content = @Content(
					mediaType = "application/json",
					schema = @Schema(implementation = BaseApiResponse.class)
				)
			)
		}
	)
	@GetMapping("/health")
	public Mono<ResponseEntity<BaseApiResponse<HealthResponse>>> healthCheck(ServerWebExchange exchange) {
		return processRequest(exchange, RabbitMQConstants.AUTH_QUEUE);
	}

	@GetMapping("/error")
	public Mono<ResponseEntity<BaseApiResponse<Object>>> healthCheck2(ServerWebExchange exchange) {
		return processRequest(exchange, RabbitMQConstants.AUTH_QUEUE);
	}


	@Operation(
		summary = "구글 OAuth 안드로이드 로그인",
		description = "안드로이드 앱에서 전달받은 구글 OAuth 토큰을 검증하고 회원가입/로그인을 처리합니다.",
		responses = {
			@ApiResponse(
				responseCode = "201",
				description = "로그인 성공",
				content = @Content(
					mediaType = "application/json",
					schema = @Schema(implementation = BaseApiResponse.class)
				)
			),
			@ApiResponse(
				responseCode = "400",
				description = "잘못된 요청 (유효하지 않은 인증코드)",
				content = @Content(
					mediaType = "application/json",
					schema = @Schema(implementation = BaseApiResponse.class)
				)
			),
		}
	)
	@PostMapping("/google/android")
	public Mono<ResponseEntity<BaseApiResponse<GoogleOAuthResponse>>> handleGoogleAndroidLogin(@RequestBody @Valid Mono<GoogleOAuthRequest> request, ServerWebExchange exchange) {
		return processRequest(exchange, RabbitMQConstants.AUTH_QUEUE);
	}

//	@RequestMapping("/**")
//	public Mono<ResponseEntity<ApiResponse<>>> handleAuthServiceRequest(ServerWebExchange exchange) {
//		return processRequest(exchange, RabbitMQConstants.AUTH_QUEUE);
//	}
}