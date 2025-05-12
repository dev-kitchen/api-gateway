package com.linkedout.apigateway.filter;

import com.linkedout.apigateway.security.JwtTokenProvider;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * JWT 기반 인증을 처리하는 WebFlux {@link WebFilter} 구현체. 이 필터는 요청에서 JWT 토큰을 추출하고 검증하며, 유효한 경우 이후 처리를 위해
 * 리액터의 보안 컨텍스트를 채우기 위한 인증 세부 정보를 검색합니다.
 *
 * <p>이 클래스는 JWT 토큰을 파싱, 검증 및 인증 세부 정보를 추출하는 유틸리티를 제공하는 {@link JwtTokenProvider}와 함께 작동합니다.
 *
 * <p>필터는 각각의 수신되는 {@link ServerWebExchange} 요청에 대해 작동하며 "Authorization" 헤더에 "Bearer " 접두사가 있는 유효한
 * JWT 토큰이 있는지 확인합니다. 검증이 성공하면 인증 정보가 하위 필터나 컴포넌트가 접근할 수 있도록 보안 컨텍스트에 추가됩니다.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter implements WebFilter {

  private final JwtTokenProvider jwtTokenProvider;

  /**
   * 들어오는 {@link ServerWebExchange}에 JWT 기반 인증을 처리하는 필터링 로직을 적용합니다. 유효한 JWT 토큰이 요청에서 감지되면 사용자 인증 세부
   * 정보를 추출하여 리액티브 체인의 후속 처리를 위해 보안 컨텍스트에 추가합니다.
   *
   * @param exchange HTTP 요청과 응답을 포함하는 현재 웹 교환
   * @param chain 체인의 다음 필터에 위임하는 웹 필터 체인
   * @return JWT 기반 인증 여부와 관계없이 필터 작업이 완료될 때 완료되는 {@link Mono}
   */
  @Override
  @NonNull
  public Mono<Void> filter(ServerWebExchange exchange, @NonNull WebFilterChain chain) {
    String token = resolveToken(exchange.getRequest());

    if (token != null && jwtTokenProvider.validateToken(token)) {
      Authentication authentication = jwtTokenProvider.getAuthentication(token);
      return chain
          .filter(exchange)
          .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication));
    }

    return chain.filter(exchange);
  }

  /**
   * 주어진 {@link ServerHttpRequest}의 Authorization 헤더에서 JWT 토큰을 추출하여 반환합니다. 이 메서드는 "Bearer " 접두사의 존재
   * 여부를 확인하고 그 뒤의 토큰 부분을 반환합니다.
   *
   * @param request Authorization 헤더를 검색할 {@link ServerHttpRequest}
   * @return Authorization 헤더에 "Bearer " 접두사와 함께 토큰이 있으면 JWT 토큰을 {@link String}으로 반환하고, 헤더가 없거나 유효한
   *     토큰이 없으면 {@code null}을 반환
   */
  private String resolveToken(ServerHttpRequest request) {
    String bearerToken = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
    if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
      return bearerToken.substring(7);
    }
    return null;
  }
}
