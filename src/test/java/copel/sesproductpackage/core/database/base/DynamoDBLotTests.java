package copel.sesproductpackage.core.database.base;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static com.github.stefanbirkner.systemlambda.SystemLambda.*;

import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.PageIterable;
import software.amazon.awssdk.enhanced.dynamodb.model.ScanEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClientBuilder;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.core.pagination.sync.SdkIterable;

class DynamoDBLotTests {

    @DynamoDbBean
    public static class TestDynamoEntity extends DynamoDB<TestDynamoEntity> {
        public TestDynamoEntity() {
            super("TestTable", TestDynamoEntity.class);
        }
        @Override public void save() {}
        @Override public void delete() {}
        @Override public void fetch() {}
        
        // Must provide a partition key method for TableSchema.fromBean
        @DynamoDbPartitionKey
        public String getId() { return "id"; }
        public void setId(String id) {}
    }
    
    // We need a subclass of DynamoDBLot to instantiate for testing
    static class TestDynamoDBLot extends DynamoDBLot<TestDynamoEntity> {
        public TestDynamoDBLot() {
            super("TestTable", TestDynamoEntity.class);
        }
    }

    @Test
    void testConstructorAndFetch() throws Exception {
        try (MockedStatic<DynamoDbClient> mockedClient = mockStatic(DynamoDbClient.class);
             MockedStatic<DynamoDbEnhancedClient> mockedEnhanced = mockStatic(DynamoDbEnhancedClient.class)) {

            // Mock DynamoDbClient builder
            DynamoDbClient mockDbClient = mock(DynamoDbClient.class);
            DynamoDbClientBuilder mockDbBuilder = mock(DynamoDbClientBuilder.class);
            when(DynamoDbClient.builder()).thenReturn(mockDbBuilder);
            when(mockDbBuilder.region(any(Region.class))).thenReturn(mockDbBuilder);
            when(mockDbBuilder.credentialsProvider(any(software.amazon.awssdk.auth.credentials.AwsCredentialsProvider.class))).thenReturn(mockDbBuilder);
            when(mockDbBuilder.build()).thenReturn(mockDbClient);

            // Mock DynamoDbEnhancedClient builder
            DynamoDbEnhancedClient mockEnhancedClient = mock(DynamoDbEnhancedClient.class);
            DynamoDbEnhancedClient.Builder mockEnhancedBuilder = mock(DynamoDbEnhancedClient.Builder.class);
            when(DynamoDbEnhancedClient.builder()).thenReturn(mockEnhancedBuilder);
            when(mockEnhancedBuilder.dynamoDbClient(any(DynamoDbClient.class))).thenReturn(mockEnhancedBuilder);
            when(mockEnhancedBuilder.build()).thenReturn(mockEnhancedClient);

            // Mock Table
            DynamoDbTable<TestDynamoEntity> mockTable = mock(DynamoDbTable.class);
            when(mockEnhancedClient.table(anyString(), any(TableSchema.class))).thenReturn(mockTable);

            // Test Constructor with different env vars
            withEnvironmentVariable("AWS_LAMBDA_FUNCTION_NAME", "true").execute(() -> {
                new TestDynamoDBLot();
                verify(mockDbBuilder, atLeastOnce()).region(Region.AP_NORTHEAST_1);
            });
            
            withEnvironmentVariable("CI", "true").execute(() -> {
                new TestDynamoDBLot();
                // verify credentials provider logic if needed
            });

            // Normal case (no env vars)
            new TestDynamoDBLot();

            // Test fetchByPk
            TestDynamoDBLot lot = new TestDynamoDBLot();
            
            // Mock query response
            PageIterable<TestDynamoEntity> mockPageIterable = mock(PageIterable.class);
            SdkIterable<TestDynamoEntity> mockSdkIterable = mock(SdkIterable.class);
            when(mockTable.query(any(software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional.class))).thenReturn(mockPageIterable);
            when(mockPageIterable.items()).thenReturn(mockSdkIterable);
            when(mockSdkIterable.iterator()).thenReturn(Collections.emptyIterator());
            
            lot.fetchByPk("pk");
            verify(mockTable).query(any(software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional.class));

            // Test fetchByColumn
            when(mockTable.scan(any(ScanEnhancedRequest.class))).thenReturn(mockPageIterable);
            lot.fetchByColumn("col", "val");
            verify(mockTable).scan(any(ScanEnhancedRequest.class));
            
            // Test iterator and toString
            assertNotNull(lot.iterator());
            assertNotNull(lot.toString());
        }
    }
}
