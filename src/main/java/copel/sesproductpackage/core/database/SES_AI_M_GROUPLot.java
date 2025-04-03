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
 * 送信元グループマスタ(SES_AI_M_GROUP)テーブルのLotクラス.
 *
 * @author 鈴木一矢
 *
 */
public class SES_AI_M_GROUPLot extends EntityLotBase<SES_AI_M_GROUP> {
    /**
     * 全件SELECT文.
     */
    private final static String SELECT_ALL_SQL = "SELECT from_group, group_name, register_date, register_user FROM SES_AI_M_GROUP";

    public SES_AI_M_GROUPLot() {
        super();
    }

    @Override
    public void selectAll(Connection connection) throws SQLException {
        PreparedStatement preparedStatement = connection.prepareStatement(SELECT_ALL_SQL);
        ResultSet resultSet = preparedStatement.executeQuery();
        this.entityLot = new ArrayList<SES_AI_M_GROUP>();
        while (resultSet.next()) {
            SES_AI_M_GROUP SES_AI_M_GROUP = new SES_AI_M_GROUP();
            SES_AI_M_GROUP.setFromGroup(resultSet.getString("from_group"));
            SES_AI_M_GROUP.setGroupName(resultSet.getString("group_name"));
            SES_AI_M_GROUP.setRegisterDate(new OriginalDateTime(resultSet.getString("register_date")));
            SES_AI_M_GROUP.setRegisterUser(resultSet.getString("register_user"));
            this.entityLot.add(SES_AI_M_GROUP);
        }
    }
}
