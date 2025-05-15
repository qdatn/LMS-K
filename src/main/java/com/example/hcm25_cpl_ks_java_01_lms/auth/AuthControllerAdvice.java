package com.example.hcm25_cpl_ks_java_01_lms.auth;

import com.example.hcm25_cpl_ks_java_01_lms.config.security.JwtTokenUtils;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice(assignableTypes = {AuthRestController.class, AuthController.class})
public class AuthControllerAdvice { 
    @Autowired
    private JwtTokenUtils jwtTokenUtils;

    @ModelAttribute("valid")
    public boolean checkJwt(HttpServletRequest request) {
        String jwt = null;
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("jwt".equals(cookie.getName())) {
                    jwt = cookie.getValue();
                    break;
                }
            }
        }

        return jwt != null && !jwt.isEmpty() && jwtTokenUtils.validateToken(jwt);
    }
}
