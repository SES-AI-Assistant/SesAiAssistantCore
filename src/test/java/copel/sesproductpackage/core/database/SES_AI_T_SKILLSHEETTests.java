package copel.sesproductpackage.core.database;

import java.sql.Connection;
import java.sql.SQLException;

import org.junit.jupiter.api.Test;

import copel.sesproductpackage.core.util.DBConnection;

class SES_AI_T_SKILLSHEETTests {

    @Test
    void selectByPkWithoutRawContentTest() throws ClassNotFoundException, SQLException {
        Connection connection = DBConnection.getConnection();
        SES_AI_T_SKILLSHEET SES_AI_T_SKILLSHEET = new SES_AI_T_SKILLSHEET();
        SES_AI_T_SKILLSHEET.setFileId("552575336303821212");
        SES_AI_T_SKILLSHEET.selectByPk(connection);
        System.out.println(SES_AI_T_SKILLSHEET.toString());
    }

    @Test
    void selectByPkTest() throws ClassNotFoundException, SQLException {
        Connection connection = DBConnection.getConnection();
        SES_AI_T_SKILLSHEET SES_AI_T_SKILLSHEET = new SES_AI_T_SKILLSHEET();
        SES_AI_T_SKILLSHEET.setFileId("552575336303821212");
        SES_AI_T_SKILLSHEET.selectByPkWithoutRawContent(connection);
        System.out.println(SES_AI_T_SKILLSHEET.getFileId());
        System.out.println(SES_AI_T_SKILLSHEET.getFileContentSummary());
        System.out.println(SES_AI_T_SKILLSHEET.getFromGroup());
        System.out.println(SES_AI_T_SKILLSHEET.getFromId());
        System.out.println(SES_AI_T_SKILLSHEET.getFromName());
    }

    @Test
    void searchByFileContentTest() throws ClassNotFoundException, SQLException {
        Connection connection = DBConnection.getConnection();
        SES_AI_T_SKILLSHEETLot SES_AI_T_SKILLSHEETLot = new SES_AI_T_SKILLSHEETLot();
        SES_AI_T_SKILLSHEETLot.searchByFileContent(connection, "java");
        System.out.println(SES_AI_T_SKILLSHEETLot);
    }
}
