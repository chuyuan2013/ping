package com.liuzhijun.ping.service;

import reactor.core.publisher.Mono;

public interface PingService {

    Mono<String> ping(String str);
}
