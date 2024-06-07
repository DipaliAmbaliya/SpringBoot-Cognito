package com.simform.filter;

import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class JWTAuthenticationFilter extends OncePerRequestFilter {

    @Value("${aws.cognito.jwk}")
    private String jwk;

    private static final String[] PUBLIC_ENDPOINTS = {
            "/api/users/sign-up",
            "/api/users/sign-in",
            "/api/users/modify-mfa-setting",
            "/api/users/verify-mfa-code"
    };

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        boolean result = false;
        for (String endPoint : PUBLIC_ENDPOINTS) {
            if (request.getRequestURI().equals(endPoint)) {
                result = true;
                break;
            }
        }
        if (result) {
            filterChain.doFilter(request, response);
        } else {
            String authorizationHeader = request.getHeader("Authorization");
            String accessToken = null;
            if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
                accessToken = authorizationHeader.substring("Bearer ".length());
                try {
                    JWKSet jwkSet = fetchJWKSet();
                    JWSObject jwsObject = JWSObject.parse(accessToken);
                    JWSHeader header = jwsObject.getHeader();
                    JWK jwk = jwkSet.getKeyByKeyId(header.getKeyID());
                    if (jwk != null) {
                        JWSVerifier verifier = new RSASSAVerifier((RSAPublicKey) jwk.toRSAKey().toPublicKey());
                        if (jwsObject.verify(verifier)) {
                            Payload payload = jwsObject.getPayload();
                            Map<String, Object> map = payload.toJSONObject();
                            Date expirationTime = map.get("exp") != null ? new Date((long) map.get("exp") * 1000) : null;
                            if (expirationTime != null && expirationTime.before(new Date())) {
                                response.sendError(401, "Unauthorized");
                            }
                            String username = (String) map.get("sub");
                            List<GrantedAuthority> authorities = new ArrayList<>();
                            authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
                            UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(username, null, authorities);
                            SecurityContextHolder.getContext().setAuthentication(authenticationToken);
                            filterChain.doFilter(request, response);
                        } else {
                            response.sendError(401, "Unauthorized");
                        }
                    } else {
                        response.sendError(401, "Unauthorized");
                    }
                } catch (Exception e) {
                    log.error("Exception {}", e.getMessage());
                    response.sendError(401, "Unauthorized");
                }
            } else {
                response.sendError(401, "Unauthorized");
            }
        }
    }

    private JWKSet fetchJWKSet() throws Exception {
        URL jwkSetURL = new URL(jwk);
        InputStream inputStream = jwkSetURL.openStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder jwkSetString = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            jwkSetString.append(line);
        }
        return JWKSet.parse(jwkSetString.toString());
    }
}
