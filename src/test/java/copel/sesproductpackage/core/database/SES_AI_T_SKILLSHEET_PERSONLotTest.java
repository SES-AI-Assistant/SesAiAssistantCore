package copel.sesproductpackage.core.database;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import copel.sesproductpackage.core.unit.LogicalOperators;
import copel.sesproductpackage.core.unit.Vector;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SES_AI_T_SKILLSHEET_PERSONLotTest {

  private Connection mockConnection;
  private PreparedStatement mockPreparedStatement;
  private ResultSet mockResultSet;
  private ResultSetMetaData mockResultSetMetaData;
  private Vector mockVector;

  @BeforeEach
  void setUp() throws SQLException {
    mockConnection = mock(Connection.class);
    mockPreparedStatement = mock(PreparedStatement.class);
    mockResultSet = mock(ResultSet.class);
    mockResultSetMetaData = mock(ResultSetMetaData.class);
    mockVector = mock(Vector.class);

    when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
    when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
    when(mockResultSet.getMetaData()).thenReturn(mockResultSetMetaData);

    when(mockVector.toString()).thenReturn("[0.1, 0.2]");
  }

  private void prepareMockResultSet(boolean hasDistance) throws SQLException {
    when(mockResultSet.next()).thenReturn(true, false);
    when(mockResultSet.getString("file_id")).thenReturn("f1");
    when(mockResultSet.getString("person_id")).thenReturn("p1");

    if (hasDistance) {
      when(mockResultSetMetaData.getColumnCount()).thenReturn(2);
      when(mockResultSetMetaData.getColumnLabel(1)).thenReturn("file_id");
      when(mockResultSetMetaData.getColumnLabel(2)).thenReturn("distance");
      when(mockResultSet.getDouble("distance")).thenReturn(0.85);
    } else {
      when(mockResultSetMetaData.getColumnCount()).thenReturn(1);
      when(mockResultSetMetaData.getColumnLabel(1)).thenReturn("file_id");
    }
  }

  @Test
  void testRetrieveByPersonVector() throws SQLException {
    prepareMockResultSet(true);

    SES_AI_T_SKILLSHEET_PERSONLot lot = new SES_AI_T_SKILLSHEET_PERSONLot();
    lot.retrieveByPersonVector(mockConnection, mockVector, 0.5, 10);

    assertEquals(1, lot.size());
    assertEquals("p1", lot.get(0).getPersonId());
    assertEquals(0.85, lot.get(0).getDistance());

    verify(mockPreparedStatement).setString(1, "[0.1, 0.2]");
    verify(mockPreparedStatement).setDouble(3, 0.5);
    verify(mockPreparedStatement).setInt(4, 10);
  }

  @Test
  void testRetrieveByPersonVector_NullConnection() throws SQLException {
    SES_AI_T_SKILLSHEET_PERSONLot lot = new SES_AI_T_SKILLSHEET_PERSONLot();
    lot.retrieveByPersonVector(null, mockVector, 0.5, 10);
    assertEquals(0, lot.size());
  }

  @Test
  void testRetrieveByPersonVector_NullQuery() throws SQLException {
    SES_AI_T_SKILLSHEET_PERSONLot lot = new SES_AI_T_SKILLSHEET_PERSONLot();
    lot.retrieveByPersonVector(mockConnection, null, 0.5, 10);
    assertEquals(0, lot.size());
  }

  @Test
  void testRetrieveBySkillSheetVector() throws SQLException {
    prepareMockResultSet(true);

    SES_AI_T_SKILLSHEET_PERSONLot lot = new SES_AI_T_SKILLSHEET_PERSONLot();
    lot.retrieveBySkillSheetVector(mockConnection, mockVector, 0.6, 5);

    assertEquals(1, lot.size());
    assertEquals("p1", lot.get(0).getPersonId());
    verify(mockPreparedStatement).setDouble(3, 0.6);
    verify(mockPreparedStatement).setInt(4, 5);
  }

  @Test
  void testRetrieveByPersonRawContent_Single() throws SQLException {
    prepareMockResultSet(false);

    SES_AI_T_SKILLSHEET_PERSONLot lot = new SES_AI_T_SKILLSHEET_PERSONLot();
    lot.retrieveByPersonRawContent(mockConnection, "keyword");

    assertEquals(1, lot.size());
    assertEquals("p1", lot.get(0).getPersonId());
    verify(mockPreparedStatement).setString(1, "%keyword%");
  }

  @Test
  void testRetrieveByPersonRawContent_Multiple() throws SQLException {
    prepareMockResultSet(false);

    SES_AI_T_SKILLSHEET_PERSONLot lot = new SES_AI_T_SKILLSHEET_PERSONLot();
    lot.retrieveByPersonRawContent(
        mockConnection, "k1", List.of(new LogicalOperators(LogicalOperators.論理演算子.AND, "k2")));

    assertEquals(1, lot.size());
    verify(mockPreparedStatement).setString(1, "%k1%");
    verify(mockPreparedStatement).setString(2, "%k2%");
  }

  @Test
  void testRetrieveBySkillSheetRawContent_Single() throws SQLException {
    prepareMockResultSet(false);

    SES_AI_T_SKILLSHEET_PERSONLot lot = new SES_AI_T_SKILLSHEET_PERSONLot();
    lot.retrieveBySkillSheetRawContent(mockConnection, "skill");

    assertEquals(1, lot.size());
    verify(mockPreparedStatement).setString(1, "%skill%");
  }

  @Test
  void testRetrieveBySkillSheetRawContent_Multiple() throws SQLException {
    prepareMockResultSet(false);

    SES_AI_T_SKILLSHEET_PERSONLot lot = new SES_AI_T_SKILLSHEET_PERSONLot();
    lot.retrieveBySkillSheetRawContent(
        mockConnection, "s1", List.of(new LogicalOperators(LogicalOperators.論理演算子.OR, "s2")));

    assertEquals(1, lot.size());
    verify(mockPreparedStatement).setString(1, "%s1%");
    verify(mockPreparedStatement).setString(2, "%s2%");
  }

  @Test
  void testSelectAll() throws SQLException {
    SES_AI_T_SKILLSHEET_PERSONLot lot = new SES_AI_T_SKILLSHEET_PERSONLot();
    assertDoesNotThrow(() -> lot.selectAll(mockConnection));
  }

  @Test
  void testGetEntityByPk() throws SQLException {
    prepareMockResultSet(false);

    SES_AI_T_SKILLSHEET_PERSONLot lot = new SES_AI_T_SKILLSHEET_PERSONLot();
    lot.retrieveByPersonRawContent(mockConnection, "keyword"); // loads "p1" into the lot

    org.junit.jupiter.api.Assertions.assertNotNull(lot.getEntityByPk("p1"));
    org.junit.jupiter.api.Assertions.assertNotNull(lot.getEntityByPk(" p1 "));
    org.junit.jupiter.api.Assertions.assertNull(lot.getEntityByPk("p2"));
    org.junit.jupiter.api.Assertions.assertNull(lot.getEntityByPk(null));

    SES_AI_T_SKILLSHEET_PERSONLot emptyLot = new SES_AI_T_SKILLSHEET_PERSONLot();
    org.junit.jupiter.api.Assertions.assertNull(emptyLot.getEntityByPk("p1"));
  }

  @Test
  void testTo要員選出用文章() throws SQLException {
    prepareMockResultSet(false);

    SES_AI_T_SKILLSHEET_PERSONLot lot = new SES_AI_T_SKILLSHEET_PERSONLot();
    lot.retrieveByPersonRawContent(mockConnection, "keyword"); // loads "p1"

    lot.get(0).setContentSummary("cs");
    lot.get(0).setFileContentSummary("fs");

    String result = lot.to要員選出用文章();
    assertEquals("1人目：要員ID：p1 内容：csfs\n", result);

    SES_AI_T_SKILLSHEET_PERSONLot emptyLot = new SES_AI_T_SKILLSHEET_PERSONLot();
    assertEquals("", emptyLot.to要員選出用文章());
  }
}
