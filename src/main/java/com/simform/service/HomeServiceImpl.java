package com.simform.service;

import com.simform.util.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class HomeServiceImpl implements HomeService {
    @Override
    public ApiResponse getData(HttpServletRequest request) {
        return ApiResponse
                .builder()
                .data("Hi")
                .message("Data fetched")
                .statusCode(HttpStatus.OK.value())
                .status(false)
                .build();
    }
}
