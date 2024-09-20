package com.liuzhijun.ping.controller;

import com.liuzhijun.ping.service.PingService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
public class PingController {

    @Resource
    private PingService pingService;

    @GetMapping("/ping")
    public Mono<String> ping() {

        return pingService.ping("Hello");
    }


}
