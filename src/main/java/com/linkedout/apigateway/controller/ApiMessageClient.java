package com.linkedout.apigateway.controller;

import com.linkedout.apigateway.service.MessageResponseHandlerService;
import com.linkedout.apigateway.util.JsonUtils;
import com.linkedout.common.constant.RabbitMQConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.linkedout.common.dto.BaseApiResponse;
import com.linkedout.common.dto.ApiRequestData;
import com.linkedout.common.dto.ApiResponseData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * API-Gateway 에서 마이크로서비스로 메시지를 전송하는 기본 컨트롤러
 *
 * <p>이 추상 클래스는 HTTP 요청을 받아 RabbitMQ 메시지로 변환하고 전송하는 공통 로직을 제공합니다. 각 서비스별 컨트롤러는 이 클래스를 상속받아 구현합니다.
 */
@Slf4j
@RequiredArgsConstructor
public abstract class ApiMessageClient {

  protected final RabbitTemplate rabbitTemplate;
  protected final MessageResponseHandlerService messageResponseHandlerService;
  protected final JsonUtils jsonUtils;

  /**
   * HTTP 요청을 RabbitMQ 메시지로 변환하여 전송하고 응답을 처리
   *
   * @param exchange 현재 요청/응답 컨텍스트
   * @return 마이크로서비스의 응답을 포함한 API 응답
   */
  protected <T> Mono<ResponseEntity<BaseApiResponse<T>>> processRequest(
      ServerWebExchange exchange) {
    ServerHttpRequest request = exchange.getRequest();
    String path = request.getPath().value();
    // 경로에 따라 적절한 라우팅 키 결정
    String routingKey = determineRoutingKey(path);
    // 요청 본문 읽기
    return request
        .getBody()
        .collectList()
        .flatMap(
            dataBuffers -> {
              // 요청 본문을 문자열로 변환
              String requestBody = "";
              if (!dataBuffers.isEmpty()) {
                StringBuilder sb = new StringBuilder();
                dataBuffers.forEach(
                    buffer -> {
                      byte[] bytes = new byte[buffer.readableByteCount()];
                      buffer.read(bytes);
                      sb.append(new String(bytes, java.nio.charset.StandardCharsets.UTF_8));
                    });
                requestBody = sb.toString();
              }

              // 요청 데이터 객체 생성
              ApiRequestData ApiRequestData = new ApiRequestData();
              ApiRequestData.setPath(request.getPath().value());
              ApiRequestData.setMethod(request.getMethod().name());
              ApiRequestData.setHeaders(getHeadersMap(request.getHeaders()));
              ApiRequestData.setBody(requestBody);
              ApiRequestData.setQueryParams(
                  exchange.getRequest().getQueryParams().toSingleValueMap());

              // 메시지 상관관계 ID 생성
              String correlationId = UUID.randomUUID().toString();
              // RabbitMQ로 메시지 전송
              rabbitTemplate.convertAndSend(
                  RabbitMQConstants.API_EXCHANGE,
                  routingKey,
                  ApiRequestData,
                  message -> {
                    message.getMessageProperties().setCorrelationId(correlationId);
                    message
                        .getMessageProperties()
                        .getHeaders()
                        .put(AmqpHeaders.CORRELATION_ID, correlationId);
                    return message;
                  });
              // 비동기 응답 처리
              return messageResponseHandlerService
                  .awaitResponse(correlationId)
                  .map(this::createApiResponseEntity);
            });
  }

  /**
   * 요청 경로에 따라 적절한 라우팅 키를 결정
   *
   * @param path 요청 경로
   * @return 라우팅 키
   */
  private String determineRoutingKey(String path) {
    // 경로 패턴에 따라 라우팅 키 반환
    if (path.startsWith("/api/auth")) {
      return RabbitMQConstants.AUTH_API_ROUTING_KEY;
      //		} else if (path.startsWith("/api/users")) {
      //			return RabbitMQConstants.USER_ROUTING_KEY;
    } else if (path.startsWith("/api/account")) {
      return RabbitMQConstants.ACCOUNT_ROUTING_KEY;
    } else {
      // 기본값 또는 예외 처리
      throw new IllegalArgumentException("지원하지 않는 API 경로: " + path);
    }
  }

  /** ApiResponseData를 ResponseEntity<ApiResponse<?>> 응답 형식으로 변환 */
  protected <T> ResponseEntity<BaseApiResponse<T>> createApiResponseEntity(
      ApiResponseData responseData) {
    HttpStatus httpStatus = HttpStatus.valueOf(responseData.getStatusCode());

    // ApiResponse 객체 생성
    BaseApiResponse<T> apiResponse = createApiResponse(responseData);

    // ResponseEntity에 상태 코드와 함께 ApiResponse 객체를 담아 반환
    return ResponseEntity.status(httpStatus).body(apiResponse);
  }

  /** ApiResponseData를 API 응답 형식으로 변환 */
  protected <T> BaseApiResponse<T> createApiResponse(ApiResponseData responseData) {
    HttpStatus httpStatus = HttpStatus.valueOf(responseData.getStatusCode());

    // Content-Type 확인
    String contentType = responseData.getHeaders().get("Content-Type");

    // JSON 응답인 경우 파싱 처리
    if (contentType != null && contentType.contains("application/json")) {
      try {
        // JSON 문자열을 Object로 파싱
        ObjectMapper objectMapper = new ObjectMapper();
        Object parsedBody = objectMapper.readValue(responseData.getBody(), Object.class);

        // 타입 캐스팅 추가
        @SuppressWarnings("unchecked")
        T typedBody = (T) parsedBody;

        if (httpStatus.is2xxSuccessful()) {
          return BaseApiResponse.success(
              httpStatus.value(),
              typedBody, // 파싱된 객체 사용
              httpStatus.getReasonPhrase());
        } else {
          return BaseApiResponse.error(httpStatus.value(), typedBody, httpStatus.getReasonPhrase());
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

  /** JSON 파싱 실패 시 또는 JSON이 아닌 경우 사용할 응답 생성 메서드 */
  private <T> BaseApiResponse<T> fallbackResponse(
      ApiResponseData responseData, HttpStatus httpStatus) {
    // 타입 캐스팅 추가
    @SuppressWarnings("unchecked")
    T body = (T) responseData.getBody();

    if (httpStatus.is2xxSuccessful()) {
      return BaseApiResponse.success(httpStatus.value(), body, httpStatus.getReasonPhrase());
    } else {
      return BaseApiResponse.error(httpStatus.value(), body, httpStatus.getReasonPhrase());
    }
  }

  /** HttpHeaders를 Map으로 변환 */
  protected Map<String, String> getHeadersMap(HttpHeaders headers) {
    Map<String, String> headersMap = new HashMap<>();
    headers.forEach(
        (name, values) -> {
          String headerValue = String.join(", ", values);
          headersMap.put(name, headerValue);
        });
    return headersMap;
  }
}
