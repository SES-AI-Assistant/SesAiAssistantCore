package copel.sesproductpackage.core.database.base;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import copel.sesproductpackage.core.util.EnvUtils;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import software.amazon.awssdk.core.pagination.sync.SdkIterable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.model.PageIterable;
import software.amazon.awssdk.enhanced.dynamodb.model.ScanEnhancedRequest;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClientBuilder;

class DynamoDBLotTests {

  @DynamoDbBean
  public static class TestDynamoEntity extends DynamoDB<TestDynamoEntity> {
    public TestDynamoEntity() {
      super("TestTable", TestDynamoEntity.class);
    }

    @Override
    public void save() {}

    @Override
    public void delete() {}

    @Override
    public void fetch() {}

    @Override
    @DynamoDbPartitionKey
    public String getPartitionKey() {
      return super.getPartitionKey();
    }
  }

  static class TestDynamoDBLot extends DynamoDBLot<TestDynamoEntity> {
    public TestDynamoDBLot() {
      super("TestTable", TestDynamoEntity.class);
    }
  }

  @Test
  @SuppressWarnings("unchecked")
  void testConstructorAndFetch() throws Exception {
    try (MockedStatic<DynamoDbClient> mockedClient = mockStatic(DynamoDbClient.class);
        MockedStatic<DynamoDbEnhancedClient> mockedEnhanced =
            mockStatic(DynamoDbEnhancedClient.class)) {

      DynamoDbClient mockDbClient = mock(DynamoDbClient.class);
      DynamoDbClientBuilder mockDbBuilder = mock(DynamoDbClientBuilder.class);
      when(DynamoDbClient.builder()).thenReturn(mockDbBuilder);
      when(mockDbBuilder.region(any(Region.class))).thenReturn(mockDbBuilder);
      when(mockDbBuilder.credentialsProvider(
              any(software.amazon.awssdk.auth.credentials.AwsCredentialsProvider.class)))
          .thenReturn(mockDbBuilder);
      when(mockDbBuilder.build()).thenReturn(mockDbClient);

      DynamoDbEnhancedClient mockEnhancedClient = mock(DynamoDbEnhancedClient.class);
      DynamoDbEnhancedClient.Builder mockEnhancedBuilder =
          mock(DynamoDbEnhancedClient.Builder.class);
      when(DynamoDbEnhancedClient.builder()).thenReturn(mockEnhancedBuilder);
      when(mockEnhancedBuilder.dynamoDbClient(any(DynamoDbClient.class)))
          .thenReturn(mockEnhancedBuilder);
      when(mockEnhancedBuilder.build()).thenReturn(mockEnhancedClient);

      DynamoDbTable<TestDynamoEntity> mockTable = mock(DynamoDbTable.class);
      when(mockEnhancedClient.table(anyString(), any(TableSchema.class))).thenReturn(mockTable);

      try (MockedStatic<EnvUtils> mockedEnv = mockStatic(EnvUtils.class)) {
        mockedEnv.when(() -> EnvUtils.get("AWS_LAMBDA_FUNCTION_NAME")).thenReturn("true");
        new TestDynamoDBLot();
        verify(mockDbBuilder, atLeastOnce()).region(Region.AP_NORTHEAST_1);
      }

      try (MockedStatic<EnvUtils> mockedEnv = mockStatic(EnvUtils.class)) {
        mockedEnv.when(() -> EnvUtils.get("CI")).thenReturn("true");
        new TestDynamoDBLot();
      }

      new TestDynamoDBLot();

      TestDynamoDBLot lot = new TestDynamoDBLot();

      PageIterable<TestDynamoEntity> mockPageIterable = mock(PageIterable.class);
      SdkIterable<TestDynamoEntity> mockSdkIterable = mock(SdkIterable.class);
      when(mockTable.query(
              any(software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional.class)))
          .thenReturn(mockPageIterable);
      when(mockPageIterable.items()).thenReturn(mockSdkIterable);
      when(mockSdkIterable.iterator()).thenReturn(Collections.emptyIterator());

      lot.fetchByPk("pk");
      verify(mockTable)
          .query(any(software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional.class));

      when(mockTable.scan(any(ScanEnhancedRequest.class))).thenReturn(mockPageIterable);
      lot.fetchByColumn("col", "val");
      verify(mockTable).scan(any(ScanEnhancedRequest.class));

      assertNotNull(lot.iterator());
      assertNotNull(lot.toString());

      TestDynamoEntity entity1 = new TestDynamoEntity();
      TestDynamoEntity entity2 = new TestDynamoEntity();
      assertTrue(entity1.equals(entity1));
      assertFalse(entity1.equals(null));
      assertFalse(entity1.equals("string"));
      assertTrue(entity1.equals(entity2));
      assertEquals(entity1.hashCode(), entity2.hashCode());

      entity1.setPartitionKey("pk1");
      assertFalse(entity1.equals(entity2));
      entity2.setPartitionKey("pk1");
      assertTrue(entity1.equals(entity2));

      entity1.setSortKey("sk1");
      assertFalse(entity1.equals(entity2));
      entity2.setSortKey("sk1");
      assertTrue(entity1.equals(entity2));

      entity1.setTimestamp("ts1");
      assertFalse(entity1.equals(entity2));
      entity2.setTimestamp("ts1");
      assertTrue(entity1.equals(entity2));

      assertNotNull(entity1.toString());
      assertTrue(entity1.canEqual(entity2));
    }
  }

  @Test
  void testToStringException() {
    TestDynamoDBLot lot = new TestDynamoDBLot();
    // Force a JsonProcessingException by adding an object that Jackson can't
    // serialize easily if we control the list
    // But entityLot is public, so we can inject something
    lot.entityLot.add(
        new TestDynamoEntity() {
          @Override
          public String getPartitionKey() {
            throw new RuntimeException("Forced error");
          }
        });
    assertEquals("[]", lot.toString());
  }
}
