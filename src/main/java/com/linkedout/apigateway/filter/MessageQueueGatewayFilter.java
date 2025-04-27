package com.linkedout.apigateway.filter;

import com.linkedout.apigateway.model.ApiResponse;
import com.linkedout.apigateway.model.RequestData;
import com.linkedout.apigateway.service.ResponseHandlerService;
import com.linkedout.apigateway.util.JsonUtils;
import lombok.Getter;
import lombok.Setter;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * API Gateway에서 HTTP 요청을 RabbitMQ 메시지로 변환하여 전송하는 필터
 * 
 * 이 필터는 클라이언트로부터 받은 HTTP 요청을 RabbitMQ 메시지로 변환하고,
 * 해당 서비스의 응답을 다시 클라이언트에게 전달하는 역할을 합니다.
 * Spring Cloud Gateway의 필터 체인에서 동작하며, 리액티브 프로그래밍 방식으로 구현되었습니다.
 * 
 * {@code @Component}: 
 *    - Spring이 이 클래스를 컴포넌트로 인식하고 자동으로 Bean으로 등록하도록 하는 애노테이션
 */
@Component
public class MessageQueueGatewayFilter extends AbstractGatewayFilterFactory<MessageQueueGatewayFilter.Config> {
    
    /**
     * RabbitMQ 메시지를 전송하기 위한 템플릿
     * RabbitTemplate은 Spring AMQP에서 제공하는 클래스로, 메시지 큐와의 통신을 쉽게 해줍니다.
     */
    private final RabbitTemplate rabbitTemplate;
    
    /**
     * 비동기 응답을 처리하기 위한 서비스
     * 마이크로서비스로부터의 응답을 처리하고 클라이언트에게 전달하는 역할을 합니다.
     */
    private final ResponseHandlerService responseHandlerService;
    
    /**
     * JSON 변환을 위한 유틸리티
     * 객체와 JSON 문자열 간의 변환을 처리합니다.
     */
    private final JsonUtils jsonUtils;
    
    /**
     * 생성자를 통한 의존성 주입
     * Spring은 생성자를 통해 필요한 Bean들을 자동으로 주입합니다.
     * 
     * @param rabbitTemplate RabbitMQ와 통신하기 위한 템플릿
     * @param responseHandlerService 응답 처리를 위한 서비스
     * @param jsonUtils JSON 변환을 위한 유틸리티
     */
    public MessageQueueGatewayFilter(
            RabbitTemplate rabbitTemplate,
            ResponseHandlerService responseHandlerService,
            JsonUtils jsonUtils) {
        // 부모 클래스 생성자 호출 - 설정 클래스 타입을 지정
        super(Config.class);
        this.rabbitTemplate = rabbitTemplate;
        this.responseHandlerService = responseHandlerService;
        this.jsonUtils = jsonUtils;
    }
    
    /**
     * 필터 로직을 정의하는 메서드
     * 
     * 이 메서드에서는 HTTP 요청을 RabbitMQ 메시지로 변환하고,
     * 비동기적으로 응답을 기다린 후 클라이언트에게 전달하는 로직을 구현합니다.
     * 
     * @param config 필터 설정 (메시지를 전송할 큐 이름 등)
     * @return GatewayFilter 인스턴스 (람다 표현식으로 정의)
     */
    @Override
    public GatewayFilter apply(Config config) {
        // 람다 표현식으로 GatewayFilter 인터페이스 구현
        // exchange: 현재 HTTP 요청/응답 컨텍스트
        // chain: 다음 필터로 요청을 전달하는 체인
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            
            // 요청 본문을 읽어서 메시지로 변환
            // Mono는 리액티브 프로그래밍의 중심 개념으로 0 또는 1개의 결과를 비동기적으로 제공하는 발행자(Publisher)입니다.
            return request.getBody()  // body를 Flux<DataBuffer>로 반환 (여러 청크로 나뉜 데이터 스트림)
                .collectList()  // 모든 DataBuffer를 모아서 List<DataBuffer>로 변환
                .flatMap(dataBuffers -> {  // flatMap은 비동기 작업을 연결하는 데 사용하는 연산자
                    // 요청 본문 읽기
                    String requestBody = "";
                    if (!dataBuffers.isEmpty()) {
                        StringBuilder sb = new StringBuilder();
                        // 각 DataBuffer에서 바이트를 읽어 문자열로 변환
                        dataBuffers.forEach(buffer -> {
                            byte[] bytes = new byte[buffer.readableByteCount()];
                            buffer.read(bytes);
                            sb.append(new String(bytes, StandardCharsets.UTF_8));
                        });
                        requestBody = sb.toString();
                    }
                    
                    // 요청 데이터 객체 생성 및 설정
                    // 이 객체는 RabbitMQ를 통해 마이크로서비스로 전송됩니다.
                    RequestData requestData = new RequestData();
                    requestData.setPath(request.getPath().value());  // 요청 경로 (/api/users/123 등)
                    requestData.setMethod(request.getMethod().name());  // HTTP 메서드 (GET, POST 등)
                    requestData.setHeaders(getHeadersMap(request.getHeaders()));  // HTTP 헤더
                    requestData.setBody(requestBody);  // 요청 본문
                    requestData.setQueryParams(exchange.getRequest().getQueryParams().toSingleValueMap());  // 쿼리 파라미터
                    
                    // 메시지 상관관계 ID 생성
                    // 이 ID는 요청과 응답을 연결하는 데 사용됩니다.
                    String correlationId = UUID.randomUUID().toString();
                    exchange.getAttributes().put("correlationId", correlationId);
                    
                    // RabbitMQ로 메시지 전송
                    // config.getQueueName()은 GatewayConfig에서 지정한 큐 이름 (예: user-service-queue)
                    rabbitTemplate.convertAndSend(config.getQueueName(), requestData, message -> {
                        // 메시지 속성에 상관관계 ID 설정
                        message.getMessageProperties().setCorrelationId(correlationId);
                        return message;
                    });
                    
                    // 비동기 응답 처리
                    // 마이크로서비스가 응답을 보낼 때까지 기다린 후 클라이언트에게 전달
                    return handleAsyncResponse(exchange, correlationId);
                });
        };
    }
    
    /**
     * 비동기 응답을 처리하는 메서드
     * 
     * 이 메서드는 마이크로서비스로부터의 응답을 기다리고,
     * 응답이 도착하면 HTTP 응답으로 변환하여 클라이언트에게 전송합니다.
     * 응답은 표준화된 ApiResponse 형식으로 변환됩니다.
     * 
     * @param exchange 현재 HTTP 요청/응답 컨텍스트
     * @param correlationId 요청과 응답을 연결하는 상관관계 ID
     * @return 응답 처리가 완료되면 완료되는 Mono<Void>
     */
    private Mono<Void> handleAsyncResponse(ServerWebExchange exchange, String correlationId) {
        // ResponseHandlerService에서 응답을 기다림
        // Mono는 "미래에 완료될 비동기 작업"을 표현하는 리액티브 타입
        return responseHandlerService.awaitResponse(correlationId)
            .flatMap(responseData -> {
                // 응답이 도착하면 HTTP 응답 구성
                ServerHttpResponse response = exchange.getResponse();
                
                // HTTP 상태 코드 설정
                HttpStatus httpStatus = HttpStatus.valueOf(responseData.getStatusCode());
                response.setStatusCode(httpStatus);
                
                // 응답 헤더 설정
                responseData.getHeaders().forEach((key, value) -> 
                    response.getHeaders().add(key, value));
                
                // Content-Type이 없으면 JSON으로 설정
                if (!response.getHeaders().containsKey("Content-Type")) {
                    response.getHeaders().add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
                }
                
                // 응답 본문 준비
                String responseBody = responseData.getBody();
                byte[] responseBodyBytes;
                
                try {
                    // 성공적인 응답인 경우
                    if (httpStatus.is2xxSuccessful()) {
                        // 표준 응답 형식으로 래핑 (마이크로서비스가 반환한 정확한 상태 코드 사용)
                        ApiResponse<?> apiResponse = ApiResponse.success(
                                httpStatus.value(),
                                responseBody,  // 원본 응답 본문을 data 필드에 할당
                                httpStatus.getReasonPhrase());
                        
                        // ObjectMapper를 사용하여 JSON 직렬화
                        responseBodyBytes = jsonUtils.toJson(apiResponse).getBytes(StandardCharsets.UTF_8);
                    } else {
                        // 오류 응답인 경우
                        ApiResponse<?> apiResponse = ApiResponse.builder()
                                .status(httpStatus.value())
                                .message(httpStatus.getReasonPhrase())
                                .error(new ApiResponse.ErrorInfo(
                                        "ERR_" + httpStatus.value(), 
                                        responseBody))
                                .build();
                        
                        // ObjectMapper를 사용하여 JSON 직렬화
                        responseBodyBytes = jsonUtils.toJson(apiResponse).getBytes(StandardCharsets.UTF_8);
                    }
                } catch (Exception e) {
                    // JSON 변환 실패 시 원본 응답 사용
                    responseBodyBytes = responseBody.getBytes(StandardCharsets.UTF_8);
                }
                
                // 응답 본문 설정
                DataBuffer buffer = response.bufferFactory().wrap(responseBodyBytes);
                
                // 응답 전송 (writeWith는 응답 본문을 작성하고 응답을 완료함)
                return response.writeWith(Mono.just(buffer));
            });
    }
    
    /**
     * HttpHeaders를 Map<String, String>으로 변환하는 유틸리티 메서드
     * 
     * @param headers HTTP 헤더
     * @return 변환된 헤더 맵
     */
    private Map<String, String> getHeadersMap(HttpHeaders headers) {
        Map<String, String> headersMap = new HashMap<>();
        headers.forEach((name, values) -> {
            // 여러 값을 가진 헤더는 쉼표로 구분하여 하나의 문자열로 결합
            String headerValue = String.join(", ", values);
            headersMap.put(name, headerValue);
        });
        return headersMap;
    }
    
    /**
     * 필터 설정을 위한 내부 클래스
     * 
     * 이 클래스는 필터가 사용할 설정 정보를 담고 있습니다.
     * 주로 메시지를 전송할 큐 이름을 지정하는 데 사용됩니다.
     * 
     * Lombok의 @Getter, @Setter 애노테이션을 사용하여 게터/세터 메서드를 자동 생성합니다.
     */
    @Setter
    @Getter
    public static class Config {
        /**
         * 메시지를 전송할 RabbitMQ 큐 이름
         */
        private String queueName;
        
        /**
         * 생성자
         * 
         * @param queueName 메시지를 전송할 큐 이름
         */
        public Config(String queueName) {
            this.queueName = queueName;
        }
    }
}