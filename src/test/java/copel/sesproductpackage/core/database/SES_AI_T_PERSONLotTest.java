package copel.sesproductpackage.core.database;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import copel.sesproductpackage.core.database.base.EntityLotBase;
import copel.sesproductpackage.core.unit.LogicalOperators;
import copel.sesproductpackage.core.unit.LogicalOperators.論理演算子;
import copel.sesproductpackage.core.unit.OriginalDateTime;
import copel.sesproductpackage.core.unit.Vector;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SES_AI_T_PERSONLotTest {

  private Connection mockConn;
  private PreparedStatement mockStmt;
  private ResultSet mockRs;

  @BeforeEach
  void setUp() throws SQLException {
    mockConn = mock(Connection.class);
    mockStmt = mock(PreparedStatement.class);
    mockRs = mock(ResultSet.class);

    when(mockConn.prepareStatement(anyString())).thenReturn(mockStmt);
    when(mockStmt.executeQuery()).thenReturn(mockRs);
  }

  private void setupDefaultResultSet() throws SQLException {
    when(mockRs.next()).thenReturn(true).thenReturn(false);
    when(mockRs.getString("person_id")).thenReturn("pid1");
    when(mockRs.getString("from_group")).thenReturn("fg1");
    when(mockRs.getString("from_id")).thenReturn("fid1");
    when(mockRs.getString("from_name")).thenReturn("fname1");
    when(mockRs.getString("file_id")).thenReturn("file1");
    when(mockRs.getString("raw_content")).thenReturn("raw1");
    when(mockRs.getString("content_summary")).thenReturn("summary1");
    when(mockRs.getString("register_date")).thenReturn("2023-01-01 12:00:00");
    when(mockRs.getString("register_user")).thenReturn("user1");
    when(mockRs.getString("ttl")).thenReturn("2024-01-01 12:00:00");
    when(mockRs.getDouble("distance")).thenReturn(0.5);
  }

  private Vector createTestVector() throws Exception {
    Vector vector = new Vector(null);
    Field valueField = Vector.class.getDeclaredField("value");
    valueField.setAccessible(true);
    valueField.set(vector, new float[] {1.0f, 2.0f});
    return vector;
  }

  @Test
  void testSelectAll() throws SQLException {
    setupDefaultResultSet();
    SES_AI_T_PERSONLot lot = new SES_AI_T_PERSONLot();
    lot.selectAll(mockConn);
    assertEquals(1, lot.size());
    assertDoesNotThrow(() -> new SES_AI_T_PERSONLot().selectAll(null));
  }

  @Test
  void testGetSelectSqlAndGetSelectLikeSql() throws Exception {
    SES_AI_T_PERSONLot lot = new SES_AI_T_PERSONLot();
    Method getSelectSql = EntityLotBase.class.getDeclaredMethod("getSelectSql");
    getSelectSql.setAccessible(true);
    Method getSelectLikeSql = EntityLotBase.class.getDeclaredMethod("getSelectLikeSql");
    getSelectLikeSql.setAccessible(true);
    assertNotNull(getSelectSql.invoke(lot));
    assertNotNull(getSelectLikeSql.invoke(lot));
  }

  @Test
  void testMapResultSetWithNullDates() throws SQLException {
    when(mockRs.next()).thenReturn(true, false);
    when(mockRs.getString("person_id")).thenReturn("pid1");
    when(mockRs.getString("from_group")).thenReturn("fg1");
    when(mockRs.getString("from_id")).thenReturn("fid1");
    when(mockRs.getString("from_name")).thenReturn("fname1");
    when(mockRs.getString("file_id")).thenReturn("file1");
    when(mockRs.getString("raw_content")).thenReturn("raw1");
    when(mockRs.getString("content_summary")).thenReturn("s1");
    when(mockRs.getString("register_date")).thenReturn(null);
    when(mockRs.getString("register_user")).thenReturn("user1");
    when(mockRs.getString("ttl")).thenReturn(null);
    SES_AI_T_PERSONLot lot = new SES_AI_T_PERSONLot();
    lot.selectAll(mockConn);
    assertEquals(1, lot.size());
    assertNotNull(lot.get(0).getPersonId());
  }

  @Test
  void testGetEntityByPk() throws SQLException {
    setupDefaultResultSet();
    SES_AI_T_PERSONLot lot = new SES_AI_T_PERSONLot();
    lot.selectAll(mockConn);

    assertNotNull(lot.getEntityByPk("pid1"));
    assertNotNull(lot.getEntityByPk("  pid1  "));
    assertNull(lot.getEntityByPk("nonexistent"));
    assertNull(lot.getEntityByPk(null));

    SES_AI_T_PERSONLot emptyLot = new SES_AI_T_PERSONLot();
    assertNull(emptyLot.getEntityByPk("anyId"));

    when(mockRs.next()).thenReturn(true, false);
    when(mockRs.getString("person_id")).thenReturn("emptyId");
    when(mockRs.getString("from_group")).thenReturn("g");
    when(mockRs.getString("from_id")).thenReturn("i");
    when(mockRs.getString("from_name")).thenReturn("n");
    when(mockRs.getString("file_id")).thenReturn("f");
    when(mockRs.getString("raw_content")).thenReturn("r");
    when(mockRs.getString("content_summary")).thenReturn("s");
    when(mockRs.getString("register_date")).thenReturn("2023-01-01 12:00:00");
    when(mockRs.getString("register_user")).thenReturn("u");
    when(mockRs.getString("ttl")).thenReturn("2024-01-01 12:00:00");
    SES_AI_T_PERSONLot lotMatch = new SES_AI_T_PERSONLot();
    lotMatch.selectAll(mockConn);
    assertNotNull(lotMatch.getEntityByPk("emptyId"));
    assertEquals("emptyId", lotMatch.getEntityByPk("  emptyId  ").getPersonId());
  }

  @Test
  void testGetEntityByPkSecondElement() throws SQLException {
    when(mockRs.next()).thenReturn(true, true, false);
    when(mockRs.getString("person_id")).thenReturn("pid1", "pid2");
    when(mockRs.getString("from_group")).thenReturn("fg1", "fg2");
    when(mockRs.getString("from_id")).thenReturn("fid1", "fid2");
    when(mockRs.getString("from_name")).thenReturn("fname1", "fname2");
    when(mockRs.getString("file_id")).thenReturn("file1", "file2");
    when(mockRs.getString("raw_content")).thenReturn("raw1", "raw2");
    when(mockRs.getString("content_summary")).thenReturn("s1", "s2");
    when(mockRs.getString("register_date")).thenReturn("2023-01-01 12:00:00");
    when(mockRs.getString("register_user")).thenReturn("user1", "user2");
    when(mockRs.getString("ttl")).thenReturn("2024-01-01 12:00:00");
    SES_AI_T_PERSONLot lot = new SES_AI_T_PERSONLot();
    lot.selectAll(mockConn);
    assertEquals(2, lot.size());
    assertNotNull(lot.getEntityByPk("pid2"));
    assertEquals("pid2", lot.getEntityByPk("pid2").getPersonId());
  }

  @Test
  void testIsExistByFileId() throws SQLException {
    setupDefaultResultSet();
    SES_AI_T_PERSONLot lot = new SES_AI_T_PERSONLot();
    lot.selectAll(mockConn);

    assertTrue(lot.isExistByFileId("file1"));
    assertFalse(lot.isExistByFileId("nonexistent"));
    assertFalse(lot.isExistByFileId(null));

    SES_AI_T_PERSONLot emptyLot = new SES_AI_T_PERSONLot();
    assertFalse(emptyLot.isExistByFileId("any"));
  }

  @Test
  void testRetrieve() throws Exception {
    setupDefaultResultSet();
    // retrievePaged は先に COUNT(*) し、続けて本検索するため 2 回 executeQuery する
    when(mockRs.getLong(1)).thenReturn(1L);
    when(mockRs.next()).thenReturn(true, true, false);
    SES_AI_T_PERSONLot lot = new SES_AI_T_PERSONLot();
    lot.retrieve(mockConn, createTestVector(), 10);
    assertEquals(1, lot.size());
    assertEquals(0.5, lot.get(0).getDistance());

    SES_AI_T_PERSONLot lotForNull = new SES_AI_T_PERSONLot();
    lotForNull.retrieve(null, null, 0);
    assertTrue(lotForNull.isEmpty());

    when(mockRs.next()).thenReturn(false);
    SES_AI_T_PERSONLot lotEmpty = new SES_AI_T_PERSONLot();
    lotEmpty.retrieve(mockConn, createTestVector(), 10);
    assertTrue(lotEmpty.isEmpty());

    setupDefaultResultSet();
    when(mockRs.getLong(1)).thenReturn(1L);
    when(mockRs.next()).thenReturn(true, true, false);
    lot.retrieve(mockConn, null, 1);
    verify(mockStmt).setString(1, null);
  }

  @Test
  void testSearchByRawContentSingle() throws SQLException {
    setupDefaultResultSet();
    SES_AI_T_PERSONLot lot = new SES_AI_T_PERSONLot();
    lot.searchByRawContent(mockConn, "query");
    assertEquals(1, lot.size());
  }

  @Test
  void testSearchByRawContentMultiple() throws SQLException {
    setupDefaultResultSet();
    SES_AI_T_PERSONLot lot = new SES_AI_T_PERSONLot();
    List<LogicalOperators> queries = new ArrayList<>();
    queries.add(new LogicalOperators(論理演算子.AND, "val1"));
    queries.add(new LogicalOperators(論理演算子.OR, "val2"));
    lot.searchByRawContent(mockConn, "first", queries);
    assertEquals(1, lot.size());
  }

  @Test
  void testSelectByAndQuery() throws SQLException {
    setupDefaultResultSet();
    SES_AI_T_PERSONLot lot = new SES_AI_T_PERSONLot();
    Map<String, String> query = new HashMap<>();
    query.put("col1", "val1");
    query.put("col2", "val2");
    lot.selectByAndQuery(mockConn, query);
    assertEquals(1, lot.size());
  }

  @Test
  void testSelectByOrQuery() throws SQLException {
    setupDefaultResultSet();
    SES_AI_T_PERSONLot lot = new SES_AI_T_PERSONLot();
    Map<String, String> query = new HashMap<>();
    query.put("col1", "val1");
    query.put("col2", "val2");
    lot.selectByOrQuery(mockConn, query);
    assertEquals(1, lot.size());
  }

  @Test
  void testSelectByRegisterDateAfter() throws SQLException {
    setupDefaultResultSet();
    SES_AI_T_PERSONLot lot = new SES_AI_T_PERSONLot();
    lot.selectByRegisterDateAfter(mockConn, new OriginalDateTime());
    assertEquals(1, lot.size());

    SES_AI_T_PERSONLot lot2 = new SES_AI_T_PERSONLot();
    setupDefaultResultSet();
    lot2.selectByRegisterDateAfter(mockConn, null);
    assertEquals(1, lot2.size());
  }

  @Test
  void testToPersonSelectionText() throws SQLException {
    setupDefaultResultSet();
    SES_AI_T_PERSONLot lot = new SES_AI_T_PERSONLot();
    lot.selectAll(mockConn);

    assertFalse(lot.to要員選出用文章().isEmpty());

    SES_AI_T_PERSONLot emptyLot = new SES_AI_T_PERSONLot();
    assertTrue(emptyLot.to要員選出用文章().isEmpty());
    assertEquals("", emptyLot.toString());

    when(mockRs.next()).thenReturn(true, true, false);
    when(mockRs.getString("person_id")).thenReturn("p1", "p2");
    when(mockRs.getString("from_group")).thenReturn("g1", "g2");
    when(mockRs.getString("from_id")).thenReturn("i1", "i2");
    when(mockRs.getString("from_name")).thenReturn("n1", "n2");
    when(mockRs.getString("file_id")).thenReturn("f1", "f2");
    when(mockRs.getString("raw_content")).thenReturn("r1", "r2");
    when(mockRs.getString("content_summary")).thenReturn("s1", "s2");
    when(mockRs.getString("register_date")).thenReturn("2023-01-01 12:00:00");
    when(mockRs.getString("register_user")).thenReturn("u1", "u2");
    when(mockRs.getString("ttl")).thenReturn("2024-01-01 12:00:00");
    SES_AI_T_PERSONLot lotTwo = new SES_AI_T_PERSONLot();
    lotTwo.selectAll(mockConn);
    String text = lotTwo.to要員選出用文章();
    assertTrue(text.contains("1人目："));
    assertTrue(text.contains("2人目："));
    assertFalse(lotTwo.toString().isEmpty());
  }

  @Test
  void testSearchByRawContentMultipleVariations() throws SQLException {
    setupDefaultResultSet();
    SES_AI_T_PERSONLot lot = new SES_AI_T_PERSONLot();
    List<LogicalOperators> queries = new ArrayList<>();
    queries.add(null);
    lot.searchByRawContent(mockConn, "first", queries);
    assertEquals(1, lot.size());

    setupDefaultResultSet();
    SES_AI_T_PERSONLot lot2 = new SES_AI_T_PERSONLot();
    lot2.searchByRawContent(mockConn, "first", null);
    assertEquals(1, lot2.size());
  }
}
