package copel.sesproductpackage.core.database;

import java.sql.Connection;
import java.sql.SQLException;

import org.junit.jupiter.api.Test;

import copel.sesproductpackage.core.util.DBConnection;

class SES_AI_PERSONTests {

    @Test
    void selectByPkTest() throws SQLException, ClassNotFoundException {
        Connection connection = DBConnection.getConnection();
        SES_AI_T_PERSON SES_AI_T_PERSON = new SES_AI_T_PERSON();
        SES_AI_T_PERSON.setPersonId("0062ad24f3");
        SES_AI_T_PERSON.selectByPk(connection);
        System.out.println(SES_AI_T_PERSON.toString());
    }
}
