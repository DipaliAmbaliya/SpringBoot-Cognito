package com.simform.controller;

import com.simform.dto.AttributeVerificationDto;
import com.simform.dto.MfaModificationDto;
import com.simform.dto.MfaVerificationDto;
import com.simform.dto.SignInDto;
import com.simform.dto.SignUpDto;
import com.simform.service.UserService;
import com.simform.util.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/sign-up")
    public ResponseEntity<ApiResponse> signUp(@RequestBody SignUpDto signUpDto) {
        ApiResponse apiResponse = userService.signUp(signUpDto);
        return new ResponseEntity<>(apiResponse, HttpStatusCode.valueOf(apiResponse.getStatusCode()));
    }

    @PostMapping("/sign-in")
    public ResponseEntity<ApiResponse> signIn(@RequestBody SignInDto signInDto) {
        ApiResponse apiResponse = userService.signIn(signInDto);
        return new ResponseEntity<>(apiResponse, HttpStatusCode.valueOf(apiResponse.getStatusCode()));
    }

    @PostMapping("/send-code")
    public ResponseEntity<ApiResponse> sendVerificationCode(HttpServletRequest request, @RequestBody AttributeVerificationDto verificationDto) {
        ApiResponse apiResponse = userService.sendVerificationCode(request, verificationDto);
        return new ResponseEntity<>(apiResponse, HttpStatusCode.valueOf(apiResponse.getStatusCode()));
    }

    @PostMapping("/verify-code")
    public ResponseEntity<ApiResponse> verifyCode(HttpServletRequest request, @RequestBody AttributeVerificationDto verificationDto) {
        ApiResponse apiResponse = userService.verifyCode(request, verificationDto);
        return new ResponseEntity<>(apiResponse, HttpStatusCode.valueOf(apiResponse.getStatusCode()));
    }

    @PostMapping("/modify-mfa-setting")
    public ResponseEntity<ApiResponse> modifyMfaSetting(@RequestBody MfaModificationDto mfaModificationDto) {
        ApiResponse apiResponse = userService.modifyMfaSetting(mfaModificationDto);
        return new ResponseEntity<>(apiResponse, HttpStatusCode.valueOf(apiResponse.getStatusCode()));
    }

    @PostMapping("/verify-mfa-code")
    public ResponseEntity<ApiResponse> verifyMfaCode(@RequestBody MfaVerificationDto mfaVerificationDto) {
        ApiResponse apiResponse = userService.verifyMfaCode(mfaVerificationDto);
        return new ResponseEntity<>(apiResponse, HttpStatusCode.valueOf(apiResponse.getStatusCode()));
    }
}
