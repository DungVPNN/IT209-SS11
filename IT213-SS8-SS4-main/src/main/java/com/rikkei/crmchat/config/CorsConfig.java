package com.rikkei.crmchat.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Cấu hình CORS tập trung, an toàn cho API.
 *
 * Chỉ cho phép các origin tin cậy (không dùng "*"), giới hạn method và header.
 * Đây là lớp phòng thủ chung cho toàn bộ /api/**, bổ trợ cho @CrossOrigin ở controller.
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("https://crm.rikkei.edu.vn", "http://localhost:5173")
                .allowedMethods("GET", "POST")
                .allowedHeaders("Content-Type", "Authorization")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
