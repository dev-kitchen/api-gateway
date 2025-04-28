package com.linkedout.apigateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * API Gateway의 라우팅 설정을 담당하는 설정 클래스
 * <p>
 * 이 클래스는 클라이언트 요청을 어떤 경로로 라우팅할지 정의합니다.
 * Spring Cloud Gateway는 요청 경로(path)에 따라 적절한 컨트롤러로 요청을 전달합니다.
 */
@Configuration
public class GatewayConfig {

    /**
     * API Gateway의 라우트를 정의하는 Bean
     * <p>
     * 이 메서드는 다양한 경로에 대한 라우팅 규칙을 정의합니다.
     * 이전에는 MessageQueueGatewayFilter를 사용했지만, 이제는 컨트롤러가 그 역할을 담당합니다.
     * 따라서 여기서는 단순히 요청을 적절한 컨트롤러로 포워딩하는 역할만 합니다.
     */
    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
            // 헬스체크 경로 설정 - HealthCheckController에서 처리
            .route("health-check", r -> r.path("/health")
                .uri("forward:/health"))
                
            // 사용자 서비스 경로 - UserServiceController에서 처리
            .route("user-service", r -> r.path("/api/users/**")
                .uri("forward:/api/users"))
                
            // 레시피 서비스 경로 - RecipeServiceController에서 처리
            .route("recipe-service", r -> r.path("/api/recipes/**")
                .uri("forward:/api/recipes"))
                
            .build();
    }
}