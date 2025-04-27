package com.linkedout.apigateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * 모든 요청에 대해 MDC(Mapped Diagnostic Context)에 correlationId를 추가하는 필터
 * <p>
 * 이 필터는 각 요청에 고유한 correlationId를 생성하고 MDC에 저장합니다.
 * 이를 통해 로그에 요청별 식별자를 포함시켜 로그 추적 및 디버깅을 용이하게 합니다.
 * <p>
 * {@code @Component}: 
 *    - Spring이 이 클래스를 컴포넌트로 인식하고 Bean으로 등록하도록 함
 */
@Component
public class CorrelationIdFilter implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(CorrelationIdFilter.class);
    private static final String CORRELATION_ID_KEY = "correlationId";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 기존 correlationId가 있으면 사용, 없으면 새로 생성
        String correlationId = exchange.getRequest().getHeaders().getFirst(CORRELATION_ID_KEY);
        if (correlationId == null || correlationId.isEmpty()) {
            correlationId = UUID.randomUUID().toString();
        }
        
        // correlationId를 exchange 속성에 저장
        exchange.getAttributes().put(CORRELATION_ID_KEY, correlationId);
        
        // MDC에 correlationId 설정
			String finalCorrelationId = correlationId;
			return chain.filter(exchange)
            .contextWrite(context -> {
                MDC.put(CORRELATION_ID_KEY, finalCorrelationId);
                return context;
            })
            .doFinally(signalType -> {
                // 필터 처리 완료 후 MDC 정리
                MDC.remove(CORRELATION_ID_KEY);
            });
    }

    @Override
    public int getOrder() {
        // 로깅 필터보다 먼저 실행되어야 함
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }
}