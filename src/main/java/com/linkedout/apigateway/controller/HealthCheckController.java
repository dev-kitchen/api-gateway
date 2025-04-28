package com.linkedout.apigateway.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
public class HealthCheckController {

	@GetMapping("/api/health")
	public Mono<ResponseEntity<Map<String, Object>>> healthCheck() {
		Map<String, Object> response = new HashMap<>();
		response.put("status", "UP");
		response.put("timestamp", LocalDateTime.now().toString());
		response.put("service", "api-gateway");

		return Mono.just(ResponseEntity.ok(response));
	}
}