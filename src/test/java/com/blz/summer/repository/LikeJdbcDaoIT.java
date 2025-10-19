package com.blz.summer.repository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@Testcontainers
class LikeJdbcDaoIT {

    @Container
    static final PostgreSQLContainer<?> PG =
            new PostgreSQLContainer<>("postgres:17-alpine")
                    .withDatabaseName("app_db")
                    .withUsername("app")
                    .withPassword("mypassword");

    @DynamicPropertySource
    static void registerProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", PG::getJdbcUrl);
        registry.add("spring.datasource.username", PG::getUsername);
        registry.add("spring.datasource.password", PG::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }

    @Autowired
    LikeJdbcDao dao;

    @Autowired
    NamedParameterJdbcTemplate jdbc;

    @Test
    void testIncrementsAutomatically() {
        String slug = "post/hello";
        long c1 = dao.incrementAtomic(slug);
        long c2 = dao.incrementAtomic(slug);
        long c3 = dao.get(slug);

        assertEquals(1, c1);
        assertEquals(2, c2);
        assertEquals(2, c3);
    }

    @Test
    void testConcurrentIncrementsAreThreadSafe() throws InterruptedException {
        String slug = "post/current-test";
        int numThreads = 10;
        int incrementsThread = 10;
        int expectedTotal = numThreads * incrementsThread;

        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch latch = new CountDownLatch(numThreads);

        for (int i = 0; i < numThreads; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < incrementsThread; j++) {
                        dao.incrementAtomic(slug);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        long finalCount = dao.get(slug);
        assertEquals(expectedTotal, finalCount,
                "Expected " + expectedTotal + " likes from concurrent increments");


    }


















}
