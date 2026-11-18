package com.aidenjee.jumpbeyondfriends.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service 
public class EnrollService {
    private final String enrollCodeHash;
    
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public EnrollService(
        @Value("${app.enroll.code-hash}") String enrollCodeHash
    ) {
        this.enrollCodeHash = enrollCodeHash;
    }

    public boolean isValidCode(String code) {
        return code != null && encoder.matches(code, enrollCodeHash);
    } 
}
