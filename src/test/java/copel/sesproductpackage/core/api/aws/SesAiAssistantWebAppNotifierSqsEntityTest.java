package copel.sesproductpackage.core.api.aws;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.amazonaws.services.sqs.model.SendMessageResult;
import copel.sesproductpackage.core.api.SesAiAssistantWebAppNotifierRequestEntity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

class SesAiAssistantWebAppNotifierSqsEntityTest {

  private MockedStatic<AmazonSQSClientBuilder> mockedBuilder;
  private AmazonSQS mockSqs;

  @BeforeEach
  void setUp() {
    mockedBuilder = mockStatic(AmazonSQSClientBuilder.class);
    mockSqs = mock(AmazonSQS.class);
    AmazonSQSClientBuilder mockB = mock(AmazonSQSClientBuilder.class);
    lenient().when(mockB.withRegion(any(Regions.class))).thenReturn(mockB);
    lenient().when(mockB.withEndpointConfiguration(any())).thenReturn(mockB);
    lenient().when(mockB.withCredentials(any())).thenReturn(mockB);
    when(mockB.build()).thenReturn(mockSqs);
    mockedBuilder.when(AmazonSQSClientBuilder::standard).thenReturn(mockB);
  }

  @AfterEach
  void tearDown() {
    mockedBuilder.close();
  }

  @Test
  void testGetMessageBody() throws Exception {
    SesAiAssistantWebAppNotifierRequestEntity request =
        new SesAiAssistantWebAppNotifierRequestEntity(
            "user1", "test title", "test body", "/icon.png", "/badge.png", "test-tag");
    SesAiAssistantWebAppNotifierSqsEntity entity =
        new SesAiAssistantWebAppNotifierSqsEntity("http://sqs/test", request);

    String json = entity.getMessageBody();
    assertTrue(json.contains("\"user_id\":\"user1\""));
    assertTrue(json.contains("\"title\":\"test title\""));
    assertTrue(json.contains("\"body\":\"test body\""));
    assertTrue(json.contains("\"icon\":\"/icon.png\""));
    assertTrue(json.contains("\"badge\":\"/badge.png\""));
    assertTrue(json.contains("\"tag\":\"test-tag\""));
  }

  @Test
  void testSendMessage() throws Exception {
    SesAiAssistantWebAppNotifierRequestEntity request =
        new SesAiAssistantWebAppNotifierRequestEntity(
            "user1", "title", "body", "/icon.png", "/badge.png", "tag");
    SesAiAssistantWebAppNotifierSqsEntity entity =
        new SesAiAssistantWebAppNotifierSqsEntity("http://sqs/test", request);

    SendMessageResult mockRes = new SendMessageResult().withMessageId("msg123");
    when(mockSqs.sendMessage(any(SendMessageRequest.class))).thenReturn(mockRes);

    SendMessageResult result = entity.sendMessage();
    assertTrue(result.getMessageId().equals("msg123"));
  }
}
