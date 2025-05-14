package com.linkedout.apigateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;

/**
 * API Gateway 애플리케이션의 메인 클래스
 *
 * <p>이 클래스는 Spring Cloud Gateway 애플리케이션의 시작점입니다. API Gateway는 마이크로서비스 아키텍처에서 클라이언트의 요청을 적절한 서비스로
 * 라우팅하는 역할을 합니다. 클라이언트는 여러 마이크로서비스에 직접 접근하지 않고, 이 API Gateway를 통해 모든 서비스에 접근합니다.
 *
 * <p>{@code @SpringBootApplication:} - Spring Boot 애플리케이션의 시작점을 나타내는 애노테이션
 * - @Configuration, @EnableAutoConfiguration, @ComponentScan을 합친 것과 같은 효과가 있음 - 자동 설정, 컴포넌트 스캔 등
 * Spring Boot의 기본 기능을 활성화함
 *
 * <p>{@code @EnableRabbit:} - RabbitMQ 메시지 브로커를 사용하기 위한 설정을 활성화하는 애노테이션 - 메시지 큐를 통한 비동기 통신을 가능하게 함
 * - RabbitMQ는 서비스 간 통신에 사용되는 메시지 큐 시스템임
 *
 * <p>{@code @ConditionalOnProperty:} - 특정 프로퍼티 값에 따라 Bean의 생성 여부를 결정하는 애노테이션 -
 * spring.rabbitmq.enabled 속성이 true일 때만 RabbitMQ 관련 Bean들이 생성됨 - matchIfMissing=true로 설정되어 있어 해당 속성이
 * 없으면 기본값은 true로 간주됨 - 즉, 명시적으로 false로 설정하지 않는 한 RabbitMQ 기능이 활성화됨
 */
@SpringBootApplication
@EnableRabbit
@ConditionalOnProperty(
    name = "spring.rabbitmq.enabled",
    havingValue = "true",
    matchIfMissing = true)
public class ApiGatewayApplication {

  /**
   * 애플리케이션의 진입점(엔트리 포인트)인 main 메서드
   *
   * <p>Spring Boot 애플리케이션을 시작하는 메서드입니다. SpringApplication.run() 메서드를 호출하여 Spring 컨텍스트를 초기화하고, 내장 웹
   * 서버를 시작하며, 필요한 모든 Bean들을 생성합니다.
   *
   * @param args 명령행 인자(커맨드 라인 아규먼트)
   */
  public static void main(String[] args) {
    // Spring Boot 애플리케이션을 실행합니다.
    // ApiGatewayApplication.class를 구성 클래스로 사용하여 애플리케이션 컨텍스트를 생성합니다.
    SpringApplication.run(ApiGatewayApplication.class, args);
  }
}
