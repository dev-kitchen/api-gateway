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
@Tag(name = "Gateway", description = "API-Gateway 엔드포인트")
public class GatewayController {


	@Operation(
		summary = "API-Gateway Prometheus 헬스 체크",
		description = "API-Gateway 시스템 모니터링을 위한 프로메테우스 메트릭을 제공합니다."
	)
	@GetMapping("/actuator/prometheus")
	public Mono<ResponseEntity<Map<String, Object>>> prometheusHealthCheck() {
		Map<String, Object> response = new HashMap<>();
		response.put("status", 200);

		return Mono.just(ResponseEntity.ok(response));
	}
}
