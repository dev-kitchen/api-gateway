package com.linkedout.apigateway.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.text.SimpleDateFormat;

/**
 * Jackson JSON 처리 관련 설정 클래스
 *
 * <p>이 클래스는 JSON 직렬화/역직렬화를 위한 ObjectMapper Bean을 설정합니다.
 *
 * <p>{@code @Configuration}: - Spring의 설정 클래스임을 나타내는 애노테이션 - Spring IoC 컨테이너에 의해 Bean 정의의 소스로 처리됨
 */
@Configuration
public class JacksonConfig {

  /**
   * JSON 직렬화/역직렬화를 위한 ObjectMapper Bean 정의
   *
   * <p>이 ObjectMapper는 애플리케이션 전체에서 사용되며, 필요에 따라 설정을 커스터마이징할 수 있습니다.
   *
   * @return 설정된 ObjectMapper 인스턴스
   */
  @Bean
  public ObjectMapper objectMapper() {
		ObjectMapper objectMapper = new ObjectMapper();

		objectMapper.registerModule(new JavaTimeModule());

    // 날짜/시간 값을 ISO-8601 형식으로 직렬화하도록 설정
		objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

    // 빈 Bean에 대해 오류를 발생시키지 않도록 설정
		objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);

		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		objectMapper.setDateFormat(dateFormat);

    return objectMapper;
  }
}
