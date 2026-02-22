package copel.sesproductpackage.core.util;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

class DBConnectionTests {

  @Test
  void testConstructor() {
    assertNotNull(new DBConnection());
  }

  @Test
  void testGetConnection() throws Exception {
    // Properties の初期化
    Field propertiesField = Properties.class.getDeclaredField("properties");
    propertiesField.setAccessible(true);
    Map<String, String> propertiesMap = (Map<String, String>) propertiesField.get(null);
    propertiesMap.put("SES_DB_ENDPOINT_URL", "jdbc:postgresql://localhost/test");
    propertiesMap.put("SES_DB_USER_NAME", "user");
    propertiesMap.put("SES_DB_USER_PASSWORD", "pass");

    try (MockedStatic<DriverManager> mockedDriverManager = mockStatic(DriverManager.class)) {
      Connection mockConn = mock(Connection.class);
      mockedDriverManager
          .when(() -> DriverManager.getConnection(anyString(), anyString(), anyString()))
          .thenReturn(mockConn);

      Connection conn = DBConnection.getConnection();
      assertNotNull(conn);
      verify(mockConn).setAutoCommit(false);
    }
  }
}
