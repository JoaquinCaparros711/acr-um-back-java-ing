package com.odontogestion.api.telemetry;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link OtelRequestFilter}.
 *
 * <p>Uses {@link InMemorySpanExporter} to capture produced spans and assert
 * that span attributes match expected HTTP semantic conventions.</p>
 *
 * <p>Pattern: AAA (Arrange-Act-Assert) with descriptive test names.</p>
 */
class OtelRequestFilterTest {

    private InMemorySpanExporter spanExporter;
    private OtelRequestFilter filter;

    @BeforeEach
    void setUp() {
        // Arrange: set up an in-memory OTel SDK for span capture
        spanExporter = InMemorySpanExporter.create();
        final SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
                .addSpanProcessor(SimpleSpanProcessor.create(spanExporter))
                .build();

        OpenTelemetrySdk.builder()
                .setTracerProvider(tracerProvider)
                .buildAndRegisterGlobal();

        filter = new OtelRequestFilter();
    }

    @AfterEach
    void tearDown() {
        GlobalOpenTelemetry.resetForTest();
        spanExporter.reset();
    }

    @Test
    @DisplayName("should_CreateSpan_When_HttpRequestIsProcessed")
    void should_CreateSpan_When_HttpRequestIsProcessed() throws Exception {
        // Arrange
        final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/patients");
        final MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(200);
        final FilterChain chain = mock(FilterChain.class);

        // Act
        filter.doFilterInternal(request, response, chain);

        // Assert
        final List<SpanData> spans = spanExporter.getFinishedSpanItems();
        assertThat(spans).hasSize(1);
        final SpanData span = spans.get(0);
        assertThat(span.getName()).isEqualTo("GET /api/v1/patients");
        assertThat(span.getAttributes().get(
                io.opentelemetry.api.common.AttributeKey.stringKey("http.method")
        )).isEqualTo("GET");
    }

    @Test
    @DisplayName("should_SetErrorStatus_When_ResponseIs5xx")
    void should_SetErrorStatus_When_ResponseIs5xx() throws Exception {
        // Arrange
        final MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/patients");
        final MockHttpServletResponse response = new MockHttpServletResponse();
        final FilterChain chain = mock(FilterChain.class);
        doAnswer(inv -> {
            response.setStatus(500);
            return null;
        }).when(chain).doFilter(request, response);

        // Act
        filter.doFilterInternal(request, response, chain);

        // Assert
        final List<SpanData> spans = spanExporter.getFinishedSpanItems();
        assertThat(spans).hasSize(1);
        assertThat(spans.get(0).getStatus().getStatusCode())
                .isEqualTo(io.opentelemetry.api.trace.StatusCode.ERROR);
    }

    @Test
    @DisplayName("should_NotFilter_When_PathIsActuator")
    void should_NotFilter_When_PathIsActuator() {
        // Arrange
        final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/health");

        // Act
        final boolean shouldSkip = filter.shouldNotFilter(request);

        // Assert
        assertThat(shouldSkip).isTrue();
    }

    @Test
    @DisplayName("should_FilterNormally_When_PathIsApiEndpoint")
    void should_FilterNormally_When_PathIsApiEndpoint() {
        // Arrange
        final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/patients");

        // Act
        final boolean shouldSkip = filter.shouldNotFilter(request);

        // Assert
        assertThat(shouldSkip).isFalse();
    }
}
