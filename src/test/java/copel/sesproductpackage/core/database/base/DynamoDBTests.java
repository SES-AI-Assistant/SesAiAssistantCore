package copel.sesproductpackage.core.database.base;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Iterator;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClientBuilder;

class DynamoDBTests {

    private MockedStatic<DynamoDbClient> mockedClient;
    private MockedStatic<DynamoDbEnhancedClient> mockedEnhancedClient;
    private DynamoDbClient mockDbClient;
    private DynamoDbEnhancedClient mockEnhancedClient;
    private DynamoDbTable<TestDynamoEntity> mockTable;

    // テスト用エンティティ
    @software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean
    public static class TestDynamoEntity extends DynamoDB<TestDynamoEntity> {
        public TestDynamoEntity() { super("test-table", TestDynamoEntity.class); }
        @Override public void save() {}
        @Override public void delete() {}
        @Override public void fetch() {}
        
        @Override
        @software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey
        public String getPartitionKey() { return super.getPartitionKey(); }
    }

    public static class TestDynamoLot extends DynamoDBLot<TestDynamoEntity> {
        public TestDynamoLot() { super("test-table", TestDynamoEntity.class); }
    }

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        mockedClient = mockStatic(DynamoDbClient.class);
        mockedEnhancedClient = mockStatic(DynamoDbEnhancedClient.class);
        
        mockDbClient = mock(DynamoDbClient.class);
        DynamoDbClientBuilder mockBuilder = mock(DynamoDbClientBuilder.class);
        when(mockBuilder.region(any())).thenReturn(mockBuilder);
        when(mockBuilder.credentialsProvider(any())).thenReturn(mockBuilder);
        when(mockBuilder.build()).thenReturn(mockDbClient);
        mockedClient.when(DynamoDbClient::builder).thenReturn(mockBuilder);

        mockEnhancedClient = mock(DynamoDbEnhancedClient.class);
        DynamoDbEnhancedClient.Builder mockEnhancedBuilder = mock(DynamoDbEnhancedClient.Builder.class);
        when(mockEnhancedBuilder.dynamoDbClient(any())).thenReturn(mockEnhancedBuilder);
        when(mockEnhancedBuilder.build()).thenReturn(mockEnhancedClient);
        mockedEnhancedClient.when(DynamoDbEnhancedClient::builder).thenReturn(mockEnhancedBuilder);

        mockTable = mock(DynamoDbTable.class);
        when(mockEnhancedClient.table(anyString(), any(TableSchema.class))).thenReturn(mockTable);
    }

    @AfterEach
    void tearDown() {
        mockedClient.close();
        mockedEnhancedClient.close();
    }

    @Test
    void testDynamoDBBasic() {
        TestDynamoEntity entity = new TestDynamoEntity();
        entity.setPartitionKey("pk");
        entity.setSortKey("sk");
        entity.setTimestamp("2026-01-01");

        assertEquals("pk", entity.getPartitionKey());
        assertEquals("sk", entity.getSortKey());
        assertEquals("2026-01-01", entity.getTimestamp());
        
        assertNotNull(entity.toString());
    }

    @Test
    void testDynamoDBLotBasic() {
        TestDynamoLot lot = new TestDynamoLot();
        TestDynamoEntity e1 = new TestDynamoEntity();
        lot.entityLot.add(e1);
        
        Iterator<TestDynamoEntity> it = lot.iterator();
        assertTrue(it.hasNext());
        assertEquals(e1, it.next());
        
        assertNotNull(lot.toString());
    }
}
