package com.simform.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class MfaVerificationDto {
    private String username;
    private String mfaCode;
    private String sessionId;
}
