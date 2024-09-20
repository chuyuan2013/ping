package com.liuzhijun.ping.service.impl;

import com.liuzhijun.ping.service.PingService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.File;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
public class PingServiceImpl implements PingService {
    @Resource
    private WebClient webClient;

    @Value("${server.lock.path}")
    private String lockFilePath;

    @Value("${spring.application.maxRequestsPerSecond}")
    private int maxRequestsPerSecond;
    private static final AtomicInteger requestCount = new AtomicInteger(0); // 请求计数

    /**
     * 发送 ping 请求，并返回响应
     * @return 响应字符串
     */
    @Override
    public Mono<String> ping(String str) {
        if (requestCount.get() >= maxRequestsPerSecond) {
            log.info("Request not send as being \"rate limited\".");
            return Mono.error(new RuntimeException("Request not send as being \"rate limited\"."));
        }
        checkLockFile();

        try (FileChannel channel = FileChannel.open(new File(lockFilePath).toPath(), StandardOpenOption.READ, StandardOpenOption.WRITE);
             FileLock lock = channel.lock()) {

            requestCount.incrementAndGet(); // 增加请求计数

            return webClient.post() // 使用 POST 方法发送请求
                    .bodyValue(str) // 发送 "Hello" 字符串
                    .retrieve()
                    .onStatus(status -> status.value() == 429, clientResponse -> {
                        return Mono.error(new RuntimeException("429 Too Many Requests"));
                    })
                    .bodyToMono(String.class)
                    .doOnSuccess(response -> {
                        log.info("Request sent & Pong Respond: {}", response);
                    })
                    .doOnError(e -> {
                        log.info("Request send & Pong throttled it");
                    });
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("lock file locking fail");
        }

    }


    /**
     * 检查锁文件是否存在，不存在则创建
     */
    private void checkLockFile() {
        if (lockFilePath == null) {
            throw new IllegalArgumentException("lockFilePath cannot be null");
        }

        File file = new File(lockFilePath);
        if(!file.exists()){
            try {
                file.createNewFile();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 定时重置请求计数
     */
    @Scheduled(fixedRate = 1000)
    public void RequestCountReset() {
        if(requestCount.get() >= maxRequestsPerSecond){
            requestCount.set(0);
        }
    }
}
