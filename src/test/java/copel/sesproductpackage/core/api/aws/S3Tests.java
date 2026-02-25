package copel.sesproductpackage.core.api.aws;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

class S3Tests {

  private S3Client mockS3Client;
  private S3 s3;

  @BeforeEach
  void setUp() {
    mockS3Client = mock(S3Client.class, RETURNS_DEEP_STUBS);
    // We need to inject the mockS3Client into the s3 object.
    // Since the constructor creates a new one, we use reflection or a hack.
    s3 = new S3("bucket", "key", Region.AP_NORTHEAST_1);
    s3.setS3Client(mockS3Client);
  }

  @Test
  void testSave() {
    s3.setData("test data".getBytes());
    s3.save();
    verify(mockS3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    assertNotNull(s3.getUpdateDate());
  }

  @Test
  void testSaveNulls() {
    s3.setBucketName(null);
    s3.save(); // Should log warnd and return
    verify(mockS3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
  }

  @Test
  void testSaveException() {
    s3.setData(new byte[1]);
    when(mockS3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
        .thenThrow(new RuntimeException("error"));
    s3.save(); // Should catch exception
  }

  @Test
  void testRead() throws Exception {
    byte[] data = "hello".getBytes();
    ResponseInputStream<GetObjectResponse> ris =
        new ResponseInputStream<>(
            GetObjectResponse.builder().build(), new ByteArrayInputStream(data));
    when(mockS3Client.getObject(any(GetObjectRequest.class))).thenReturn(ris);

    HeadObjectResponse hor =
        HeadObjectResponse.builder().lastModified(java.time.Instant.now()).build();
    when(mockS3Client.headObject(any(HeadObjectRequest.class))).thenReturn(hor);

    s3.read();
    assertArrayEquals(data, s3.getData());
    assertNotNull(s3.getUpdateDate());
  }

  @Test
  void testReadException() {
    when(mockS3Client.getObject(any(GetObjectRequest.class)))
        .thenThrow(new RuntimeException("error"));
    assertThrows(IOException.class, () -> s3.read());
  }

  @Test
  void testDelete() {
    s3.delete();
    verify(mockS3Client).deleteObject(any(DeleteObjectRequest.class));
    assertNull(s3.getUpdateDate());
  }

  @Test
  void testDeleteException() {
    when(mockS3Client.deleteObject(any(DeleteObjectRequest.class)))
        .thenThrow(new RuntimeException("error"));
    s3.delete(); // Should catch
  }

  @Test
  void testCreateDownloadUrl() throws Exception {
    try (MockedStatic<S3Presigner> mockedPresigner = mockStatic(S3Presigner.class)) {
      S3Presigner mockPresigner = mock(S3Presigner.class);
      S3Presigner.Builder mockBuilder = mock(S3Presigner.Builder.class);
      when(mockBuilder.region(any())).thenReturn(mockBuilder);
      when(mockBuilder.credentialsProvider(any())).thenReturn(mockBuilder);
      when(mockBuilder.build()).thenReturn(mockPresigner);
      mockedPresigner.when(S3Presigner::builder).thenReturn(mockBuilder);

      PresignedGetObjectRequest mockPresigned = mock(PresignedGetObjectRequest.class);
      when(mockPresigned.url()).thenReturn(new URL("http://example.com"));
      when(mockPresigner.presignGetObject(any(GetObjectPresignRequest.class)))
          .thenReturn(mockPresigned);

      String url = s3.createDownloadUrl();
      assertEquals("http://example.com", url);

      // expireMinutes variant
      url = s3.createDownloadUrl(5);
      assertEquals("http://example.com", url);
    }
  }

  @Test
  void testCreateDownloadUrlException() {
    try (MockedStatic<S3Presigner> mockedPresigner = mockStatic(S3Presigner.class)) {
      mockedPresigner.when(S3Presigner::builder).thenThrow(new RuntimeException("error"));
      assertNull(s3.createDownloadUrl());
    }
  }
}
