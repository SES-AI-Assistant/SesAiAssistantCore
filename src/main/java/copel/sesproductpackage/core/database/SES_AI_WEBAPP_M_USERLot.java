package copel.sesproductpackage.core.database;

import copel.sesproductpackage.core.database.base.EntityLotBase;
import copel.sesproductpackage.core.unit.OriginalDateTime;
import copel.sesproductpackage.core.unit.Role;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

/**
 * 【Entityクラス】 システムユーザーマスタ(SES_AI_WEBAPP_M_USER)テーブルのLotクラス.
 *
 * @author 鈴木一矢
 */
public class SES_AI_WEBAPP_M_USERLot extends EntityLotBase<SES_AI_WEBAPP_M_USER> {
  /** 全件SELECT文. */
  private static final String SELECT_ALL_SQL =
      "SELECT user_id, user_name, company_id, role_cd, register_date, register_user FROM SES_AI_WEBAPP_M_USER";

  public SES_AI_WEBAPP_M_USERLot() {
    super();
  }

  @Override
  public void selectAll(Connection connection) throws SQLException {
    PreparedStatement preparedStatement = connection.prepareStatement(SELECT_ALL_SQL);
    ResultSet resultSet = preparedStatement.executeQuery();
    this.entityLot = new ArrayList<>();
    while (resultSet.next()) {
      SES_AI_WEBAPP_M_USER sesAiWebappMUser = new SES_AI_WEBAPP_M_USER();
      sesAiWebappMUser.setUserId(resultSet.getString("user_id"));
      sesAiWebappMUser.setUserName(resultSet.getString("user_name"));
      sesAiWebappMUser.setCompanyId(resultSet.getString("company_id"));
      sesAiWebappMUser.setRole(Role.getEnum(resultSet.getString("role_cd")));
      sesAiWebappMUser.setRegisterDate(new OriginalDateTime(resultSet.getString("register_date")));
      sesAiWebappMUser.setRegisterUser(resultSet.getString("register_user"));
      this.entityLot.add(sesAiWebappMUser);
    }
  }
}
