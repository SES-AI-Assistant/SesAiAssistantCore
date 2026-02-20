package copel.sesproductpackage.core.database;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import copel.sesproductpackage.core.unit.OriginalDateTime;
import copel.sesproductpackage.core.unit.Vector;

class SES_AI_T_JOBLotTests {

    @Test
    void testJOB() throws SQLException {
        Connection connection = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);
        when(connection.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeUpdate()).thenReturn(1);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true, false);
        
        SES_AI_T_JOB job = new SES_AI_T_JOB();
        job.setRegisterDate(new OriginalDateTime());
        job.setTtl(new OriginalDateTime());
        
        assertEquals(1, job.insert(connection));
        job.setJobId("J1");
        assertTrue(job.updateByPk(connection));
        job.selectByPk(connection);
        assertTrue(job.deleteByPk(connection));
        
        assertEquals("案件ID：J1内容：null", job.to案件選出用文章());
        assertNotNull(job.toString());
    }

    @Test
    void testJOBLot() throws Exception {
        Connection connection = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);
        when(connection.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true, false, true, false, true, false, true, false, true, false);
        when(rs.getString("job_id")).thenReturn("J1");
        when(rs.getDouble("distance")).thenReturn(0.5);
        
        SES_AI_T_JOBLot lot = new SES_AI_T_JOBLot();
        lot.selectAll(connection);
        assertEquals(1, lot.size());
        
        Vector vector = new Vector(null);
        java.lang.reflect.Field valueField = Vector.class.getDeclaredField("value");
        valueField.setAccessible(true);
        valueField.set(vector, new float[]{0.1f});
        
        lot.retrieve(connection, vector, 10);
        assertEquals(1, lot.size());
        
        lot.searchByRawContent(connection, "q");
        assertEquals(1, lot.size());

        lot.selectByAndQuery(connection, Map.of("c", "v"));
        assertEquals(1, lot.size());

        lot.selectByOrQuery(connection, Map.of("c", "v"));
        assertEquals(1, lot.size());
        
        SES_AI_T_JOB job = new SES_AI_T_JOB();
        job.setJobId("J1");
        lot.add(job);
        assertEquals(job, lot.getEntityByPk("J1"));
        assertNotNull(lot.to案件選出用文章());
    }
}
