package com.odontogestion.api.telemetry;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapGetter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * Servlet filter that wraps every HTTP request in an OpenTelemetry span,
 * propagates W3C TraceContext headers, and records HTTP request counters.
 *
 * <p>Equivalent to the OTel Fiber middleware used in the Go reference service.</p>
 */
@Slf4j
@Component
@Order(1)
public class OtelRequestFilter extends OncePerRequestFilter {

    private static final String INSTRUMENTATION_SCOPE = "com.odontogestion.api";

    // W3C TraceContext header getter for context propagation
    private static final TextMapGetter<HttpServletRequest> HEADER_GETTER =
            new TextMapGetter<>() {
                @Override
                public Iterable<String> keys(@NonNull HttpServletRequest carrier) {
                    return Collections.list(carrier.getHeaderNames());
                }

                @Override
                @Nullable
                public String get(@Nullable HttpServletRequest carrier, @NonNull String key) {
                    return carrier == null ? null : carrier.getHeader(key);
                }
            };

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        final String method = request.getMethod();
        final String path = request.getRequestURI();
        final String spanName = method + " " + path;

        // Extract propagated trace context from incoming W3C headers
        final Context parentContext = GlobalOpenTelemetry.getPropagators()
                .getTextMapPropagator()
                .extract(Context.current(), request, HEADER_GETTER);

        final Tracer tracer = GlobalOpenTelemetry.getTracer(INSTRUMENTATION_SCOPE);
        final Span span = tracer.spanBuilder(spanName)
                .setParent(parentContext)
                .setSpanKind(SpanKind.SERVER)
                .setAttribute("http.method", method)
                .setAttribute("http.target", path)
                .setAttribute("http.scheme", request.getScheme())
                .setAttribute("http.host", request.getServerName())
                .startSpan();

        try (Scope ignored = span.makeCurrent()) {
            filterChain.doFilter(request, response);

            final int statusCode = response.getStatus();
            span.setAttribute("http.status_code", statusCode);

            if (statusCode >= 500) {
                span.setStatus(StatusCode.ERROR, "HTTP " + statusCode);
            } else {
                span.setStatus(StatusCode.OK);
            }

            recordHttpRequestMetric(method, path, statusCode);

        } catch (Exception ex) {
            span.setStatus(StatusCode.ERROR, ex.getMessage());
            span.recordException(ex);
            throw ex;
        } finally {
            span.end();
        }
    }

    /**
     * Increments the http.server.requests counter metric (equivalent to the
     * request_count gauge in the Go middleware).
     */
    private void recordHttpRequestMetric(String method, String path, int statusCode) {
        try {
            final Meter meter = GlobalOpenTelemetry.getMeter(INSTRUMENTATION_SCOPE);
            final LongCounter counter = meter
                    .counterBuilder("http.server.requests")
                    .setDescription("Total number of HTTP requests received")
                    .setUnit("{requests}")
                    .build();

            counter.add(1,
                    io.opentelemetry.api.common.Attributes.builder()
                            .put("http.method", method)
                            .put("http.route", path)
                            .put("http.status_code", String.valueOf(statusCode))
                            .build()
            );
        } catch (Exception ex) {
            log.warn("[OtelRequestFilter] Failed to record HTTP metric: {}", ex.getMessage());
        }
    }

    /** Skip OTel instrumentation for Actuator internal endpoints. */
    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        final String path = request.getRequestURI();
        return path.startsWith("/actuator");
    }
}
