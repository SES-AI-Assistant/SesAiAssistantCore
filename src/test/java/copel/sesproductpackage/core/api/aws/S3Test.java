package copel.sesproductpackage.core.api.aws;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import copel.sesproductpackage.core.util.ApplicationClock;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Date;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
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

class S3Test {

  private S3Client mockS3Client;
  private S3 s3;

  @BeforeEach
  void setUp() {
    mockS3Client = mock(S3Client.class, RETURNS_DEEP_STUBS);
    try (MockedStatic<S3Client> mockedS3 = mockStatic(S3Client.class)) {
      software.amazon.awssdk.services.s3.S3ClientBuilder builder =
          mock(software.amazon.awssdk.services.s3.S3ClientBuilder.class);
      when(S3Client.builder()).thenReturn(builder);
      when(builder.credentialsProvider(any())).thenReturn(builder);
      when(builder.region(any())).thenReturn(builder);
      when(builder.build()).thenReturn(mockS3Client);

      s3 = new S3("bucket", "key", Region.AP_NORTHEAST_1);
    }
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
    s3.save(); // Should log warn and return
    s3.setBucketName("b");
    s3.setObjectKey(null);
    s3.save();
    s3.setObjectKey("k");
    s3.setData(null);
    s3.save();
    verify(mockS3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
  }

  @Test
  void testSaveException() {
    s3.setData(new byte[1]);
    when(mockS3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
        .thenThrow(new RuntimeException("error"));
    assertDoesNotThrow(() -> s3.save());
  }

  @Test
  void testRead() throws Exception {
    byte[] data = "hello".getBytes();
    ResponseInputStream<GetObjectResponse> ris =
        new ResponseInputStream<>(
            GetObjectResponse.builder().build(), new ByteArrayInputStream(data));
    when(mockS3Client.getObject(any(GetObjectRequest.class))).thenReturn(ris);

    HeadObjectResponse hor = HeadObjectResponse.builder().lastModified(Instant.now()).build();
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
    assertDoesNotThrow(() -> s3.delete());
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

  /** {@link S3#save()} のテスト。 */
  @Nested
  class save {

    /** 正常系のテスト。 */
    @Nested
    class 正常系 {

      /** テストの後に時計を既定のシステム時計に戻す。後続のテストに固定時計が残らないようにする。 */
      @AfterEach
      void tearDown() {
        ApplicationClock.INSTANCE.resetToDefault();
      }

      /**
       * save が保存時点の更新日時を時計から取得することを確かめる。 S3 に保存した時点の更新日時が本番の現在時刻の取得元に従うことを保証する。
       *
       * @given 時計が UTC の "2026-01-15T03:04:05Z" の固定時計に設定され、データが "test data" のバイト列である
       * @when save を引数なしで呼び出す
       * @then getUpdateDate が "2026-01-15T03:04:05Z" に対応する Date を返す
       */
      @Test
      void 時計が固定時計に設定されている場合_固定時刻の更新日時を返す() {
        ApplicationClock.INSTANCE.setClock(
            Clock.fixed(Instant.parse("2026-01-15T03:04:05Z"), ZoneId.of("Asia/Tokyo")));
        s3.setData("test data".getBytes());

        s3.save();

        assertEquals(Date.from(Instant.parse("2026-01-15T03:04:05Z")), s3.getUpdateDate());
      }
    }
  }
}
