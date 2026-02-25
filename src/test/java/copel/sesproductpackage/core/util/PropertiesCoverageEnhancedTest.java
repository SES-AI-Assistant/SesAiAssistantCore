package copel.sesproductpackage.core.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

class PropertiesCoverageEnhancedTest {

  @Test
  void testLoadLoopEdgeCases() throws Exception {
    S3Client mockS3 = mock(S3Client.class);

    // Content with:
    // 1. Valid property
    // 2. Empty line
    // 3. Line without =
    // 4. Line with extra = (split limit test)
    // 5. Line with only =
    String content = "key1=value1\n\nno-equals\nkey2=value2=extra\n=";
    InputStream is = new ByteArrayInputStream(content.getBytes());
    software.amazon.awssdk.services.s3.model.GetObjectResponse response =
        software.amazon.awssdk.services.s3.model.GetObjectResponse.builder().build();
    software.amazon.awssdk.core.ResponseInputStream<
            software.amazon.awssdk.services.s3.model.GetObjectResponse>
        s3Stream = new software.amazon.awssdk.core.ResponseInputStream<>(response, is);

    when(mockS3.getObject(any(GetObjectRequest.class))).thenReturn(s3Stream);

    Properties.load(mockS3);

    assertEquals("value1", Properties.get("key1"));
    assertEquals("value2=extra", Properties.get("key2"));
  }

  @Test
  void testLoadIOException() throws Exception {
    S3Client mockS3 = mock(S3Client.class);
    InputStream mockIs = mock(InputStream.class);
    when(mockIs.read()).thenThrow(new IOException("Forced IO exception"));
    when(mockIs.read(any(byte[].class), anyInt(), anyInt()))
        .thenThrow(new IOException("Forced IO exception"));

    software.amazon.awssdk.services.s3.model.GetObjectResponse response =
        software.amazon.awssdk.services.s3.model.GetObjectResponse.builder().build();
    software.amazon.awssdk.core.ResponseInputStream<
            software.amazon.awssdk.services.s3.model.GetObjectResponse>
        s3Stream = new software.amazon.awssdk.core.ResponseInputStream<>(response, mockIs);

    when(mockS3.getObject(any(GetObjectRequest.class))).thenReturn(s3Stream);

    // Should catch the exception and log error, not throw
    assertDoesNotThrow(() -> Properties.load(mockS3));
  }
}
