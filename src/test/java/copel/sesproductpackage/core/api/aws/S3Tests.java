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
        
        String url2 = s3.createDownloadUrl(10);
        assertEquals("https://example.com", url2);
        
        verify(mockPresigner, times(2)).presignGetObject(any(GetObjectPresignRequest.class));
    }

    @Test
    void testAccessorsAndLombok() {
        S3 s3 = new S3("bucket", "key", Region.AP_NORTHEAST_1);
        s3.setBucketName("b2");
        s3.setObjectKey("k2");
        byte[] data = "test".getBytes();
        s3.setData(data);
        java.util.Date now = new java.util.Date();
        s3.setUpdateDate(now);
        
        assertEquals("b2", s3.getBucketName());
        assertEquals("k2", s3.getObjectKey());
        assertArrayEquals(data, s3.getData());
        assertEquals(now, s3.getUpdateDate());
        assertNotNull(s3.getS3Client());
        
        // Lombok equals, hashCode, toString
        S3 s3Other = new S3("b2", "k2", Region.AP_NORTHEAST_1);
        s3Other.setData(data);
        s3Other.setUpdateDate(now);
        
        assertNotNull(s3.toString());
        assertNotNull(s3.hashCode());
        assertTrue(s3.equals(s3));
        assertTrue(s3.canEqual(s3Other));
        assertFalse(s3.equals(null));
        assertFalse(s3.equals(new Object()));
        
        // Test field-by-field equals
        s3Other.setBucketName("diff");
        assertNotEquals(s3, s3Other);
        s3Other.setBucketName("b2");
        assertEquals(s3, s3Other);
        
        s3Other.setObjectKey(null);
        assertNotEquals(s3, s3Other);
        s3Other.setObjectKey("k2");
        
        s3Other.setData(null);
        assertNotEquals(s3, s3Other);
    }

    @Test
    void testDownloadUrlNullConfig() throws Exception {
        S3 s3 = new S3("b", "k", Region.AP_NORTHEAST_1);
        when(mockS3Client.serviceClientConfiguration()).thenReturn(null);
        assertNull(s3.createDownloadUrl());
    }

    @Test
    void testReadError() {
        S3 s3 = new S3("b", "k", Region.AP_NORTHEAST_1);
        when(mockS3Client.getObject(any(GetObjectRequest.class))).thenThrow(new RuntimeException("error"));
        assertThrows(IOException.class, () -> s3.read());
        
        // Covering the case where headObject throws
        reset(mockS3Client);
        byte[] content = "file content".getBytes();
        GetObjectResponse getResponse = GetObjectResponse.builder().build();
        ResponseInputStream<GetObjectResponse> s3Stream = new ResponseInputStream<>(getResponse, new ByteArrayInputStream(content));
        when(mockS3Client.getObject(any(GetObjectRequest.class))).thenReturn(s3Stream);
        when(mockS3Client.headObject(any(HeadObjectRequest.class))).thenThrow(new RuntimeException("head error"));
        assertThrows(IOException.class, () -> s3.read());
    }

    @Test
    void testSaveError() {
        S3 s3 = new S3(null, null, Region.AP_NORTHEAST_1);
        s3.save(); // Should log warn and return
        
        s3 = new S3("b", "k", Region.AP_NORTHEAST_1);
        s3.setBucketName("bucket"); // Non-null
        s3.setObjectKey(null); // Null
        s3.save();
        
        s3.setObjectKey("key");
        s3.setData(null); // Null
        s3.save();

        s3.setData("d".getBytes());
        doThrow(new RuntimeException("error")).when(mockS3Client).putObject(any(PutObjectRequest.class), any(software.amazon.awssdk.core.sync.RequestBody.class));
        s3.save(); // Should catch exception
    }

    @Test
    void testDeleteError() {
        S3 s3 = new S3("b", "k", Region.AP_NORTHEAST_1);
        doThrow(new RuntimeException("error")).when(mockS3Client).deleteObject(any(DeleteObjectRequest.class));
        s3.delete(); // Should catch exception
    }

    @Test
    void testCreateDownloadUrlError() {
        S3 s3 = new S3("b", "k", Region.AP_NORTHEAST_1);
        when(mockS3Client.serviceClientConfiguration()).thenThrow(new RuntimeException("error"));
        assertNull(s3.createDownloadUrl());
        
        // Test catch block inside createDownloadUrl(long)
        reset(mockS3Client);
        S3ServiceClientConfiguration mockConfig = mock(S3ServiceClientConfiguration.class);
        when(mockConfig.region()).thenReturn(Region.AP_NORTHEAST_1);
        when(mockS3Client.serviceClientConfiguration()).thenReturn(mockConfig);
        
        mockedPresigner.when(S3Presigner::builder).thenThrow(new RuntimeException("presigner error"));
        assertNull(s3.createDownloadUrl(10));
    }
}
