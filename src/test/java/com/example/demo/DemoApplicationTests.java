package com.example.demo;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
    "spring.main.allow-bean-definition-overriding=true"
})
class DemoApplicationTests {

    @Test
    void contextLoads() {
        // Пустой тест - просто проверяем загрузку контекста
    }
}