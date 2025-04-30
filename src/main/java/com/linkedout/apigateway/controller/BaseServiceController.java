package com.linkedout.apigateway.controller;

import com.linkedout.apigateway.service.ResponseHandlerService;
import com.linkedout.apigateway.util.JsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.linkedout.common.dto.ApiResponse;
import com.linkedout.common.dto.RequestData;
import com.linkedout.common.dto.ResponseData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 마이크로서비스로 메시지를 전송하는 기본 컨트롤러
 * <p>
 * 이 추상 클래스는 HTTP 요청을 받아 RabbitMQ 메시지로 변환하고 전송하는
 * 공통 로직을 제공합니다. 각 서비스별 컨트롤러는 이 클래스를 상속받아 구현합니다.
 */
@Slf4j
@RequiredArgsConstructor
public abstract class BaseServiceController {

	protected final RabbitTemplate rabbitTemplate;
	protected final ResponseHandlerService responseHandlerService;
	protected final JsonUtils jsonUtils;

	/**
	 * HTTP 요청을 RabbitMQ 메시지로 변환하여 전송하고 응답을 처리
	 *
	 * @param exchange  현재 요청/응답 컨텍스트
	 * @param queueName 메시지를 전송할 큐 이름
	 * @return 마이크로서비스의 응답을 포함한 API 응답
	 */
	protected Mono<ApiResponse<?>> processRequest(ServerWebExchange exchange, String queueName) {
		ServerHttpRequest request = exchange.getRequest();

		// 요청 본문 읽기
		return request.getBody()
			.collectList()
			.flatMap(dataBuffers -> {
				// 요청 본문을 문자열로 변환
				String requestBody = "";
				if (!dataBuffers.isEmpty()) {
					StringBuilder sb = new StringBuilder();
					dataBuffers.forEach(buffer -> {
						byte[] bytes = new byte[buffer.readableByteCount()];
						buffer.read(bytes);
						sb.append(new String(bytes, java.nio.charset.StandardCharsets.UTF_8));
					});
					requestBody = sb.toString();
				}

				// 요청 데이터 객체 생성
				RequestData requestData = new RequestData();
				requestData.setPath(request.getPath().value());
				requestData.setMethod(request.getMethod().name());
				requestData.setHeaders(getHeadersMap(request.getHeaders()));
				requestData.setBody(requestBody);
				requestData.setQueryParams(exchange.getRequest().getQueryParams().toSingleValueMap());

				// 메시지 상관관계 ID 생성
				String correlationId = UUID.randomUUID().toString();
				// RabbitMQ로 메시지 전송
				rabbitTemplate.convertAndSend(queueName, requestData, message -> {
					message.getMessageProperties().setCorrelationId(correlationId);
					message.getMessageProperties().getHeaders().put(AmqpHeaders.CORRELATION_ID, correlationId);
					return message;
				});
				// 비동기 응답 처리
				return responseHandlerService.awaitResponse(correlationId)
					.map(this::createApiResponse);
			});
	}

	/**
	 * ResponseData를 API 응답 형식으로 변환
	 */
	protected ApiResponse<?> createApiResponse(ResponseData responseData) {
		HttpStatus httpStatus = HttpStatus.valueOf(responseData.getStatusCode());

		// Content-Type 확인
		String contentType = responseData.getHeaders().get("Content-Type");

		// JSON 응답인 경우 파싱 처리
		if (contentType != null && contentType.contains("application/json")) {
			try {
				// JSON 문자열을 Object로 파싱
				ObjectMapper objectMapper = new ObjectMapper();
				Object parsedBody = objectMapper.readValue(responseData.getBody(), Object.class);

				if (httpStatus.is2xxSuccessful()) {
					return ApiResponse.success(
						httpStatus.value(),
						parsedBody,  // 파싱된 객체 사용
						httpStatus.getReasonPhrase()
					);
				} else {
					return ApiResponse.builder()
						.status(httpStatus.value())
						.message(httpStatus.getReasonPhrase())
						.error(new ApiResponse.ErrorInfo(
							"ERR_" + httpStatus.value(),
							(String) parsedBody))  // 파싱된 객체 사용
						.build();
				}
			} catch (JsonProcessingException e) {
				// JSON 파싱 실패 시 원본 문자열 사용
				log.error("Failed to parse JSON response", e);
				return fallbackResponse(responseData, httpStatus);
			}
		} else {
			// JSON이 아닌 경우 기존 처리 방식 유지
			return fallbackResponse(responseData, httpStatus);
		}
	}

	/**
	 * JSON 파싱 실패 시 또는 JSON이 아닌 경우 사용할 응답 생성 메서드
	 */
	private ApiResponse<?> fallbackResponse(ResponseData responseData, HttpStatus httpStatus) {
		if (httpStatus.is2xxSuccessful()) {
			return ApiResponse.success(
				httpStatus.value(),
				responseData.getBody(),
				httpStatus.getReasonPhrase()
			);
		} else {
			return ApiResponse.builder()
				.status(httpStatus.value())
				.message(httpStatus.getReasonPhrase())
				.error(new ApiResponse.ErrorInfo(
					"ERR_" + httpStatus.value(),
					responseData.getBody()))
				.build();
		}
	}

	/**
	 * HttpHeaders를 Map으로 변환
	 */
	protected Map<String, String> getHeadersMap(HttpHeaders headers) {
		Map<String, String> headersMap = new HashMap<>();
		headers.forEach((name, values) -> {
			String headerValue = String.join(", ", values);
			headersMap.put(name, headerValue);
		});
		return headersMap;
	}
}