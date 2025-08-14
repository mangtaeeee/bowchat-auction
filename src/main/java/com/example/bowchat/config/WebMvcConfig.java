package com.example.bowchat.config;

//S3 적용 전 로컬 파일 업로드 설정 클래스

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 클라이언트에서 "/uploads/..."로 요청하면 실제 파일 시스템 "/Library/Study/..."에서 응답
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:/Library/Study/uploads");
    }
}
