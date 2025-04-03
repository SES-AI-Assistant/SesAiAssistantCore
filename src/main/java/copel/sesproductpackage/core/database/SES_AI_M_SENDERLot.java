package copel.sesproductpackage.core.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import copel.sesproductpackage.core.database.base.EntityLotBase;
import copel.sesproductpackage.core.unit.OriginalDateTime;

/**
 * 【Entityクラス】
 * 送信者マスタ(SES_AI_M_SENDER)テーブルのLotクラス.
 *
 * @author 鈴木一矢
 *
 */
public class SES_AI_M_SENDERLot extends EntityLotBase<SES_AI_M_SENDER> {
    /**
     * 全件SELECT文.
     */
    private final static String SELECT_ALL_SQL = "SELECT from_id, from_name, company_id, register_date, register_user FROM SES_AI_M_SENDER";

    public SES_AI_M_SENDERLot() {
        super();
    }

    @Override
    public void selectAll(Connection connection) throws SQLException {
        PreparedStatement preparedStatement = connection.prepareStatement(SELECT_ALL_SQL);
        ResultSet resultSet = preparedStatement.executeQuery();
        this.entityLot = new ArrayList<SES_AI_M_SENDER>();
        while (resultSet.next()) {
            SES_AI_M_SENDER SES_AI_M_SENDER = new SES_AI_M_SENDER();
            SES_AI_M_SENDER.setFromId(resultSet.getString("from_id"));
            SES_AI_M_SENDER.setFromName(resultSet.getString("from_name"));
            SES_AI_M_SENDER.setCompanyId(resultSet.getString("company_id"));
            SES_AI_M_SENDER.setRegisterDate(new OriginalDateTime(resultSet.getString("register_date")));
            SES_AI_M_SENDER.setRegisterUser(resultSet.getString("register_user"));
            this.entityLot.add(SES_AI_M_SENDER);
        }
    }
}
