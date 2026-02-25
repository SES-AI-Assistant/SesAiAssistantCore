package copel.sesproductpackage.core.database;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import copel.sesproductpackage.core.database.SES_AI_API_USAGE_HISTORY.ApiType;
import copel.sesproductpackage.core.database.SES_AI_API_USAGE_HISTORY.Provider;
import java.math.BigDecimal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClientBuilder;

class SES_AI_API_USAGE_HISTORYTests {

  private MockedStatic<DynamoDbClient> mockedClient;
  private MockedStatic<DynamoDbEnhancedClient> mockedEnhancedClient;
  private DynamoDbTable<SES_AI_API_USAGE_HISTORY> mockTable;

  @BeforeEach
  @SuppressWarnings("unchecked")
  void setUp() {
    mockedClient = mockStatic(DynamoDbClient.class);
    mockedEnhancedClient = mockStatic(DynamoDbEnhancedClient.class);

    DynamoDbClient mockDbClient = mock(DynamoDbClient.class);
    DynamoDbClientBuilder mockBuilder = mock(DynamoDbClientBuilder.class);
    when(mockBuilder.region(any())).thenReturn(mockBuilder);
    when(mockBuilder.credentialsProvider(any())).thenReturn(mockBuilder);
    when(mockBuilder.build()).thenReturn(mockDbClient);
    mockedClient.when(DynamoDbClient::builder).thenReturn(mockBuilder);

    DynamoDbEnhancedClient mockEnhancedClient = mock(DynamoDbEnhancedClient.class);
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
  void saveTest() {
    SES_AI_API_USAGE_HISTORY history = new SES_AI_API_USAGE_HISTORY();
    history.setProvider(Provider.OpenAI);
    history.setModel("gpt-4o-mini");
    history.setUsageMonth("202501");
    history.save();
    verify(mockTable, times(1)).putItem(any(SES_AI_API_USAGE_HISTORY.class));

    // Reset mock for negative cases
    reset(mockTable);

    // Null/Empty cases
    SES_AI_API_USAGE_HISTORY h;

    h = new SES_AI_API_USAGE_HISTORY();
    h.setProvider(null);
    h.save();

    h = new SES_AI_API_USAGE_HISTORY();
    h.setProvider(Provider.OpenAI);
    h.setModel(null);
    h.save();

    h = new SES_AI_API_USAGE_HISTORY();
    h.setProvider(Provider.OpenAI);
    h.setModel("");
    h.save();

    h = new SES_AI_API_USAGE_HISTORY();
    h.setProvider(Provider.OpenAI);
    h.setModel("m");
    h.setUsageMonth(null);
    h.save();

    h = new SES_AI_API_USAGE_HISTORY();
    h.setProvider(Provider.OpenAI);
    h.setModel("m");
    h.setUsageMonth("");
    h.save();

    verify(mockTable, never()).putItem(any(SES_AI_API_USAGE_HISTORY.class));
  }

  @Test
  void deleteTest() {
    SES_AI_API_USAGE_HISTORY history = new SES_AI_API_USAGE_HISTORY();
    history.setProvider(Provider.OpenAI);
    history.setModel("gpt-4o-mini");
    history.setUsageMonth("202501");
    history.setUserId("test_user_id");
    history.setApiType(ApiType.Generate);
    history.delete();
    verify(mockTable).deleteItem(any(software.amazon.awssdk.enhanced.dynamodb.Key.class));
  }

  @Test
  void fetchTest() {
    SES_AI_API_USAGE_HISTORY history = new SES_AI_API_USAGE_HISTORY();
    history.setProvider(Provider.OpenAI);
    history.setModel("gpt-4o-mini");
    history.setUsageMonth("202501");
    history.setUserId("test_user_id");
    history.setApiType(ApiType.Generate);
    history.setInputCount(new BigDecimal(10));
    history.setOutputCount(new BigDecimal(5));
    history.setTimestamp("2026-01-01T00:00:00Z");

    doAnswer(
            invocation -> {
              java.util.function.Consumer<
                      software.amazon.awssdk.enhanced.dynamodb.model.GetItemEnhancedRequest.Builder>
                  consumer = invocation.getArgument(0);
              software.amazon.awssdk.enhanced.dynamodb.model.GetItemEnhancedRequest.Builder
                  builder =
                      mock(
                          software.amazon.awssdk.enhanced.dynamodb.model.GetItemEnhancedRequest
                              .Builder.class,
                          RETURNS_DEEP_STUBS);
              consumer.accept(builder);
              return history;
            })
        .when(mockTable)
        .getItem(any(java.util.function.Consumer.class));

    SES_AI_API_USAGE_HISTORY target = new SES_AI_API_USAGE_HISTORY();
    // Set keys so partitionKey and sortKey are generated for Key.builder() if
    // needed by mock
    // internally
    target.setProvider(Provider.OpenAI);
    target.setModel("gpt-4o-mini");
    target.setUsageMonth("202501");
    target.setUserId("test_user_id");
    target.setApiType(ApiType.Generate);

    target.fetch();

    assertEquals(Provider.OpenAI, target.getProvider());
    assertEquals("gpt-4o-mini", target.getModel());
    assertEquals("202501", target.getUsageMonth());
    assertEquals("test_user_id", target.getUserId());
    assertEquals(ApiType.Generate, target.getApiType());
    assertEquals(new BigDecimal(10), target.getInputCount());
    assertEquals(new BigDecimal(5), target.getOutputCount());
    assertEquals("2026-01-01T00:00:00Z", target.getTimestamp());

    assertNotNull(target.toString());

    // Null case
    reset(mockTable);
    when(mockTable.getItem(any(java.util.function.Consumer.class))).thenReturn(null);
    target.fetch();
  }

  @Test
  void testKeys() {
    SES_AI_API_USAGE_HISTORY history = new SES_AI_API_USAGE_HISTORY();
    history.setProvider(Provider.Google);
    history.setModel("gemini");
    history.setUsageMonth("202602");
    history.setUserId("user1");
    history.setApiType(ApiType.Embedding);

    assertEquals("Google#gemini#202602", history.getPartitionKey());
    assertEquals("user1#Embedding", history.getSortKey());
  }

  @Test
  void testLombokAndAccessors() {
    SES_AI_API_USAGE_HISTORY h1 = new SES_AI_API_USAGE_HISTORY();
    h1.setProvider(Provider.OpenAI);
    h1.setModel("m1");
    h1.setUsageMonth("202501");
    h1.setUserId("u1");
    h1.setApiType(ApiType.Generate);
    h1.setInputCount(new BigDecimal(10));
    h1.setOutputCount(new BigDecimal(5));
    h1.setTimestamp("2026");

    assertEquals(Provider.OpenAI, h1.getProvider());
    assertEquals("m1", h1.getModel());
    assertEquals("202501", h1.getUsageMonth());
    assertEquals("u1", h1.getUserId());
    assertEquals(ApiType.Generate, h1.getApiType());
    assertEquals(new BigDecimal(10), h1.getInputCount());
    assertEquals(new BigDecimal(5), h1.getOutputCount());
    assertEquals("2026", h1.getTimestamp());

    // equals, hashCode, toString
    SES_AI_API_USAGE_HISTORY h2 = new SES_AI_API_USAGE_HISTORY();
    h2.setProvider(Provider.OpenAI);
    h2.setModel("m1");
    h2.setUsageMonth("202501");
    h2.setUserId("u1");
    h2.setApiType(ApiType.Generate);
    h2.setInputCount(new BigDecimal(10));
    h2.setOutputCount(new BigDecimal(5));
    h2.setTimestamp("2026");

    assertEquals(h1, h2);
    assertEquals(h1.hashCode(), h2.hashCode());
    assertNotNull(h1.toString());
    assertFalse(h1.equals(null));
    assertFalse(h1.equals(new Object()));
  }

  @Test
  void testEnumProvider() {
    for (Provider p : Provider.values()) {
      assertEquals(p, Provider.valueOf(p.name()));
    }
  }

  @Test
  void testEnumApiType() {
    for (ApiType a : ApiType.values()) {
      assertEquals(a, ApiType.valueOf(a.name()));
    }
  }

  @Test
  void testDynamoDBMethodsExhaustive() {
    SES_AI_API_USAGE_HISTORY history = new SES_AI_API_USAGE_HISTORY();

    // save() null/empty branches
    history.setProvider(null);
    history.save();

    history.setProvider(Provider.OpenAI);
    history.setModel(null);
    history.save();

    history.setModel("");
    history.save();

    history.setModel("gpt");
    history.setUsageMonth(null);
    history.save();

    history.setUsageMonth("");
    history.save();

    verify(mockTable, never()).putItem(any(SES_AI_API_USAGE_HISTORY.class));

    // fetch()
    history.setProvider(Provider.OpenAI);
    history.setModel("gpt");
    history.setUsageMonth("202501");
    history.setUserId("U1");
    history.setApiType(ApiType.Generate);

    when(mockTable.getItem(any(java.util.function.Consumer.class))).thenReturn(null);
    history.fetch(); // Should do nothing

    SES_AI_API_USAGE_HISTORY result = new SES_AI_API_USAGE_HISTORY();
    result.setProvider(Provider.Google);
    result.setInputCount(new BigDecimal(100));
    when(mockTable.getItem(any(java.util.function.Consumer.class))).thenReturn(result);
    history.fetch();
    assertEquals(Provider.Google, history.getProvider());

    // delete()
    history.delete();
    verify(mockTable).deleteItem(any(software.amazon.awssdk.enhanced.dynamodb.Key.class));
  }

  @Test
  void testAddCounts() {
    SES_AI_API_USAGE_HISTORY history = new SES_AI_API_USAGE_HISTORY();
    // int overloads
    history.addInputCount(10);
    assertEquals(new BigDecimal(10), history.getInputCount());
    history.addOutputCount(20);
    assertEquals(new BigDecimal(20), history.getOutputCount());

    // BigDecimal overloads
    history.addInputCount(new BigDecimal(5));
    assertEquals(new BigDecimal(15), history.getInputCount());
    history.addOutputCount(new BigDecimal(5));
    assertEquals(new BigDecimal(25), history.getOutputCount());

    // Covering toString and other Lombok methods
    assertNotNull(history.toString());
    assertNotNull(history.hashCode());
    assertTrue(history.equals(history));
  }
}
