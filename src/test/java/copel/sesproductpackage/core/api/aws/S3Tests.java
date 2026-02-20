package copel.sesproductpackage.core.api.aws;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Instant;
import java.net.URL;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3ServiceClientConfiguration;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

class S3Tests {

    private MockedStatic<S3Client> mockedS3Client;
    private MockedStatic<S3Presigner> mockedPresigner;
    private S3Client mockS3Client;

    @BeforeEach
    void setUp() {
        mockedS3Client = mockStatic(S3Client.class);
        mockedPresigner = mockStatic(S3Presigner.class);
        mockS3Client = mock(S3Client.class);
        
        S3ClientBuilder mockBuilder = mock(S3ClientBuilder.class);
        when(mockBuilder.region(any())).thenReturn(mockBuilder);
        when(mockBuilder.credentialsProvider(any())).thenReturn(mockBuilder);
        when(mockBuilder.build()).thenReturn(mockS3Client);
        mockedS3Client.when(S3Client::builder).thenReturn(mockBuilder);
        
        S3ServiceClientConfiguration mockConfig = mock(S3ServiceClientConfiguration.class);
        when(mockConfig.region()).thenReturn(Region.AP_NORTHEAST_1);
        when(mockS3Client.serviceClientConfiguration()).thenReturn(mockConfig);
    }

    @AfterEach
    void tearDown() {
        mockedS3Client.close();
        mockedPresigner.close();
    }

    @Test
    void testSave() {
        S3 s3 = new S3("bucket", "key", Region.AP_NORTHEAST_1);
        s3.setData("hello".getBytes());
        s3.save();
        verify(mockS3Client).putObject(any(PutObjectRequest.class), any(software.amazon.awssdk.core.sync.RequestBody.class));
        assertNotNull(s3.getUpdateDate());
    }

    @Test
    void testRead() throws Exception {
        S3 s3 = new S3("bucket", "key", Region.AP_NORTHEAST_1);
        
        byte[] content = "file content".getBytes();
        GetObjectResponse getResponse = GetObjectResponse.builder().build();
        ResponseInputStream<GetObjectResponse> s3Stream = new ResponseInputStream<>(getResponse, new ByteArrayInputStream(content));
        when(mockS3Client.getObject(any(GetObjectRequest.class))).thenReturn(s3Stream);
        
        HeadObjectResponse headResponse = HeadObjectResponse.builder()
                .lastModified(Instant.now())
                .build();
        when(mockS3Client.headObject(any(HeadObjectRequest.class))).thenReturn(headResponse);
        
        s3.read();
        assertArrayEquals(content, s3.getData());
        assertNotNull(s3.getUpdateDate());
    }

    @Test
    void testDelete() {
        S3 s3 = new S3("bucket", "key", Region.AP_NORTHEAST_1);
        s3.delete();
        verify(mockS3Client).deleteObject(any(DeleteObjectRequest.class));
        assertNull(s3.getUpdateDate());
    }

    @Test
    void testCreateDownloadUrl() throws Exception {
        S3 s3 = new S3("bucket", "key", Region.AP_NORTHEAST_1);
        
        S3Presigner mockPresigner = mock(S3Presigner.class);
        S3Presigner.Builder mockPresignBuilder = mock(S3Presigner.Builder.class);
        when(mockPresignBuilder.region(any())).thenReturn(mockPresignBuilder);
        when(mockPresignBuilder.credentialsProvider(any())).thenReturn(mockPresignBuilder);
        when(mockPresignBuilder.build()).thenReturn(mockPresigner);
        mockedPresigner.when(S3Presigner::builder).thenReturn(mockPresignBuilder);
        
        PresignedGetObjectRequest mockPresignRes = mock(PresignedGetObjectRequest.class);
        when(mockPresignRes.url()).thenReturn(new URL("https://example.com"));
        when(mockPresigner.presignGetObject(any(GetObjectPresignRequest.class))).thenReturn(mockPresignRes);
        
        String url = s3.createDownloadUrl();
        assertEquals("https://example.com", url);
    }
}
