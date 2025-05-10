package com.linkedout.apigateway.controller;

import com.linkedout.common.dto.BaseApiResponse;
import com.linkedout.common.dto.HealthResponse;
import com.linkedout.common.dto.auth.oauth.google.GoogleOAuthRequest;
import com.linkedout.common.dto.auth.oauth.google.GoogleOAuthResponse;
import com.linkedout.common.messaging.ApiMessageClient;
import com.linkedout.common.messaging.ApiMessageResponseHandler;
import com.linkedout.common.schema.GoogleOAuthResponseSchema;
import com.linkedout.common.util.JsonUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/api/auth")
public class AuthServiceController extends ApiMessageClient {

  public AuthServiceController(
      RabbitTemplate rabbitTemplate,
      ApiMessageResponseHandler messageResponseHandlerService,
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
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = BaseApiResponse.class))),
        @ApiResponse(
            responseCode = "503",
            description = "서비스 이용 불가",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = BaseApiResponse.class)))
      })
  @GetMapping("/health")
  public Mono<ResponseEntity<BaseApiResponse<HealthResponse>>> healthCheck(
      ServerWebExchange exchange) {
    return sendMessage(exchange);
  }

  @Operation(
      summary = "구글 OAuth 안드로이드 로그인",
      description = "안드로이드 앱에서 전달받은 구글 OAuth 토큰을 검증하고 회원가입/로그인을 처리합니다.",
      requestBody =
          @io.swagger.v3.oas.annotations.parameters.RequestBody(
              description = "구글 OAuth 인증 요청",
              content =
                  @Content(
                      mediaType = "application/json",
                      schema = @Schema(implementation = GoogleOAuthRequest.class))),
      responses = {
        @ApiResponse(
            responseCode = "201",
            description = "로그인/회원가입 성공",
            content =
                @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = GoogleOAuthResponseSchema.class))),
      })
  @PostMapping("/google/android")
  public Mono<ResponseEntity<BaseApiResponse<GoogleOAuthResponse>>> handleGoogleAndroidLogin(
      @RequestBody @Validated GoogleOAuthRequest request, ServerWebExchange exchange) {
    return sendMessage(exchange);
  }

  //	@RequestMapping("/**")
  //	public Mono<ResponseEntity<ApiResponse<>>> handleAuthServiceRequest(ServerWebExchange exchange)
  // {
  //		return processRequest(exchange, RabbitMQConstants.AUTH_QUEUE);
  //	}
}
