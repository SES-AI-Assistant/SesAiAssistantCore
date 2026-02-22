package copel.sesproductpackage.core.database;

import copel.sesproductpackage.core.database.base.EntityLotBase;
import copel.sesproductpackage.core.unit.OriginalDateTime;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

/**
 * 【Entityクラス】 会社マスタ(SES_AI_M_COMPANY)テーブルのLotクラス.
 *
 * @author 鈴木一矢
 */
public class SES_AI_M_COMPANYLot extends EntityLotBase<SES_AI_M_COMPANY> {
  /** 全件SELECT文. */
  private static final String SELECT_ALL_SQL =
      "SELECT company_id, company_name, memo, register_date, register_user FROM SES_AI_M_COMPANY";

  public SES_AI_M_COMPANYLot() {
    super();
  }

  @Override
  public void selectAll(Connection connection) throws SQLException {
    PreparedStatement preparedStatement = connection.prepareStatement(SELECT_ALL_SQL);
    ResultSet resultSet = preparedStatement.executeQuery();
    this.entityLot = new ArrayList<>();
    while (resultSet.next()) {
      SES_AI_M_COMPANY sesAiMCompany = new SES_AI_M_COMPANY();
      sesAiMCompany.setCompanyId(resultSet.getString("company_id"));
      sesAiMCompany.setCompanyName(resultSet.getString("company_name"));
      sesAiMCompany.setMemo(resultSet.getString("memo"));
      sesAiMCompany.setRegisterDate(new OriginalDateTime(resultSet.getString("register_date")));
      sesAiMCompany.setRegisterUser(resultSet.getString("register_user"));
      this.entityLot.add(sesAiMCompany);
    }
  }
}
