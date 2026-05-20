package com.erainfotech.ums.controller;

import com.erainfotech.ums.auth.dto.LoginRequest;
import com.erainfotech.ums.auth.dto.LogoutRequest;
import com.erainfotech.ums.auth.dto.RefreshTokenRequest;
import com.erainfotech.ums.auth.dto.TokenResponse;
import com.erainfotech.ums.auth.service.AuthenticationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
@CrossOrigin("*")
public class AuthController {

    private final AuthenticationService authenticationService;

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return authenticationService.login(request);
    }

    @PostMapping("/refresh")
    public TokenResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return authenticationService.refresh(request);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@Valid @RequestBody LogoutRequest request) {
        authenticationService.logout(request);
    }
}
