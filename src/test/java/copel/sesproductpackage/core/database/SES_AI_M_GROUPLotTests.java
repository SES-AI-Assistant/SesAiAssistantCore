package copel.sesproductpackage.core.database;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.jupiter.api.Test;

class SES_AI_M_GROUPLotTests {

    @Test
    void testSelectAll() throws SQLException {
        Connection connection = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);
        when(connection.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true, false);
        when(rs.getString(anyString())).thenReturn("test");
        
        SES_AI_M_GROUPLot lot = new SES_AI_M_GROUPLot();
        lot.selectAll(connection);
        assertEquals(1, lot.size());
    }
}
