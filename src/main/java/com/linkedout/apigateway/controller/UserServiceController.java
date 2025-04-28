package com.linkedout.apigateway.controller;

import com.linkedout.apigateway.model.ApiResponse;
import com.linkedout.apigateway.service.ResponseHandlerService;
import com.linkedout.apigateway.util.JsonUtils;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;


@RestController
@RequestMapping("/api/users")
public class UserServiceController extends BaseServiceController {

    private static final String USER_SERVICE_QUEUE = "user-service-queue";
    
    public UserServiceController(
            RabbitTemplate rabbitTemplate,
            ResponseHandlerService responseHandlerService,
            JsonUtils jsonUtils) {
        super(rabbitTemplate, responseHandlerService, jsonUtils);
    }

    @RequestMapping("/**")
    public Mono<ApiResponse<?>> handleUserServiceRequest(ServerWebExchange exchange) {
        return processRequest(exchange, USER_SERVICE_QUEUE);
    }
}