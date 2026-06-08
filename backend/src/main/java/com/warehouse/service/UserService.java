package com.warehouse.service;

import com.warehouse.entity.User;
import com.warehouse.repository.UserRepository;
import com.warehouse.security.JwtUtil;
import com.warehouse.security.LoginResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    public LoginResponse login(String username, String password) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!user.getPassword().equals(password)) {
            throw new RuntimeException("Invalid password");
        }

        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getUsername(), user.getRole());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId(), user.getUsername());

        return new LoginResponse(accessToken, refreshToken, LoginResponse.UserInfo.from(user));
    }

    public LoginResponse refreshToken(String refreshToken) {
        if (!jwtUtil.validateRefreshToken(refreshToken)) {
            throw new RuntimeException("Invalid or expired refresh token");
        }

        Long userId = jwtUtil.getUserId(refreshToken);
        String username = jwtUtil.getUsername(refreshToken);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String newAccessToken = jwtUtil.generateAccessToken(user.getId(), user.getUsername(), user.getRole());
        String newRefreshToken = jwtUtil.generateRefreshToken(user.getId(), user.getUsername());

        return new LoginResponse(newAccessToken, newRefreshToken, LoginResponse.UserInfo.from(user));
    }

    public User register(User user) {
        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            throw new RuntimeException("Username already exists");
        }
        return userRepository.save(user);
    }
}
