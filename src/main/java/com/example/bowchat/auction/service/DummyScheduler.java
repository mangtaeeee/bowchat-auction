//package com.example.bowchat.auction.service;
//
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
//import org.springframework.data.redis.core.StringRedisTemplate;
//import org.springframework.scheduling.annotation.Scheduled;
//import org.springframework.stereotype.Service;
//
//import java.net.InetAddress;
//import java.net.UnknownHostException;
//import java.time.LocalDateTime;
//
//@Service
//@Slf4j
//@RequiredArgsConstructor
//public class DummyScheduler {
//
//
//    private final StringRedisTemplate redisTemplate;
//    private static final String EXECUTION_KEY = "myTask:executions";
//
//    @Scheduled(cron = "0 * * * * *") // 매 분 0초마다 실행
//    @SchedulerLock(name = "myTask", lockAtMostFor = "PT5M", lockAtLeastFor = "PT5S")
//    public void runTask() throws UnknownHostException {
//        String serverName = InetAddress.getLocalHost().getHostName();
//        String timestamp = LocalDateTime.now().toString();
//
//        // 실행 로그 메시지
//        String message = "[" + serverName + "] 실행됨 at " + timestamp;
//
//        // Redis에 실행 로그 저장
//        redisTemplate.opsForList().rightPush(EXECUTION_KEY, message);
//
//        // 콘솔 + 로그에도 출력
//        System.out.println(message);
//        log.info(message);
//    }
//
//
//}