package com.warehouse.controller;

import com.warehouse.annotation.OperationLog;
import com.warehouse.common.ApiResponse;
import com.warehouse.entity.OperationType;
import com.warehouse.entity.User;
import com.warehouse.service.UserService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // For local dev, overridden by Nginx in prod
public class UserController {
    private final UserService userService;

    @PostMapping("/login")
    @OperationLog(type = OperationType.LOGIN, description = "用户登录", target = "用户")
    public ApiResponse<User> login(@RequestBody LoginRequest request) {
        return ApiResponse.success(userService.login(request.getUsername(), request.getPassword()));
    }

    @PostMapping("/register")
    @OperationLog(type = OperationType.CREATE, description = "用户注册", target = "用户")
    public ApiResponse<User> register(@RequestBody User user) {
        return ApiResponse.success(userService.register(user));
    }

    @Data
    public static class LoginRequest {
        private String username;
        private String password;
    }
}
