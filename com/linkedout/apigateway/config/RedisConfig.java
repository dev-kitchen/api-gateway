package com.linkedout.apigateway.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.linkedout.common.dto.ApiResponseData;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 설정 클래스
 * 
 * <p>이 클래스는 Redis 연결 및 직렬화 설정을 제공합니다.
 * 메시지 응답 처리를 위한 ReactiveRedisTemplate을 구성합니다.
 */
@Configuration
public class RedisConfig {

    /**
     * ApiResponseData를 위한 ReactiveRedisTemplate 빈 구성
     * 
     * @param connectionFactory Redis 연결 팩토리
     * @param objectMapper JSON 직렬화를 위한 ObjectMapper
     * @return ApiResponseData를 처리하기 위한 ReactiveRedisTemplate 인스턴스
     */
    @Bean
    public ReactiveRedisTemplate<String, ApiResponseData> reactiveRedisTemplate(
            ReactiveRedisConnectionFactory connectionFactory,
            ObjectMapper objectMapper) {
        
        // ApiResponseData를 위한 Jackson 직렬화기 설정
        Jackson2JsonRedisSerializer<ApiResponseData> serializer = 
                new Jackson2JsonRedisSerializer<>(objectMapper, ApiResponseData.class);
        
        // 문자열 키와 ApiResponseData 값을 위한 직렬화 컨텍스트 구성
        RedisSerializationContext<String, ApiResponseData> serializationContext = 
                RedisSerializationContext.<String, ApiResponseData>newSerializationContext()
                        .key(StringRedisSerializer.UTF_8)
                        .value(serializer)
                        .hashKey(StringRedisSerializer.UTF_8)
                        .hashValue(serializer)
                        .build();
        
        return new ReactiveRedisTemplate<>(connectionFactory, serializationContext);
    }
}