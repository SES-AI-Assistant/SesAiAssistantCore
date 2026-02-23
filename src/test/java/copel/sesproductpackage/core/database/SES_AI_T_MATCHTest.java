package copel.sesproductpackage.core.database;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import copel.sesproductpackage.core.unit.MatchingStatus;
import copel.sesproductpackage.core.unit.OriginalDateTime;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.junit.jupiter.api.Test;

class SES_AI_T_MATCHTest {

  @Test
  void testAllBranches() throws Exception {
    Connection conn = mock(Connection.class);
    PreparedStatement ps = mock(PreparedStatement.class);
    ResultSet rs = mock(ResultSet.class);
    when(conn.prepareStatement(anyString())).thenReturn(ps);
    when(ps.executeQuery()).thenReturn(rs);
    when(ps.executeUpdate()).thenReturn(1);
    when(rs.next()).thenReturn(true, false);
    when(rs.getString(anyString())).thenReturn("val");

    SES_AI_T_MATCH match = new SES_AI_T_MATCH();
    match.setJobId("j1");
    match.setPersonId("p1");
    assertTrue(match.hasJobId());
    assertTrue(match.hasPersonId());

    match.setJobId(null);
    assertFalse(match.hasJobId());
    match.setPersonId(null);
    assertFalse(match.hasPersonId());

    // insert
    match.insert(null);
    match.setStatus(MatchingStatus.提案中);
    match.insert(conn);
    match.setStatus(null);
    match.insert(conn);

    // selectByPk
    match.selectByPk(null);
    match.setMatchingId(null); match.selectByPk(conn);
    match.setMatchingId("id");
    when(rs.next()).thenReturn(true);
    match.selectByPk(conn);

    // updateByPk
    match.updateByPk(null);
    match.setMatchingId(null); match.updateByPk(conn);
    match.setMatchingId("id");
    match.setRegisterDate(new OriginalDateTime());
    match.updateByPk(conn);
    match.setRegisterDate(null);
    match.updateByPk(conn);

    // deleteByPk
    match.deleteByPk(null);
    match.setMatchingId(null); match.deleteByPk(conn);
    match.setMatchingId("id");
    match.deleteByPk(conn);
  }
}
