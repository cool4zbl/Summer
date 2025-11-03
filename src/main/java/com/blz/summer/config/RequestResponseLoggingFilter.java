package com.blz.summer.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

@Component
public class RequestResponseLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestResponseLoggingFilter.class);
    private static final int MAX_BODY_LENGTH = 2048;
    private static final Set<Integer> AUTH_FAILURE_STATUSES = Set.of(
            HttpServletResponse.SC_UNAUTHORIZED,
            HttpServletResponse.SC_FORBIDDEN);
    private static final Set<String> STATIC_EXTENSIONS = Set.of(
            ".css",
            ".js",
            ".png",
            ".jpg",
            ".jpeg",
            ".gif",
            ".svg",
            ".ico",
            ".txt",
            ".map");
    private static final List<Predicate<String>> NOISY_STATIC_PATH_PREDICATES = List.of(
            path -> path.equals("/favicon.ico"),
            path -> path.equals("/robots.txt"),
            path -> path.startsWith("/.well-known"),
            path -> path.startsWith("/static/"),
            path -> path.startsWith("/assets/"),
            path -> path.startsWith("/webjars/"),
            RequestResponseLoggingFilter::hasStaticExtension);

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
        String uri = buildRequestUri(request);
        int status = response.getStatus();

        if (shouldSkipLogging(uri, status)) {
            return;
        }

        if (!shouldLog(status)) {
            return;
        }

        String requestBody = toDisplayString(request.getContentAsByteArray(), request.getCharacterEncoding());
        String responseBody = toDisplayString(response.getContentAsByteArray(), response.getCharacterEncoding());
        String userAgent = headerValue(request, "User-Agent");
        String xForwardedFor = headerValue(request, "X-Forwarded-For");
        String remoteAddr = request.getRemoteAddr();
        if (remoteAddr == null || remoteAddr.isBlank()) {
            remoteAddr = "<unknown>";
        }

        Object[] args = new Object[] {
                request.getMethod(),
                uri,
                status,
                duration,
                remoteAddr,
                xForwardedFor,
                userAgent,
                requestBody,
                responseBody
        };

        if (status >= HttpStatus.INTERNAL_SERVER_ERROR.value()) {
            log.error(
                    "HTTP {} {} status={} duration={}ms clientIp={} xForwardedFor={} userAgent={} requestBody={} responseBody={}",
                    args);
        } else if (isAuthenticationFailure(status)) {
            log.warn(
                    "HTTP {} {} status={} duration={}ms clientIp={} xForwardedFor={} userAgent={} requestBody={} responseBody={}",
                    args);
        } else if (status >= HttpStatus.BAD_REQUEST.value()) {
            log.debug(
                    "HTTP {} {} status={} duration={}ms clientIp={} xForwardedFor={} userAgent={} requestBody={} responseBody={}",
                    args);
        } else {
            log.debug(
                    "HTTP {} {} status={} duration={}ms clientIp={} xForwardedFor={} userAgent={} requestBody={} responseBody={}",
                    args);
        }
    }

    private boolean shouldLog(int status) {
        if (status >= HttpStatus.INTERNAL_SERVER_ERROR.value()) {
            return true;
        }
        if (isAuthenticationFailure(status)) {
            return true;
        }
        return log.isDebugEnabled();
    }

    private boolean shouldSkipLogging(String uri, int status) {
        if (uri == null) {
            return false;
        }
        if (status == HttpServletResponse.SC_NOT_FOUND && isNoisyStaticProbe(uri)) {
            return true;
        }
        return false;
    }

    private boolean isNoisyStaticProbe(String uri) {
        return NOISY_STATIC_PATH_PREDICATES.stream().anyMatch(predicate -> predicate.test(uri));
    }

    private static boolean hasStaticExtension(String uri) {
        return STATIC_EXTENSIONS.stream().anyMatch(uri::endsWith);
    }

    private boolean isAuthenticationFailure(int status) {
        return AUTH_FAILURE_STATUSES.contains(status);
    }

    private String headerValue(HttpServletRequest request, String headerName) {
        String value = request.getHeader(headerName);
        if (value == null || value.isBlank()) {
            return "<absent>";
        }
        return value;
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
