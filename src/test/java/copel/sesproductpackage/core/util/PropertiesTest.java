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
import java.util.Map;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

class PropertiesTest {

  @Test
  void testBasicGetterSetter() {
    assertNotNull(new Properties());
  }

  @Test
  @SuppressWarnings("unchecked")
  void testLoadAndGetters() throws Exception {
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
}
