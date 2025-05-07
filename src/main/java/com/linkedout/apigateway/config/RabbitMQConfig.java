package com.linkedout.apigateway.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.linkedout.common.constant.RabbitMQConstants;

/**
 * RabbitMQ 설정을 담당하는 클래스
 *
 * <p>이 클래스는 RabbitMQ 메시지 브로커와의 통신에 필요한 다양한 Bean들을 정의합니다. RabbitMQ는 메시지 큐 시스템으로, 이 프로젝트에서는 API
 * Gateway와 마이크로서비스 간의 비동기 통신을 위해 사용됩니다.
 *
 * <p>{@code @Configuration}: - Spring의 설정 클래스임을 나타내는 애노테이션 - Spring IoC 컨테이너에 의해 Bean 정의의 소스로 처리됨
 *
 * <p>{@code @ConditionalOnProperty}: - 지정된 속성 값에 따라 이 설정 클래스의 활성화 여부를 결정하는 애노테이션 -
 * spring.rabbitmq.enabled 속성이 true일 때만 이 설정 클래스가 활성화됨 - matchIfMissing=true로 설정되어 있어 해당 속성이 없으면
 * 기본값은 true로 간주됨 - 즉, 설정 파일에서 명시적으로 false로 설정하지 않는 한 RabbitMQ 관련 설정이 활성화됨
 */
@Configuration
@ConditionalOnProperty(
    name = "spring.rabbitmq.enabled",
    havingValue = "true",
    matchIfMissing = true)
public class RabbitMQConfig {

  /**
   * 마이크로서비스 통신을 위한 중앙 교환기를 정의하는 Bean
   *
   * <p>이 교환기는 게이트웨이와 모든 마이크로서비스 간의 메시지 라우팅을 담당합니다. 라우팅 키에 따라 메시지를 적절한 큐로 전달합니다.
   *
   * @return 서비스 교환기
   */
  @Bean
  public DirectExchange apiExchange() {
    // 첫 번째 매개변수: 교환기 이름
    // 두 번째 매개변수: durable (true로 설정하면 RabbitMQ 서버가 재시작되어도 교환기가 유지됨)
    // 세 번째 매개변수: autoDelete (자동 삭제 여부)
    return new DirectExchange(RabbitMQConstants.API_EXCHANGE, true, false);
  }

  /**
   * API 게이트웨이 통신을 위한 RabbitMQ 큐를 정의합니다.
   *
   * <p>이 큐는 API 게이트웨이와 다른 마이크로서비스 간의 비동기 메시지 전달에 사용됩니다. non-durable로 생성되어 RabbitMQ 브로커가 재시작될 때 큐가
   * 유지되지 않습니다.
   *
   * @return RabbitMQConstants.GATEWAY_QUEUE 이름으로 지정된 non-durable 큐의 새 인스턴스
   */
  @Bean
  public Queue apiGatewayQueue() {
    return new Queue(RabbitMQConstants.GATEWAY_QUEUE, false);
  }

  /**
   * API 게이트웨이 큐와 직접 교환기 간의 바인딩을 특정 라우팅 키를 사용하여 정의합니다.
   *
   * <p>이 바인딩을 통해 교환기로 전송된 메시지가 정의된 라우팅 키를 사용하여 API 게이트웨이 큐로 라우팅될 수 있습니다.
   *
   * @return API 게이트웨이 큐를 지정된 라우팅 키로 교환기에 바인딩하는 Binding 인스턴스
   */
  @Bean
  public Binding responseBinding() {
    return BindingBuilder.bind(apiGatewayQueue()) // GATEWAY_QUEUE
        .to(apiExchange()) // Exchange
        .with(RabbitMQConstants.API_RESPONSE_ROUTING_KEY); // 라우팅 키
  }

  /**
   * JSON 형식의 메시지 변환기를 정의하는 Bean
   *
   * <p>이 변환기는 Java 객체와 JSON 메시지 간의 변환을 담당합니다. RabbitMQ 메시지의 본문을 JSON 형식으로 직렬화하거나 역직렬화할 때 사용됩니다.
   *
   * <p>Jackson2JsonMessageConverter는 Jackson 라이브러리를 사용하여 Java 객체를 JSON으로, JSON을 Java 객체로 변환해주는
   * 클래스입니다.
   *
   * @return JSON 메시지 변환기
   */
  @Bean
  public MessageConverter jsonMessageConverter() {
    return new Jackson2JsonMessageConverter();
  }

  /**
   * RabbitMQ와의 통신을 담당하는 템플릿을 정의하는 Bean
   *
   * <p>RabbitTemplate은 RabbitMQ와의 통신을 쉽게 해주는 클래스로, 메시지 전송, 수신 등의 작업을 처리합니다.
   *
   * @param connectionFactory RabbitMQ 연결 팩토리 (Spring Boot가 자동으로 주입)
   * @return 설정된 RabbitTemplate
   */
  @Bean
  public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
    // RabbitTemplate 생성
    RabbitTemplate template = new RabbitTemplate(connectionFactory);
    // JSON 메시지 변환기 설정
    template.setMessageConverter(jsonMessageConverter());
    return template;
  }
}
