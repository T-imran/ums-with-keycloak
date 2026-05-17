package com.erainfotech.ums.service;

import com.erainfotech.ums.dto.LoginRequest;
import com.erainfotech.ums.dto.LogoutRequest;
import com.erainfotech.ums.dto.RefreshTokenRequest;
import com.erainfotech.ums.dto.RegisterRequest;
import com.erainfotech.ums.dto.TokenResponse;
import com.erainfotech.ums.dto.UserResponse;

public interface AuthService {

    TokenResponse login(LoginRequest request);

    UserResponse register(RegisterRequest request);

    TokenResponse refresh(RefreshTokenRequest request);

    void logout(LogoutRequest request);
}
