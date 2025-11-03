package com.blz.summer.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;
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
    private static final Set<String> SUPPRESSED_PATHS = Set.of(
            "/favicon.ico",
            "/robots.txt",
            "/sitemap.xml",
            "/.env",
            "/.git/config",
            "/.svn/entries");
    private static final Set<String> SUPPRESSED_EXTENSIONS = Set.of(
            "js", "css", "map", "ico", "png", "jpg", "jpeg", "gif", "svg", "woff", "woff2",
            "ttf", "php", "asp", "aspx", "jsp", "cgi", "zip", "rar", "gz", "tar", "7z", "bak",
            "sql", "env", "ini");
    private static final String[] SUPPRESSED_PREFIXES = {
            "/.well-known",
            "/wp-",
            "/boaform",
            "/hudson",
            "/jenkins"
    };
    private static final String[] GEO_HEADERS = {
            "CF-IPCountry",
            "CloudFront-Viewer-Country",
            "X-AppEngine-Country",
            "X-Geo-Country",
            "X-Country-Code"
    };

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
        HttpStatus status = HttpStatus.resolve(response.getStatus());

        if (shouldSuppressProbeLogging(status, request)) {
            if (log.isTraceEnabled()) {
                log.trace("Suppressing probe request {} {} status={} from {} forwardedFor={}",
                        request.getMethod(),
                        buildRequestUri(request),
                        response.getStatus(),
                        request.getRemoteAddr(),
                        headerOrDefault(request, "X-Forwarded-For", "<none>"));
            }
            return;
        }

        String requestBody = toDisplayString(request.getContentAsByteArray(), request.getCharacterEncoding());
        String responseBody = toDisplayString(response.getContentAsByteArray(), response.getCharacterEncoding());
        String requestUri = buildRequestUri(request);
        String remoteAddr = request.getRemoteAddr();
        String forwardedFor = headerOrDefault(request, "X-Forwarded-For", "<none>");
        String userAgent = headerOrDefault(request, "User-Agent", "<unknown>");
        String geo = resolveGeo(request);

        if (status != null && status.is5xxServerError()) {
            log.warn("HTTP {} {} status={} duration={}ms clientIp={} forwardedFor={} geo={} userAgent={} requestBody={} responseBody={}",
                    request.getMethod(),
                    requestUri,
                    response.getStatus(),
                    duration,
                    remoteAddr,
                    forwardedFor,
                    geo,
                    userAgent,
                    requestBody,
                    responseBody);
            return;
        }

        if (status == HttpStatus.UNAUTHORIZED || status == HttpStatus.FORBIDDEN) {
            log.warn("HTTP {} {} status={} duration={}ms clientIp={} forwardedFor={} geo={} userAgent={} requestBody={} responseBody={}",
                    request.getMethod(),
                    requestUri,
                    response.getStatus(),
                    duration,
                    remoteAddr,
                    forwardedFor,
                    geo,
                    userAgent,
                    requestBody,
                    responseBody);
            return;
        }

        if (status != null && status.is4xxClientError()) {
            if (log.isTraceEnabled()) {
                log.trace("HTTP {} {} status={} duration={}ms clientIp={} forwardedFor={} geo={} userAgent={} requestBody={} responseBody={}",
                        request.getMethod(),
                        requestUri,
                        response.getStatus(),
                        duration,
                        remoteAddr,
                        forwardedFor,
                        geo,
                        userAgent,
                        requestBody,
                        responseBody);
            }
            return;
        }

        log.debug("HTTP {} {} status={} duration={}ms clientIp={} forwardedFor={} geo={} userAgent={} requestBody={} responseBody={}",
                request.getMethod(),
                requestUri,
                response.getStatus(),
                duration,
                remoteAddr,
                forwardedFor,
                geo,
                userAgent,
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

    private boolean shouldSuppressProbeLogging(HttpStatus status, HttpServletRequest request) {
        if (status == null || status.value() != HttpStatus.NOT_FOUND.value()) {
            return false;
        }

        String uri = request.getRequestURI();
        if (uri == null || uri.isBlank()) {
            return false;
        }

        if (SUPPRESSED_PATHS.contains(uri)) {
            return true;
        }

        String lowerUri = uri.toLowerCase(Locale.ROOT);
        for (String prefix : SUPPRESSED_PREFIXES) {
            if (lowerUri.startsWith(prefix)) {
                return true;
            }
        }

        int lastSlash = lowerUri.lastIndexOf('/');
        String fileName = lastSlash >= 0 ? lowerUri.substring(lastSlash + 1) : lowerUri;
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot >= 0 && lastDot < fileName.length() - 1) {
            String extension = fileName.substring(lastDot + 1);
            if (SUPPRESSED_EXTENSIONS.contains(extension)) {
                return true;
            }
        }

        return false;
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

    private String headerOrDefault(HttpServletRequest request, String headerName, String defaultValue) {
        String value = request.getHeader(headerName);
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private String resolveGeo(HttpServletRequest request) {
        for (String header : GEO_HEADERS) {
            String value = request.getHeader(header);
            if (value != null && !value.isBlank()) {
                return header + "=" + value;
            }
        }
        return "<unknown>";
    }
}
