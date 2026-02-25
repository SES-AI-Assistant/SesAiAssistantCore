package copel.sesproductpackage.core.api.aws;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;

import java.net.URL;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3ServiceClientConfiguration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

class S3PresignTests {
  private MockedStatic<S3Client> mockedS3Client;
  private MockedStatic<S3Presigner> mockedPresigner;

  @BeforeEach
  void setUp() {
    mockedS3Client = mockStatic(S3Client.class);
    mockedPresigner = mockStatic(S3Presigner.class);
  }

  @AfterEach
  void tearDown() {
    mockedS3Client.close();
    mockedPresigner.close();
  }

  @Test
  void testCreateDownloadUrl() throws Exception {
    S3Client mockDbClient = mock(S3Client.class);
    S3ClientBuilder mockBuilder = mock(S3ClientBuilder.class);
    when(mockBuilder.region(any())).thenReturn(mockBuilder);
    when(mockBuilder.credentialsProvider(any())).thenReturn(mockBuilder);
    when(mockBuilder.build()).thenReturn(mockDbClient);
    mockedS3Client.when(S3Client::builder).thenReturn(mockBuilder);

    S3ServiceClientConfiguration mockConfig = mock(S3ServiceClientConfiguration.class);
    when(mockConfig.region()).thenReturn(Region.AP_NORTHEAST_1);
    when(mockDbClient.serviceClientConfiguration()).thenReturn(mockConfig);

    S3 s3 = new S3("b", "k", Region.AP_NORTHEAST_1);

    S3Presigner mockPresignerObj = mock(S3Presigner.class);
    S3Presigner.Builder mockPresignBuilder = mock(S3Presigner.Builder.class);
    when(mockPresignBuilder.region(any())).thenReturn(mockPresignBuilder);
    when(mockPresignBuilder.credentialsProvider(any())).thenReturn(mockPresignBuilder);
    when(mockPresignBuilder.build()).thenReturn(mockPresignerObj);
    mockedPresigner.when(S3Presigner::builder).thenReturn(mockPresignBuilder);

    PresignedGetObjectRequest mockPresignRes = mock(PresignedGetObjectRequest.class);
    when(mockPresignRes.url()).thenReturn(new URL("https://example.com"));
    when(mockPresignerObj.presignGetObject(any(GetObjectPresignRequest.class)))
        .thenReturn(mockPresignRes);

    assertEquals("https://example.com", s3.createDownloadUrl());
  }

  @Test
  void testCreateDownloadUrl_Exception() throws Exception {
    // S3Clientのモックセットアップ（コンストラクタとconfig参照用）
    S3Client mockDbClient = mock(S3Client.class);
    S3ClientBuilder mockBuilder = mock(S3ClientBuilder.class);
    when(mockBuilder.region(any())).thenReturn(mockBuilder);
    when(mockBuilder.credentialsProvider(any())).thenReturn(mockBuilder);
    when(mockBuilder.build()).thenReturn(mockDbClient);
    mockedS3Client.when(S3Client::builder).thenReturn(mockBuilder);

    S3ServiceClientConfiguration mockConfig = mock(S3ServiceClientConfiguration.class);
    when(mockConfig.region()).thenReturn(Region.AP_NORTHEAST_1);
    when(mockDbClient.serviceClientConfiguration()).thenReturn(mockConfig);

    // インスタンス作成
    S3 s3 = new S3("b", "k", Region.AP_NORTHEAST_1);

    // S3Presignerのモックセットアップ
    S3Presigner mockPresignerObj = mock(S3Presigner.class);
    S3Presigner.Builder mockPresignBuilder = mock(S3Presigner.Builder.class);
    when(mockPresignBuilder.region(any())).thenReturn(mockPresignBuilder);
    when(mockPresignBuilder.credentialsProvider(any())).thenReturn(mockPresignBuilder);
    when(mockPresignBuilder.build()).thenReturn(mockPresignerObj);
    mockedPresigner.when(S3Presigner::builder).thenReturn(mockPresignBuilder);

    // 【重要】署名実行時に例外を発生させる
    when(mockPresignerObj.presignGetObject(any(GetObjectPresignRequest.class)))
        .thenThrow(new RuntimeException("Simulated Presign Error"));

    // 実行：例外が発生して catch ブロックに入り、null が返ることを確認
    String url = s3.createDownloadUrl();

    assertNull(url);
  }
}
