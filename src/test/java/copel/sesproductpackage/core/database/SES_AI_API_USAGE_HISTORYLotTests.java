package copel.sesproductpackage.core.database;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import copel.sesproductpackage.core.database.SES_AI_API_USAGE_HISTORY.Provider;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.Page;
import software.amazon.awssdk.enhanced.dynamodb.model.PageIterable;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClientBuilder;

class SES_AI_API_USAGE_HISTORYLotTests {

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
    void fetchByProviderTest() {
        PageIterable<SES_AI_API_USAGE_HISTORY> mockIterable = mock(PageIterable.class);
        SES_AI_API_USAGE_HISTORY history = new SES_AI_API_USAGE_HISTORY();
        history.setInputCount(new BigDecimal(100));
        history.setOutputCount(new BigDecimal(0));
        
        software.amazon.awssdk.core.pagination.sync.SdkIterable<SES_AI_API_USAGE_HISTORY> mockItems = mock(software.amazon.awssdk.core.pagination.sync.SdkIterable.class);
        doAnswer(invocation -> {
            java.util.function.Consumer<SES_AI_API_USAGE_HISTORY> consumer = invocation.getArgument(0);
            consumer.accept(history);
            return null;
        }).when(mockItems).forEach(any(java.util.function.Consumer.class));
        
        when(mockIterable.items()).thenReturn(mockItems);
        when(mockTable.scan(any(software.amazon.awssdk.enhanced.dynamodb.model.ScanEnhancedRequest.class))).thenReturn(mockIterable);

        SES_AI_API_USAGE_HISTORYLot entityLot = new SES_AI_API_USAGE_HISTORYLot();
        entityLot.fetchByProvider(Provider.OpenAI);
        
        assertEquals(new BigDecimal(100), entityLot.getSumInputCount());
        assertEquals(new BigDecimal(0), entityLot.getSumOutputCount());
        assertNotNull(entityLot.toString());
    }

    @Test
    void fetchByPkTest() {
        PageIterable<SES_AI_API_USAGE_HISTORY> mockIterable = mock(PageIterable.class);
        software.amazon.awssdk.core.pagination.sync.SdkIterable<SES_AI_API_USAGE_HISTORY> mockItems = mock(software.amazon.awssdk.core.pagination.sync.SdkIterable.class);
        // forEach は何もしないデフォルト挙動
        when(mockIterable.items()).thenReturn(mockItems);
        when(mockTable.query(any(QueryConditional.class))).thenReturn(mockIterable);

        SES_AI_API_USAGE_HISTORYLot entityLot = new SES_AI_API_USAGE_HISTORYLot();
        entityLot.fetchByPk("partitionKey");
        verify(mockTable).query(any(QueryConditional.class));
    }
}
