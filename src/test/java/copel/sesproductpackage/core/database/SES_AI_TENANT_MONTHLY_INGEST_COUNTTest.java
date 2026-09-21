package copel.sesproductpackage.core.database;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import copel.sesproductpackage.core.database.SES_AI_M_INGEST_ROUTE.ChannelType;
import copel.sesproductpackage.core.database.base.DynamoDbClientFactory;
import copel.sesproductpackage.core.unit.OriginalDateTime;
import copel.sesproductpackage.core.util.Properties;
import copel.sesproductpackage.core.util.SsmParameterKey;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.GetItemEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClientBuilder;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;

@DisplayName("SES_AI_TENANT_MONTHLY_INGEST_COUNT Test")
class SES_AI_TENANT_MONTHLY_INGEST_COUNTTest {

  private MockedStatic<DynamoDbClient> mockedClientStatic;
  private MockedStatic<DynamoDbEnhancedClient> mockedEnhancedClientStatic;
  private DynamoDbClient mockDbClient;
  private DynamoDbTable<SES_AI_TENANT_MONTHLY_INGEST_COUNT> mockTable;

  @BeforeEach
  @SuppressWarnings("unchecked")
  void setUp() {
    mockedClientStatic = mockStatic(DynamoDbClient.class);
    mockedEnhancedClientStatic = mockStatic(DynamoDbEnhancedClient.class);

    mockDbClient = mock(DynamoDbClient.class);
    DynamoDbClientBuilder mockBuilder = mock(DynamoDbClientBuilder.class);
    when(mockBuilder.region(any())).thenReturn(mockBuilder);
    when(mockBuilder.credentialsProvider(any())).thenReturn(mockBuilder);
    when(mockBuilder.build()).thenReturn(mockDbClient);
    mockedClientStatic.when(DynamoDbClient::builder).thenReturn(mockBuilder);

    DynamoDbEnhancedClient mockEnhancedClient = mock(DynamoDbEnhancedClient.class);
    DynamoDbEnhancedClient.Builder mockEnhancedBuilder = mock(DynamoDbEnhancedClient.Builder.class);
    when(mockEnhancedBuilder.dynamoDbClient(any())).thenReturn(mockEnhancedBuilder);
    when(mockEnhancedBuilder.build()).thenReturn(mockEnhancedClient);
    mockedEnhancedClientStatic.when(DynamoDbEnhancedClient::builder).thenReturn(mockEnhancedBuilder);

    mockTable = mock(DynamoDbTable.class);
    when(mockEnhancedClient.table(anyString(), any(TableSchema.class))).thenReturn(mockTable);

    // テスト用にモッククライアントを設定
    SES_AI_TENANT_MONTHLY_INGEST_COUNT.setDynamoDbClient(mockDbClient);
  }

  @AfterEach
  void tearDown() {
    SES_AI_TENANT_MONTHLY_INGEST_COUNT.setDynamoDbClient(null);
    mockedClientStatic.close();
    mockedEnhancedClientStatic.close();
  }

  @Nested
  @DisplayName("エンティティプロパティおよび基本動作テスト")
  class EntityPropertyTests {

    @Test
    @DisplayName("デフォルトコンストラクタおよびテーブル名解決")
    void testConstructorAndTableNameResolution() {
      SES_AI_TENANT_MONTHLY_INGEST_COUNT entity = new SES_AI_TENANT_MONTHLY_INGEST_COUNT();
      assertNotNull(entity);
      assertEquals("SES_AI_TENANT_MONTHLY_INGEST_COUNT", SES_AI_TENANT_MONTHLY_INGEST_COUNT.resolveTableName());

      try (MockedStatic<Properties> mockedProperties = mockStatic(Properties.class)) {
        mockedProperties
            .when(() -> Properties.get(SsmParameterKey.TENANT_MONTHLY_INGEST_COUNT_TABLE_NAME.getKey()))
            .thenReturn("custom-table-name");
        assertEquals("custom-table-name", SES_AI_TENANT_MONTHLY_INGEST_COUNT.resolveTableName());

        mockedProperties
            .when(() -> Properties.get(SsmParameterKey.TENANT_MONTHLY_INGEST_COUNT_TABLE_NAME.getKey()))
            .thenReturn("");
        assertEquals("SES_AI_TENANT_MONTHLY_INGEST_COUNT", SES_AI_TENANT_MONTHLY_INGEST_COUNT.resolveTableName());

        mockedProperties
            .when(() -> Properties.get(SsmParameterKey.TENANT_MONTHLY_INGEST_COUNT_TABLE_NAME.getKey()))
            .thenReturn("   ");
        assertEquals("SES_AI_TENANT_MONTHLY_INGEST_COUNT", SES_AI_TENANT_MONTHLY_INGEST_COUNT.resolveTableName());

        mockedProperties
            .when(() -> Properties.get(SsmParameterKey.TENANT_MONTHLY_INGEST_COUNT_TABLE_NAME.getKey()))
            .thenReturn(null);
        assertEquals("SES_AI_TENANT_MONTHLY_INGEST_COUNT", SES_AI_TENANT_MONTHLY_INGEST_COUNT.resolveTableName());
      }
    }

    @Test
    @DisplayName("パーティションキーおよびソートキーの生成")
    void testKeys() {
      SES_AI_TENANT_MONTHLY_INGEST_COUNT entity = new SES_AI_TENANT_MONTHLY_INGEST_COUNT();
      entity.setTenantId("tenant-123");
      entity.setYearMonth("202609");
      entity.setChannelType(ChannelType.EMAIL);

      assertEquals("tenant-123#202609", entity.getPartitionKey());
      assertEquals("EMAIL", entity.getSortKey());

      SES_AI_TENANT_MONTHLY_INGEST_COUNT lineEntity = new SES_AI_TENANT_MONTHLY_INGEST_COUNT();
      lineEntity.setTenantId("tenant-456");
      lineEntity.setYearMonth("202610");
      lineEntity.setChannelType(ChannelType.LINE);

      assertEquals("tenant-456#202610", lineEntity.getPartitionKey());
      assertEquals("LINE", lineEntity.getSortKey());

      SES_AI_TENANT_MONTHLY_INGEST_COUNT nullChannel = new SES_AI_TENANT_MONTHLY_INGEST_COUNT();
      nullChannel.setTenantId("tenant-789");
      nullChannel.setYearMonth("202611");
      nullChannel.setChannelType(null);

      assertEquals("tenant-789#202611", nullChannel.getPartitionKey());
      assertNull(nullChannel.getSortKey());
    }

    @Test
    @DisplayName("各getterおよびsetterの確認")
    void testGettersAndSetters() {
      SES_AI_TENANT_MONTHLY_INGEST_COUNT entity = new SES_AI_TENANT_MONTHLY_INGEST_COUNT();
      entity.setTenantId("tenant-abc");
      entity.setYearMonth("202610");
      entity.setChannelType(ChannelType.LINE);
      entity.setCount(150L);
      entity.setTimestamp("2026-10-01T12:00:00Z");

      assertEquals("tenant-abc", entity.getTenantId());
      assertEquals("202610", entity.getYearMonth());
      assertEquals(ChannelType.LINE, entity.getChannelType());
      assertEquals(150L, entity.getCount());
      assertEquals("2026-10-01T12:00:00Z", entity.getTimestamp());

      entity.setChannelType(ChannelType.EMAIL);
      assertEquals(ChannelType.EMAIL, entity.getChannelType());

      entity.setChannelType(null);
      assertNull(entity.getChannelType());
    }

    @Test
    @DisplayName("equals, hashCode, canEqual, toString の網羅テスト")
    void testEqualsAndHashCode() {
      SES_AI_TENANT_MONTHLY_INGEST_COUNT e1 = createEntity("t1", "202609", ChannelType.EMAIL, 10L, "2026-09-01T00:00:00Z");
      SES_AI_TENANT_MONTHLY_INGEST_COUNT e2 = createEntity("t1", "202609", ChannelType.EMAIL, 10L, "2026-09-01T00:00:00Z");

      assertEquals(e1, e2);
      assertEquals(e1.hashCode(), e2.hashCode());
      assertTrue(e1.canEqual(e2));
      assertEquals(e1, e1);
      assertFalse(e1.equals(null));
      assertFalse(e1.equals("other type"));

      // 各フィールドの不一致テスト
      SES_AI_TENANT_MONTHLY_INGEST_COUNT diffTenant = createEntity("t2", "202609", ChannelType.EMAIL, 10L, "2026-09-01T00:00:00Z");
      assertNotEquals(e1, diffTenant);

      SES_AI_TENANT_MONTHLY_INGEST_COUNT diffYm = createEntity("t1", "202610", ChannelType.EMAIL, 10L, "2026-09-01T00:00:00Z");
      assertNotEquals(e1, diffYm);

      SES_AI_TENANT_MONTHLY_INGEST_COUNT diffCt = createEntity("t1", "202609", ChannelType.LINE, 10L, "2026-09-01T00:00:00Z");
      assertNotEquals(e1, diffCt);

      SES_AI_TENANT_MONTHLY_INGEST_COUNT diffCount = createEntity("t1", "202609", ChannelType.EMAIL, 99L, "2026-09-01T00:00:00Z");
      assertNotEquals(e1, diffCount);

      // callSuper = false のため親クラスの timestamp フィールドが異なっていても等価と判定されること
      SES_AI_TENANT_MONTHLY_INGEST_COUNT diffTimestamp =
          createEntity("t1", "202609", ChannelType.EMAIL, 10L, "2026-09-02T00:00:00Z");
      assertEquals(e1, diffTimestamp);
      assertEquals(e1.hashCode(), diffTimestamp.hashCode());

      assertNotNull(e1.toString());
    }

    private SES_AI_TENANT_MONTHLY_INGEST_COUNT createEntity(
        String tenantId, String yearMonth, ChannelType channelType, Long count, String timestamp) {
      SES_AI_TENANT_MONTHLY_INGEST_COUNT e = new SES_AI_TENANT_MONTHLY_INGEST_COUNT();
      e.setTenantId(tenantId);
      e.setYearMonth(yearMonth);
      e.setChannelType(channelType);
      e.setCount(count);
      e.setTimestamp(timestamp);
      return e;
    }
  }

  @Nested
  @DisplayName("DynamoDB CRUD 操作テスト (save, delete, fetch)")
  class CrudOperationTests {

    @Test
    @DisplayName("save: 正常系（putItem が実行され timestamp が設定されること）")
    void testSaveSuccess() {
      SES_AI_TENANT_MONTHLY_INGEST_COUNT entity = new SES_AI_TENANT_MONTHLY_INGEST_COUNT();
      entity.setTenantId("tenant-1");
      entity.setYearMonth("202609");
      entity.setChannelType(ChannelType.EMAIL);
      entity.setCount(5L);

      entity.save();

      verify(mockTable, times(1)).putItem(entity);
      assertNotNull(entity.getTimestamp());
      assertDoesNotThrow(() -> Instant.parse(entity.getTimestamp()));
    }

    @Test
    @DisplayName("save: 必須項目欠落によるスキップ")
    void testSaveValidationSkip() {
      SES_AI_TENANT_MONTHLY_INGEST_COUNT e;

      // tenantId null / empty
      e = new SES_AI_TENANT_MONTHLY_INGEST_COUNT();
      e.setTenantId(null);
      e.setYearMonth("202609");
      e.setChannelType(ChannelType.EMAIL);
      e.save();

      e = new SES_AI_TENANT_MONTHLY_INGEST_COUNT();
      e.setTenantId("");
      e.setYearMonth("202609");
      e.setChannelType(ChannelType.EMAIL);
      e.save();

      // yearMonth null / empty
      e = new SES_AI_TENANT_MONTHLY_INGEST_COUNT();
      e.setTenantId("tenant-1");
      e.setYearMonth(null);
      e.setChannelType(ChannelType.EMAIL);
      e.save();

      e = new SES_AI_TENANT_MONTHLY_INGEST_COUNT();
      e.setTenantId("tenant-1");
      e.setYearMonth("");
      e.setChannelType(ChannelType.EMAIL);
      e.save();

      // channelType null
      e = new SES_AI_TENANT_MONTHLY_INGEST_COUNT();
      e.setTenantId("tenant-1");
      e.setYearMonth("202609");
      e.setChannelType(null);
      e.save();

      verify(mockTable, never()).putItem(any(SES_AI_TENANT_MONTHLY_INGEST_COUNT.class));
    }

    @Test
    @DisplayName("delete: 正常系およびキー未設定時のスキップ")
    void testDelete() {
      SES_AI_TENANT_MONTHLY_INGEST_COUNT entity = new SES_AI_TENANT_MONTHLY_INGEST_COUNT();
      entity.setTenantId("tenant-1");
      entity.setYearMonth("202609");
      entity.setChannelType(ChannelType.EMAIL);

      entity.delete();

      ArgumentCaptor<Key> keyCaptor = ArgumentCaptor.forClass(Key.class);
      verify(mockTable, times(1)).deleteItem(keyCaptor.capture());
      Key deletedKey = keyCaptor.getValue();
      assertEquals("tenant-1#202609", deletedKey.partitionKeyValue().s());
      assertEquals("EMAIL", deletedKey.sortKeyValue().get().s());

      // キー未設定時スキップ（PK null）
      reset(mockTable);
      SES_AI_TENANT_MONTHLY_INGEST_COUNT emptyEntity = new SES_AI_TENANT_MONTHLY_INGEST_COUNT();
      emptyEntity.delete();
      verify(mockTable, never()).deleteItem(any(Key.class));

      // キー未設定時スキップ（PK 非null、SK null）
      reset(mockTable);
      SES_AI_TENANT_MONTHLY_INGEST_COUNT partialEntity = new SES_AI_TENANT_MONTHLY_INGEST_COUNT();
      partialEntity.setTenantId("t1");
      partialEntity.setYearMonth("202609");
      partialEntity.delete();
      verify(mockTable, never()).deleteItem(any(Key.class));
    }

    @Test
    @DisplayName("fetch: 正常系およびキー未設定/レコード不在時")
    void testFetch() {
      SES_AI_TENANT_MONTHLY_INGEST_COUNT fetchedData = new SES_AI_TENANT_MONTHLY_INGEST_COUNT();
      fetchedData.setTenantId("tenant-1");
      fetchedData.setYearMonth("202609");
      fetchedData.setChannelType(ChannelType.LINE);
      fetchedData.setCount(99L);
      fetchedData.setTimestamp("2026-09-15T10:00:00Z");

      doAnswer(
              invocation -> {
                Consumer<GetItemEnhancedRequest.Builder> consumer = invocation.getArgument(0);
                GetItemEnhancedRequest.Builder builder =
                    mock(GetItemEnhancedRequest.Builder.class, RETURNS_DEEP_STUBS);
                consumer.accept(builder);
                return fetchedData;
              })
          .when(mockTable)
          .getItem(any(Consumer.class));

      SES_AI_TENANT_MONTHLY_INGEST_COUNT target = new SES_AI_TENANT_MONTHLY_INGEST_COUNT();
      target.setTenantId("tenant-1");
      target.setYearMonth("202609");
      target.setChannelType(ChannelType.LINE);

      target.fetch();

      assertEquals("tenant-1", target.getTenantId());
      assertEquals("202609", target.getYearMonth());
      assertEquals(ChannelType.LINE, target.getChannelType());
      assertEquals(99L, target.getCount());
      assertEquals("2026-09-15T10:00:00Z", target.getTimestamp());

      // レコード不在（null返却時）
      reset(mockTable);
      when(mockTable.getItem(any(Consumer.class))).thenReturn(null);
      target.fetch();
      assertEquals(99L, target.getCount()); // 値が維持されること

      // キー未設定時スキップ（PK null）
      reset(mockTable);
      SES_AI_TENANT_MONTHLY_INGEST_COUNT emptyEntity = new SES_AI_TENANT_MONTHLY_INGEST_COUNT();
      emptyEntity.fetch();
      verify(mockTable, never()).getItem(any(Consumer.class));

      // キー未設定時スキップ（PK 非null、SK null）
      reset(mockTable);
      SES_AI_TENANT_MONTHLY_INGEST_COUNT partialEntity = new SES_AI_TENANT_MONTHLY_INGEST_COUNT();
      partialEntity.setTenantId("t1");
      partialEntity.setYearMonth("202609");
      partialEntity.fetch();
      verify(mockTable, never()).getItem(any(Consumer.class));
    }
  }

  @Nested
  @DisplayName("アトミック加算ロジック increment テスト")
  class IncrementTests {

    @Test
    @DisplayName("increment(tenantId, channelType): 現在年月(JST)・amount=1 で加算されること")
    void testIncrementTwoArguments() {
      OriginalDateTime nowDt = new OriginalDateTime();
      String expectedYearMonth = nowDt.getYYYYMM();

      SES_AI_TENANT_MONTHLY_INGEST_COUNT.increment("tenant-001", ChannelType.EMAIL);

      ArgumentCaptor<UpdateItemRequest> captor = ArgumentCaptor.forClass(UpdateItemRequest.class);
      verify(mockDbClient, times(1)).updateItem(captor.capture());

      UpdateItemRequest request = captor.getValue();
      assertEquals("SES_AI_TENANT_MONTHLY_INGEST_COUNT", request.tableName());

      // Key の検証
      Map<String, AttributeValue> key = request.key();
      assertEquals("tenant-001#" + expectedYearMonth, key.get("partitionKey").s());
      assertEquals("EMAIL", key.get("sortKey").s());

      // updateExpression の検証
      assertEquals(
          "SET #upd = :now, #tid = if_not_exists(#tid, :tid), #ym = if_not_exists(#ym, :ym), #ct = if_not_exists(#ct, :ct) ADD #cnt :inc",
          request.updateExpression());

      // expressionAttributeNames の検証
      Map<String, String> names = request.expressionAttributeNames();
      assertEquals("timestamp", names.get("#upd"));
      assertEquals("tenantId", names.get("#tid"));
      assertEquals("yearMonth", names.get("#ym"));
      assertEquals("channelType", names.get("#ct"));
      assertEquals("count", names.get("#cnt"));

      // expressionAttributeValues の検証
      Map<String, AttributeValue> values = request.expressionAttributeValues();
      assertNotNull(values.get(":now").s());
      assertDoesNotThrow(() -> Instant.parse(values.get(":now").s()));
      assertEquals("tenant-001", values.get(":tid").s());
      assertEquals(expectedYearMonth, values.get(":ym").s());
      assertEquals("EMAIL", values.get(":ct").s());
      assertEquals("1", values.get(":inc").n());
    }

    @Test
    @DisplayName("increment(tenantId, channelType, amount): 現在年月(JST)・指定 amount で加算されること")
    void testIncrementThreeArguments() {
      OriginalDateTime nowDt = new OriginalDateTime();
      String expectedYearMonth = nowDt.getYYYYMM();

      SES_AI_TENANT_MONTHLY_INGEST_COUNT.increment("tenant-001", ChannelType.EMAIL, 5L);

      ArgumentCaptor<UpdateItemRequest> captor = ArgumentCaptor.forClass(UpdateItemRequest.class);
      verify(mockDbClient, times(1)).updateItem(captor.capture());

      UpdateItemRequest request = captor.getValue();
      assertEquals("SES_AI_TENANT_MONTHLY_INGEST_COUNT", request.tableName());

      // Key の検証
      Map<String, AttributeValue> key = request.key();
      assertEquals("tenant-001#" + expectedYearMonth, key.get("partitionKey").s());
      assertEquals("EMAIL", key.get("sortKey").s());

      // updateExpression の検証
      assertEquals(
          "SET #upd = :now, #tid = if_not_exists(#tid, :tid), #ym = if_not_exists(#ym, :ym), #ct = if_not_exists(#ct, :ct) ADD #cnt :inc",
          request.updateExpression());

      // expressionAttributeNames の検証
      Map<String, String> names = request.expressionAttributeNames();
      assertEquals("timestamp", names.get("#upd"));
      assertEquals("tenantId", names.get("#tid"));
      assertEquals("yearMonth", names.get("#ym"));
      assertEquals("channelType", names.get("#ct"));
      assertEquals("count", names.get("#cnt"));

      // expressionAttributeValues の検証
      Map<String, AttributeValue> values = request.expressionAttributeValues();
      assertNotNull(values.get(":now").s());
      assertDoesNotThrow(() -> Instant.parse(values.get(":now").s()));
      assertEquals("tenant-001", values.get(":tid").s());
      assertEquals(expectedYearMonth, values.get(":ym").s());
      assertEquals("EMAIL", values.get(":ct").s());
      assertEquals("5", values.get(":inc").n());
    }

    @Test
    @DisplayName("increment(tenantId, yearMonth, channelType, amount): 4引数での完全なパラメータ検証")
    void testIncrementFourArguments() {
      try (MockedStatic<Properties> mockedProperties = mockStatic(Properties.class)) {
        mockedProperties
            .when(() -> Properties.get(SsmParameterKey.TENANT_MONTHLY_INGEST_COUNT_TABLE_NAME.getKey()))
            .thenReturn("custom-ingest-table");

        SES_AI_TENANT_MONTHLY_INGEST_COUNT.increment("tenant-999", "202612", ChannelType.LINE, 10L);

        ArgumentCaptor<UpdateItemRequest> captor = ArgumentCaptor.forClass(UpdateItemRequest.class);
        verify(mockDbClient, times(1)).updateItem(captor.capture());

        UpdateItemRequest request = captor.getValue();
        assertEquals("custom-ingest-table", request.tableName());

        // Key の検証
        Map<String, AttributeValue> key = request.key();
        assertEquals("tenant-999#202612", key.get("partitionKey").s());
        assertEquals("LINE", key.get("sortKey").s());

        // updateExpression の検証
        assertEquals(
            "SET #upd = :now, #tid = if_not_exists(#tid, :tid), #ym = if_not_exists(#ym, :ym), #ct = if_not_exists(#ct, :ct) ADD #cnt :inc",
            request.updateExpression());

        // expressionAttributeNames の検証
        Map<String, String> names = request.expressionAttributeNames();
        assertEquals("timestamp", names.get("#upd"));
        assertEquals("tenantId", names.get("#tid"));
        assertEquals("yearMonth", names.get("#ym"));
        assertEquals("channelType", names.get("#ct"));
        assertEquals("count", names.get("#cnt"));

        // expressionAttributeValues の検証
        Map<String, AttributeValue> values = request.expressionAttributeValues();
        assertNotNull(values.get(":now").s());
        assertEquals("tenant-999", values.get(":tid").s());
        assertEquals("202612", values.get(":ym").s());
        assertEquals("LINE", values.get(":ct").s());
        assertEquals("10", values.get(":inc").n());
      }
    }

    @Test
    @DisplayName("channelType が null の場合スキップされること（2引数、3引数、4引数）")
    void testIncrementSkippedWhenChannelTypeNull() {
      // 2引数
      reset(mockDbClient);
      SES_AI_TENANT_MONTHLY_INGEST_COUNT.increment("tenant-001", null);
      verify(mockDbClient, never()).updateItem(any(UpdateItemRequest.class));

      // 3引数
      reset(mockDbClient);
      SES_AI_TENANT_MONTHLY_INGEST_COUNT.increment("tenant-001", null, 5L);
      verify(mockDbClient, never()).updateItem(any(UpdateItemRequest.class));

      // 4引数
      reset(mockDbClient);
      SES_AI_TENANT_MONTHLY_INGEST_COUNT.increment("tenant-001", "202609", null, 1L);
      verify(mockDbClient, never()).updateItem(any(UpdateItemRequest.class));
    }

    @Test
    @DisplayName("tenantId が null または空文字・空白の場合スキップされること")
    void testIncrementSkippedWhenTenantIdInvalid() {
      List<String> invalidInputs = List.of("", "   ", "\t", "\n");
      for (String invalid : invalidInputs) {
        // 4引数
        reset(mockDbClient);
        SES_AI_TENANT_MONTHLY_INGEST_COUNT.increment(invalid, "202609", ChannelType.EMAIL, 1L);
        verify(mockDbClient, never()).updateItem(any(UpdateItemRequest.class));

        // 3引数
        reset(mockDbClient);
        SES_AI_TENANT_MONTHLY_INGEST_COUNT.increment(invalid, ChannelType.EMAIL, 1L);
        verify(mockDbClient, never()).updateItem(any(UpdateItemRequest.class));

        // 2引数
        reset(mockDbClient);
        SES_AI_TENANT_MONTHLY_INGEST_COUNT.increment(invalid, ChannelType.EMAIL);
        verify(mockDbClient, never()).updateItem(any(UpdateItemRequest.class));
      }

      // null の場合
      reset(mockDbClient);
      SES_AI_TENANT_MONTHLY_INGEST_COUNT.increment(null, "202609", ChannelType.EMAIL, 1L);
      verify(mockDbClient, never()).updateItem(any(UpdateItemRequest.class));

      reset(mockDbClient);
      SES_AI_TENANT_MONTHLY_INGEST_COUNT.increment(null, ChannelType.EMAIL, 1L);
      verify(mockDbClient, never()).updateItem(any(UpdateItemRequest.class));

      reset(mockDbClient);
      SES_AI_TENANT_MONTHLY_INGEST_COUNT.increment(null, ChannelType.EMAIL);
      verify(mockDbClient, never()).updateItem(any(UpdateItemRequest.class));
    }

    @Test
    @DisplayName("yearMonth が null または空文字・空白の場合スキップされること")
    void testIncrementSkippedWhenYearMonthInvalid() {
      List<String> invalidInputs = List.of("", "   ", "\t", "\n");
      for (String invalid : invalidInputs) {
        reset(mockDbClient);
        SES_AI_TENANT_MONTHLY_INGEST_COUNT.increment("tenant-001", invalid, ChannelType.EMAIL, 1L);
        verify(mockDbClient, never()).updateItem(any(UpdateItemRequest.class));
      }
      reset(mockDbClient);
      SES_AI_TENANT_MONTHLY_INGEST_COUNT.increment("tenant-001", null, ChannelType.EMAIL, 1L);
      verify(mockDbClient, never()).updateItem(any(UpdateItemRequest.class));
    }

    @Test
    @DisplayName("amount が 0 または負数の場合スキップされること")
    void testIncrementSkippedWhenAmountNonPositive() {
      // 4引数: amount = 0
      reset(mockDbClient);
      SES_AI_TENANT_MONTHLY_INGEST_COUNT.increment("tenant-001", "202609", ChannelType.EMAIL, 0L);
      verify(mockDbClient, never()).updateItem(any(UpdateItemRequest.class));

      // 4引数: amount = -1
      reset(mockDbClient);
      SES_AI_TENANT_MONTHLY_INGEST_COUNT.increment("tenant-001", "202609", ChannelType.EMAIL, -1L);
      verify(mockDbClient, never()).updateItem(any(UpdateItemRequest.class));

      // 3引数: amount = 0
      reset(mockDbClient);
      SES_AI_TENANT_MONTHLY_INGEST_COUNT.increment("tenant-001", ChannelType.EMAIL, 0L);
      verify(mockDbClient, never()).updateItem(any(UpdateItemRequest.class));

      // 3引数: amount = -5
      reset(mockDbClient);
      SES_AI_TENANT_MONTHLY_INGEST_COUNT.increment("tenant-001", ChannelType.EMAIL, -5L);
      verify(mockDbClient, never()).updateItem(any(UpdateItemRequest.class));
    }

    @Test
    @DisplayName("updateItem 実行時に例外が発生した場合は再スローされること")
    void testIncrementThrowsException() {
      doThrow(DynamoDbException.builder().message("DynamoDB error").build())
          .when(mockDbClient)
          .updateItem(any(UpdateItemRequest.class));

      assertThrows(
          DynamoDbException.class,
          () -> SES_AI_TENANT_MONTHLY_INGEST_COUNT.increment("tenant-001", "202609", ChannelType.EMAIL, 1L));
    }

    @Test
    @DisplayName("DynamoDbClient の遅延初期化とキャッシュの動作確認")
    void testGetDynamoDbClientLazyInitialization() {
      // 一旦 null にリセット
      SES_AI_TENANT_MONTHLY_INGEST_COUNT.setDynamoDbClient(null);

      try (MockedStatic<DynamoDbClientFactory> mockedFactory = mockStatic(DynamoDbClientFactory.class)) {
        DynamoDbClient clientFromFactory = mock(DynamoDbClient.class);
        mockedFactory.when(DynamoDbClientFactory::create).thenReturn(clientFromFactory);

        DynamoDbClient client1 = SES_AI_TENANT_MONTHLY_INGEST_COUNT.getDynamoDbClient();
        DynamoDbClient client2 = SES_AI_TENANT_MONTHLY_INGEST_COUNT.getDynamoDbClient();

        assertSame(clientFromFactory, client1);
        assertSame(client1, client2);
        // キャッシュされているため create は1度しか呼ばれないこと
        mockedFactory.verify(DynamoDbClientFactory::create, times(1));
      }
    }
  }
}
