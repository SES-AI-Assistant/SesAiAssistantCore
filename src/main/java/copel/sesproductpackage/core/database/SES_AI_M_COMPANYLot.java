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
 * 会社マスタ(SES_AI_M_COMPANY)テーブルのLotクラス.
 *
 * @author 鈴木一矢
 *
 */
public class SES_AI_M_COMPANYLot extends EntityLotBase<SES_AI_M_COMPANY> {
    /**
     * 全件SELECT文.
     */
    private final static String SELECT_ALL_SQL = "SELECT company_id, company_name, memo, register_date, register_user FROM SES_AI_M_COMPANY";

    public SES_AI_M_COMPANYLot() {
        super();
    }

    @Override
    public void selectAll(Connection connection) throws SQLException {
        PreparedStatement preparedStatement = connection.prepareStatement(SELECT_ALL_SQL);
        ResultSet resultSet = preparedStatement.executeQuery();
        this.entityLot = new ArrayList<SES_AI_M_COMPANY>();
        while (resultSet.next()) {
            SES_AI_M_COMPANY SES_AI_M_COMPANY = new SES_AI_M_COMPANY();
            SES_AI_M_COMPANY.setCompanyId(resultSet.getString("company_id"));
            SES_AI_M_COMPANY.setCompanyName(resultSet.getString("company_name"));
            SES_AI_M_COMPANY.setMemo(resultSet.getString("memo"));
            SES_AI_M_COMPANY.setRegisterDate(new OriginalDateTime(resultSet.getString("register_date")));
            SES_AI_M_COMPANY.setRegisterUser(resultSet.getString("register_user"));
            this.entityLot.add(SES_AI_M_COMPANY);
        }
    }
}
