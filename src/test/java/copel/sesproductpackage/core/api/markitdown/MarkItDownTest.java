package copel.sesproductpackage.core.api.markitdown;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.LambdaClientBuilder;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.services.lambda.model.InvokeResponse;
import software.amazon.awssdk.services.lambda.model.LambdaException;
import copel.sesproductpackage.core.util.EnvUtils;

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
  void invokeThrowsWhenFunctionNameMissing() {
    try (MockedStatic<EnvUtils> env = mockStatic(EnvUtils.class)) {
      env.when(() -> EnvUtils.get(MarkItDown.ENV_MARKITDOWN_LAMBDA_FUNCTION_NAME)).thenReturn(null);
      IllegalStateException ex =
          assertThrows(
              IllegalStateException.class,
              () ->
                  MarkItDown.invoke(
                      MarkItDown.MarkitdownLambdaRequestEntity.builder().url("https://x").build()));
      assertTrue(ex.getMessage().contains("MARKITDOWN_LAMBDA_FUNCTION_NAME"));
    }
  }

  @Test
  void invokeThrowsWhenFunctionNameBlank() {
    try (MockedStatic<EnvUtils> env = mockStatic(EnvUtils.class)) {
      env.when(() -> EnvUtils.get(MarkItDown.ENV_MARKITDOWN_LAMBDA_FUNCTION_NAME)).thenReturn("  ");
      assertThrows(
          IllegalStateException.class,
          () ->
              MarkItDown.invoke(
                  MarkItDown.MarkitdownLambdaRequestEntity.builder().url("https://x").build()));
    }
  }

  @Test
  void invokeUsesDefaultRegionWhenAwsRegionBlank() {
    String okJson =
        "{\"success\":true,\"markdown\":\"# Hi\",\"title\":null,\"error\":null}";
    try (MockedStatic<EnvUtils> env = mockStatic(EnvUtils.class);
        MockedStatic<LambdaClient> lambda = mockStatic(LambdaClient.class)) {
      env.when(() -> EnvUtils.get(MarkItDown.ENV_MARKITDOWN_LAMBDA_FUNCTION_NAME)).thenReturn("fn");
      env.when(() -> EnvUtils.get("AWS_REGION")).thenReturn(" ");

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
    String okJson =
        "{\"success\":true,\"markdown\":\"m\",\"title\":\"t\",\"error\":null}";
    try (MockedStatic<EnvUtils> env = mockStatic(EnvUtils.class);
        MockedStatic<LambdaClient> lambda = mockStatic(LambdaClient.class)) {
      env.when(() -> EnvUtils.get(MarkItDown.ENV_MARKITDOWN_LAMBDA_FUNCTION_NAME)).thenReturn("fn");
      env.when(() -> EnvUtils.get("AWS_REGION")).thenReturn("eu-west-1");

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
    String okJson =
        "{\"success\":true,\"markdown\":\"# body\",\"title\":\"T\",\"error\":null}";
    try (MockedStatic<EnvUtils> env = mockStatic(EnvUtils.class);
        MockedStatic<LambdaClient> lambda = mockStatic(LambdaClient.class)) {
      env.when(() -> EnvUtils.get(MarkItDown.ENV_MARKITDOWN_LAMBDA_FUNCTION_NAME)).thenReturn("fn");
      env.when(() -> EnvUtils.get("AWS_REGION")).thenReturn(null);

      mockLambdaChain(lambda, okJson);

      MarkItDown.MarkitdownLambdaRequestEntity req =
          MarkItDown.MarkitdownLambdaRequestEntity.builder()
              .s3(MarkItDown.MarkitdownLambdaRequestEntity.S3ObjectRef.builder()
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
    try (MockedStatic<EnvUtils> env = mockStatic(EnvUtils.class);
        MockedStatic<LambdaClient> lambda = mockStatic(LambdaClient.class)) {
      env.when(() -> EnvUtils.get(MarkItDown.ENV_MARKITDOWN_LAMBDA_FUNCTION_NAME)).thenReturn("fn");
      env.when(() -> EnvUtils.get("AWS_REGION")).thenReturn(null);
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
    try (MockedStatic<EnvUtils> env = mockStatic(EnvUtils.class);
        MockedStatic<LambdaClient> lambda = mockStatic(LambdaClient.class)) {
      env.when(() -> EnvUtils.get(MarkItDown.ENV_MARKITDOWN_LAMBDA_FUNCTION_NAME)).thenReturn("fn");
      env.when(() -> EnvUtils.get("AWS_REGION")).thenReturn(null);

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
    try (MockedStatic<EnvUtils> env = mockStatic(EnvUtils.class);
        MockedStatic<LambdaClient> lambda = mockStatic(LambdaClient.class)) {
      env.when(() -> EnvUtils.get(MarkItDown.ENV_MARKITDOWN_LAMBDA_FUNCTION_NAME)).thenReturn("fn");
      env.when(() -> EnvUtils.get("AWS_REGION")).thenReturn(null);

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
    try (MockedStatic<EnvUtils> env = mockStatic(EnvUtils.class);
        MockedStatic<LambdaClient> lambda = mockStatic(LambdaClient.class)) {
      env.when(() -> EnvUtils.get(MarkItDown.ENV_MARKITDOWN_LAMBDA_FUNCTION_NAME)).thenReturn("fn");
      env.when(() -> EnvUtils.get("AWS_REGION")).thenReturn(null);

      InvokeResponse response =
          InvokeResponse.builder().functionError(null).payload(null).build();
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
    try (MockedStatic<EnvUtils> env = mockStatic(EnvUtils.class);
        MockedStatic<LambdaClient> lambda = mockStatic(LambdaClient.class)) {
      env.when(() -> EnvUtils.get(MarkItDown.ENV_MARKITDOWN_LAMBDA_FUNCTION_NAME)).thenReturn("fn");
      env.when(() -> EnvUtils.get("AWS_REGION")).thenReturn(null);

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
    try (MockedStatic<EnvUtils> env = mockStatic(EnvUtils.class);
        MockedStatic<LambdaClient> lambda = mockStatic(LambdaClient.class)) {
      env.when(() -> EnvUtils.get(MarkItDown.ENV_MARKITDOWN_LAMBDA_FUNCTION_NAME)).thenReturn("fn");
      env.when(() -> EnvUtils.get("AWS_REGION")).thenReturn(null);

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

  private static LambdaClient mockLambdaChain(MockedStatic<LambdaClient> lambda, String payloadUtf8) {
    InvokeResponse response =
        InvokeResponse.builder().payload(SdkBytes.fromUtf8String(payloadUtf8)).build();
    return mockLambdaChain(lambda, response);
  }

  private static LambdaClient mockLambdaChain(MockedStatic<LambdaClient> lambda, InvokeResponse response) {
    LambdaClient client = mock(LambdaClient.class);
    LambdaClientBuilder builder = mock(LambdaClientBuilder.class);
    lambda.when(LambdaClient::builder).thenReturn(builder);
    when(builder.region(any(Region.class))).thenReturn(builder);
    when(builder.credentialsProvider(any())).thenReturn(builder);
    when(builder.build()).thenReturn(client);
    when(client.invoke(any(InvokeRequest.class))).thenReturn(response);
    return client;
  }
}
