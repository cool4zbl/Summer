package com.blz.summer.repository;

import com.blz.summer.port.LikeCounterPort;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class LikeJdbcDao implements LikeCounterPort {
    private final NamedParameterJdbcTemplate jdbc;

    @Override
    public long incrementAtomic(String slug) {
        String sql = """
            INSERT INTO likes(slug, count) VALUES (:slug, 1)
            ON CONFLICT (slug)
            DO UPDATE SET count = likes.count + 1, updated_at = now()
            RETURNING count;
        """;
        return jdbc.queryForObject(sql, Map.of("slug", slug), Long.class);
    }

    @Override
    public long get(String slug) {
        return jdbc.queryForObject(
                "SELECT count FROM likes where slug:=slug",
                Map.of("slug", slug),
                Long.class
        );
    }
}
