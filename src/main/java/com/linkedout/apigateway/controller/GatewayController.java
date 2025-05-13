package com.linkedout.apigateway.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@RestController
@Tag(name = "Health Chek", description = "API-Gateway 상태 확인 엔드포인트")
public class GatewayController {

  @Operation(
      summary = "시스템 상태 확인",
      description = "MSA 구성에서 유일한 HTTP 통신 통로인 API-Gateway의 현재 상태를 제공합니다.")
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "서비스 정상"
            // todo 응답 dto
            // 			content = @Content(schema = @Schema(implementation = HealthResponse.class))
            )
      })
  //	@PreAuthorize("hasRole('ADMIN')")
  //	@PreAuthorize("hasRole('USER')")
  @GetMapping("/api/health")
  public Mono<ResponseEntity<Map<String, Object>>> healthCheck() {
    Map<String, Object> response = new HashMap<>();
    response.put("status", 200);

    return Mono.just(ResponseEntity.ok(response));
  }

  @GetMapping("/actuator/prometheus")
  public Mono<ResponseEntity<Map<String, Object>>> prometheusHealthCheck() {
    Map<String, Object> response = new HashMap<>();
    response.put("status", 200);

    return Mono.just(ResponseEntity.ok(response));
  }
}
