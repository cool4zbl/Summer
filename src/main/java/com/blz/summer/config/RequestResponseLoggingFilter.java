package com.blz.summer.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

@Component
public class RequestResponseLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestResponseLoggingFilter.class);
    private static final int MAX_BODY_LENGTH = 2048;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        ContentCachingRequestWrapper requestWrapper = wrapRequest(request);
        ContentCachingResponseWrapper responseWrapper = wrapResponse(response);

        long start = System.currentTimeMillis();
        try {
            filterChain.doFilter(requestWrapper, responseWrapper);
        } finally {
            long duration = System.currentTimeMillis() - start;
            logRequestAndResponse(requestWrapper, responseWrapper, duration);
            responseWrapper.copyBodyToResponse();
        }
    }

    private void logRequestAndResponse(ContentCachingRequestWrapper request,
                                       ContentCachingResponseWrapper response,
                                       long duration) {
        String requestBody = toDisplayString(request.getContentAsByteArray(), request.getCharacterEncoding());
        String responseBody = toDisplayString(response.getContentAsByteArray(), response.getCharacterEncoding());

        log.debug("HTTP {} {} status={} duration={}ms requestBody={} responseBody={}",
                request.getMethod(),
                buildRequestUri(request),
                response.getStatus(),
                duration,
                requestBody,
                responseBody);
    }

    private ContentCachingRequestWrapper wrapRequest(HttpServletRequest request) {
        if (request instanceof ContentCachingRequestWrapper existingWrapper) {
            return existingWrapper;
        }
        return new ContentCachingRequestWrapper(request);
    }

    private ContentCachingResponseWrapper wrapResponse(HttpServletResponse response) {
        if (response instanceof ContentCachingResponseWrapper existingWrapper) {
            return existingWrapper;
        }
        return new ContentCachingResponseWrapper(response);
    }

    private String buildRequestUri(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String query = request.getQueryString();
        return query == null ? uri : uri + "?" + query;
    }

    private String toDisplayString(byte[] body, String encoding) {
        if (body == null || body.length == 0) {
            return "<empty>";
        }
        Charset charset = resolveCharset(encoding);
        String payload = new String(body, charset);
        if (payload.length() > MAX_BODY_LENGTH) {
            return payload.substring(0, MAX_BODY_LENGTH) + "...(truncated)";
        }
        return payload;
    }

    private Charset resolveCharset(String encoding) {
        if (encoding == null || encoding.isBlank()) {
            return StandardCharsets.UTF_8;
        }
        try {
            return Charset.forName(encoding);
        } catch (Exception ex) {
            log.debug("Falling back to UTF-8 for unsupported charset '{}'", encoding, ex);
            return StandardCharsets.UTF_8;
        }
    }
}
