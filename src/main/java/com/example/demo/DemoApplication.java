package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@SpringBootApplication
public class DemoApplication {

	public static void main(String[] args) {
		System.out.println("[DemoApplication] Starting JSON Schema Validator Application");
		System.out.println("[DemoApplication] CORS configured to allow all origins");
		SpringApplication.run(DemoApplication.class, args);
		System.out.println("[DemoApplication] Application started successfully");
	}
	
	@Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                System.out.println("[DemoApplication] Configuring CORS mappings");
                System.out.println("[DemoApplication] Allowing all origins, methods, and headers");
                
                registry.addMapping("/**")
                    .allowedOriginPatterns("*") // Используем allowedOriginPatterns вместо allowedOrigins для поддержки всех доменов
                    .allowedMethods("*")
                    .allowedHeaders("*")
                    .allowCredentials(true)
                    .maxAge(3600);
                
                System.out.println("[DemoApplication] CORS configuration completed:");
                System.out.println("[DemoApplication] - Mapping: /**");
                System.out.println("[DemoApplication] - Allowed origins: ALL (*)");
                System.out.println("[DemoApplication] - Allowed methods: ALL (*)");
                System.out.println("[DemoApplication] - Allowed headers: ALL (*)");
                System.out.println("[DemoApplication] - Allow credentials: true");
                System.out.println("[DemoApplication] - Max age: 3600 seconds");
            }
        };
    }
}