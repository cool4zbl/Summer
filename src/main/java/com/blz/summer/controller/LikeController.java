package com.blz.summer.controller;

import com.blz.summer.service.LikeService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/v1/likes")
@RequiredArgsConstructor
public class LikeController {

    private final LikeService service;

    @GetMapping("/{slug}")
    public Map<String, Object> get(@PathVariable String slug) {
        return Map.of("slug", slug, "count", service.get(slug));
    }

    @PostMapping("/{slug}")
    public Map<String, Object> inc(
            @PathVariable String slug,
            @RequestHeader(value="Idempotency-Key", required = false) String idemKey
//            HttpServletRequest req
    ) {
        long count = service.increment(slug);
        return Map.of("slug", slug, "count", count);
    }

}
