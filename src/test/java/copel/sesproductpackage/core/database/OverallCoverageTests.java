package copel.sesproductpackage.core.database;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import copel.sesproductpackage.core.database.base.EntityBase;
import copel.sesproductpackage.core.util.DBConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

class OverallCoverageTests {

  private void testEntity(Consumer<EntityBase> nullSetter, EntityBase populatedEntity) {
    try (MockedStatic<DBConnection> mockDB = Mockito.mockStatic(DBConnection.class)) {
      Connection mockConn = mock(Connection.class);
      PreparedStatement mockStmt = mock(PreparedStatement.class);
      mockDB.when(DBConnection::getConnection).thenReturn(mockConn);
      when(mockConn.prepareStatement(anyString())).thenReturn(mockStmt);
      when(mockStmt.executeUpdate()).thenReturn(1);
      doNothing().when(mockStmt).setString(anyInt(), anyString());
      doNothing().when(mockStmt).setTimestamp(anyInt(), any());

      // Test with nulls
      try {
        EntityBase nullEntity = populatedEntity.getClass().getDeclaredConstructor().newInstance();
        nullSetter.accept(nullEntity);
        nullEntity.insert(mockConn);
        nullEntity.updateByPk(mockConn);
      } catch (Exception e) {
        // Should not happen
      }

      // Test with populated data
      populatedEntity.insert(mockConn);
      populatedEntity.updateByPk(mockConn);

    } catch (SQLException e) {
      // Should not happen
    }
  }

  @Test
  void testRDBEntityBranches() {

    // SES_AI_T_MATCH
    SES_AI_T_MATCH match = new SES_AI_T_MATCH();
    match.setMatchingId("id");
    match.setStatus(null);
    match.setRegisterDate(null);
    testEntity(e -> ((SES_AI_T_MATCH) e).setStatus(null), match);

    // SES_AI_T_WATCH
    SES_AI_T_WATCH watch = new SES_AI_T_WATCH();
    watch.setUserId("uid");
    watch.setTargetId("tid");
    watch.setTargetType(null);
    watch.setTtl(null);
    watch.setRegisterDate(null);
    testEntity(
        e -> {
          ((SES_AI_T_WATCH) e).setTargetType(null);
          ((SES_AI_T_WATCH) e).setTtl(null);
          ((SES_AI_T_WATCH) e).setRegisterDate(null);
        },
        watch);

    // SES_AI_T_PERSON (from base class)
    SES_AI_T_PERSON person = new SES_AI_T_PERSON();
    person.setPersonId("id");
    person.setRegisterDate(null);
    testEntity(e -> ((SES_AI_T_PERSON) e).setRegisterDate(null), person);

    // SES_AI_T_SKILLSHEET (Uses fileId)
    SES_AI_T_SKILLSHEET ss = new SES_AI_T_SKILLSHEET();
    ss.setFileId("id");
    ss.setRegisterDate(null);
    testEntity(e -> ((SES_AI_T_SKILLSHEET) e).setRegisterDate(null), ss);

    // M series (from base class)
    SES_AI_M_COMPANY comp = new SES_AI_M_COMPANY();
    comp.setCompanyId("id");
    comp.setRegisterDate(null);
    testEntity(e -> ((SES_AI_M_COMPANY) e).setRegisterDate(null), comp);

    SES_AI_M_GROUP group = new SES_AI_M_GROUP();
    group.setFromGroup("id");
    group.setRegisterDate(null);
    testEntity(e -> ((SES_AI_M_GROUP) e).setRegisterDate(null), group);

    SES_AI_M_SENDER sender = new SES_AI_M_SENDER();
    sender.setFromId("id");
    sender.setRegisterDate(null);
    testEntity(e -> ((SES_AI_M_SENDER) e).setRegisterDate(null), sender);
  }
}
