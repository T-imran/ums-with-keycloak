package com.erainfotech.ums.controller;

import com.erainfotech.ums.dto.LoginAttemptResponse;
import com.erainfotech.ums.service.LoginAttemptAdminService;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/login-attempts")
public class LoginAttemptAdminController {

    private final LoginAttemptAdminService loginAttemptAdminService;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_ADMIN')")
    public List<LoginAttemptResponse> listLoginAttempts(
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String clientId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            @RequestParam(required = false) Integer first,
            @RequestParam(required = false) Integer max) {
        return loginAttemptAdminService.listLoginAttempts(userId, clientId, from, to, first, max);
    }
}
