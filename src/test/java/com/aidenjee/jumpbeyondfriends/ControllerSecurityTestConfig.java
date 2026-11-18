package com.aidenjee.jumpbeyondfriends;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;

import com.aidenjee.jumpbeyondfriends.auth.AuthService;
import com.aidenjee.jumpbeyondfriends.auth.SecurityConfig;

@TestConfiguration(proxyBeanMethods = false)
@Import({SecurityConfig.class, AuthService.class})
class ControllerSecurityTestConfig {
    @Bean
    ClientRegistrationRepository clientRegistrationRepository() {
        return new InMemoryClientRegistrationRepository(TestGoogleUsers.GOOGLE_CLIENT);
    }
}
