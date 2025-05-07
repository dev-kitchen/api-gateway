package com.linkedout.apigateway.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.config.CorsRegistry;
import org.springframework.web.reactive.config.WebFluxConfigurer;

/**
 * WebFlux 설정을 커스터마이징하기 위한 설정 클래스
 *
 * <p>이 클래스는 {@code @Configuration} 어노테이션이 적용되어 있고 {@code WebFluxConfigurer}를 구현하여 WebFlux 프레임워크의 기본
 * 설정을 커스터마이징할 수 있게 해줍니다. 구체적으로 Cross-Origin Resource Sharing (CORS) 정책을 설정하는 데 사용됩니다.
 *
 * <p>CORS 매핑은 모든 출처에서의 요청을 허용하고 지정된 HTTP 메서드와 헤더를 사용할 수 있도록 정의됩니다.
 */
@Configuration
public class WebConfig implements WebFluxConfigurer {

  @Override
  public void addCorsMappings(CorsRegistry registry) {
    registry
        .addMapping("/**")
        //			.allowCredentials(true)
        .allowedOrigins("*")
        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
        .allowedHeaders("*")
        .maxAge(3600);
  }
}
