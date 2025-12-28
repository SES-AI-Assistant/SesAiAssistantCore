package copel.sesproductpackage.core.database;

import java.sql.Connection;
import java.sql.SQLException;

import org.junit.jupiter.api.Test;

import copel.sesproductpackage.core.unit.OriginalDateTime;
import copel.sesproductpackage.core.util.DBConnection;

class SES_AI_T_PERSONLotTests {

    @Test
    void selectByRegisterDateAfterTest() throws ClassNotFoundException, SQLException {
        Connection connection = DBConnection.getConnection();
        OriginalDateTime dateTime = new OriginalDateTime();
        dateTime.minusMinutes(570);
        System.out.println(dateTime);
        SES_AI_T_PERSONLot SES_AI_T_PERSONLot = new SES_AI_T_PERSONLot();
        SES_AI_T_PERSONLot.selectByRegisterDateAfter(connection, dateTime);
        System.out.println(SES_AI_T_PERSONLot);
    }
}
