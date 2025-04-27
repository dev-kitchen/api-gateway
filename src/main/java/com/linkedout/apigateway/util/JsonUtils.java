package com.linkedout.apigateway.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

/**
 * JSON 변환 유틸리티 클래스
 * <p>
 * 이 클래스는 Jackson ObjectMapper를 사용하여 객체와 JSON 문자열 간의 
 * 변환을 쉽게 처리하는 유틸리티 메서드를 제공합니다.
 * <p>
 * {@code @Component}: 
 *    - Spring이 이 클래스를 컴포넌트로 인식하고 Bean으로 등록하도록 함
 */
@Component
public class JsonUtils {
    
    /**
     * ObjectMapper 인스턴스
     * Jackson 라이브러리에서 제공하는 JSON 변환 핵심 클래스입니다.
     */
    private final ObjectMapper objectMapper;
    
    /**
     * 생성자
     * 
     * @param objectMapper Spring이 자동으로 주입하는 ObjectMapper 인스턴스
     */
    public JsonUtils(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
    
    /**
     * 객체를 JSON 문자열로 변환
     * 
     * @param object 변환할 객체
     * @return 객체를 표현하는 JSON 문자열
     * @throws RuntimeException JSON 변환 중 오류 발생 시
     */
    public String toJson(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON 변환 중 오류 발생", e);
        }
    }
    
    /**
     * JSON 문자열을 지정된 클래스의 객체로 변환
     * 
     * @param json JSON 문자열
     * @param valueType 변환할 객체의 클래스
     * @param <T> 반환 객체의 타입
     * @return 변환된 객체
     * @throws RuntimeException JSON 변환 중 오류 발생 시
     */
    public <T> T fromJson(String json, Class<T> valueType) {
        try {
            return objectMapper.readValue(json, valueType);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON 변환 중 오류 발생", e);
        }
    }
}