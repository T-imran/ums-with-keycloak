package com.erainfotech.ums.controller;

import com.erainfotech.ums.dto.LoginSecurityAttributesRequest;
import com.erainfotech.ums.service.UserSecurityAttributeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users/{userId}/security-attributes")
public class UserSecurityAttributeController {

    private final UserSecurityAttributeService userSecurityAttributeService;

    @PostMapping("/login-context")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_ADMIN')")
    public void updateLoginSecurityAttributes(@PathVariable String userId,
                                              @Valid @RequestBody LoginSecurityAttributesRequest request) {
        userSecurityAttributeService.updateLoginSecurityAttributes(userId, request.toLoginContext());
    }
}
