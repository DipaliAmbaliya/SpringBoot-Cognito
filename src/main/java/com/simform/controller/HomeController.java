package com.simform.controller;

import com.simform.service.HomeService;
import com.simform.util.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/home")
public class HomeController {

    @Autowired
    private HomeService homeService;

    @GetMapping
    public ResponseEntity<ApiResponse> getData(HttpServletRequest request) {
        ApiResponse data = homeService.getData(request);
        return new ResponseEntity<>(data, HttpStatusCode.valueOf(data.getStatusCode()));
    }
}
