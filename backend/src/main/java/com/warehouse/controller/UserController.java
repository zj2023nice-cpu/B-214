package com.warehouse.controller;

import com.warehouse.common.ApiResponse;
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
    public ApiResponse<User> login(@RequestBody LoginRequest request) {
        return ApiResponse.success(userService.login(request.getUsername(), request.getPassword()));
    }

    @PostMapping("/register")
    public ApiResponse<User> register(@RequestBody User user) {
        return ApiResponse.success(userService.register(user));
    }

    @Data
    public static class LoginRequest {
        private String username;
        private String password;
    }
}
