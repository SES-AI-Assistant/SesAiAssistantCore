package copel.sesproductpackage.core.api.slack;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.net.http.HttpClient;
import java.net.http.HttpResponse;

import org.apache.http.HttpException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import copel.sesproductpackage.core.api.slack.SlackWebhookMessageEntity.AccessoryType;
import copel.sesproductpackage.core.api.slack.SlackWebhookMessageEntity.BlockType;
import copel.sesproductpackage.core.api.slack.SlackWebhookMessageEntity.ImageAccessory;
import copel.sesproductpackage.core.api.slack.SlackWebhookMessageEntity.SlackMessageBlock;
import copel.sesproductpackage.core.api.slack.SlackWebhookMessageEntity.TextObject;
import copel.sesproductpackage.core.api.slack.SlackWebhookMessageEntity.TextType;

class SlackNotifierTest {

    private MockedStatic<HttpClient> mockedHttpClient;
    private HttpClient mockClient;

    @BeforeEach
    void setUp() {
        mockedHttpClient = mockStatic(HttpClient.class);
        mockClient = mock(HttpClient.class);
        mockedHttpClient.when(HttpClient::newHttpClient).thenReturn(mockClient);
    }

    @AfterEach
    void tearDown() {
        mockedHttpClient.close();
    }

    @Test
    void sendSuccessTest() throws Exception {
        String jsonRes = "{\"ok\": true, \"ts\": \"12345.678\"}";
        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(200);
        when(mockResponse.body()).thenReturn(jsonRes);
        doReturn(mockResponse).when(mockClient).send(any(), any());

        SlackWebhookMessageEntity entity = new SlackWebhookMessageEntity();
        String ts = SlackNotifier.send("token", entity);
        assertEquals("12345.678", ts);
    }

    @Test
    void sendFailureStatusTest() throws Exception {
        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(500);
        doReturn(mockResponse).when(mockClient).send(any(), any());

        SlackWebhookMessageEntity entity = new SlackWebhookMessageEntity();
        assertThrows(HttpException.class, () -> SlackNotifier.send("token", entity));
    }

    @Test
    void sendFailureOkFalseTest() throws Exception {
        String jsonRes = "{\"ok\": false, \"error\": \"invalid_auth\"}";
        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(200);
        when(mockResponse.body()).thenReturn(jsonRes);
        doReturn(mockResponse).when(mockClient).send(any(), any());

        SlackWebhookMessageEntity entity = new SlackWebhookMessageEntity();
        assertThrows(HttpException.class, () -> SlackNotifier.send("token", entity));
    }

    @Test
    void sendByWebhookSuccessTest() throws Exception {
        HttpResponse<Void> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(200);
        doReturn(mockResponse).when(mockClient).send(any(), any());

        SlackWebhookMessageEntity entity = new SlackWebhookMessageEntity();
        SlackNotifier.sendByWebhook("http://webhook", entity);
        verify(mockClient).send(any(), any());
    }

    @Test
    void sendByWebhookFailureTest() throws Exception {
        HttpResponse<Void> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(400);
        doReturn(mockResponse).when(mockClient).send(any(), any());

        SlackWebhookMessageEntity entity = new SlackWebhookMessageEntity();
        assertThrows(HttpException.class, () -> SlackNotifier.sendByWebhook("http://webhook", entity));
    }

    @Test
    void testMessageEntityBuilder() throws Exception {
        SlackWebhookMessageEntity entity = SlackWebhookMessageEntity.builder()
                .channel("C123")
                .text("hello")
                .threadTs("111.222")
                .build();
        
        entity.addBlock(SlackMessageBlock.builder()
                .type(BlockType.HEADER)
                .text(TextObject.builder().type(TextType.PLAIN_TEXT).text("header").build())
                .build());
        
        assertNotNull(entity.toJson());
        assertTrue(entity.toJson().contains("C123"));
    }

    @Test
    void testTextObjectUtils() {
        assertEquals("<https://example.com | label>", TextObject.リンクテキスト("label", "https://example.com"));
        assertEquals("```code```", TextObject.コードスニペット("code").getText());
        
        TextObject obj = TextObject.builder().type(TextType.MRKDWN).text("test").build();
        assertEquals(TextType.MRKDWN, obj.getType());
        assertEquals("test", obj.getText());
    }
}
