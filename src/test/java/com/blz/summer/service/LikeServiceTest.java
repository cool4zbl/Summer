package com.blz.summer.service;

import com.blz.summer.port.LikeCounterPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LikeServiceTest {

    @Mock
    private LikeCounterPort likeCounterPort;

    @InjectMocks
    private LikeService likeService;

    @Test
    void shouldIncrementLikeCount() {
        String slug = "test-post";
        when(likeCounterPort.incrementAtomic(slug)).thenReturn(5L);

        long result = likeService.increment(slug);

        assertEquals(5L, result);
        verify(likeCounterPort, times(1)).incrementAtomic(slug);
    }

    @Test
    void shouldGetLikeCount() {
        String slug = "test-post";
        when(likeCounterPort.get(slug)).thenReturn(10L);

        long result = likeService.get(slug);

        assertEquals(10L, result);
        verify(likeCounterPort, times(1)).get(slug);

    }

}
