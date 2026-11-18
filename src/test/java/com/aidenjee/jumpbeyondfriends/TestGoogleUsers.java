package com.aidenjee.jumpbeyondfriends;

import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;

final class TestGoogleUsers {
    static final String GOOGLE_SUB = "google-player-a";
    static final String NAME = "Player A";
    static final String AVATAR_URL = "https://example.com/player-a.png";
    static final String ENROLLMENT_CODE = "test-enrollment-code";
    static final ClientRegistration GOOGLE_CLIENT = CommonOAuth2Provider.GOOGLE.getBuilder("google")
        .clientId("test-google-client")
        .clientSecret("test-google-secret")
        .build();

    private TestGoogleUsers() {}

    static OidcLoginRequestPostProcessor googleUser(String subject) {
        return oidcLogin()
            .clientRegistration(GOOGLE_CLIENT)
            .idToken(token -> token.subject(subject)
                .claim("name", NAME)
                .claim("picture", AVATAR_URL));
    }
}
