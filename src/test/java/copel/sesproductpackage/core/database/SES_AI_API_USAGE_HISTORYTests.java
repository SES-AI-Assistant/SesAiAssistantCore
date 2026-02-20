package copel.sesproductpackage.core.database;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import copel.sesproductpackage.core.database.SES_AI_API_USAGE_HISTORY.ApiType;
import copel.sesproductpackage.core.database.SES_AI_API_USAGE_HISTORY.Provider;
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
        history.setUserId("test_user_id");
        history.setApiType(ApiType.Generate);
        history.setInputCount(new BigDecimal(10));
        history.setOutputCount(new BigDecimal(1));
        history.save();

        // Null checks in save
        SES_AI_API_USAGE_HISTORY emptyHistory = new SES_AI_API_USAGE_HISTORY();
        emptyHistory.save(); // Should return immediately
        verify(mockTable, times(1)).putItem(any(SES_AI_API_USAGE_HISTORY.class));
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
        
        when(mockTable.getItem(any(java.util.function.Consumer.class))).thenReturn(history);
        
        history.fetch();
        assertNotNull(history.toString());
    }

    @Test
    void testAddCounts() {
        SES_AI_API_USAGE_HISTORY history = new SES_AI_API_USAGE_HISTORY();
        history.addInputCount(10);
        assertEquals(new BigDecimal(10), history.getInputCount());
        history.addInputCount(new BigDecimal(5));
        assertEquals(new BigDecimal(15), history.getInputCount());

        history.addOutputCount(20);
        assertEquals(new BigDecimal(20), history.getOutputCount());
        history.addOutputCount(new BigDecimal(5));
        assertEquals(new BigDecimal(25), history.getOutputCount());
    }
}
