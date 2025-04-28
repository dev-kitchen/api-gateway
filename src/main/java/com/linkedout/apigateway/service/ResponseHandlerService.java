package com.linkedout.apigateway.service;

import com.linkedout.apigateway.model.ResponseData;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 마이크로서비스 응답을 비동기적으로 처리하는 서비스
 * <p>
 * 이 서비스는 RabbitMQ를 통해 마이크로서비스로부터 받은 응답을 처리하고,
 * 해당 응답을 기다리고 있는 클라이언트 요청과 연결시켜 주는 역할을 합니다.
 * 리액티브 프로그래밍 방식으로 구현되어 있어 비동기 처리가 가능합니다.
 * <p>
 * {@code @Service}:
 * - Spring이 이 클래스를 서비스 컴포넌트로 인식하고 Bean으로 등록하도록 하는 애노테이션
 * - 비즈니스 로직을 처리하는 컴포넌트임을 나타냄
 */
@Service
public class ResponseHandlerService {
	public static final String GATEWAY_QUEUE = "api-gateway-queue";

	/**
	 * 응답 핸들러 맵
	 * <p>
	 * correlationId를 키로 하고, 해당 요청에 대한 응답을 기다리는 Sink를 값으로 가지는 맵입니다.
	 * ConcurrentHashMap을 사용하여 멀티스레드 환경에서의 동시성 문제를 해결합니다.
	 * <p>
	 * Sink란?
	 * - 리액터 프로젝트의 개념으로, 데이터를 수동으로 발행(emit)할 수 있는 객체입니다.
	 * - 여기서는 Sinks.One을 사용하여 정확히 하나의 값만 발행할 수 있는 Sink를 생성합니다.
	 */
	private final Map<String, Sinks.One<ResponseData>> responseHandlers = new ConcurrentHashMap<>();

	/**
	 * 특정 correlationId에 대한 응답을 기다리는 Mono를 반환하는 메서드
	 * <p>
	 * 이 메서드는 클라이언트 요청과 연관된 correlationId로 Sink를 생성하고,
	 * 이를 맵에 저장한 후, 해당 Sink를 통해 응답을 기다리는 Mono를 반환합니다.
	 * <p>
	 * Mono란?
	 * - 리액티브 프로그래밍에서 0 또는 1개의 결과를 비동기적으로 제공하는 발행자(Publisher)입니다.
	 * - "미래에 완료될 가능성이 있는 작업"을 나타내며, 콜백 대신 선언적 방식으로 비동기 작업을 처리합니다.
	 * - 이를 통해 비동기 코드를 더 선형적이고 읽기 쉽게 작성할 수 있습니다.
	 *
	 * @param correlationId 요청과 응답을 연결하는 상관관계 ID
	 * @return 응답 데이터를 포함하는 Mono
	 */
	public Mono<ResponseData> awaitResponse(String correlationId) {
		// Sinks.one()을 사용하여 단일 값 발행이 가능한 Sink 생성
		Sinks.One<ResponseData> sink = Sinks.one();
		// 맵에 correlationId와 Sink를 저장
		responseHandlers.put(correlationId, sink);

		// Sink를 Mono로 변환하고 타임아웃 설정 (30초)
		return sink.asMono()  // Sink를 Mono로 변환
			.timeout(Duration.ofSeconds(30))  // 30초 타임아웃 설정 (응답이 없으면 예외 발생)
			.doFinally(signalType -> responseHandlers.remove(correlationId));  // 처리 완료 후 맵에서 항목 제거
	}

	/**
	 * RabbitMQ 응답 큐에서 메시지를 수신하고 처리하는 메서드
	 * <p>
	 * 이 메서드는 RabbitMQ의 'gateway-response-queue'라는 큐에서 메시지를 수신하고,
	 * 해당 메시지의 correlationId를 사용하여 올바른 Sink를 찾아 응답을 전달합니다.
	 * <p>
	 * {@code @RabbitListener}:
	 * - 지정된 RabbitMQ 큐에서 메시지를 수신하는 메서드임을 나타내는 애노테이션
	 * - Spring AMQP가 자동으로 이 메서드를 메시지 리스너로 등록함
	 * - 큐에 메시지가 도착하면 자동으로 이 메서드가 호출됨
	 * <p>
	 * {@code @ConditionalOnProperty}:
	 * - 지정된 속성 값에 따라 이 메서드를 메시지 리스너로 등록할지 여부를 결정하는 애노테이션
	 * - spring.rabbitmq.enabled 속성이 true일 때만 이 메서드가 활성화됨
	 * - matchIfMissing=true로 설정되어 있어 해당 속성이 없으면 기본값은 true로 간주됨
	 *
	 * @param responseData 마이크로서비스로부터 받은 응답 데이터
	 */
	@RabbitListener(queues = GATEWAY_QUEUE)
	@ConditionalOnProperty(name = "spring.rabbitmq.enabled", havingValue = "true", matchIfMissing = true)
	public void handleResponse(ResponseData responseData) {
		// 응답 데이터에서 correlationId 추출
		String correlationId = responseData.getCorrelationId();
		// correlationId를 사용하여 맵에서 해당 Sink 찾기
		Sinks.One<ResponseData> sink = responseHandlers.get(correlationId);

		// Sink가 존재하면 응답 데이터 발행
		if (sink != null) {
			// tryEmitValue는 Sink에 값을 발행하는 메서드
			// 이렇게 하면 awaitResponse 메서드에서 반환한 Mono에 값이 전달됨
			sink.tryEmitValue(responseData);
		}
	}
}