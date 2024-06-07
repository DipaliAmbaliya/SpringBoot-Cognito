package com.simform.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SignInResponseDto {
    private String accessToken;
    private String idToken;
    private String refreshToken;
    private String tokenType;
    private int expirationTime;
}
