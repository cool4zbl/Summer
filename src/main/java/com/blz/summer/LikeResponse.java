package com.blz.summer;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Represents the like_count for a slug")
public record LikeResponse(
        @Schema(description = "Unique slug", example = "blog-123")
        String slug,
        @Schema(description = "Current like_count", example = "42")
        long like_count
) {}
