package com.linkedout.apigateway.config;

import com.linkedout.apigateway.filter.MessageQueueGatewayFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * API Gateway의 라우팅 설정을 담당하는 설정 클래스
 * <p>
 * 이 클래스는 클라이언트 요청을 어떤 서비스로 라우팅할지 정의합니다.
 * Spring Cloud Gateway는 요청 경로(path)에 따라 적절한 서비스로 요청을 전달합니다.
 * 일반적인 API Gateway는 HTTP 요청을 다른 서비스로 직접 전달하지만,
 * 이 프로젝트에서는 RabbitMQ라는 메시지 큐를 통해 비동기 방식으로 요청을 처리합니다.
 * <p>
 * {@code @Configuration}: 
 *    - Spring의 설정 클래스임을 나타내는 애노테이션
 *    - Spring IoC 컨테이너에 의해 Bean 정의의 소스로 처리됨
 *    - 이 클래스 안에 정의된 @Bean 메서드들이 Spring 컨테이너에 등록됨
 */
@Configuration
public class GatewayConfig {
    
    /**
     * RabbitMQ로 메시지를 전송하는 필터 주입
     * <p>
     * {@code @Autowired}: 
     *    - Spring이 MessageQueueGatewayFilter 타입의 Bean을 찾아 자동으로 주입(의존성 주입)
     *    - 이 필터는 HTTP 요청을 RabbitMQ 메시지로 변환하는 역할을 함
     */
    @Autowired
    private MessageQueueGatewayFilter messageQueueGatewayFilter;
    
    /**
     * API Gateway의 라우트를 정의하는 Bean
     * <p>
     * 라우트란 클라이언트 요청을 어떤 조건에 따라 어디로 보낼지 정의한 규칙입니다.
     * Spring Cloud Gateway에서는 요청 경로, 헤더, 쿼리 파라미터 등 다양한 조건으로
     * 라우팅 규칙을 정의할 수 있습니다.
     * <p>
     * 이 프로젝트에서는 각 요청을 해당하는 RabbitMQ 큐로 전송합니다:
     * - /api/users/** 경로로 오는 요청 → user-service-queue 큐로 전송
     * - /api/recipes/** 경로로 오는 요청 → recipe-service-queue 큐로 전송
     * <p>
     * {@code @Bean}:
     *    - 이 메서드가 Spring 컨테이너에 의해 관리되는 Bean을 생성함을 나타냄
     *    - 메서드의 반환값이 Spring 컨테이너에 등록됨
     * 
     * @param builder 라우트 구성을 도와주는 RouteLocatorBuilder 객체 (Spring이 자동 주입)
     * @return 정의된 라우트들이 포함된 RouteLocator 객체
     */
    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
            // 사용자 서비스 라우팅 설정
            .route("user-service", r -> r.path("/api/users/**")  // /api/users/로 시작하는 모든 요청을 매칭
                .filters(f -> f.filter(messageQueueGatewayFilter.apply(
                    new MessageQueueGatewayFilter.Config("user-service-queue"))))  // 요청을 메시지로 변환하여 user-service-queue로 전송
                .uri("no://op"))  // 실제로 호출되지 않는 더미 URI (메시지 큐로 전송하므로 HTTP 직접 호출을 하지 않음)
                
            // 레시피 서비스 라우팅 설정
            .route("recipe-service", r -> r.path("/api/recipes/**")  // /api/recipes/로 시작하는 모든 요청을 매칭
                .filters(f -> f.filter(messageQueueGatewayFilter.apply(
                    new MessageQueueGatewayFilter.Config("recipe-service-queue"))))  // 요청을 메시지로 변환하여 recipe-service-queue로 전송
                .uri("no://op"))  // 실제로 호출되지 않는 더미 URI
                
            .build();  // 라우트 구성을 완료하고 RouteLocator 객체 생성
    }
}