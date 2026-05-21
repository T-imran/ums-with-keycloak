package com.erainfotech.ums.controller;

import com.erainfotech.ums.dto.ClientResponse;
import com.erainfotech.ums.dto.CreateClientRequest;
import com.erainfotech.ums.dto.UpdateClientRequest;
import com.erainfotech.ums.service.ClientAdminService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/clients")
public class ClientAdminController {

    private final ClientAdminService clientAdminService;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_ADMIN')")
    public List<ClientResponse> listClients(@RequestParam(required = false) String clientId) {
        return clientAdminService.listClients(clientId);
    }

    @GetMapping("/{clientId}")
    @PreAuthorize("hasAnyAuthority('ROLE_SUPER_ADMIN', 'ROLE_ADMIN')")
    public ClientResponse getClient(@PathVariable String clientId) {
        return clientAdminService.findClient(clientId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
    public ClientResponse createClient(@Valid @RequestBody CreateClientRequest request) {
        return clientAdminService.createClient(request);
    }

    @PutMapping("/{clientId}")
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
    public ClientResponse updateClient(@PathVariable String clientId,
                                       @Valid @RequestBody UpdateClientRequest request) {
        return clientAdminService.updateClient(clientId, request);
    }
}
