package com.warehouse.controller;

import com.warehouse.annotation.OperationLog;
import com.warehouse.common.ApiResponse;
import com.warehouse.entity.OperationType;
import com.warehouse.entity.User;
import com.warehouse.security.LoginResponse;
import com.warehouse.service.UserService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class UserController {
    private final UserService userService;

    @PostMapping("/login")
    @OperationLog(type = OperationType.LOGIN, description = "用户登录", target = "用户")
    public ApiResponse<LoginResponse> login(@RequestBody LoginRequest request) {
        return ApiResponse.success(userService.login(request.getUsername(), request.getPassword()));
    }

    @PostMapping("/refresh")
    public ApiResponse<LoginResponse> refreshToken(@RequestBody RefreshRequest request) {
        return ApiResponse.success(userService.refreshToken(request.getRefreshToken()));
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

    @Data
    public static class RefreshRequest {
        private String refreshToken;
    }
}
