package com.simform.service;

import com.simform.dto.AttributeVerificationDto;
import com.simform.dto.MfaModificationDto;
import com.simform.dto.MfaVerificationDto;
import com.simform.dto.SignInDto;
import com.simform.dto.SignUpDto;
import com.simform.util.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;

public interface UserService {

    ApiResponse signUp(SignUpDto signUpDto);

    ApiResponse signIn(SignInDto signInDto);

    ApiResponse sendVerificationCode(HttpServletRequest request, AttributeVerificationDto verificationDto);

    ApiResponse verifyCode(HttpServletRequest request, AttributeVerificationDto verificationDto);

    ApiResponse modifyMfaSetting(MfaModificationDto mfaModificationDto);

    ApiResponse verifyMfaCode(MfaVerificationDto mfaVerificationDto);
}
