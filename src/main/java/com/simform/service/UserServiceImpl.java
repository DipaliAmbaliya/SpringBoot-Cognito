package com.simform.service;

import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProvider;
import com.amazonaws.services.cognitoidp.model.*;
import com.simform.dto.AttributeVerificationDto;
import com.simform.dto.MfaModificationDto;
import com.simform.dto.MfaVerificationDto;
import com.simform.dto.SignInDto;
import com.simform.dto.SignInResponseDto;
import com.simform.dto.SignUpDto;
import com.simform.util.ApiResponse;
import com.simform.util.Message;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class UserServiceImpl implements UserService {

    @Autowired
    private AWSCognitoIdentityProvider provider;

    @Value("${aws.cognito.user-pool-id}")
    private String userPoolId;
    @Value("${aws.cognito.client-id}")
    private String clientId;

    @Override
    public ApiResponse signUp(SignUpDto signUpDto) {
        ApiResponse apiResponse;

        try {
            // Create User with Temporary Password
            AdminCreateUserRequest userRequest = new AdminCreateUserRequest()
                    .withUserPoolId(userPoolId)
                    .withUsername(signUpDto.getEmail())
                    .withUserAttributes(
                            new AttributeType().withName("email").withValue(signUpDto.getEmail()),
                            new AttributeType().withName("name").withValue(signUpDto.getName()),
                            new AttributeType().withName("phone_number").withValue(signUpDto.getPhoneNumber())
                    )
                    .withTemporaryPassword(signUpDto.getPassword())
                    .withMessageAction("SUPPRESS");

            AdminCreateUserResult createUserResult = provider.adminCreateUser(userRequest);
            // User created with status Force Change Password

            if (createUserResult.getSdkHttpMetadata().getHttpStatusCode() == HttpStatus.OK.value()) {
                // Now store password provided by User
                AdminSetUserPasswordRequest userPasswordRequest = new AdminSetUserPasswordRequest()
                        .withUserPoolId(userPoolId)
                        .withUsername(signUpDto.getEmail())
                        .withPassword(signUpDto.getPassword()).withPermanent(true);

                provider.adminSetUserPassword(userPasswordRequest);
                // User created with status Confirmed

                apiResponse = ApiResponse
                        .builder()
                        .status(true)
                        .data("")
                        .message(Message.SIGN_UP)
                        .statusCode(HttpStatus.CREATED.value())
                        .build();
            } else {
                apiResponse = ApiResponse
                        .builder()
                        .status(false)
                        .data(null)
                        .message(Message.INTERNAL_SERVER_ERROR + "doing registration")
                        .statusCode(createUserResult.getSdkHttpMetadata().getHttpStatusCode())
                        .build();
            }
        } catch (Exception e) {
            log.error("Exception {}", e.getMessage());
            apiResponse = ApiResponse
                    .builder()
                    .status(false)
                    .data(null)
                    .message(Message.EXCEPTION_INTERNAL_SERVER_ERROR)
                    .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .build();
        }

        return apiResponse;
    }

    @Override
    public ApiResponse signIn(SignInDto signInDto) {
        ApiResponse apiResponse;

        Map<String, String> userDetails = new HashMap<>();
        userDetails.put("USERNAME", signInDto.getEmail());
        userDetails.put("PASSWORD", signInDto.getPassword());

        AdminInitiateAuthRequest authRequest = new AdminInitiateAuthRequest()
                .withAuthFlow(AuthFlowType.ADMIN_USER_PASSWORD_AUTH)
                .withUserPoolId(userPoolId)
                .withClientId(clientId)
                .withAuthParameters(userDetails);

        try {
            // Perform authentication
            AdminInitiateAuthResult authResult = provider.adminInitiateAuth(authRequest);

            AuthenticationResultType authenticationResultType;

            // Check if authResult has success status
            if (authResult.getSdkHttpMetadata().getHttpStatusCode() == HttpStatus.OK.value()) {

                // Check the challenge name is not null and not empty
                if (authResult.getChallengeName() != null && !authResult.getChallengeName().isEmpty()) {
                    if (authResult.getChallengeName().equals(Message.SMS_MFA)) {
                        String token = authResult.getSession();
                        apiResponse = ApiResponse
                                .builder()
                                .status(true)
                                .data(token)
                                .message(Message.SENT_CODE_TO_MOBILE)
                                .statusCode(HttpStatus.OK.value())
                                .build();
                    } else {
                        apiResponse = ApiResponse
                                .builder()
                                .status(false)
                                .data(null)
                                .message(Message.RETRY)
                                .statusCode(HttpStatus.BAD_REQUEST.value())
                                .build();
                    }
                } else {

                    // If challenge name is null or empty then response back tokens
                    authenticationResultType = authResult.getAuthenticationResult();

                    SignInResponseDto signInResponseDto = getSignInResponseDto(authenticationResultType);

                    apiResponse = ApiResponse
                            .builder()
                            .status(false)
                            .data(signInResponseDto)
                            .message(Message.SIGN_IN)
                            .statusCode(HttpStatus.OK.value())
                            .build();
                }
            } else {
                apiResponse = ApiResponse
                        .builder()
                        .status(false)
                        .data(null)
                        .message(Message.INTERNAL_SERVER_ERROR + "doing sign in")
                        .statusCode(authResult.getSdkHttpMetadata().getHttpStatusCode())
                        .build();
            }
        } catch (Exception e) {
            log.error("Exception {}", e.getMessage());
            apiResponse = ApiResponse
                    .builder()
                    .status(false)
                    .data(null)
                    .message(Message.EXCEPTION_INTERNAL_SERVER_ERROR)
                    .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .build();
        }

        return apiResponse;
    }

    @Override
    public ApiResponse sendVerificationCode(HttpServletRequest request, AttributeVerificationDto verificationDto) {
        AttributeType attributeType = null;
        ApiResponse apiResponse;

        String token = request.getHeader("Authorization").substring(7);

        // Check attribute name
        if (verificationDto.getAttributeName().equals("phone_number")) {

            // Set details for phone number
            attributeType = new AttributeType()
                    .withName(verificationDto.getAttributeName())
                    .withValue(verificationDto.getAttributeValue());
        } else if (verificationDto.getAttributeName().equals("email")) {

            // Set details for email
            attributeType = new AttributeType()
                    .withName(verificationDto.getAttributeName())
                    .withValue(verificationDto.getAttributeValue());
        }

        UpdateUserAttributesRequest userAttributesRequest = new UpdateUserAttributesRequest()
                .withAccessToken(token)
                .withUserAttributes(attributeType);

        try {
            // Update user attribute
            UpdateUserAttributesResult userAttributesResult = provider.updateUserAttributes(userAttributesRequest);

            // Check status
            if (userAttributesResult.getSdkHttpMetadata().getHttpStatusCode() == HttpStatus.OK.value()) {
                GetUserAttributeVerificationCodeRequest verificationCodeRequest = new GetUserAttributeVerificationCodeRequest()
                        .withAccessToken(token)
                        .withAttributeName(verificationDto.getAttributeName());

                // Get verification code
                GetUserAttributeVerificationCodeResult verificationCodeResult = provider.getUserAttributeVerificationCode(verificationCodeRequest);

                // Check status
                if (verificationCodeResult.getSdkHttpMetadata().getHttpStatusCode() == HttpStatus.OK.value()) {
                    apiResponse = ApiResponse
                            .builder()
                            .status(true)
                            .data("")
                            .message("Verification code sent to " + verificationDto.getAttributeName() + " " + verificationDto.getAttributeValue())
                            .statusCode(HttpStatus.OK.value())
                            .build();
                } else {
                    apiResponse = ApiResponse
                            .builder()
                            .status(false)
                            .data(null)
                            .message(Message.INTERNAL_SERVER_ERROR + "sending verification code to " + verificationDto.getAttributeValue())
                            .statusCode(verificationCodeResult.getSdkHttpMetadata().getHttpStatusCode())
                            .build();
                }
            } else {
                apiResponse = ApiResponse
                        .builder()
                        .status(false)
                        .data(null)
                        .message(Message.INTERNAL_SERVER_ERROR + "sending verification code to " + verificationDto.getAttributeValue())
                        .statusCode(userAttributesResult.getSdkHttpMetadata().getHttpStatusCode())
                        .build();
            }
        } catch (Exception e) {
            log.error("Exception {}", e.getMessage());
            apiResponse = ApiResponse
                    .builder()
                    .status(false)
                    .data(null)
                    .message(Message.EXCEPTION_INTERNAL_SERVER_ERROR)
                    .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .build();
        }

        return apiResponse;
    }

    @Override
    public ApiResponse verifyCode(HttpServletRequest request, AttributeVerificationDto verificationDto) {
        ApiResponse apiResponse;

        String token = request.getHeader("Authorization").substring(7);

        VerifyUserAttributeRequest userAttributeRequest = new VerifyUserAttributeRequest()
                .withAccessToken(token)
                .withAttributeName(verificationDto.getAttributeName())
                .withCode(verificationDto.getCode());

        try {
            // Verify code
            VerifyUserAttributeResult userAttributeResult = provider.verifyUserAttribute(userAttributeRequest);

            // Check status
            if (userAttributeResult.getSdkHttpMetadata().getHttpStatusCode() == HttpStatus.OK.value()) {
                apiResponse = ApiResponse
                        .builder()
                        .status(true)
                        .data("")
                        .message("Verification for " + verificationDto.getAttributeName() + " is successful")
                        .statusCode(HttpStatus.OK.value())
                        .build();
            } else {
                apiResponse = ApiResponse
                        .builder()
                        .status(false)
                        .data(null)
                        .message(Message.INTERNAL_SERVER_ERROR + "doing verification for " + verificationDto.getAttributeName())
                        .statusCode(userAttributeResult.getSdkHttpMetadata().getHttpStatusCode())
                        .build();
            }

        } catch (Exception e) {
            log.info("Exception {}", e.getMessage());
            apiResponse = ApiResponse
                    .builder()
                    .status(false)
                    .data(null)
                    .message(Message.EXCEPTION_INTERNAL_SERVER_ERROR)
                    .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .build();
        }

        return apiResponse;
    }

    @Override
    public ApiResponse modifyMfaSetting(MfaModificationDto mfaModificationDto) {
        ApiResponse apiResponse = new ApiResponse();

        AdminGetUserRequest getUserRequest = new AdminGetUserRequest()
                .withUserPoolId(userPoolId)
                .withUsername(mfaModificationDto.getEmail());

        try {
            // Get user details
            AdminGetUserResult getUserResult = provider.adminGetUser(getUserRequest);

            // Check status
            if (getUserResult.getSdkHttpMetadata().getHttpStatusCode() == HttpStatus.OK.value()) {

                SMSMfaSettingsType mfaSettingsType;
                AssociateSoftwareTokenRequest associateSoftwareTokenRequest;
                if (mfaModificationDto.isEnable()) {
                    mfaSettingsType = new SMSMfaSettingsType()
                            .withEnabled(true)
                            .withPreferredMfa(mfaModificationDto.isEnable());
                } else {
                    mfaSettingsType = new SMSMfaSettingsType()
                            .withEnabled(false)
                            .withPreferredMfa(mfaModificationDto.isEnable());
                }

                AdminSetUserMFAPreferenceRequest mfaPreferenceRequest = new AdminSetUserMFAPreferenceRequest()
                        .withSMSMfaSettings(mfaSettingsType)
                        .withUsername(mfaModificationDto.getEmail())
                        .withUserPoolId(userPoolId);

                // Modify MFA preference
                AdminSetUserMFAPreferenceResult mfaPreferenceResult = provider.adminSetUserMFAPreference(mfaPreferenceRequest);

                // Check status
                if (mfaPreferenceResult.getSdkHttpMetadata().getHttpStatusCode() == HttpStatus.OK.value()) {
                    if (mfaModificationDto.isEnable()) {
                        apiResponse = ApiResponse
                                .builder()
                                .status(true)
                                .data("")
                                .message(Message.MFA_ENABLED)
                                .statusCode(HttpStatus.OK.value())
                                .build();
                    } else {
                        apiResponse = ApiResponse
                                .builder()
                                .status(true)
                                .data("")
                                .message(Message.MFA_DISABLED)
                                .statusCode(HttpStatus.OK.value())
                                .build();
                    }
                } else {
                    apiResponse = ApiResponse
                            .builder()
                            .status(false)
                            .data(null)
                            .message(Message.INTERNAL_SERVER_ERROR + "modifying MFA settings")
                            .statusCode(mfaPreferenceResult.getSdkHttpMetadata().getHttpStatusCode())
                            .build();
                }
            }
        } catch (Exception e) {
            log.error("Exception {}", e.getMessage());
            apiResponse = ApiResponse
                    .builder()
                    .status(false)
                    .data(null)
                    .message(Message.EXCEPTION_INTERNAL_SERVER_ERROR)
                    .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .build();
        }

        return apiResponse;
    }

    @Override
    public ApiResponse verifyMfaCode(MfaVerificationDto mfaVerificationDto) {
        ApiResponse apiResponse;

        Map<String, String> authParams = new HashMap<>();
        authParams.put("USERNAME", mfaVerificationDto.getUsername());
        authParams.put("SMS_MFA_CODE", mfaVerificationDto.getMfaCode());

        RespondToAuthChallengeRequest authChallengeRequest = new RespondToAuthChallengeRequest()
                .withChallengeName(ChallengeNameType.SMS_MFA)
                .withClientId(clientId)
                .withSession(mfaVerificationDto.getSessionId())
                .withChallengeResponses(authParams);

        try {
            // Respond to Auth Challenge
            RespondToAuthChallengeResult authChallengeResult = provider.respondToAuthChallenge(authChallengeRequest);

            // Check status
            if (authChallengeResult.getSdkHttpMetadata().getHttpStatusCode() == HttpStatus.OK.value()) {

                // Check if challenge is null
                if (authChallengeResult.getChallengeName() == null) {
                    SignInResponseDto signInResponseDto = getSignInResponseDto(authChallengeResult.getAuthenticationResult());

                    apiResponse = ApiResponse
                            .builder()
                            .status(true)
                            .data(signInResponseDto)
                            .message(Message.SIGN_IN)
                            .statusCode(HttpStatus.OK.value())
                            .build();
                } else {
                    apiResponse = ApiResponse
                            .builder()
                            .status(false)
                            .data(null)
                            .message(Message.INVALID_MFA_CODE)
                            .statusCode(HttpStatus.BAD_REQUEST.value())
                            .build();
                }
            } else {
                apiResponse = ApiResponse
                        .builder()
                        .status(false)
                        .data(null)
                        .message(Message.INTERNAL_SERVER_ERROR + "verifying mfa")
                        .statusCode(authChallengeResult.getSdkHttpMetadata().getHttpStatusCode())
                        .build();
            }

        } catch (Exception e) {
            log.error("Exception {}", e.getMessage());
            apiResponse = ApiResponse
                    .builder()
                    .status(false)
                    .data(null)
                    .message(Message.EXCEPTION_INTERNAL_SERVER_ERROR)
                    .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .build();
        }

        return apiResponse;
    }

    private SignInResponseDto getSignInResponseDto(AuthenticationResultType resultType) {
        SignInResponseDto signInResponseDto = new SignInResponseDto();
        signInResponseDto.setAccessToken(resultType.getAccessToken());
        signInResponseDto.setIdToken(resultType.getIdToken());
        signInResponseDto.setRefreshToken(resultType.getRefreshToken());
        signInResponseDto.setExpirationTime(resultType.getExpiresIn());
        signInResponseDto.setTokenType(resultType.getTokenType());
        return signInResponseDto;
    }
}
