package copel.sesproductpackage.core.database.base;

import static com.github.stefanbirkner.systemlambda.SystemLambda.withEnvironmentVariable;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

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

    // Must provide a partition key method for TableSchema.fromBean
    @DynamoDbPartitionKey
    public String getId() {
      return "id";
    }

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
        MockedStatic<DynamoDbEnhancedClient> mockedEnhanced =
            mockStatic(DynamoDbEnhancedClient.class)) {

      // Mock DynamoDbClient builder
      DynamoDbClient mockDbClient = mock(DynamoDbClient.class);
      DynamoDbClientBuilder mockDbBuilder = mock(DynamoDbClientBuilder.class);
      when(DynamoDbClient.builder()).thenReturn(mockDbBuilder);
      when(mockDbBuilder.region(any(Region.class))).thenReturn(mockDbBuilder);
      when(mockDbBuilder.credentialsProvider(
              any(software.amazon.awssdk.auth.credentials.AwsCredentialsProvider.class)))
          .thenReturn(mockDbBuilder);
      when(mockDbBuilder.build()).thenReturn(mockDbClient);

      // Mock DynamoDbEnhancedClient builder
      DynamoDbEnhancedClient mockEnhancedClient = mock(DynamoDbEnhancedClient.class);
      DynamoDbEnhancedClient.Builder mockEnhancedBuilder =
          mock(DynamoDbEnhancedClient.Builder.class);
      when(DynamoDbEnhancedClient.builder()).thenReturn(mockEnhancedBuilder);
      when(mockEnhancedBuilder.dynamoDbClient(any(DynamoDbClient.class)))
          .thenReturn(mockEnhancedBuilder);
      when(mockEnhancedBuilder.build()).thenReturn(mockEnhancedClient);

      // Mock Table
      DynamoDbTable<TestDynamoEntity> mockTable = mock(DynamoDbTable.class);
      when(mockEnhancedClient.table(anyString(), any(TableSchema.class))).thenReturn(mockTable);

      // Test Constructor with different env vars
      withEnvironmentVariable("AWS_LAMBDA_FUNCTION_NAME", "true")
          .execute(
              () -> {
                new TestDynamoDBLot();
                verify(mockDbBuilder, atLeastOnce()).region(Region.AP_NORTHEAST_1);
              });

      withEnvironmentVariable("CI", "true").execute(TestDynamoDBLot::new);

      // Normal case (no env vars)
      new TestDynamoDBLot();

      // Test fetchByPk
      TestDynamoDBLot lot = new TestDynamoDBLot();

      // Mock query response
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

      // Test fetchByColumn
      when(mockTable.scan(any(ScanEnhancedRequest.class))).thenReturn(mockPageIterable);
      lot.fetchByColumn("col", "val");
      verify(mockTable).scan(any(ScanEnhancedRequest.class));

      // Test iterator and toString
      assertNotNull(lot.iterator());
      assertNotNull(lot.toString());

      // Test toString exception path mapping (objectMapper failure)
      // Since toString() in DynamoDB/Lot uses ObjectMapper, it's hard to force an
      // exception
      // without mocking the mapper itself, which is private static final.

      // Test equals and hashCode for DynamoDB subclass
      TestDynamoEntity entity1 = new TestDynamoEntity();
      TestDynamoEntity entity2 = new TestDynamoEntity();
      assertEquals(entity1, entity1);
      assertNotEquals(entity1, null);
      assertNotEquals(entity1, "string");
      assertEquals(entity1, entity2);
      assertEquals(entity1.hashCode(), entity2.hashCode());

      entity1.setPartitionKey("pk1");
      assertNotEquals(entity1, entity2);
      entity2.setPartitionKey("pk1");
      assertEquals(entity1, entity2);

      entity1.setSortKey("sk1");
      assertNotEquals(entity1, entity2);
      entity2.setSortKey("sk1");
      assertEquals(entity1, entity2);

      entity1.setTimestamp("ts1");
      assertNotEquals(entity1, entity2);
      entity2.setTimestamp("ts1");
      assertEquals(entity1, entity2);

      assertNotNull(entity1.toString());
      assertNotNull(entity1.canEqual(entity2));
    }
  }
}
