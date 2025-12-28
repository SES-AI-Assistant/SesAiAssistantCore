package copel.sesproductpackage.core.database;

import java.sql.Connection;
import java.sql.SQLException;

import org.junit.jupiter.api.Test;

import copel.sesproductpackage.core.util.DBConnection;

class SES_AI_T_JOBLotTests {

    @Test
    void test() throws ClassNotFoundException, SQLException {
        Connection connection = DBConnection.getConnection();
        SES_AI_T_JOBLot SES_AI_T_JOBLot = new SES_AI_T_JOBLot();
        SES_AI_T_JOBLot.selectAll(connection);
        System.out.println(SES_AI_T_JOBLot.size());
    }
}
