package copel.sesproductpackage.core.database;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.jupiter.api.Test;

import copel.sesproductpackage.core.unit.OriginalDateTime;
import copel.sesproductpackage.core.api.gpt.Transformer;

class SES_AI_T_JOBTests {

    @Test
    void testJOBMethods() throws Exception {
        Connection connection = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);
        when(connection.prepareStatement(anyString())).thenReturn(ps);
        when(ps.executeUpdate()).thenReturn(1);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true);
        when(rs.getInt(1)).thenReturn(0);
        
        SES_AI_T_JOB job = new SES_AI_T_JOB();
        job.setJobId("J1");
        
        // uniqueCheck
        assertTrue(job.uniqueCheck(connection, 0.8));
        
        // updateByPk
        assertTrue(job.updateByPk(connection));
        
        // deleteByPk
        assertTrue(job.deleteByPk(connection));
        
        // embedding
        Transformer mockTrans = mock(Transformer.class);
        when(mockTrans.embedding(anyString())).thenReturn(new float[]{0.1f});
        job.setContentSummary("summary");
        job.embedding(mockTrans);
        assertNotNull(job.getVectorData());
    }

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
