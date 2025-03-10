/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.awssdk.v2_2;

import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.equalTo;
import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.satisfies;
import static io.opentelemetry.semconv.incubating.GenAiIncubatingAttributes.GEN_AI_OPERATION_NAME;
import static io.opentelemetry.semconv.incubating.GenAiIncubatingAttributes.GEN_AI_REQUEST_MAX_TOKENS;
import static io.opentelemetry.semconv.incubating.GenAiIncubatingAttributes.GEN_AI_REQUEST_MODEL;
import static io.opentelemetry.semconv.incubating.GenAiIncubatingAttributes.GEN_AI_REQUEST_STOP_SEQUENCES;
import static io.opentelemetry.semconv.incubating.GenAiIncubatingAttributes.GEN_AI_REQUEST_TEMPERATURE;
import static io.opentelemetry.semconv.incubating.GenAiIncubatingAttributes.GEN_AI_REQUEST_TOP_P;
import static io.opentelemetry.semconv.incubating.GenAiIncubatingAttributes.GEN_AI_RESPONSE_FINISH_REASONS;
import static io.opentelemetry.semconv.incubating.GenAiIncubatingAttributes.GEN_AI_SYSTEM;
import static io.opentelemetry.semconv.incubating.GenAiIncubatingAttributes.GEN_AI_TOKEN_TYPE;
import static io.opentelemetry.semconv.incubating.GenAiIncubatingAttributes.GEN_AI_USAGE_INPUT_TOKENS;
import static io.opentelemetry.semconv.incubating.GenAiIncubatingAttributes.GEN_AI_USAGE_OUTPUT_TOKENS;
import static io.opentelemetry.semconv.incubating.GenAiIncubatingAttributes.GenAiSystemIncubatingValues.AWS_BEDROCK;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.KeyValue;
import io.opentelemetry.api.common.Value;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.instrumentation.awssdk.v2_2.recording.RecordingExtension;
import io.opentelemetry.instrumentation.testing.junit.InstrumentationExtension;
import io.opentelemetry.semconv.incubating.GenAiIncubatingAttributes;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.core.document.Document;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClientBuilder;
import software.amazon.awssdk.services.bedrockruntime.model.ContentBlock;
import software.amazon.awssdk.services.bedrockruntime.model.ConversationRole;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseRequest;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseResponse;
import software.amazon.awssdk.services.bedrockruntime.model.InferenceConfiguration;
import software.amazon.awssdk.services.bedrockruntime.model.Message;
import software.amazon.awssdk.services.bedrockruntime.model.Tool;
import software.amazon.awssdk.services.bedrockruntime.model.ToolConfiguration;
import software.amazon.awssdk.services.bedrockruntime.model.ToolInputSchema;
import software.amazon.awssdk.services.bedrockruntime.model.ToolResultBlock;
import software.amazon.awssdk.services.bedrockruntime.model.ToolResultContentBlock;
import software.amazon.awssdk.services.bedrockruntime.model.ToolSpecification;

public abstract class AbstractAws2BedrockRuntimeTest {
  protected static final String INSTRUMENTATION_NAME = "io.opentelemetry.aws-sdk-2.2";

  private static final String API_URL = "https://bedrock-runtime.us-east-1.amazonaws.com";

  protected static final AttributeKey<String> EVENT_NAME = AttributeKey.stringKey("event.name");

  @RegisterExtension static final RecordingExtension recording = new RecordingExtension(API_URL);

  protected abstract InstrumentationExtension getTesting();

  protected abstract ClientOverrideConfiguration.Builder createOverrideConfigurationBuilder();

  protected static void configureClient(BedrockRuntimeClientBuilder builder) {
    builder
        .region(Region.US_EAST_1)
        .endpointOverride(URI.create("http://localhost:" + recording.getPort()));
    if (recording.isRecording()) {
      builder.putAuthScheme(new FixedHostAwsV4AuthScheme(API_URL));
    } else {
      builder.credentialsProvider(
          StaticCredentialsProvider.create(AwsBasicCredentials.create("testing", "testing")));
    }
  }

  @Test
  void testConverseBasic() {
    BedrockRuntimeClientBuilder builder = BedrockRuntimeClient.builder();
    builder.overrideConfiguration(createOverrideConfigurationBuilder().build());
    configureClient(builder);
    BedrockRuntimeClient client = builder.build();

    String modelId = "amazon.titan-text-lite-v1";
    ConverseResponse response =
        client.converse(
            ConverseRequest.builder()
                .modelId(modelId)
                .messages(
                    Message.builder()
                        .role(ConversationRole.USER)
                        .content(ContentBlock.fromText("Say this is a test"))
                        .build())
                .build());

    assertThat(response.output().message().content().get(0).text())
        .isEqualTo("Hi there! How can I help you today?");

    getTesting()
        .waitAndAssertTraces(
            trace ->
                trace.hasSpansSatisfyingExactly(
                    span ->
                        span.hasName("chat amazon.titan-text-lite-v1")
                            .hasKind(SpanKind.CLIENT)
                            .hasAttributesSatisfying(
                                equalTo(GEN_AI_SYSTEM, AWS_BEDROCK),
                                equalTo(
                                    GEN_AI_OPERATION_NAME,
                                    GenAiIncubatingAttributes.GenAiOperationNameIncubatingValues
                                        .CHAT),
                                equalTo(GEN_AI_REQUEST_MODEL, modelId),
                                equalTo(GEN_AI_USAGE_INPUT_TOKENS, 8),
                                equalTo(GEN_AI_USAGE_OUTPUT_TOKENS, 14),
                                equalTo(GEN_AI_RESPONSE_FINISH_REASONS, asList("end_turn")))));

    getTesting()
        .waitAndAssertMetrics(
            INSTRUMENTATION_NAME,
            metric ->
                metric
                    .hasName("gen_ai.client.token.usage")
                    .hasUnit("{token}")
                    .hasDescription("Measures number of input and output tokens used")
                    .hasHistogramSatisfying(
                        histogram ->
                            histogram.hasPointsSatisfying(
                                point ->
                                    point
                                        .hasSum(8)
                                        .hasCount(1)
                                        .hasAttributesSatisfyingExactly(
                                            equalTo(GEN_AI_SYSTEM, AWS_BEDROCK),
                                            equalTo(
                                                GEN_AI_TOKEN_TYPE,
                                                GenAiIncubatingAttributes
                                                    .GenAiTokenTypeIncubatingValues.INPUT),
                                            equalTo(
                                                GEN_AI_OPERATION_NAME,
                                                GenAiIncubatingAttributes
                                                    .GenAiOperationNameIncubatingValues.CHAT),
                                            equalTo(GEN_AI_REQUEST_MODEL, modelId)),
                                point ->
                                    point
                                        .hasSum(14)
                                        .hasCount(1)
                                        .hasAttributesSatisfyingExactly(
                                            equalTo(GEN_AI_SYSTEM, AWS_BEDROCK),
                                            equalTo(
                                                GEN_AI_TOKEN_TYPE,
                                                GenAiIncubatingAttributes
                                                    .GenAiTokenTypeIncubatingValues.COMPLETION),
                                            equalTo(
                                                GEN_AI_OPERATION_NAME,
                                                GenAiIncubatingAttributes
                                                    .GenAiOperationNameIncubatingValues.CHAT),
                                            equalTo(GEN_AI_REQUEST_MODEL, modelId)))),
            metric ->
                metric
                    .hasName("gen_ai.client.operation.duration")
                    .hasUnit("s")
                    .hasDescription("GenAI operation duration")
                    .hasHistogramSatisfying(
                        histogram ->
                            histogram.hasPointsSatisfying(
                                point ->
                                    point
                                        .hasSumGreaterThan(0.0)
                                        .hasAttributesSatisfyingExactly(
                                            equalTo(GEN_AI_SYSTEM, AWS_BEDROCK),
                                            equalTo(
                                                GEN_AI_OPERATION_NAME,
                                                GenAiIncubatingAttributes
                                                    .GenAiOperationNameIncubatingValues.CHAT),
                                            equalTo(GEN_AI_REQUEST_MODEL, modelId)))));

    SpanContext spanCtx = getTesting().waitForTraces(1).get(0).get(0).getSpanContext();

    getTesting()
        .waitAndAssertLogRecords(
            log ->
                log.hasAttributesSatisfyingExactly(
                        equalTo(GEN_AI_SYSTEM, AWS_BEDROCK),
                        equalTo(EVENT_NAME, "gen_ai.user.message"))
                    .hasSpanContext(spanCtx)
                    .hasBody(Value.of(KeyValue.of("content", Value.of("Say this is a test")))),
            log ->
                log.hasAttributesSatisfyingExactly(
                        equalTo(GEN_AI_SYSTEM, AWS_BEDROCK), equalTo(EVENT_NAME, "gen_ai.choice"))
                    .hasSpanContext(spanCtx)
                    .hasBody(
                        Value.of(
                            KeyValue.of("finish_reason", Value.of("end_turn")),
                            KeyValue.of("index", Value.of(0)),
                            KeyValue.of(
                                "content", Value.of("Hi there! How can I help you today?")))));
  }

  @Test
  void testConverseOptions() {
    BedrockRuntimeClientBuilder builder = BedrockRuntimeClient.builder();
    builder.overrideConfiguration(createOverrideConfigurationBuilder().build());
    configureClient(builder);
    BedrockRuntimeClient client = builder.build();

    String modelId = "amazon.titan-text-lite-v1";
    ConverseResponse response =
        client.converse(
            ConverseRequest.builder()
                .modelId(modelId)
                .messages(
                    Message.builder()
                        .role(ConversationRole.USER)
                        .content(ContentBlock.fromText("Say this is a test"))
                        .build())
                .inferenceConfig(
                    InferenceConfiguration.builder()
                        .maxTokens(10)
                        .temperature(0.8f)
                        .topP(1f)
                        .stopSequences("|")
                        .build())
                .build());

    assertThat(response.output().message().content().get(0).text()).isEqualTo("This is an LLM (");

    getTesting()
        .waitAndAssertTraces(
            trace ->
                trace.hasSpansSatisfyingExactly(
                    span ->
                        span.hasName("chat amazon.titan-text-lite-v1")
                            .hasKind(SpanKind.CLIENT)
                            .hasAttributesSatisfying(
                                equalTo(GEN_AI_SYSTEM, AWS_BEDROCK),
                                equalTo(
                                    GEN_AI_OPERATION_NAME,
                                    GenAiIncubatingAttributes.GenAiOperationNameIncubatingValues
                                        .CHAT),
                                equalTo(GEN_AI_REQUEST_MODEL, modelId),
                                equalTo(GEN_AI_REQUEST_MAX_TOKENS, 10),
                                satisfies(
                                    GEN_AI_REQUEST_TEMPERATURE,
                                    temp -> temp.isCloseTo(0.8, within(0.0001))),
                                equalTo(GEN_AI_REQUEST_TOP_P, 1.0),
                                equalTo(GEN_AI_REQUEST_STOP_SEQUENCES, asList("|")),
                                equalTo(GEN_AI_USAGE_INPUT_TOKENS, 8),
                                equalTo(GEN_AI_USAGE_OUTPUT_TOKENS, 10),
                                equalTo(GEN_AI_RESPONSE_FINISH_REASONS, asList("max_tokens")))));

    getTesting()
        .waitAndAssertMetrics(
            INSTRUMENTATION_NAME,
            metric ->
                metric
                    .hasName("gen_ai.client.token.usage")
                    .hasUnit("{token}")
                    .hasDescription("Measures number of input and output tokens used")
                    .hasHistogramSatisfying(
                        histogram ->
                            histogram.hasPointsSatisfying(
                                point ->
                                    point
                                        .hasSum(8)
                                        .hasCount(1)
                                        .hasAttributesSatisfyingExactly(
                                            equalTo(GEN_AI_SYSTEM, AWS_BEDROCK),
                                            equalTo(
                                                GEN_AI_TOKEN_TYPE,
                                                GenAiIncubatingAttributes
                                                    .GenAiTokenTypeIncubatingValues.INPUT),
                                            equalTo(
                                                GEN_AI_OPERATION_NAME,
                                                GenAiIncubatingAttributes
                                                    .GenAiOperationNameIncubatingValues.CHAT),
                                            equalTo(GEN_AI_REQUEST_MODEL, modelId)),
                                point ->
                                    point
                                        .hasSum(10)
                                        .hasCount(1)
                                        .hasAttributesSatisfyingExactly(
                                            equalTo(GEN_AI_SYSTEM, AWS_BEDROCK),
                                            equalTo(
                                                GEN_AI_TOKEN_TYPE,
                                                GenAiIncubatingAttributes
                                                    .GenAiTokenTypeIncubatingValues.COMPLETION),
                                            equalTo(
                                                GEN_AI_OPERATION_NAME,
                                                GenAiIncubatingAttributes
                                                    .GenAiOperationNameIncubatingValues.CHAT),
                                            equalTo(GEN_AI_REQUEST_MODEL, modelId)))),
            metric ->
                metric
                    .hasName("gen_ai.client.operation.duration")
                    .hasUnit("s")
                    .hasDescription("GenAI operation duration")
                    .hasHistogramSatisfying(
                        histogram ->
                            histogram.hasPointsSatisfying(
                                point ->
                                    point
                                        .hasSumGreaterThan(0.0)
                                        .hasAttributesSatisfyingExactly(
                                            equalTo(GEN_AI_SYSTEM, AWS_BEDROCK),
                                            equalTo(
                                                GEN_AI_OPERATION_NAME,
                                                GenAiIncubatingAttributes
                                                    .GenAiOperationNameIncubatingValues.CHAT),
                                            equalTo(GEN_AI_REQUEST_MODEL, modelId)))));

    SpanContext spanCtx = getTesting().waitForTraces(1).get(0).get(0).getSpanContext();

    getTesting()
        .waitAndAssertLogRecords(
            log ->
                log.hasAttributesSatisfyingExactly(
                        equalTo(GEN_AI_SYSTEM, AWS_BEDROCK),
                        equalTo(EVENT_NAME, "gen_ai.user.message"))
                    .hasSpanContext(spanCtx)
                    .hasBody(Value.of(KeyValue.of("content", Value.of("Say this is a test")))),
            log ->
                log.hasAttributesSatisfyingExactly(
                        equalTo(GEN_AI_SYSTEM, AWS_BEDROCK), equalTo(EVENT_NAME, "gen_ai.choice"))
                    .hasSpanContext(spanCtx)
                    .hasBody(
                        Value.of(
                            KeyValue.of("finish_reason", Value.of("max_tokens")),
                            KeyValue.of("index", Value.of(0)),
                            KeyValue.of("content", Value.of("This is an LLM (")))));
  }

  @Test
  void testConverseToolCall() {
    BedrockRuntimeClientBuilder builder = BedrockRuntimeClient.builder();
    builder.overrideConfiguration(createOverrideConfigurationBuilder().build());
    configureClient(builder);
    BedrockRuntimeClient client = builder.build();

    String modelId = "amazon.nova-micro-v1:0";
    List<Message> messages = new ArrayList<>();
    messages.add(
        Message.builder()
            .role(ConversationRole.USER)
            .content(
                ContentBlock.fromText("What is the weather in Seattle and San Francisco today?"))
            .build());
    ConverseResponse response0 =
        client.converse(
            ConverseRequest.builder()
                .modelId(modelId)
                .messages(messages)
                .toolConfig(currentWeatherToolConfig())
                .build());

    String seattleToolUseId0 = "";
    String sanFranciscoToolUseId0 = "";
    for (ContentBlock content : response0.output().message().content()) {
      if (content.toolUse() == null) {
        continue;
      }
      String toolUseId = content.toolUse().toolUseId();
      switch (content.toolUse().input().asMap().get("location").asString()) {
        case "Seattle":
          seattleToolUseId0 = toolUseId;
          break;
        case "San Francisco":
          sanFranciscoToolUseId0 = toolUseId;
          break;
        default:
          throw new IllegalArgumentException("Invalid tool use: " + content.toolUse());
      }
    }
    String seattleToolUseId = seattleToolUseId0;
    String sanFranciscoToolUseId = sanFranciscoToolUseId0;

    getTesting()
        .waitAndAssertTraces(
            trace ->
                trace.hasSpansSatisfyingExactly(
                    span ->
                        span.hasName("chat amazon.nova-micro-v1:0")
                            .hasKind(SpanKind.CLIENT)
                            .hasAttributesSatisfying(
                                equalTo(GEN_AI_SYSTEM, AWS_BEDROCK),
                                equalTo(
                                    GEN_AI_OPERATION_NAME,
                                    GenAiIncubatingAttributes.GenAiOperationNameIncubatingValues
                                        .CHAT),
                                equalTo(GEN_AI_REQUEST_MODEL, modelId),
                                equalTo(GEN_AI_USAGE_INPUT_TOKENS, 415),
                                equalTo(GEN_AI_USAGE_OUTPUT_TOKENS, 162),
                                equalTo(GEN_AI_RESPONSE_FINISH_REASONS, asList("tool_use")))));

    getTesting()
        .waitAndAssertMetrics(
            INSTRUMENTATION_NAME,
            metric ->
                metric
                    .hasName("gen_ai.client.token.usage")
                    .hasUnit("{token}")
                    .hasDescription("Measures number of input and output tokens used")
                    .hasHistogramSatisfying(
                        histogram ->
                            histogram.hasPointsSatisfying(
                                point ->
                                    point
                                        .hasSum(415)
                                        .hasCount(1)
                                        .hasAttributesSatisfyingExactly(
                                            equalTo(GEN_AI_SYSTEM, AWS_BEDROCK),
                                            equalTo(
                                                GEN_AI_TOKEN_TYPE,
                                                GenAiIncubatingAttributes
                                                    .GenAiTokenTypeIncubatingValues.INPUT),
                                            equalTo(
                                                GEN_AI_OPERATION_NAME,
                                                GenAiIncubatingAttributes
                                                    .GenAiOperationNameIncubatingValues.CHAT),
                                            equalTo(GEN_AI_REQUEST_MODEL, modelId)),
                                point ->
                                    point
                                        .hasSum(162)
                                        .hasCount(1)
                                        .hasAttributesSatisfyingExactly(
                                            equalTo(GEN_AI_SYSTEM, AWS_BEDROCK),
                                            equalTo(
                                                GEN_AI_TOKEN_TYPE,
                                                GenAiIncubatingAttributes
                                                    .GenAiTokenTypeIncubatingValues.COMPLETION),
                                            equalTo(
                                                GEN_AI_OPERATION_NAME,
                                                GenAiIncubatingAttributes
                                                    .GenAiOperationNameIncubatingValues.CHAT),
                                            equalTo(GEN_AI_REQUEST_MODEL, modelId)))),
            metric ->
                metric
                    .hasName("gen_ai.client.operation.duration")
                    .hasUnit("s")
                    .hasDescription("GenAI operation duration")
                    .hasHistogramSatisfying(
                        histogram ->
                            histogram.hasPointsSatisfying(
                                point ->
                                    point
                                        .hasSumGreaterThan(0.0)
                                        .hasAttributesSatisfyingExactly(
                                            equalTo(GEN_AI_SYSTEM, AWS_BEDROCK),
                                            equalTo(
                                                GEN_AI_OPERATION_NAME,
                                                GenAiIncubatingAttributes
                                                    .GenAiOperationNameIncubatingValues.CHAT),
                                            equalTo(GEN_AI_REQUEST_MODEL, modelId)))));

    SpanContext spanCtx0 = getTesting().waitForTraces(1).get(0).get(0).getSpanContext();

    getTesting()
        .waitAndAssertLogRecords(
            log ->
                log.hasAttributesSatisfyingExactly(
                        equalTo(GEN_AI_SYSTEM, AWS_BEDROCK),
                        equalTo(EVENT_NAME, "gen_ai.user.message"))
                    .hasSpanContext(spanCtx0)
                    .hasBody(
                        Value.of(
                            KeyValue.of(
                                "content",
                                Value.of(
                                    "What is the weather in Seattle and San Francisco today?")))),
            log ->
                log.hasAttributesSatisfyingExactly(
                        equalTo(GEN_AI_SYSTEM, AWS_BEDROCK), equalTo(EVENT_NAME, "gen_ai.choice"))
                    .hasSpanContext(spanCtx0)
                    .hasBody(
                        Value.of(
                            KeyValue.of("finish_reason", Value.of("tool_use")),
                            KeyValue.of("index", Value.of(0)),
                            KeyValue.of(
                                "toolCalls",
                                Value.of(
                                    Value.of(
                                        KeyValue.of("name", Value.of("get_current_weather")),
                                        KeyValue.of(
                                            "arguments", Value.of("{\"location\":\"Seattle\"}")),
                                        KeyValue.of("id", Value.of(seattleToolUseId)),
                                        KeyValue.of("type", Value.of("function"))),
                                    Value.of(
                                        KeyValue.of("name", Value.of("get_current_weather")),
                                        KeyValue.of(
                                            "arguments",
                                            Value.of("{\"location\":\"San Francisco\"}")),
                                        KeyValue.of("id", Value.of(sanFranciscoToolUseId)),
                                        KeyValue.of("type", Value.of("function"))))),
                            KeyValue.of(
                                "content",
                                Value.of(
                                    "<thinking> The User has asked for the current weather in two locations: Seattle and San Francisco. To provide the requested information, I will use the \"get_current_weather\" tool for each location separately. </thinking>\n")))));

    getTesting().clearData();

    messages.add(response0.output().message());
    messages.add(
        Message.builder()
            .role(ConversationRole.USER)
            .content(
                ContentBlock.fromToolResult(
                    ToolResultBlock.builder()
                        .content(
                            ToolResultContentBlock.builder()
                                .json(
                                    Document.mapBuilder()
                                        .putString("weather", "50 degrees and raining")
                                        .build())
                                .build())
                        .toolUseId(seattleToolUseId)
                        .build()),
                ContentBlock.fromToolResult(
                    ToolResultBlock.builder()
                        .content(
                            ToolResultContentBlock.builder()
                                .json(
                                    Document.mapBuilder()
                                        .putString("weather", "70 degrees and sunny")
                                        .build())
                                .build())
                        .toolUseId(sanFranciscoToolUseId)
                        .build()))
            .build());

    ConverseResponse response1 =
        client.converse(
            ConverseRequest.builder()
                .modelId(modelId)
                .messages(messages)
                .toolConfig(currentWeatherToolConfig())
                .build());

    assertThat(response1.output().message().content().get(0).text())
        .contains(
            "The current weather in Seattle is 50 degrees and raining. "
                + "In San Francisco, the weather is 70 degrees and sunny.");

    getTesting()
        .waitAndAssertTraces(
            trace ->
                trace.hasSpansSatisfyingExactly(
                    span ->
                        span.hasName("chat amazon.nova-micro-v1:0")
                            .hasKind(SpanKind.CLIENT)
                            .hasAttributesSatisfying(
                                equalTo(GEN_AI_SYSTEM, AWS_BEDROCK),
                                equalTo(
                                    GEN_AI_OPERATION_NAME,
                                    GenAiIncubatingAttributes.GenAiOperationNameIncubatingValues
                                        .CHAT),
                                equalTo(GEN_AI_REQUEST_MODEL, modelId),
                                equalTo(GEN_AI_USAGE_INPUT_TOKENS, 554),
                                equalTo(GEN_AI_USAGE_OUTPUT_TOKENS, 57),
                                equalTo(GEN_AI_RESPONSE_FINISH_REASONS, asList("end_turn")))));

    getTesting()
        .waitAndAssertMetrics(
            INSTRUMENTATION_NAME,
            metric ->
                metric
                    .hasName("gen_ai.client.token.usage")
                    .hasUnit("{token}")
                    .hasDescription("Measures number of input and output tokens used")
                    .hasHistogramSatisfying(
                        histogram ->
                            histogram.hasPointsSatisfying(
                                point ->
                                    point
                                        .hasSum(554)
                                        .hasCount(1)
                                        .hasAttributesSatisfyingExactly(
                                            equalTo(GEN_AI_SYSTEM, AWS_BEDROCK),
                                            equalTo(
                                                GEN_AI_TOKEN_TYPE,
                                                GenAiIncubatingAttributes
                                                    .GenAiTokenTypeIncubatingValues.INPUT),
                                            equalTo(
                                                GEN_AI_OPERATION_NAME,
                                                GenAiIncubatingAttributes
                                                    .GenAiOperationNameIncubatingValues.CHAT),
                                            equalTo(GEN_AI_REQUEST_MODEL, modelId)),
                                point ->
                                    point
                                        .hasSum(57)
                                        .hasCount(1)
                                        .hasAttributesSatisfyingExactly(
                                            equalTo(GEN_AI_SYSTEM, AWS_BEDROCK),
                                            equalTo(
                                                GEN_AI_TOKEN_TYPE,
                                                GenAiIncubatingAttributes
                                                    .GenAiTokenTypeIncubatingValues.COMPLETION),
                                            equalTo(
                                                GEN_AI_OPERATION_NAME,
                                                GenAiIncubatingAttributes
                                                    .GenAiOperationNameIncubatingValues.CHAT),
                                            equalTo(GEN_AI_REQUEST_MODEL, modelId)))),
            metric ->
                metric
                    .hasName("gen_ai.client.operation.duration")
                    .hasUnit("s")
                    .hasDescription("GenAI operation duration")
                    .hasHistogramSatisfying(
                        histogram ->
                            histogram.hasPointsSatisfying(
                                point ->
                                    point
                                        .hasSumGreaterThan(0.0)
                                        .hasAttributesSatisfyingExactly(
                                            equalTo(GEN_AI_SYSTEM, AWS_BEDROCK),
                                            equalTo(
                                                GEN_AI_OPERATION_NAME,
                                                GenAiIncubatingAttributes
                                                    .GenAiOperationNameIncubatingValues.CHAT),
                                            equalTo(GEN_AI_REQUEST_MODEL, modelId)))));

    SpanContext spanCtx1 = getTesting().waitForTraces(1).get(0).get(0).getSpanContext();

    getTesting()
        .waitAndAssertLogRecords(
            log ->
                log.hasAttributesSatisfyingExactly(
                        equalTo(GEN_AI_SYSTEM, AWS_BEDROCK),
                        equalTo(EVENT_NAME, "gen_ai.user.message"))
                    .hasSpanContext(spanCtx1)
                    .hasBody(
                        Value.of(
                            KeyValue.of(
                                "content",
                                Value.of(
                                    "What is the weather in Seattle and San Francisco today?")))),
            log ->
                log.hasAttributesSatisfyingExactly(
                        equalTo(GEN_AI_SYSTEM, AWS_BEDROCK),
                        equalTo(EVENT_NAME, "gen_ai.assistant.message"))
                    .hasSpanContext(spanCtx1)
                    .hasBody(
                        Value.of(
                            KeyValue.of(
                                "toolCalls",
                                Value.of(
                                    Value.of(
                                        KeyValue.of("name", Value.of("get_current_weather")),
                                        KeyValue.of(
                                            "arguments", Value.of("{\"location\":\"Seattle\"}")),
                                        KeyValue.of("id", Value.of(seattleToolUseId)),
                                        KeyValue.of("type", Value.of("function"))),
                                    Value.of(
                                        KeyValue.of("name", Value.of("get_current_weather")),
                                        KeyValue.of(
                                            "arguments",
                                            Value.of("{\"location\":\"San Francisco\"}")),
                                        KeyValue.of("id", Value.of(sanFranciscoToolUseId)),
                                        KeyValue.of("type", Value.of("function"))))),
                            KeyValue.of(
                                "content",
                                Value.of(
                                    "<thinking> The User has asked for the current weather in two locations: Seattle and San Francisco. To provide the requested information, I will use the \"get_current_weather\" tool for each location separately. </thinking>\n")))),
            log ->
                log.hasAttributesSatisfyingExactly(
                        equalTo(GEN_AI_SYSTEM, AWS_BEDROCK),
                        equalTo(EVENT_NAME, "gen_ai.tool.message"))
                    .hasSpanContext(spanCtx1)
                    .hasBody(
                        Value.of(
                            KeyValue.of("id", Value.of(seattleToolUseId)),
                            KeyValue.of(
                                "content", Value.of("{\"weather\":\"50 degrees and raining\"}")))),
            log ->
                log.hasAttributesSatisfyingExactly(
                        equalTo(GEN_AI_SYSTEM, AWS_BEDROCK),
                        equalTo(EVENT_NAME, "gen_ai.tool.message"))
                    .hasSpanContext(spanCtx1)
                    .hasBody(
                        Value.of(
                            KeyValue.of("id", Value.of(sanFranciscoToolUseId)),
                            KeyValue.of(
                                "content", Value.of("{\"weather\":\"70 degrees and sunny\"}")))),
            log ->
                log.hasAttributesSatisfyingExactly(
                        equalTo(GEN_AI_SYSTEM, AWS_BEDROCK), equalTo(EVENT_NAME, "gen_ai.choice"))
                    .hasSpanContext(spanCtx1)
                    .hasBody(
                        Value.of(
                            KeyValue.of("finish_reason", Value.of("end_turn")),
                            KeyValue.of("index", Value.of(0)),
                            KeyValue.of(
                                "content",
                                Value.of(
                                    "<thinking> The tool has provided the current weather for both locations. Now I will compile this information and present it to the User. </thinking>\n"
                                        + "\n"
                                        + "The current weather in Seattle is 50 degrees and raining. In San Francisco, the weather is 70 degrees and sunny.")))));
  }

  private static ToolConfiguration currentWeatherToolConfig() {
    return ToolConfiguration.builder()
        .tools(
            Tool.builder()
                .toolSpec(
                    ToolSpecification.builder()
                        .name("get_current_weather")
                        .description("Get the current weather in a given location.")
                        .inputSchema(
                            ToolInputSchema.builder()
                                .json(
                                    Document.mapBuilder()
                                        .putString("type", "object")
                                        .putDocument(
                                            "properties",
                                            Document.mapBuilder()
                                                .putDocument(
                                                    "location",
                                                    Document.mapBuilder()
                                                        .putString("type", "string")
                                                        .putString(
                                                            "description", "The name of the city")
                                                        .build())
                                                .build())
                                        .putList(
                                            "required",
                                            singletonList(Document.fromString("location")))
                                        .build())
                                .build())
                        .build())
                .build())
        .build();
  }
}
