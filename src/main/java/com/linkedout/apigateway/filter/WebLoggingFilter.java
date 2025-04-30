package com.linkedout.apigateway.filter;

import io.micrometer.common.lang.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 모든 요청과 응답을 로깅하는 WebFilter
 * <p>
 * WebFilter는 Spring WebFlux 기반 애플리케이션에서 모든 컨트롤러 요청 전/후에 실행됩니다.
 * Gateway 라우트 설정과 독립적으로 작동하므로, 컨트롤러로 향하는 모든 요청에 적용됩니다.
 * </p>
 */
@Component
public class WebLoggingFilter implements WebFilter, Ordered {

	private static final Logger logger = LoggerFactory.getLogger("com.linkedout.apigateway.filter.WebLoggingFilter");

	@Override
	public Mono<Void> filter(@NonNull ServerWebExchange exchange, @NonNull WebFilterChain chain) {
		// 요청 정보
		ServerHttpRequest request = exchange.getRequest();
		String method = request.getMethod().toString();
		String path = request.getURI().getPath();
		String requestId = request.getId();
		MultiValueMap<String, String> queryParams = request.getQueryParams();

		// 요청 시작 로깅
		logger.info("\n>>>\n {} {} (ID: {})\n  headers: {}\n  params: {}",
			method, path, requestId, exchange.getRequest().getHeaders(), queryParams);
		// 요청 본문 로깅 여부 결정
		boolean shouldLogRequestBody = shouldLogBody(request.getHeaders().getContentType());


		if (!shouldLogRequestBody) {
		}


		// 필터 체인 실행 후 응답 로깅
		return logResponse(exchange, chain, method, path, requestId);
	}

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

	// 응답 로깅을 위한 메소드
	private Mono<Void> logResponse(ServerWebExchange exchange, WebFilterChain chain,
																 String method, String path, String requestId) {
		long startTime = System.currentTimeMillis();
		logger.info(String.valueOf(exchange));
		return chain.filter(exchange)
			.doFinally(signalType -> {
				long endTime = System.currentTimeMillis();
				long duration = endTime - startTime;

				logger.info("""
						\n<<<
						  {} {} {}
						  status: {}
						  {}ms""",
					method,
					path,
					requestId,
					exchange.getResponse().getStatusCode(),
					duration);
			});
	}


	@Override
	public int getOrder() {
		// 가장 먼저 실행되도록 높은 우선순위 설정
		return Ordered.HIGHEST_PRECEDENCE;
	}
}