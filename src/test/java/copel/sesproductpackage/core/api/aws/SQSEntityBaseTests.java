package copel.sesproductpackage.core.api.aws;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.amazonaws.services.sqs.model.SendMessageResult;

class SQSEntityBaseTests {

    private MockedStatic<AmazonSQSClientBuilder> mockedBuilder;
    private AmazonSQS mockSqs;

    static class TestSQSEntity extends SQSEntityBase {
        public TestSQSEntity(Regions region, String queueUrl) { super(region, queueUrl); }
        @Override protected String getMessageBody() { return "{\"test\":\"body\"}"; }
    }

    @BeforeEach
    void setUp() {
        mockedBuilder = mockStatic(AmazonSQSClientBuilder.class);
        mockSqs = mock(AmazonSQS.class);
        AmazonSQSClientBuilder mockB = mock(AmazonSQSClientBuilder.class);
        when(mockB.withRegion(any(Regions.class))).thenReturn(mockB);
        when(mockB.build()).thenReturn(mockSqs);
        mockedBuilder.when(AmazonSQSClientBuilder::standard).thenReturn(mockB);
    }

    @AfterEach
    void tearDown() {
        mockedBuilder.close();
    }

    @Test
    void testSendMessage() throws Exception {
        TestSQSEntity entity = new TestSQSEntity(Regions.AP_NORTHEAST_1, "http://sqs");
        
        SendMessageResult mockRes = new SendMessageResult().withMessageId("mid123");
        when(mockSqs.sendMessage(any(SendMessageRequest.class))).thenReturn(mockRes);
        
        SendMessageResult result = entity.sendMessage();
        assertEquals("mid123", result.getMessageId());
    }
}
