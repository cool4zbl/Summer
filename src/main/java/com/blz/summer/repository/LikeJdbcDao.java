package com.blz.summer.repository;

import com.blz.summer.port.LikeCounterPort;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.dao.EmptyResultDataAccessException;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class LikeJdbcDao implements LikeCounterPort {
    private final NamedParameterJdbcTemplate jdbc;

    @Override
    public long incrementAtomic(String slug) {
        String sql = """
            INSERT INTO post_likes(slug, likes) VALUES (:slug, 1)
            ON CONFLICT (slug)
            DO UPDATE SET likes = post_likes.likes + 1
            RETURNING likes;
        """;
        Long result = jdbc.queryForObject(sql, Map.of("slug", slug), Long.class);
        return result != null ? result : 0L;
    }

    @Override
    public long get(String slug) {
        try {
            return jdbc.queryForObject(
                    "SELECT likes FROM post_likes WHERE slug = :slug",
                    Map.of("slug", slug),
                    Long.class
            );
        } catch (EmptyResultDataAccessException e) {
            return 0L;
        }
    }
}
