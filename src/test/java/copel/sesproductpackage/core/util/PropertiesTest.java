package copel.sesproductpackage.core.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParametersByPathRequest;
import software.amazon.awssdk.services.ssm.model.GetParametersByPathResponse;
import software.amazon.awssdk.services.ssm.model.Parameter;

class PropertiesTest {

  @Test
  void testBasicGetterSetter() {
    assertNotNull(new Properties());
  }

  @Test
  @SuppressWarnings("unchecked")
  void testLoadAndGetters() throws Exception {
    Field configBucketField = Properties.class.getDeclaredField("CONFIG_BUCKET");
    configBucketField.setAccessible(true);
    configBucketField.set(null, "test-bucket");

    S3Client mockS3 = mock(S3Client.class);
    String content = "KEY1=VAL1\nINT=10\nDBL=10.5\nARRAY=A,B,C\n";
    InputStream is = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
    ResponseInputStream<GetObjectResponse> s3Stream =
        new ResponseInputStream<>(GetObjectResponse.builder().build(), is);
    when(mockS3.getObject(any(GetObjectRequest.class))).thenReturn(s3Stream);

    Properties.load(mockS3);

    assertEquals("VAL1", Properties.get("KEY1"));
    assertEquals(10, Properties.getInt("INT"));
    assertEquals(10.5, Properties.getDouble("DBL"));
    assertArrayEquals(new String[] {"A", "B", "C"}, Properties.getAsArray("ARRAY"));
  }

  @Test
  void testLoadIOException() throws Exception {
    S3Client mockS3 = mock(S3Client.class);
    InputStream mockIs = mock(InputStream.class);
    when(mockIs.read()).thenThrow(new IOException("Forced IO exception"));
    when(mockIs.read(any(byte[].class), anyInt(), anyInt()))
        .thenThrow(new IOException("Forced IO exception"));

    ResponseInputStream<GetObjectResponse> s3Stream =
        new ResponseInputStream<>(GetObjectResponse.builder().build(), mockIs);

    when(mockS3.getObject(any(GetObjectRequest.class))).thenReturn(s3Stream);

    assertDoesNotThrow(() -> Properties.load(mockS3));
  }

  @Test
  @SuppressWarnings("unchecked")
  void testLoadNullCheck() throws Exception {
    Field configBucketField = Properties.class.getDeclaredField("CONFIG_BUCKET");
    configBucketField.setAccessible(true);
    configBucketField.set(null, "test-bucket");

    Field field = Properties.class.getDeclaredField("properties");
    field.setAccessible(true);
    Map<String, String> propertiesMap = (Map<String, String>) field.get(null);
    propertiesMap.clear();

    S3Client mockS3 = mock(S3Client.class);
    String content = "valid=key\n \ninvalid-line\n=no-key\nkey=\n";
    InputStream is = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
    ResponseInputStream<GetObjectResponse> s3Stream =
        new ResponseInputStream<>(GetObjectResponse.builder().build(), is);
    when(mockS3.getObject(any(GetObjectRequest.class))).thenReturn(s3Stream);

    Properties.load(mockS3);
    assertEquals("key", Properties.get("valid"));
    // Properties.get returns "" for missing keys
    assertEquals("", Properties.get("invalid-line"));
    assertEquals("key", Properties.get("valid"));
  }

  @Test
  @SuppressWarnings("unchecked")
  void testGettersWhenKeyMissing() throws Exception {
    Field configBucketField = Properties.class.getDeclaredField("CONFIG_BUCKET");
    configBucketField.setAccessible(true);
    configBucketField.set(null, "test-bucket");

    Field field = Properties.class.getDeclaredField("properties");
    field.setAccessible(true);
    Map<String, String> propertiesMap = (Map<String, String>) field.get(null);
    propertiesMap.clear();
    S3Client mockS3 = mock(S3Client.class);
    InputStream is = new ByteArrayInputStream("A=1".getBytes(StandardCharsets.UTF_8));
    ResponseInputStream<GetObjectResponse> s3Stream =
        new ResponseInputStream<>(GetObjectResponse.builder().build(), is);
    when(mockS3.getObject(any(GetObjectRequest.class))).thenReturn(s3Stream);
    Properties.load(mockS3);

    assertEquals("", Properties.get("MISSING_KEY"));
    assertEquals(0, Properties.getInt("MISSING_KEY"));
    assertEquals(0.0, Properties.getDouble("MISSING_KEY"));
    assertArrayEquals(new String[0], Properties.getAsArray("MISSING_KEY"));
  }

  @Test
  @SuppressWarnings("unchecked")
  void testLoadFromParameterStore() throws Exception {
    Field field = Properties.class.getDeclaredField("properties");
    field.setAccessible(true);
    Map<String, String> propertiesMap = (Map<String, String>) field.get(null);
    propertiesMap.clear();
    propertiesMap.put("infrastructure/s3/bucket/name", "S3_OLD_VALUE");

    SsmClient mockSsm = mock(SsmClient.class);
    List<Parameter> params = new ArrayList<>();
    params.add(Parameter.builder().name("/nectar/dev/infrastructure/s3/bucket/name")
        .value("test-bucket").build());
    params.add(Parameter.builder().name("/nectar/dev/infrastructure/rds/endpoint")
        .value("test-endpoint").build());

    GetParametersByPathResponse response =
        GetParametersByPathResponse.builder().parameters(params).nextToken(null).build();
    when(mockSsm.getParametersByPath(any(GetParametersByPathRequest.class)))
        .thenReturn(response);

    Properties.loadFromParameterStore(mockSsm);

    assertEquals("test-bucket",
        Properties.get("infrastructure/s3/bucket/name"));
    assertEquals("test-endpoint",
        Properties.get("infrastructure/rds/endpoint"));
  }

  @Test
  void testLoadFromParameterStoreException() throws Exception {
    SsmClient mockSsm = mock(SsmClient.class);
    when(mockSsm.getParametersByPath(any(GetParametersByPathRequest.class)))
        .thenThrow(new RuntimeException("SSM access failed"));

    assertDoesNotThrow(() -> Properties.loadFromParameterStore(mockSsm));
  }

  @Test
  @SuppressWarnings("unchecked")
  void testLoadFromParameterStoreWithPagination() throws Exception {
    Field field = Properties.class.getDeclaredField("properties");
    field.setAccessible(true);
    Map<String, String> propertiesMap = (Map<String, String>) field.get(null);
    propertiesMap.clear();

    SsmClient mockSsm = mock(SsmClient.class);

    List<Parameter> paramsPage1 = new ArrayList<>();
    paramsPage1.add(Parameter.builder().name("/nectar/dev/param1").value("value1").build());

    List<Parameter> paramsPage2 = new ArrayList<>();
    paramsPage2.add(Parameter.builder().name("/nectar/dev/param2").value("value2").build());

    GetParametersByPathResponse responsePage1 =
        GetParametersByPathResponse.builder().parameters(paramsPage1).nextToken("next").build();
    GetParametersByPathResponse responsePage2 =
        GetParametersByPathResponse.builder().parameters(paramsPage2).nextToken(null).build();

    when(mockSsm.getParametersByPath(any(GetParametersByPathRequest.class)))
        .thenReturn(responsePage1)
        .thenReturn(responsePage2);

    Properties.loadFromParameterStore(mockSsm);

    assertEquals("value1", Properties.get("param1"));
    assertEquals("value2", Properties.get("param2"));
  }
}
