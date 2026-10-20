package copel.sesproductpackage.core.api.markitdown;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import copel.sesproductpackage.core.util.Properties;
import copel.sesproductpackage.core.util.SsmParameterKey;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.LambdaClientBuilder;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.services.lambda.model.InvokeResponse;
import software.amazon.awssdk.services.lambda.model.LambdaException;

class MarkItDownTest {

  @AfterEach
  void tearDown() {
    MarkItDown.resetTestHooks();
  }

  @Test
  void privateConstructorThrows() throws Exception {
    Constructor<MarkItDown> c = MarkItDown.class.getDeclaredConstructor();
    c.setAccessible(true);
    InvocationTargetException ex =
        assertThrows(InvocationTargetException.class, () -> c.newInstance());
    assertInstanceOf(UnsupportedOperationException.class, ex.getCause());
  }

  @Test
  void invokeUsesDefaultRegionWhenAwsRegionBlank() {
    String okJson = "{\"success\":true,\"markdown\":\"# Hi\",\"title\":null,\"error\":null}";
    try (MockedStatic<Properties> props = mockStatic(Properties.class);
        MockedStatic<LambdaClient> lambda = mockStatic(LambdaClient.class)) {
      props.when(() -> Properties.get(SsmParameterKey.MARKITDOWN_NAME.getKey())).thenReturn("fn");

      LambdaClient client = mockLambdaChain(lambda, okJson);

      MarkItDown.MarkitdownLambdaRequestEntity req =
          MarkItDown.MarkitdownLambdaRequestEntity.builder().url("https://example.com").build();
      MarkItDown.MarkitdownLambdaResponseEntity res = MarkItDown.invoke(req);
      assertTrue(res.isSuccess());
      assertEquals("# Hi", res.getMarkdown());
      verify(client).close();
    }
  }

  @Test
  void invokeUsesExplicitAwsRegion() {
    String okJson = "{\"success\":true,\"markdown\":\"m\",\"title\":\"t\",\"error\":null}";
    try (MockedStatic<Properties> props = mockStatic(Properties.class);
        MockedStatic<LambdaClient> lambda = mockStatic(LambdaClient.class)) {
      props.when(() -> Properties.get(SsmParameterKey.MARKITDOWN_NAME.getKey())).thenReturn("fn");

      LambdaClient client = mockLambdaChain(lambda, okJson);

      MarkItDown.MarkitdownLambdaResponseEntity res =
          MarkItDown.invoke(
              MarkItDown.MarkitdownLambdaRequestEntity.builder().url("https://u").build());
      assertTrue(res.isSuccess());
      assertEquals("m", res.getMarkdown());
      verify(client).close();
    }
  }

  @Test
  void invokeSuccessParsesResponse() {
    String okJson = "{\"success\":true,\"markdown\":\"# body\",\"title\":\"T\",\"error\":null}";
    try (MockedStatic<Properties> props = mockStatic(Properties.class);
        MockedStatic<LambdaClient> lambda = mockStatic(LambdaClient.class)) {
      props.when(() -> Properties.get(SsmParameterKey.MARKITDOWN_NAME.getKey())).thenReturn("fn");

      mockLambdaChain(lambda, okJson);

      MarkItDown.MarkitdownLambdaRequestEntity req =
          MarkItDown.MarkitdownLambdaRequestEntity.builder()
              .s3(
                  MarkItDown.MarkitdownLambdaRequestEntity.S3ObjectRef.builder()
                      .bucket("b")
                      .key("k")
                      .build())
              .filename("f.docx")
              .build();
      MarkItDown.MarkitdownLambdaResponseEntity res = MarkItDown.invoke(req);
      assertTrue(res.isSuccess());
      assertEquals("# body", res.getMarkdown());
      assertEquals("T", res.getTitle());
      assertNull(res.getError());
    }
  }

  @Test
  void invokeThrowsWhenRequestJsonFails() {
    try (MockedStatic<Properties> props = mockStatic(Properties.class);
        MockedStatic<LambdaClient> lambda = mockStatic(LambdaClient.class)) {
      props.when(() -> Properties.get(SsmParameterKey.MARKITDOWN_NAME.getKey())).thenReturn("fn");
      mockLambdaChain(lambda, "{}");

      MarkItDown.injectJsonProcessingExceptionOnSerializeForTest = true;
      IllegalStateException ex =
          assertThrows(
              IllegalStateException.class,
              () ->
                  MarkItDown.invoke(
                      MarkItDown.MarkitdownLambdaRequestEntity.builder().url("https://x").build()));
      assertTrue(ex.getMessage().contains("JSON 化"));
      assertNotNull(ex.getCause());
    }
  }

  @Test
  void invokeThrowsWhenFunctionErrorWithPayload() {
    try (MockedStatic<Properties> props = mockStatic(Properties.class);
        MockedStatic<LambdaClient> lambda = mockStatic(LambdaClient.class)) {
      props.when(() -> Properties.get(SsmParameterKey.MARKITDOWN_NAME.getKey())).thenReturn("fn");

      InvokeResponse response =
          InvokeResponse.builder()
              .functionError("Unhandled")
              .payload(SdkBytes.fromUtf8String("{\"x\":1}"))
              .build();
      mockLambdaChain(lambda, response);

      IllegalStateException ex =
          assertThrows(
              IllegalStateException.class,
              () ->
                  MarkItDown.invoke(
                      MarkItDown.MarkitdownLambdaRequestEntity.builder().url("https://x").build()));
      assertTrue(ex.getMessage().contains("Lambda 実行エラー"));
      assertTrue(ex.getMessage().contains("Unhandled"));
    }
  }

  @Test
  void invokeThrowsWhenFunctionErrorWithNullPayload() {
    try (MockedStatic<Properties> props = mockStatic(Properties.class);
        MockedStatic<LambdaClient> lambda = mockStatic(LambdaClient.class)) {
      props.when(() -> Properties.get(SsmParameterKey.MARKITDOWN_NAME.getKey())).thenReturn("fn");

      InvokeResponse response =
          InvokeResponse.builder().functionError("Unhandled").payload(null).build();
      mockLambdaChain(lambda, response);

      IllegalStateException ex =
          assertThrows(
              IllegalStateException.class,
              () ->
                  MarkItDown.invoke(
                      MarkItDown.MarkitdownLambdaRequestEntity.builder().url("https://x").build()));
      assertTrue(ex.getMessage().contains("Lambda 実行エラー"));
    }
  }

  @Test
  void invokeThrowsWhenPayloadNullAfterOkFunctionError() {
    try (MockedStatic<Properties> props = mockStatic(Properties.class);
        MockedStatic<LambdaClient> lambda = mockStatic(LambdaClient.class)) {
      props.when(() -> Properties.get(SsmParameterKey.MARKITDOWN_NAME.getKey())).thenReturn("fn");

      InvokeResponse response = InvokeResponse.builder().functionError(null).payload(null).build();
      mockLambdaChain(lambda, response);

      IllegalStateException ex =
          assertThrows(
              IllegalStateException.class,
              () ->
                  MarkItDown.invoke(
                      MarkItDown.MarkitdownLambdaRequestEntity.builder().url("https://x").build()));
      assertTrue(ex.getMessage().contains("応答ペイロードが空"));
    }
  }

  @Test
  void invokeThrowsWhenResponseJsonInvalid() {
    try (MockedStatic<Properties> props = mockStatic(Properties.class);
        MockedStatic<LambdaClient> lambda = mockStatic(LambdaClient.class)) {
      props.when(() -> Properties.get(SsmParameterKey.MARKITDOWN_NAME.getKey())).thenReturn("fn");

      mockLambdaChain(lambda, "{not-json");

      IllegalStateException ex =
          assertThrows(
              IllegalStateException.class,
              () ->
                  MarkItDown.invoke(
                      MarkItDown.MarkitdownLambdaRequestEntity.builder().url("https://x").build()));
      assertTrue(ex.getMessage().contains("JSON 解析"));
    }
  }

  @Test
  void invokeThrowsWhenLambdaClientThrowsLambdaException() {
    try (MockedStatic<Properties> props = mockStatic(Properties.class);
        MockedStatic<LambdaClient> lambda = mockStatic(LambdaClient.class)) {
      props.when(() -> Properties.get(SsmParameterKey.MARKITDOWN_NAME.getKey())).thenReturn("fn");

      LambdaClient client = mock(LambdaClient.class);
      LambdaClientBuilder builder = mock(LambdaClientBuilder.class);
      lambda.when(LambdaClient::builder).thenReturn(builder);
      when(builder.region(any(Region.class))).thenReturn(builder);
      when(builder.credentialsProvider(any())).thenReturn(builder);
      when(builder.build()).thenReturn(client);
      when(client.invoke(any(InvokeRequest.class)))
          .thenThrow(LambdaException.builder().message("invoke failed").statusCode(500).build());

      IllegalStateException ex =
          assertThrows(
              IllegalStateException.class,
              () ->
                  MarkItDown.invoke(
                      MarkItDown.MarkitdownLambdaRequestEntity.builder().url("https://x").build()));
      assertTrue(ex.getMessage().contains("呼び出しに失敗"));
      assertInstanceOf(LambdaException.class, ex.getCause());
    }
  }

  @Test
  void entitiesSupportBuilderEqualsAndErrorDetail() {
    MarkItDown.MarkitdownLambdaRequestEntity.S3ObjectRef s3a =
        MarkItDown.MarkitdownLambdaRequestEntity.S3ObjectRef.builder().bucket("b").key("k").build();
    MarkItDown.MarkitdownLambdaRequestEntity.S3ObjectRef s3b =
        MarkItDown.MarkitdownLambdaRequestEntity.S3ObjectRef.builder().bucket("b").key("k").build();
    assertEquals(s3a, s3b);
    assertEquals(s3a.hashCode(), s3b.hashCode());
    assertNotNull(s3a.toString());

    MarkItDown.MarkitdownLambdaResponseEntity.ErrorDetail err =
        MarkItDown.MarkitdownLambdaResponseEntity.ErrorDetail.builder()
            .type("ValueError")
            .message("bad")
            .build();
    MarkItDown.MarkitdownLambdaResponseEntity res =
        MarkItDown.MarkitdownLambdaResponseEntity.builder()
            .success(false)
            .markdown(null)
            .title(null)
            .error(err)
            .build();
    assertFalse(res.isSuccess());
    assertEquals("ValueError", res.getError().getType());
    assertEquals("bad", res.getError().getMessage());
    assertNotNull(res.toString());

    MarkItDown.MarkitdownLambdaRequestEntity req =
        MarkItDown.MarkitdownLambdaRequestEntity.builder()
            .fileBase64("YWI=")
            .filename("a.txt")
            .build();
    assertEquals("YWI=", req.getFileBase64());
    assertNotNull(req.toString());
  }

  @Test
  void validateSuccessReturnsSuccessResult() {
    String okJson =
        "{\"success\":true,\"markdown\":\"# Document Title\\n\\nBody text\",\"title\":\"Document Title\",\"error\":null}";
    try (MockedStatic<Properties> props = mockStatic(Properties.class);
        MockedStatic<LambdaClient> lambda = mockStatic(LambdaClient.class)) {
      props.when(() -> Properties.get(SsmParameterKey.MARKITDOWN_NAME.getKey())).thenReturn("fn");
      mockLambdaChain(lambda, okJson);

      MarkItDown.MarkitdownLambdaRequestEntity req =
          MarkItDown.MarkitdownLambdaRequestEntity.builder().url("https://example.com/doc").build();
      MarkItDown.ValidationResult result = MarkItDown.validate(req);

      assertNotNull(result);
      assertEquals(MarkItDown.ValidationStatus.SUCCESS, result.getStatus());
      assertTrue(result.isSuccess());
      assertFalse(result.isTooLargeOrTimeout());
      assertFalse(result.isParseFailed());
      assertEquals("# Document Title\n\nBody text", result.getMarkdown());
      assertNull(result.getErrorMessage());
    }
  }

  @Test
  void validateResponseErrorWithPayloadLimitReturnsTooLargeOrTimeout() {
    String errJson =
        "{\"success\":false,\"markdown\":null,\"title\":null,\"error\":{\"type\":\"PayloadTooLargeError\",\"message\":\"ファイルサイズが上限を超えています\"}}";
    try (MockedStatic<Properties> props = mockStatic(Properties.class);
        MockedStatic<LambdaClient> lambda = mockStatic(LambdaClient.class)) {
      props.when(() -> Properties.get(SsmParameterKey.MARKITDOWN_NAME.getKey())).thenReturn("fn");
      mockLambdaChain(lambda, errJson);

      MarkItDown.MarkitdownLambdaRequestEntity req =
          MarkItDown.MarkitdownLambdaRequestEntity.builder()
              .url("https://example.com/large.pdf")
              .build();
      MarkItDown.ValidationResult result = MarkItDown.validate(req);

      assertNotNull(result);
      assertEquals(MarkItDown.ValidationStatus.TOO_LARGE_OR_TIMEOUT, result.getStatus());
      assertFalse(result.isSuccess());
      assertTrue(result.isTooLargeOrTimeout());
      assertFalse(result.isParseFailed());
      assertNull(result.getMarkdown());
      assertEquals("ファイルサイズが上限を超えています", result.getErrorMessage());
    }
  }

  @Test
  void validateResponseErrorWithEnglishLimitKeywordsReturnsTooLargeOrTimeout() {
    String errJson =
        "{\"success\":false,\"markdown\":null,\"title\":null,\"error\":{\"type\":\"LimitExceededException\",\"message\":\"Payload too large (413): exceeds limit\"}}";
    try (MockedStatic<Properties> props = mockStatic(Properties.class);
        MockedStatic<LambdaClient> lambda = mockStatic(LambdaClient.class)) {
      props.when(() -> Properties.get(SsmParameterKey.MARKITDOWN_NAME.getKey())).thenReturn("fn");
      mockLambdaChain(lambda, errJson);

      MarkItDown.MarkitdownLambdaRequestEntity req =
          MarkItDown.MarkitdownLambdaRequestEntity.builder()
              .url("https://example.com/large.pdf")
              .build();
      MarkItDown.ValidationResult result = MarkItDown.validate(req);

      assertNotNull(result);
      assertEquals(MarkItDown.ValidationStatus.TOO_LARGE_OR_TIMEOUT, result.getStatus());
      assertTrue(result.isTooLargeOrTimeout());
      assertEquals("Payload too large (413): exceeds limit", result.getErrorMessage());
    }
  }

  @Test
  void validateResponseErrorWithParseFailureReturnsParseFailed() {
    String errJson =
        "{\"success\":false,\"markdown\":null,\"title\":null,\"error\":{\"type\":\"UnsupportedFormatException\",\"message\":\"未対応のファイル形式です\"}}";
    try (MockedStatic<Properties> props = mockStatic(Properties.class);
        MockedStatic<LambdaClient> lambda = mockStatic(LambdaClient.class)) {
      props.when(() -> Properties.get(SsmParameterKey.MARKITDOWN_NAME.getKey())).thenReturn("fn");
      mockLambdaChain(lambda, errJson);

      MarkItDown.MarkitdownLambdaRequestEntity req =
          MarkItDown.MarkitdownLambdaRequestEntity.builder()
              .url("https://example.com/bad.xyz")
              .build();
      MarkItDown.ValidationResult result = MarkItDown.validate(req);

      assertNotNull(result);
      assertEquals(MarkItDown.ValidationStatus.PARSE_FAILED, result.getStatus());
      assertFalse(result.isSuccess());
      assertFalse(result.isTooLargeOrTimeout());
      assertTrue(result.isParseFailed());
      assertNull(result.getMarkdown());
      assertEquals("未対応のファイル形式です", result.getErrorMessage());
    }
  }

  @Test
  void validateResponseErrorWithRateLimitOrInvalidPayloadDoesNotTriggerTooLargeOrTimeout() {
    String errJson =
        "{\"success\":false,\"markdown\":null,\"title\":null,\"error\":{\"type\":\"RateLimitExceeded\",\"message\":\"Rate limit exceeded; Invalid payload received\"}}";
    try (MockedStatic<Properties> props = mockStatic(Properties.class);
        MockedStatic<LambdaClient> lambda = mockStatic(LambdaClient.class)) {
      props.when(() -> Properties.get(SsmParameterKey.MARKITDOWN_NAME.getKey())).thenReturn("fn");
      mockLambdaChain(lambda, errJson);

      MarkItDown.MarkitdownLambdaRequestEntity req =
          MarkItDown.MarkitdownLambdaRequestEntity.builder()
              .url("https://example.com/ratelimit.pdf")
              .build();
      MarkItDown.ValidationResult result = MarkItDown.validate(req);

      assertNotNull(result);
      assertEquals(MarkItDown.ValidationStatus.PARSE_FAILED, result.getStatus());
      assertFalse(result.isSuccess());
      assertFalse(result.isTooLargeOrTimeout());
      assertTrue(result.isParseFailed());
      assertEquals("Rate limit exceeded; Invalid payload received", result.getErrorMessage());
    }
  }

  @Test
  void validateResponseErrorWithoutErrorDetailReturnsParseFailed() {
    String errJson = "{\"success\":false,\"markdown\":null,\"title\":null,\"error\":null}";
    try (MockedStatic<Properties> props = mockStatic(Properties.class);
        MockedStatic<LambdaClient> lambda = mockStatic(LambdaClient.class)) {
      props.when(() -> Properties.get(SsmParameterKey.MARKITDOWN_NAME.getKey())).thenReturn("fn");
      mockLambdaChain(lambda, errJson);

      MarkItDown.MarkitdownLambdaRequestEntity req =
          MarkItDown.MarkitdownLambdaRequestEntity.builder()
              .url("https://example.com/unknown")
              .build();
      MarkItDown.ValidationResult result = MarkItDown.validate(req);

      assertNotNull(result);
      assertEquals(MarkItDown.ValidationStatus.PARSE_FAILED, result.getStatus());
      assertTrue(result.isParseFailed());
      assertEquals("解析に失敗しました。", result.getErrorMessage());
    }
  }

  @Test
  void validateResponseSuccessTrueWithEmptyOrBlankMarkdownReturnsParseFailed() {
    String blankJson =
        "{\"success\":true,\"markdown\":\"   \\n  \",\"title\":\"Blank\",\"error\":null}";
    try (MockedStatic<Properties> props = mockStatic(Properties.class);
        MockedStatic<LambdaClient> lambda = mockStatic(LambdaClient.class)) {
      props.when(() -> Properties.get(SsmParameterKey.MARKITDOWN_NAME.getKey())).thenReturn("fn");
      mockLambdaChain(lambda, blankJson);

      MarkItDown.MarkitdownLambdaRequestEntity req =
          MarkItDown.MarkitdownLambdaRequestEntity.builder()
              .url("https://example.com/blank")
              .build();
      MarkItDown.ValidationResult result = MarkItDown.validate(req);

      assertNotNull(result);
      assertEquals(MarkItDown.ValidationStatus.PARSE_FAILED, result.getStatus());
      assertTrue(result.isParseFailed());
      assertNull(result.getMarkdown());
      assertEquals("Markdownの抽出結果が空です。", result.getErrorMessage());
    }
  }

  @Test
  void validateResponseSuccessTrueWithNullMarkdownReturnsParseFailed() {
    String nullJson =
        "{\"success\":true,\"markdown\":null,\"title\":\"NullMarkdown\",\"error\":null}";
    try (MockedStatic<Properties> props = mockStatic(Properties.class);
        MockedStatic<LambdaClient> lambda = mockStatic(LambdaClient.class)) {
      props.when(() -> Properties.get(SsmParameterKey.MARKITDOWN_NAME.getKey())).thenReturn("fn");
      mockLambdaChain(lambda, nullJson);

      MarkItDown.MarkitdownLambdaRequestEntity req =
          MarkItDown.MarkitdownLambdaRequestEntity.builder()
              .url("https://example.com/null")
              .build();
      MarkItDown.ValidationResult result = MarkItDown.validate(req);

      assertNotNull(result);
      assertEquals(MarkItDown.ValidationStatus.PARSE_FAILED, result.getStatus());
      assertTrue(result.isParseFailed());
      assertEquals("Markdownの抽出結果が空です。", result.getErrorMessage());
    }
  }

  @Test
  void validateInvokeThrowsTimeoutExceptionReturnsTooLargeOrTimeout() {
    try (MockedStatic<Properties> props = mockStatic(Properties.class);
        MockedStatic<LambdaClient> lambda = mockStatic(LambdaClient.class)) {
      props.when(() -> Properties.get(SsmParameterKey.MARKITDOWN_NAME.getKey())).thenReturn("fn");
      mockLambdaChainThrows(
          lambda,
          new RuntimeException("Gateway Timeout", new TimeoutException("Connection timed out")));

      MarkItDown.MarkitdownLambdaRequestEntity req =
          MarkItDown.MarkitdownLambdaRequestEntity.builder()
              .url("https://example.com/timeout")
              .build();
      MarkItDown.ValidationResult result = MarkItDown.validate(req);

      assertNotNull(result);
      assertEquals(MarkItDown.ValidationStatus.TOO_LARGE_OR_TIMEOUT, result.getStatus());
      assertTrue(result.isTooLargeOrTimeout());
      assertFalse(result.isSuccess());
      assertFalse(result.isParseFailed());
      assertEquals("Gateway Timeout", result.getErrorMessage());
    }
  }

  @Test
  void validateInvokeThrowsTimedOutMessageReturnsTooLargeOrTimeout() {
    try (MockedStatic<Properties> props = mockStatic(Properties.class);
        MockedStatic<LambdaClient> lambda = mockStatic(LambdaClient.class)) {
      props.when(() -> Properties.get(SsmParameterKey.MARKITDOWN_NAME.getKey())).thenReturn("fn");
      mockLambdaChainThrows(
          lambda,
          LambdaException.builder()
              .message("Task timed out after 30.00 seconds")
              .statusCode(504)
              .build());

      MarkItDown.MarkitdownLambdaRequestEntity req =
          MarkItDown.MarkitdownLambdaRequestEntity.builder()
              .url("https://example.com/timedout")
              .build();
      MarkItDown.ValidationResult result = MarkItDown.validate(req);

      assertNotNull(result);
      assertEquals(MarkItDown.ValidationStatus.TOO_LARGE_OR_TIMEOUT, result.getStatus());
      assertTrue(result.isTooLargeOrTimeout());
      assertEquals("Lambda の呼び出しに失敗しました。", result.getErrorMessage());
    }
  }

  @Test
  void validateInvokeThrowsPayloadTooLargeReturnsTooLargeOrTimeout() {
    try (MockedStatic<Properties> props = mockStatic(Properties.class);
        MockedStatic<LambdaClient> lambda = mockStatic(LambdaClient.class)) {
      props.when(() -> Properties.get(SsmParameterKey.MARKITDOWN_NAME.getKey())).thenReturn("fn");
      mockLambdaChainThrows(
          lambda,
          LambdaException.builder()
              .message("Request payload size exceeds the limit: 413 Payload Too Large")
              .statusCode(413)
              .build());

      MarkItDown.MarkitdownLambdaRequestEntity req =
          MarkItDown.MarkitdownLambdaRequestEntity.builder()
              .url("https://example.com/huge")
              .build();
      MarkItDown.ValidationResult result = MarkItDown.validate(req);

      assertNotNull(result);
      assertEquals(MarkItDown.ValidationStatus.TOO_LARGE_OR_TIMEOUT, result.getStatus());
      assertTrue(result.isTooLargeOrTimeout());
      assertEquals("Lambda の呼び出しに失敗しました。", result.getErrorMessage());
    }
  }

  @Test
  void validateInvokeThrowsGeneralExceptionReturnsParseFailed() {
    try (MockedStatic<Properties> props = mockStatic(Properties.class);
        MockedStatic<LambdaClient> lambda = mockStatic(LambdaClient.class)) {
      props.when(() -> Properties.get(SsmParameterKey.MARKITDOWN_NAME.getKey())).thenReturn("fn");
      mockLambdaChainThrows(
          lambda, LambdaException.builder().message("Service Unavailable").statusCode(503).build());

      MarkItDown.MarkitdownLambdaRequestEntity req =
          MarkItDown.MarkitdownLambdaRequestEntity.builder().url("https://example.com/err").build();
      MarkItDown.ValidationResult result = MarkItDown.validate(req);

      assertNotNull(result);
      assertEquals(MarkItDown.ValidationStatus.PARSE_FAILED, result.getStatus());
      assertFalse(result.isSuccess());
      assertFalse(result.isTooLargeOrTimeout());
      assertTrue(result.isParseFailed());
      assertEquals("Lambda の呼び出しに失敗しました。", result.getErrorMessage());
    }
  }

  @Test
  void validateInvokeThrowsRateLimitOrMalformedPayloadReturnsParseFailed() {
    try (MockedStatic<Properties> props = mockStatic(Properties.class);
        MockedStatic<LambdaClient> lambda = mockStatic(LambdaClient.class)) {
      props.when(() -> Properties.get(SsmParameterKey.MARKITDOWN_NAME.getKey())).thenReturn("fn");
      mockLambdaChainThrows(
          lambda,
          LambdaException.builder()
              .message("Rate limit exceeded with invalid payload")
              .statusCode(429)
              .build());

      MarkItDown.MarkitdownLambdaRequestEntity req =
          MarkItDown.MarkitdownLambdaRequestEntity.builder()
              .url("https://example.com/ratelimit")
              .build();
      MarkItDown.ValidationResult result = MarkItDown.validate(req);

      assertNotNull(result);
      assertEquals(MarkItDown.ValidationStatus.PARSE_FAILED, result.getStatus());
      assertFalse(result.isTooLargeOrTimeout());
      assertTrue(result.isParseFailed());
      assertEquals("Lambda の呼び出しに失敗しました。", result.getErrorMessage());
    }
  }

  @Test
  void validateUrlExecutesValidationWithUrlRequest() {
    String okJson = "{\"success\":true,\"markdown\":\"# From URL\",\"title\":\"U\",\"error\":null}";
    try (MockedStatic<Properties> props = mockStatic(Properties.class);
        MockedStatic<LambdaClient> lambda = mockStatic(LambdaClient.class)) {
      props.when(() -> Properties.get(SsmParameterKey.MARKITDOWN_NAME.getKey())).thenReturn("fn");
      LambdaClient client = mockLambdaChain(lambda, okJson);

      MarkItDown.ValidationResult result = MarkItDown.validateUrl("https://example.com/sample.pdf");

      assertNotNull(result);
      assertTrue(result.isSuccess());
      assertEquals("# From URL", result.getMarkdown());

      ArgumentCaptor<InvokeRequest> captor = ArgumentCaptor.forClass(InvokeRequest.class);
      verify(client).invoke(captor.capture());
      String payload = captor.getValue().payload().asUtf8String();
      assertTrue(payload.contains("\"url\":\"https://example.com/sample.pdf\""));
    }
  }

  @Test
  void validateFileExecutesValidationWithFileRequest() {
    String okJson =
        "{\"success\":true,\"markdown\":\"# From File\",\"title\":\"F\",\"error\":null}";
    try (MockedStatic<Properties> props = mockStatic(Properties.class);
        MockedStatic<LambdaClient> lambda = mockStatic(LambdaClient.class)) {
      props.when(() -> Properties.get(SsmParameterKey.MARKITDOWN_NAME.getKey())).thenReturn("fn");
      LambdaClient client = mockLambdaChain(lambda, okJson);

      MarkItDown.ValidationResult result = MarkItDown.validateFile("dGVzdA==", "document.docx");

      assertNotNull(result);
      assertTrue(result.isSuccess());
      assertEquals("# From File", result.getMarkdown());

      ArgumentCaptor<InvokeRequest> captor = ArgumentCaptor.forClass(InvokeRequest.class);
      verify(client).invoke(captor.capture());
      String payload = captor.getValue().payload().asUtf8String();
      assertTrue(payload.contains("\"file_base64\":\"dGVzdA==\""));
      assertTrue(payload.contains("\"filename\":\"document.docx\""));
    }
  }

  @Test
  void validationStatusAndResultEntityTest() {
    assertEquals(3, MarkItDown.ValidationStatus.values().length);
    assertEquals(
        MarkItDown.ValidationStatus.SUCCESS, MarkItDown.ValidationStatus.valueOf("SUCCESS"));
    assertEquals(
        MarkItDown.ValidationStatus.TOO_LARGE_OR_TIMEOUT,
        MarkItDown.ValidationStatus.valueOf("TOO_LARGE_OR_TIMEOUT"));
    assertEquals(
        MarkItDown.ValidationStatus.PARSE_FAILED,
        MarkItDown.ValidationStatus.valueOf("PARSE_FAILED"));

    MarkItDown.ValidationResult r1 =
        new MarkItDown.ValidationResult(MarkItDown.ValidationStatus.SUCCESS, "content", null);
    MarkItDown.ValidationResult r2 =
        MarkItDown.ValidationResult.builder()
            .status(MarkItDown.ValidationStatus.SUCCESS)
            .markdown("content")
            .errorMessage(null)
            .build();
    assertEquals(r1, r2);
    assertEquals(r1.hashCode(), r2.hashCode());
    assertEquals("content", r1.getMarkdown());
    assertEquals(MarkItDown.ValidationStatus.SUCCESS, r1.getStatus());
    assertNull(r1.getErrorMessage());
    assertNotNull(r1.toString());

    MarkItDown.ValidationResult empty = new MarkItDown.ValidationResult();
    empty.setStatus(MarkItDown.ValidationStatus.PARSE_FAILED);
    empty.setErrorMessage("err");
    empty.setMarkdown("md");
    assertEquals(MarkItDown.ValidationStatus.PARSE_FAILED, empty.getStatus());
    assertEquals("err", empty.getErrorMessage());
    assertEquals("md", empty.getMarkdown());
  }

  private static LambdaClient mockLambdaChain(
      MockedStatic<LambdaClient> lambda, String payloadUtf8) {
    InvokeResponse response =
        InvokeResponse.builder().payload(SdkBytes.fromUtf8String(payloadUtf8)).build();
    return mockLambdaChain(lambda, response);
  }

  private static LambdaClient mockLambdaChain(
      MockedStatic<LambdaClient> lambda, InvokeResponse response) {
    LambdaClient client = mock(LambdaClient.class);
    LambdaClientBuilder builder = mock(LambdaClientBuilder.class);
    lambda.when(LambdaClient::builder).thenReturn(builder);
    when(builder.region(any(Region.class))).thenReturn(builder);
    when(builder.credentialsProvider(any())).thenReturn(builder);
    when(builder.build()).thenReturn(client);
    when(client.invoke(any(InvokeRequest.class))).thenReturn(response);
    return client;
  }

  private static LambdaClient mockLambdaChainThrows(
      MockedStatic<LambdaClient> lambda, Throwable throwable) {
    LambdaClient client = mock(LambdaClient.class);
    LambdaClientBuilder builder = mock(LambdaClientBuilder.class);
    lambda.when(LambdaClient::builder).thenReturn(builder);
    when(builder.region(any(Region.class))).thenReturn(builder);
    when(builder.credentialsProvider(any())).thenReturn(builder);
    when(builder.build()).thenReturn(client);
    when(client.invoke(any(InvokeRequest.class))).thenThrow(throwable);
    return client;
  }
}
