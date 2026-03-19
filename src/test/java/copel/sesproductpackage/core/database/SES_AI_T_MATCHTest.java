package copel.sesproductpackage.core.database;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import copel.sesproductpackage.core.unit.MatchingStatus;
import copel.sesproductpackage.core.unit.OriginalDateTime;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SES_AI_T_MATCHTest {

  private SES_AI_T_MATCH match;

  @BeforeEach
  void setUp() {
    match = new SES_AI_T_MATCH();
  }

  @Test
  void testAllBranches() throws SQLException {
    Connection conn = mock(Connection.class);
    PreparedStatement ps = mock(PreparedStatement.class);
    ResultSet rs = mock(ResultSet.class);
    when(conn.prepareStatement(anyString())).thenReturn(ps);
    when(ps.executeQuery()).thenReturn(rs);
    when(rs.next()).thenReturn(true, false);

    match.setUserId("user1");
    match.setJobId("job1");
    match.setPersonId("person1");
    match.setJobContent("job content");
    match.setPersonContent("person content");
    match.setStatus(MatchingStatus.提案中);
    match.setRegisterDate(new OriginalDateTime());
    match.setRegisterUser("admin");

    assertTrue(match.hasJobId());
    assertTrue(match.hasPersonId());
    match.setJobId(null);
    match.setPersonId("");
    assertFalse(match.hasJobId());
    assertFalse(match.hasPersonId());

    assertEquals(0, match.insert(null));
    when(ps.executeUpdate()).thenReturn(1);
    match.setStatus(MatchingStatus.提案中);
    assertEquals(1, match.insert(conn));
    match.setStatus(null);
    assertEquals(1, match.insert(conn));

    match.selectByPk(null);
    match.setMatchingId(null);
    match.selectByPk(conn);
    match.setMatchingId("id");
    when(rs.next()).thenReturn(false);
    match.selectByPk(conn);
    when(rs.next()).thenReturn(true);
    when(rs.getString("status_cd")).thenReturn("10");
    match.selectByPk(conn);

    when(ps.executeUpdate()).thenReturn(1);
    match.setMatchingId("id");
    match.setStatus(MatchingStatus.提案中);
    match.setRegisterDate(new OriginalDateTime());
    assertTrue(match.updateByPk(conn));

    match.setStatus(null);
    match.setRegisterDate(new OriginalDateTime());
    assertTrue(match.updateByPk(conn));

    match.setStatus(MatchingStatus.提案中);
    match.setRegisterDate(null);
    assertTrue(match.updateByPk(conn));

    match.setStatus(null);
    match.setRegisterDate(null);
    assertTrue(match.updateByPk(conn));

    assertFalse(match.updateByPk(null));
    match.setMatchingId(null);
    assertFalse(match.updateByPk(conn));
    match.setMatchingId("id");
    when(ps.executeUpdate()).thenReturn(1);
    assertTrue(match.updateByPk(conn));
    when(ps.executeUpdate()).thenReturn(0);
    assertFalse(match.updateByPk(conn));

    assertFalse(match.deleteByPk(null));
    match.setMatchingId(null);
    assertFalse(match.deleteByPk(conn));
    match.setMatchingId("id");
    when(ps.executeUpdate()).thenReturn(1);
    assertTrue(match.deleteByPk(conn));
    when(ps.executeUpdate()).thenReturn(0);
    assertFalse(match.deleteByPk(conn));

    SES_AI_T_MATCH m1 = new SES_AI_T_MATCH();
    SES_AI_T_MATCH m2 = new SES_AI_T_MATCH();
    m1.setMatchingId("id1");
    m2.setMatchingId("id1");
    assertEquals(m1, m1);
    assertEquals(m1, m2);
    assertEquals(m1.hashCode(), m2.hashCode());
    assertNotNull(m1.toString());
    assertNotEquals(m1, null);
    assertNotEquals(m1, new Object());

    m2.setMatchingId("id2");
    assertNotEquals(m1, m2);
    m1.setMatchingId("id2");
    assertEquals(m1, m2);

    m2.setUserId("u1");
    assertNotEquals(m1, m2);
    m1.setUserId("u1");
    assertEquals(m1, m2);

    m2.setRegisterUser("u1");
    assertNotEquals(m1, m2);
    m1.setRegisterUser("u1");
    assertEquals(m1, m2);

    assertTrue(m1.canEqual(m2));
  }
}
