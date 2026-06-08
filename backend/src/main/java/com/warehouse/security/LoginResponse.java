package com.warehouse.security;

import com.warehouse.entity.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginResponse {
    private String accessToken;
    private String refreshToken;
    private UserInfo user;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class UserInfo {
        private Long id;
        private String username;
        private String role;

        public static UserInfo from(User user) {
            return new UserInfo(user.getId(), user.getUsername(), user.getRole());
        }
    }
}
