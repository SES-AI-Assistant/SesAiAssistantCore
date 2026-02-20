package copel.sesproductpackage.core.database;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.jupiter.api.Test;

import copel.sesproductpackage.core.unit.OriginalDateTime;

class SES_AI_T_JOBTests {

    @Test
    void testJOB() throws SQLException {
        Connection connection = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);
        when(connection.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeUpdate()).thenReturn(1, 1, 1);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true, false);
        when(rs.getString(anyString())).thenReturn("test");
        
        SES_AI_T_JOB job = new SES_AI_T_JOB();
        job.setJobId("J1");
        job.setRegisterDate(new OriginalDateTime());
        
        assertEquals(1, job.insert(connection));
        assertNotNull(job.to案件選出用文章());
        assertNotNull(job.toString());
    }
}
