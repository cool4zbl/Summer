package com.blz.summer.controller;

import com.blz.summer.LikeResponse;
import com.blz.summer.service.LikeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/v1/likes")
@RequiredArgsConstructor
public class LikeController {

    private final LikeService service;

    @Operation(summary = "Get like count", description = "Returns current like count for the given slug")
    @GetMapping("/{slug}")
    public ResponseEntity<LikeResponse> get(@Parameter(description = "Unique content slug") @PathVariable String slug) {
        LikeResponse response = new LikeResponse(slug, service.get(slug));
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Increment like count", description = "Atomically increments and returns the like count for the given slug")
    @PostMapping("/{slug}")
    public ResponseEntity<LikeResponse> inc(
            @Parameter(description = "Unique content slug") @PathVariable String slug,
            @Parameter(description = "Optional idempotency key to ensure safe retries") @RequestHeader(value="Idempotency-Key", required = false) String idemKey
    ) {
        long count = service.increment(slug);
        LikeResponse response = new LikeResponse(slug, count);
        return ResponseEntity.ok(response);
    }

}
