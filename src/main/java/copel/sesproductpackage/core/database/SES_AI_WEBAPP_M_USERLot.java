package copel.sesproductpackage.core.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import copel.sesproductpackage.core.database.base.EntityLotBase;
import copel.sesproductpackage.core.unit.OriginalDateTime;
import copel.sesproductpackage.core.unit.Role;

/**
 * 【Entityクラス】
 * システムユーザーマスタ(SES_AI_WEBAPP_M_USER)テーブルのLotクラス.
 *
 * @author 鈴木一矢
 *
 */
public class SES_AI_WEBAPP_M_USERLot extends EntityLotBase<SES_AI_WEBAPP_M_USER> {
    /**
     * 全件SELECT文.
     */
    private final static String SELECT_ALL_SQL = "SELECT user_id, user_name, user_password, company_id, role_cd, register_date, register_user FROM SES_AI_WEBAPP_M_USER";

    public SES_AI_WEBAPP_M_USERLot() {
        super();
    }

    @Override
    public void selectAll(Connection connection) throws SQLException {
        PreparedStatement preparedStatement = connection.prepareStatement(SELECT_ALL_SQL);
        ResultSet resultSet = preparedStatement.executeQuery();
        this.entityLot = new ArrayList<SES_AI_WEBAPP_M_USER>();
        while (resultSet.next()) {
            SES_AI_WEBAPP_M_USER SES_AI_WEBAPP_M_USER = new SES_AI_WEBAPP_M_USER();
            SES_AI_WEBAPP_M_USER.setUserId(resultSet.getString("user_id"));
            SES_AI_WEBAPP_M_USER.setUserName(resultSet.getString("user_name"));
            SES_AI_WEBAPP_M_USER.setUserPassword(resultSet.getString("user_password"));
            SES_AI_WEBAPP_M_USER.setCompanyId(resultSet.getString("company_id"));
            SES_AI_WEBAPP_M_USER.setRole(Role.getEnum(resultSet.getString("role_cd")));
            SES_AI_WEBAPP_M_USER.setRegisterDate(new OriginalDateTime(resultSet.getString("register_date")));
            SES_AI_WEBAPP_M_USER.setRegisterUser(resultSet.getString("register_user"));
            this.entityLot.add(SES_AI_WEBAPP_M_USER);
        }
    }
}
