package com.simform.service;

import com.simform.util.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;

public interface HomeService {

    ApiResponse getData(HttpServletRequest request);
}
