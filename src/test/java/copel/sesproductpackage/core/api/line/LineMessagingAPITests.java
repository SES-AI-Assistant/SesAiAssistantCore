package copel.sesproductpackage.core.api.line;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.lang.reflect.Field;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import copel.sesproductpackage.core.util.Properties;

class LineMessagingAPITests {

    private MockedStatic<HttpClient> mockedHttpClient;
    private HttpClient mockClient;

    @BeforeAll
    @SuppressWarnings("unchecked")
    static void setupProperties() throws Exception {
        Field propertiesField = Properties.class.getDeclaredField("properties");
        propertiesField.setAccessible(true);
        Map<String, String> propertiesMap = (Map<String, String>) propertiesField.get(null);
        propertiesMap.put("LINE_PUSH_MESSAGE_API_ENDPOINT", "https://example.com/push");
        propertiesMap.put("LINE_BROADCAST_API_ENDPOINT", "https://example.com/broadcast");
        propertiesMap.put("LINE_DONLOAD_FILE_API_ENDPOINT", "https://example.com/download/%s");
    }

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
    void testAddMessageAndGetList() {
        LineMessagingAPI api = new LineMessagingAPI("token");
        api.addMessage("hello");
        assertEquals(1, api.getMessageList().size());
        assertEquals("hello", api.getMessageList().get(0));
    }

    @Test
    void testSendSeparate() throws Exception {
        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(200);
        doReturn(mockResponse).when(mockClient).send(any(), any());

        LineMessagingAPI api = new LineMessagingAPI("token");
        api.addMessage("msg1");
        api.sendSeparate("user1");
        verify(mockClient).send(any(), any());
    }

    @Test
    void testBroadCast() throws Exception {
        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(200);
        doReturn(mockResponse).when(mockClient).send(any(), any());

        LineMessagingAPI api = new LineMessagingAPI("token");
        api.addMessage("msg1");
        api.broadCast();
        verify(mockClient).send(any(), any());
    }

    @Test
    void testGetFile() throws Exception {
        byte[] content = "file".getBytes();
        HttpResponse<byte[]> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(200);
        when(mockResponse.body()).thenReturn(content);
        doReturn(mockResponse).when(mockClient).send(any(), any());

        LineMessagingAPI api = new LineMessagingAPI("token");
        byte[] result = api.getFile("mid1");
        assertArrayEquals(content, result);
    }
    
    @Test
    void testGetFileFailure() throws Exception {
        HttpResponse<byte[]> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(404);
        doReturn(mockResponse).when(mockClient).send(any(), any());

        LineMessagingAPI api = new LineMessagingAPI("token");
        byte[] result = api.getFile("mid1");
        assertNull(result);
    }
}
