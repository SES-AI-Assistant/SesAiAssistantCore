package copel.sesproductpackage.core.api.aws;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

class SecretManagerTest {

  @Test
  void testLoadValidSecret() throws Exception {
    String secretJson =
        "{\"host\":\"localhost\",\"port\":\"3306\",\"username\":\"testuser\",\"password\":\"testpass\",\"dbname\":\"testdb\"}";
    GetSecretValueResponse mockResponse = mock(GetSecretValueResponse.class);
    when(mockResponse.secretString()).thenReturn(secretJson);

    SecretsManagerClient mockClient = mock(SecretsManagerClient.class);
    when(mockClient.getSecretValue(any(Consumer.class))).thenReturn(mockResponse);

    SecretManager secretManager = new SecretManager("arn:aws:secretsmanager:region:account:secret:test");
    setClientField(secretManager, mockClient);
    secretManager.load();

    assertTrue(secretManager.isLoaded());
    assertEquals("localhost", secretManager.get("host"));
    assertEquals("3306", secretManager.get("port"));
    assertEquals("testuser", secretManager.get("username"));
    assertEquals("testpass", secretManager.get("password"));
    assertEquals("testdb", secretManager.get("dbname"));
  }

  @Test
  void testLoadEmptySecret() throws Exception {
    GetSecretValueResponse mockResponse = mock(GetSecretValueResponse.class);
    when(mockResponse.secretString()).thenReturn("");

    SecretsManagerClient mockClient = mock(SecretsManagerClient.class);
    when(mockClient.getSecretValue(any(Consumer.class))).thenReturn(mockResponse);

    SecretManager secretManager = new SecretManager("arn:aws:secretsmanager:region:account:secret:test");
    setClientField(secretManager, mockClient);
    secretManager.load();

    assertFalse(secretManager.isLoaded());
    assertEquals("", secretManager.get("nonexistent"));
  }

  @Test
  void testLoadNullSecret() throws Exception {
    GetSecretValueResponse mockResponse = mock(GetSecretValueResponse.class);
    when(mockResponse.secretString()).thenReturn(null);

    SecretsManagerClient mockClient = mock(SecretsManagerClient.class);
    when(mockClient.getSecretValue(any(Consumer.class))).thenReturn(mockResponse);

    SecretManager secretManager = new SecretManager("arn:aws:secretsmanager:region:account:secret:test");
    setClientField(secretManager, mockClient);
    secretManager.load();

    assertFalse(secretManager.isLoaded());
  }

  @Test
  void testGetNonExistentKey() throws Exception {
    String secretJson = "{\"host\":\"localhost\",\"port\":\"3306\"}";
    GetSecretValueResponse mockResponse = mock(GetSecretValueResponse.class);
    when(mockResponse.secretString()).thenReturn(secretJson);

    SecretsManagerClient mockClient = mock(SecretsManagerClient.class);
    when(mockClient.getSecretValue(any(Consumer.class))).thenReturn(mockResponse);

    SecretManager secretManager = new SecretManager("arn:aws:secretsmanager:region:account:secret:test");
    setClientField(secretManager, mockClient);
    secretManager.load();

    assertEquals("", secretManager.get("nonexistent"));
    assertNull(secretManager.getOrNull("nonexistent"));
  }

  @Test
  void testGetAll() throws Exception {
    String secretJson = "{\"host\":\"localhost\",\"port\":\"3306\",\"username\":\"user\"}";
    GetSecretValueResponse mockResponse = mock(GetSecretValueResponse.class);
    when(mockResponse.secretString()).thenReturn(secretJson);

    SecretsManagerClient mockClient = mock(SecretsManagerClient.class);
    when(mockClient.getSecretValue(any(Consumer.class))).thenReturn(mockResponse);

    SecretManager secretManager = new SecretManager("arn:aws:secretsmanager:region:account:secret:test");
    setClientField(secretManager, mockClient);
    secretManager.load();

    Map<String, String> allValues = secretManager.getAll();
    assertEquals(3, allValues.size());
    assertEquals("localhost", allValues.get("host"));
    assertEquals("3306", allValues.get("port"));
    assertEquals("user", allValues.get("username"));
  }

  @Test
  void testLoadException() throws Exception {
    SecretsManagerClient mockClient = mock(SecretsManagerClient.class);
    when(mockClient.getSecretValue(any(Consumer.class))).thenThrow(new RuntimeException("Test error"));

    SecretManager secretManager = new SecretManager("arn:aws:secretsmanager:region:account:secret:test");
    setClientField(secretManager, mockClient);

    assertThrows(RuntimeException.class, secretManager::load);
  }

  @Test
  void testIntegerPortValue() throws Exception {
    String secretJson = "{\"host\":\"localhost\",\"port\":3306,\"username\":\"user\"}";
    GetSecretValueResponse mockResponse = mock(GetSecretValueResponse.class);
    when(mockResponse.secretString()).thenReturn(secretJson);

    SecretsManagerClient mockClient = mock(SecretsManagerClient.class);
    when(mockClient.getSecretValue(any(Consumer.class))).thenReturn(mockResponse);

    SecretManager secretManager = new SecretManager("arn:aws:secretsmanager:region:account:secret:test");
    setClientField(secretManager, mockClient);
    secretManager.load();

    assertEquals("3306", secretManager.get("port"));
  }

  private void setClientField(SecretManager secretManager, SecretsManagerClient mockClient)
      throws NoSuchFieldException, IllegalAccessException {
    Field clientField = SecretManager.class.getDeclaredField("client");
    clientField.setAccessible(true);
    clientField.set(secretManager, mockClient);
  }
}
