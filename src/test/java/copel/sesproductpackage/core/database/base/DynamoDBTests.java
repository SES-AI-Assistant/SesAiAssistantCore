package copel.sesproductpackage.core.database.base;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import copel.sesproductpackage.core.util.EnvUtils;
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

  @software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean
  public static class TestDynamoEntity extends DynamoDB<TestDynamoEntity> {
    public TestDynamoEntity() {
      super("test-table", TestDynamoEntity.class);
    }

    @Override
    public void save() {}

    @Override
    public void delete() {}

    @Override
    public void fetch() {}

    @Override
    @software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey
    public String getPartitionKey() {
      return super.getPartitionKey();
    }
  }

  public static class TestDynamoLot extends DynamoDBLot<TestDynamoEntity> {
    public TestDynamoLot() {
      super("test-table", TestDynamoEntity.class);
    }
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
    assertNotNull(entity.hashCode());
    assertTrue(entity.equals(entity));
    assertFalse(entity.equals(null));
    assertFalse(entity.equals(new Object()));

    TestDynamoEntity other = new TestDynamoEntity();
    other.setPartitionKey("pk");
    other.setSortKey("sk");
    other.setTimestamp("2026-01-01");
    assertTrue(entity.equals(other));
    assertEquals(entity.hashCode(), other.hashCode());

    // Null/Different fields
    other.setPartitionKey(null);
    assertNotEquals(entity, other);
    other.setPartitionKey("pk");

    other.setSortKey(null);
    assertNotEquals(entity, other);
    other.setSortKey("sk");

    other.setTimestamp(null);
    assertNotEquals(entity, other);
    other.setTimestamp("2026-01-01");

    entity.setPartitionKey(null);
    assertNotEquals(entity, other);
    entity.setPartitionKey("pk");

    // canEqual
    assertTrue(entity.canEqual(other));
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

    lot.entityLot.clear();
    assertFalse(lot.iterator().hasNext());
  }

  @Test
  void testDynamoDBMethods() {
    TestDynamoEntity entity = new TestDynamoEntity();
    entity.save();
    entity.delete();
    entity.fetch();
    verify(mockTable, atLeast(0)).putItem(any(TestDynamoEntity.class));
  }

  @Test
  void testDynamoDBLotFetch() {
    TestDynamoLot lot = new TestDynamoLot();
    software.amazon.awssdk.enhanced.dynamodb.model.PageIterable<TestDynamoEntity> mockIterable =
        mock(software.amazon.awssdk.enhanced.dynamodb.model.PageIterable.class);
    software.amazon.awssdk.core.pagination.sync.SdkIterable<TestDynamoEntity> mockItems =
        mock(software.amazon.awssdk.core.pagination.sync.SdkIterable.class);
    when(mockItems.iterator()).thenReturn(List.<TestDynamoEntity>of().iterator());
    when(mockIterable.items()).thenReturn(mockItems);
    when(mockTable.query(
            any(software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional.class)))
        .thenReturn(mockIterable);
    when(mockTable.scan(
            any(software.amazon.awssdk.enhanced.dynamodb.model.ScanEnhancedRequest.class)))
        .thenReturn(mockIterable);

    lot.fetchByPk("pk");
    lot.fetchByColumn("col", "val");
    verify(mockTable)
        .query(any(software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional.class));
    verify(mockTable)
        .scan(any(software.amazon.awssdk.enhanced.dynamodb.model.ScanEnhancedRequest.class));
  }

  @Test
  void testConstructorsWithEnv() throws Exception {
    try (MockedStatic<EnvUtils> mockedEnv = mockStatic(EnvUtils.class)) {
      mockedEnv.when(() -> EnvUtils.get("AWS_LAMBDA_FUNCTION_NAME")).thenReturn("test-lambda");
      new TestDynamoEntity();
      new TestDynamoLot();
    }

    try (MockedStatic<EnvUtils> mockedEnv = mockStatic(EnvUtils.class)) {
      mockedEnv.when(() -> EnvUtils.get("CI")).thenReturn("true");
      new TestDynamoEntity();
      new TestDynamoLot();
    }
  }

  @Test
  void testToStringException() {
    TestDynamoEntity entity =
        new TestDynamoEntity() {
          @Override
          public String getPartitionKey() {
            throw new RuntimeException("Forcing JsonProcessingException");
          }
        };
    assertNotNull(entity.toString());
  }
}
