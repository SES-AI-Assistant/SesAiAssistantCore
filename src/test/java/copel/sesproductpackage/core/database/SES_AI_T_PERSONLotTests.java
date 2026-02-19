package copel.sesproductpackage.core.database;

import java.sql.Connection;
import java.sql.SQLException;

import org.junit.jupiter.api.Test;

import copel.sesproductpackage.core.unit.OriginalDateTime;
import copel.sesproductpackage.core.unit.Vector;
import copel.sesproductpackage.core.util.DBConnection;
import java.util.HashMap;
import java.util.Map;

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

    @Test
    void retrieveTest() throws ClassNotFoundException, SQLException {
        Connection connection = DBConnection.getConnection();

        // 1. job_IDが001a4d82d1のses_ai_t_Jobレコードをselect
        SES_AI_T_JOBLot jobLot = new SES_AI_T_JOBLot();
        Map<String, String> queryMap = new HashMap<>();
        queryMap.put("job_id", "001a4d82d1");
        jobLot.selectByAndQuery(connection, queryMap);

        SES_AI_T_JOB job = jobLot.getEntityByPk("001a4d82d1");
        if (job == null) {
            System.out.println("Job not found: 001a4d82d1");
            return;
        }

        System.out.println("Selected Job: " + job.getJobId());

        // 2. そのベクトルデータを用いてSES_AI_T_PERSONLotのretrieveがうまく動くか試す
        // k=0.5 limit=5
        Vector queryVector = job.getVectorData();
        SES_AI_T_PERSONLot personLot = new SES_AI_T_PERSONLot();
        personLot.retrieve(connection, queryVector, 0.5, 5);

        // 3. selectした結果はコンソールに出力
        System.out.println("Retrieve Result:");
        System.out.println(personLot);
    }
}
