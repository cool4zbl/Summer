package com.blz.summer.service;

import com.blz.summer.port.LikeCounterPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LikeService {

    private final LikeCounterPort counter;

//    private final RateLimiter limiter;
//    private final IdempotencyStore idem;

    public long increment(String slug) {
        // TODO: rate-limit, idempotency
        return counter.incrementAtomic(slug);
    }

    public long get(String slug) {
        return counter.get(slug);
    }

}
