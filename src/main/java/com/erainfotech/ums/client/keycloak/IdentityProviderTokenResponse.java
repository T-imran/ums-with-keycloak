package com.erainfotech.ums.client.keycloak;

public record IdentityProviderTokenResponse(
        String access_token,
        long expires_in
) {
    public String accessToken() {
        return access_token;
    }

    public long expiresIn() {
        return expires_in;
    }
}
