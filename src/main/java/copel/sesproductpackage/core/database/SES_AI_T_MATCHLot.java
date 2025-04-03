package copel.sesproductpackage.core.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import copel.sesproductpackage.core.database.base.EntityLotBase;
import copel.sesproductpackage.core.unit.OriginalDateTime;
import copel.sesproductpackage.core.unit.マッチング状態区分;

/**
 * 【Entityクラス】
 * マッチング(SES_AI_T_MATCH)テーブルのLotクラス.
 *
 * @author 鈴木一矢
 *
 */
public class SES_AI_T_MATCHLot extends EntityLotBase<SES_AI_T_MATCH> {
    /**
     * 全件SELECT文.
     */
    private final static String SELECT_ALL_SQL = "SELECT job_id, person_id, status_cd, register_date, register_user FROM SES_AI_T_MATCH";

    public SES_AI_T_MATCHLot() {
        super();
    }

    @Override
    public void selectAll(Connection connection) throws SQLException {
        PreparedStatement preparedStatement = connection.prepareStatement(SELECT_ALL_SQL);
        ResultSet resultSet = preparedStatement.executeQuery();
        this.entityLot = new ArrayList<SES_AI_T_MATCH>();
        while (resultSet.next()) {
            SES_AI_T_MATCH SES_AI_T_MATCH = new SES_AI_T_MATCH();
            SES_AI_T_MATCH.setJobId(resultSet.getString("job_id"));
            SES_AI_T_MATCH.setPersonId(resultSet.getString("person_id"));
            SES_AI_T_MATCH.setStatus(マッチング状態区分.getEnum(resultSet.getString("status_cd")));
            SES_AI_T_MATCH.setRegisterDate(new OriginalDateTime(resultSet.getString("register_date")));
            SES_AI_T_MATCH.setRegisterUser(resultSet.getString("register_user"));
            this.entityLot.add(SES_AI_T_MATCH);
        }
    }
}
