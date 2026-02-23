package copel.sesproductpackage.core.api.aws;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

class S3BasicTests {

    private S3Client mockS3Client;
    private S3 s3;

    @BeforeEach
    void setUp() throws Exception {
        mockS3Client = mock(S3Client.class);
        
        s3 = new S3("test-bucket", "test-key", Region.AP_NORTHEAST_1);
        Field clientField = S3.class.getDeclaredField("s3Client");
        clientField.setAccessible(true);
        clientField.set(s3, mockS3Client);
    }

    @Test
    void testSave() {
        s3.setData("test data".getBytes());
        s3.save();
        verify(mockS3Client, times(1)).putObject(any(PutObjectRequest.class), any(software.amazon.awssdk.core.sync.RequestBody.class));

        s3.setBucketName(null);
        s3.save(); 
        verify(mockS3Client, times(1)).putObject(any(PutObjectRequest.class), any(software.amazon.awssdk.core.sync.RequestBody.class));
    }
    
    @Test
    void testSaveException() {
        when(mockS3Client.putObject(any(PutObjectRequest.class), any(software.amazon.awssdk.core.sync.RequestBody.class)))
            .thenThrow(SdkException.builder().message("err").build());
        
        s3.setData("data".getBytes());
        assertDoesNotThrow(() -> s3.save());
    }

    @Test
    void testRead() throws IOException {
        byte[] data = "test data".getBytes();
        ResponseInputStream<GetObjectResponse> response = new ResponseInputStream<>(
            GetObjectResponse.builder().build(), new ByteArrayInputStream(data));

        when(mockS3Client.getObject(any(GetObjectRequest.class))).thenReturn(response);
        when(mockS3Client.headObject(any(HeadObjectRequest.class))).thenReturn(HeadObjectResponse.builder().lastModified(Instant.now()).build());
        
        s3.read();
        assertArrayEquals(data, s3.getData());
    }

    @Test
    void testReadThrowsException() {
        when(mockS3Client.getObject(any(GetObjectRequest.class)))
            .thenThrow(SdkException.builder().message("err").build());
        
        assertThrows(IOException.class, () -> s3.read());
    }
    
    @Test
    void testDelete() {
        s3.delete();
        verify(mockS3Client, times(1)).deleteObject(any(DeleteObjectRequest.class));
    }
    
    @Test
    void testDeleteException() {
        when(mockS3Client.deleteObject(any(DeleteObjectRequest.class)))
            .thenThrow(SdkException.builder().message("err").build());
            
        assertDoesNotThrow(() -> s3.delete());
    }
}
