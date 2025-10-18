package com.blz.summer;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Represents the likes for a slug")
public record LikeResponse(
        @Schema(description = "Unique slug", example = "blog-123")
        String slug,
        @Schema(description = "Current likes", example = "42")
        long likes
) {}
