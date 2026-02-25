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

    // hasJobId / hasPersonId
    assertTrue(match.hasJobId());
    assertTrue(match.hasPersonId());
    match.setJobId(null);
    match.setPersonId("");
    assertFalse(match.hasJobId());
    assertFalse(match.hasPersonId());

    // insert branches
    assertEquals(0, match.insert(null));
    when(ps.executeUpdate()).thenReturn(1);
    match.setStatus(MatchingStatus.提案中);
    assertEquals(1, match.insert(conn)); // status not null
    match.setStatus(null);
    assertEquals(1, match.insert(conn)); // status null

    // selectByPk branches
    match.selectByPk(null);
    match.setMatchingId(null);
    match.selectByPk(conn);
    match.setMatchingId("id");
    when(rs.next()).thenReturn(false);
    match.selectByPk(conn);
    when(rs.next()).thenReturn(true);
    when(rs.getString("status_cd")).thenReturn("提案中");
    match.selectByPk(conn);

    // updateByPk branches
    when(ps.executeUpdate()).thenReturn(1);
    match.setMatchingId("id");

    // Test all combinations of status and registerDate for updateByPk
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

    // deleteByPk branches
    assertFalse(match.deleteByPk(null));
    match.setMatchingId(null);
    assertFalse(match.deleteByPk(conn));
    match.setMatchingId("id");
    when(ps.executeUpdate()).thenReturn(1);
    assertTrue(match.deleteByPk(conn));
    when(ps.executeUpdate()).thenReturn(0);
    assertFalse(match.deleteByPk(conn));

    // Lombok methods
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
    assertFalse(m1.canEqual(new Object()));

    // Coverage for other fields in equals
    m2.setJobId("j1");
    assertNotEquals(m1, m2);
    m1.setJobId("j1");
    m2.setJobContent("c1");
    assertNotEquals(m1, m2);
    m1.setJobContent("c1");
    m2.setPersonId("p1");
    assertNotEquals(m1, m2);
    m1.setPersonId("p1");
    m2.setPersonContent("pc1");
    assertNotEquals(m1, m2);
    m1.setPersonContent("pc1");
    m2.setStatus(MatchingStatus.提案中);
    assertNotEquals(m1, m2);
    m1.setStatus(MatchingStatus.提案中);
    m2.setRegisterDate(new OriginalDateTime("2024-01-01"));
    assertNotEquals(m1, m2);
    m1.setRegisterDate(new OriginalDateTime("2024-01-01"));
    assertEquals(m1, m2);

    // More equals branches with nulls
    m2.setRegisterDate(null);
    assertNotEquals(m1, m2);
    m1.setRegisterDate(null);
    assertEquals(m1, m2);

    m2.setStatus(null);
    assertNotEquals(m1, m2);
    m1.setStatus(null);
    assertEquals(m1, m2);

    m2.setPersonContent(null);
    assertNotEquals(m1, m2);
    m1.setPersonContent(null);
    assertEquals(m1, m2);

    m2.setPersonId(null);
    assertNotEquals(m1, m2);
    m1.setPersonId(null);
    assertEquals(m1, m2);

    m2.setJobContent(null);
    assertNotEquals(m1, m2);
    m1.setJobContent(null);
    assertEquals(m1, m2);

    m2.setJobId(null);
    assertNotEquals(m1, m2);
    m1.setJobId(null);
    assertEquals(m1, m2);

    m2.setRegisterUser(null);
    assertNotEquals(m1, m2);
    m1.setRegisterUser(null);
    assertEquals(m1, m2);

    m2.setUserId(null);
    assertNotEquals(m1, m2);
    m1.setUserId(null);
    assertEquals(m1, m2);

    m2.setMatchingId(null);
    assertNotEquals(m1, m2);
    m1.setMatchingId(null);
    assertEquals(m1, m2);
  }
}
