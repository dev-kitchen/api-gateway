package com.linkedout.apigateway.filter;

import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 모든 요청과 응답을 로깅하는 글로벌 필터
 * 
 * 이 필터는 API Gateway로 들어오는 모든 요청과 나가는 모든 응답을 가로채어 로깅합니다.
 * 요청 URL, 메서드, 헤더, 본문과 응답 상태 코드, 헤더, 본문을 로그로 남깁니다.
 * 디버깅 및 모니터링 목적으로 사용됩니다.
 * 
 * {@code @Component}: 
 *    - Spring이 이 클래스를 컴포넌트로 인식하고 Bean으로 등록하도록 함
 */
@Component
public class LoggingFilter implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(LoggingFilter.class);
    
    /**
     * 본문 로깅을 건너뛸 Content-Type 목록
     * 바이너리 데이터나 대용량 데이터는 로깅하지 않음
     */
    private final Set<String> excludedContentTypes = new LinkedHashSet<>() {{
			add("multipart/form-data");
			add("application/octet-stream");
			add("application/pdf");
			add("image/");
			add("video/");
			add("audio/");
		}};

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 원본 요청과 응답
        ServerHttpRequest originalRequest = exchange.getRequest();
        ServerHttpResponse originalResponse = exchange.getResponse();
        
        // 요청 로깅을 위한 정보 수집
        String requestPath = originalRequest.getPath().value();
        String requestMethod = originalRequest.getMethod().name();
        String requestId = exchange.getRequest().getId();
        
        // 요청 시작 로깅
        logger.info(">>> 요청 시작: {} {} (ID: {})", requestMethod, requestPath, requestId);
        logger.info(">>> 요청 헤더: {}", originalRequest.getHeaders());
        
        // 요청 본문 로깅 여부 결정
        boolean shouldLogRequestBody = shouldLogBody(originalRequest.getHeaders().getContentType());
        
        if (!shouldLogRequestBody) {
            logger.info(">>> 요청 본문: [바이너리 데이터 - 로깅 생략]");
            return chain.filter(exchange).then(Mono.fromRunnable(() -> {
                logger.info("<<< 응답 상태: {}", originalResponse.getStatusCode());
                logger.info("<<< 응답 헤더: {}", originalResponse.getHeaders());
                logger.info("<<< 응답 본문: [바이너리 데이터 또는 스트리밍 데이터 - 로깅 생략]");
                logger.info("<<< 요청 종료: {} {} (ID: {})", requestMethod, requestPath, requestId);
            }));
        }
        
        // 요청 본문 로깅
        AtomicReference<String> requestBodyRef = new AtomicReference<>();
        
        ServerHttpRequestDecorator requestDecorator = new ServerHttpRequestDecorator(originalRequest) {
            @Override
            public Flux<DataBuffer> getBody() {
                return super.getBody().doOnNext(dataBuffer -> {
                    // 요청 본문 캡처
                    byte[] content = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(content);
                    // 버퍼를 소비했으므로 복제본 생성
                    DataBufferUtils.retain(dataBuffer);
                    // 요청 본문을 문자열로 변환하여 저장
                    String bodyStr = new String(content, StandardCharsets.UTF_8);
                    requestBodyRef.set(bodyStr);
                    logger.info(">>> 요청 본문: {}", bodyStr);
                });
            }
        };
        
        // 응답 로깅
        ServerHttpResponseDecorator responseDecorator = new ServerHttpResponseDecorator(originalResponse) {
            @Override
            public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
                if (shouldLogBody(getHeaders().getContentType())) {
                    return super.writeWith(
                        Flux.from(body)
                            .collectList()
                            .flatMap(dataBuffers -> {
                                // 모든 데이터 버퍼를 합치기
                                DataBufferFactory bufferFactory = originalResponse.bufferFactory();
                                DataBuffer joinedBuffer = bufferFactory.join(dataBuffers);
                                
                                byte[] content = new byte[joinedBuffer.readableByteCount()];
                                joinedBuffer.read(content);
                                // 버퍼 복제 (원본은 소비됨)
                                DataBuffer copiedBuffer = bufferFactory.wrap(content);
                                
                                // 응답 내용을 문자열로 변환
                                String bodyStr = new String(content, StandardCharsets.UTF_8);
                                
                                // 응답 로깅
                                logger.info("<<< 응답 상태: {}", getStatusCode());
                                logger.info("<<< 응답 헤더: {}", getHeaders());
                                logger.info("<<< 응답 본문: {}", bodyStr);
                                logger.info("<<< 요청 종료: {} {} (ID: {})", requestMethod, requestPath, requestId);
                                
                                // 원본 데이터 반환
                                return Mono.just(copiedBuffer);
                            })
                    );
                } else {
                    logger.info("<<< 응답 상태: {}", getStatusCode());
                    logger.info("<<< 응답 헤더: {}", getHeaders());
                    logger.info("<<< 응답 본문: [바이너리 데이터 - 로깅 생략]");
                    logger.info("<<< 요청 종료: {} {} (ID: {})", requestMethod, requestPath, requestId);
                    return super.writeWith(body);
                }
            }
            
            @Override
            public Mono<Void> writeAndFlushWith(Publisher<? extends Publisher<? extends DataBuffer>> body) {
                // 스트리밍 응답의 경우
                logger.info("<<< 응답 상태: {}", getStatusCode());
                logger.info("<<< 응답 헤더: {}", getHeaders());
                logger.info("<<< 응답 본문: [스트리밍 데이터 - 로깅 생략]");
                logger.info("<<< 요청 종료: {} {} (ID: {})", requestMethod, requestPath, requestId);
                return super.writeAndFlushWith(body);
            }
        };
        
        // 데코레이트된 요청과 응답으로 교체된 ServerWebExchange 생성
        ServerWebExchange decoratedExchange = exchange.mutate()
            .request(requestDecorator)
            .response(responseDecorator)
            .build();
        
        // 필터 체인 계속 진행
        return chain.filter(decoratedExchange);
    }
    
    /**
     * Content-Type을 확인하여 본문 로깅 여부 결정
     * 바이너리 데이터나 대용량 데이터는 로깅하지 않음
     */
    private boolean shouldLogBody(MediaType contentType) {
        if (contentType == null) {
            return true;
        }
        
        String contentTypeStr = contentType.toString();
        for (String excluded : excludedContentTypes) {
            if (contentTypeStr.contains(excluded)) {
                return false;
            }
        }
        
        return true;
    }

    /**
     * 필터 실행 순서 지정
     * 값이 작을수록 먼저 실행됨 (우선순위 높음)
     */
    @Override
    public int getOrder() {
        // 로깅 필터는 다른 필터보다 먼저 실행되어야 함
        return Ordered.HIGHEST_PRECEDENCE;
    }
}