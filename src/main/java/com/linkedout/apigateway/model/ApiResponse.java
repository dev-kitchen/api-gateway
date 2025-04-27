package com.linkedout.apigateway.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 표준화된 API 응답 형식을 위한 DTO 클래스
 * 
 * 이 클래스는 API 요청에 대한 응답을 일관된 형식으로 제공하기 위해 사용됩니다.
 * HTTP 상태 코드, 데이터, 메시지를 포함하는 구조로, 클라이언트가 응답을 쉽게 처리할 수 있게 해줍니다.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {
    
    /**
     * HTTP 상태 코드
     * 예: 200(OK), 400(Bad Request), 404(Not Found), 500(Internal Server Error) 등
     */
    private int status;
    
    /**
     * 실제 응답 데이터
     * 제네릭 타입(T)을 사용하여 다양한 형태의 데이터를 담을 수 있습니다.
     */
    private T data;
    
    /**
     * 응답 메시지
     * 성공 또는 오류에 대한 설명 메시지입니다.
     */
    private String message;
    
    /**
     * 오류 정보
     * 오류가 발생한 경우에만 포함됩니다.
     */
    private ErrorInfo error;
    
    /**
     * 성공 응답 생성 (상태 코드, 데이터, 메시지 포함)
     * 
     * @param status HTTP 상태 코드 (2xx)
     * @param data 응답 데이터
     * @param message 응답 메시지
     * @return 성공 응답 객체
     */
    public static <T> ApiResponse<T> success(int status, T data, String message) {
        return ApiResponse.<T>builder()
                .status(status)
                .data(data)
                .message(message)
                .build();
    }
    
    /**
     * 성공 응답 생성 (상태 코드, 데이터 포함)
     * 
     * @param status HTTP 상태 코드 (2xx)
     * @param data 응답 데이터
     * @return 성공 응답 객체
     */
    public static <T> ApiResponse<T> success(int status, T data) {
        return success(status, data, getDefaultMessageForStatus(status));
    }
    
    /**
     * 성공 응답 생성 (데이터, 메시지 포함, 상태 코드는 200 OK)
     * 
     * @param data 응답 데이터
     * @param message 응답 메시지
     * @return 성공 응답 객체
     */
    public static <T> ApiResponse<T> success(T data, String message) {
        return success(200, data, message);
    }
    
    /**
     * 성공 응답 생성 (데이터만 포함, 상태 코드는 200 OK)
     * 
     * @param data 응답 데이터
     * @return 성공 응답 객체
     */
    public static <T> ApiResponse<T> success(T data) {
        return success(200, data);
    }
    
    /**
     * 상태 코드에 따른 기본 메시지 반환
     * 
     * @param status HTTP 상태 코드
     * @return 상태 코드에 해당하는 기본 메시지
     */
    private static String getDefaultMessageForStatus(int status) {
        switch (status) {
            case 200: return "요청이 성공적으로 처리되었습니다.";
            case 201: return "리소스가 성공적으로 생성되었습니다.";
            case 202: return "요청이 수락되었고 처리 중입니다.";
            case 204: return "요청이 성공적으로 처리되었으며, 응답 데이터가 없습니다.";
            default: return status >= 200 && status < 300 ? "요청이 성공적으로 처리되었습니다." : "처리 중 오류가 발생했습니다.";
        }
    }
    
    /**
     * 오류 응답 생성
     * 
     * @param status HTTP 상태 코드
     * @param errorCode 오류 코드
     * @param message 오류 메시지
     * @return 오류 응답 객체
     */
    public static <T> ApiResponse<T> error(int status, String errorCode, String message) {
        return ApiResponse.<T>builder()
                .status(status)
                .message(message)
                .error(new ErrorInfo(errorCode, message))
                .build();
    }
    
    /**
     * 오류 정보를 담는 내부 클래스
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ErrorInfo {
        /**
         * 오류 코드
         * 예: "INVALID_PARAMETERS", "RESOURCE_NOT_FOUND" 등
         */
        private String code;
        
        /**
         * 오류 메시지
         * 상세한 오류 설명
         */
        private String message;
    }
}