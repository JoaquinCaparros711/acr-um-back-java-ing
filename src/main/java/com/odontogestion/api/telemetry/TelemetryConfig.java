package com.odontogestion.api.telemetry;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.exporter.otlp.http.logs.OtlpHttpLogRecordExporter;
import io.opentelemetry.exporter.otlp.http.metrics.OtlpHttpMetricExporter;
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import io.opentelemetry.sdk.logs.export.BatchLogRecordProcessor;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * OpenTelemetry SDK configuration for Spring Boot.
 *
 * <p>Configures and wires up TracerProvider, MeterProvider, and LoggerProvider.
 * When {@code OTEL_EXPORTER_OTLP_ENDPOINT} is set, all signals are exported via
 * OTLP/HTTP to Azure Monitor (or any compatible OTLP backend).
 * When absent, the SDK operates in GlobalOpenTelemetry no-op mode.</p>
 *
 * <p>Pattern: Factory Bean — single, centralized initialization point for all
 * OTel providers (mirrors the Go {@code InitTelemetry} function).</p>
 */
@Slf4j
@Configuration
public class TelemetryConfig {

    // OTel semantic convention keys — defined inline to avoid the
    // opentelemetry-semconv artifact dependency (moved to separate module in 1.26+)
    private static final AttributeKey<String> SERVICE_NAME =
            AttributeKey.stringKey("service.name");
    private static final AttributeKey<String> DEPLOYMENT_ENVIRONMENT =
            AttributeKey.stringKey("deployment.environment");
    private static final AttributeKey<String> CLOUD_PROVIDER =
            AttributeKey.stringKey("cloud.provider");
    private static final AttributeKey<String> AI_CLOUD_ROLE =
            AttributeKey.stringKey("ai.cloud.role");

    @Value("${otel.service.name:dental-management-backend}")
    private String serviceName;

    @Value("${otel.exporter.otlp.endpoint:}")
    private String otlpEndpoint;

    @Value("${spring.profiles.active:production}")
    private String environment;

    /**
     * Registers and returns the fully configured {@link OpenTelemetry} instance
     * as a Spring bean. The {@link OtelRequestFilter} and any other component
     * that injects {@code OpenTelemetry} will receive this instance.
     */
    @Bean
    public OpenTelemetry openTelemetry() {
        final boolean hasOtlpEndpoint = otlpEndpoint != null
                && !otlpEndpoint.isBlank()
                && (otlpEndpoint.startsWith("http://") || otlpEndpoint.startsWith("https://"));

        if (!hasOtlpEndpoint) {
            log.warn("[Telemetry] OTEL_EXPORTER_OTLP_ENDPOINT is empty or not a valid HTTP URL ('{}') — using GlobalOpenTelemetry no-op mode.", otlpEndpoint);
            return io.opentelemetry.api.GlobalOpenTelemetry.get();
        }

        log.info("[Telemetry] Initializing OTLP exporters → endpoint={}, service={}, env={}",
                otlpEndpoint, serviceName, environment);

        final Resource resource = buildResource();
        final SdkTracerProvider tracerProvider = buildTracerProvider(resource);
        final SdkMeterProvider meterProvider = buildMeterProvider(resource);
        final SdkLoggerProvider loggerProvider = buildLoggerProvider(resource);

        final OpenTelemetrySdk sdk = OpenTelemetrySdk.builder()
                .setTracerProvider(tracerProvider)
                .setMeterProvider(meterProvider)
                .setLoggerProvider(loggerProvider)
                .buildAndRegisterGlobal();

        // Register JVM shutdown hook to flush pending telemetry
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("[Telemetry] Shutting down OTel providers — flushing pending telemetry...");
            sdk.getSdkTracerProvider().shutdown();
            sdk.getSdkMeterProvider().shutdown();
            sdk.getSdkLoggerProvider().shutdown();
        }, "otel-shutdown-hook"));

        return sdk;
    }

    // -------------------------------------------------------------------------
    // Private builder helpers
    // -------------------------------------------------------------------------

    private Resource buildResource() {
        return Resource.getDefault().merge(
                Resource.create(Attributes.builder()
                        .put(SERVICE_NAME, serviceName)
                        .put(DEPLOYMENT_ENVIRONMENT, environment)
                        .put(CLOUD_PROVIDER, "azure")
                        .put(AI_CLOUD_ROLE, serviceName)
                        .build()
                )
        );
    }

    private SdkTracerProvider buildTracerProvider(Resource resource) {
        final OtlpHttpSpanExporter exporter = OtlpHttpSpanExporter.builder()
                .setEndpoint(otlpEndpoint + "/v1/traces")
                .setTimeout(Duration.ofSeconds(10))
                .build();

        return SdkTracerProvider.builder()
                .setResource(resource)
                .addSpanProcessor(BatchSpanProcessor.builder(exporter)
                        .setScheduleDelay(Duration.ofSeconds(2))
                        .build())
                .setSampler(Sampler.alwaysOn())
                .build();
    }

    private SdkMeterProvider buildMeterProvider(Resource resource) {
        final OtlpHttpMetricExporter exporter = OtlpHttpMetricExporter.builder()
                .setEndpoint(otlpEndpoint + "/v1/metrics")
                .setTimeout(Duration.ofSeconds(10))
                .build();

        return SdkMeterProvider.builder()
                .setResource(resource)
                .registerMetricReader(PeriodicMetricReader.builder(exporter)
                        .setInterval(Duration.ofSeconds(15))
                        .build())
                .build();
    }

    private SdkLoggerProvider buildLoggerProvider(Resource resource) {
        final OtlpHttpLogRecordExporter exporter = OtlpHttpLogRecordExporter.builder()
                .setEndpoint(otlpEndpoint + "/v1/logs")
                .setTimeout(Duration.ofSeconds(10))
                .build();

        return SdkLoggerProvider.builder()
                .setResource(resource)
                .addLogRecordProcessor(BatchLogRecordProcessor.builder(exporter).build())
                .build();
    }
}
