package copel.sesproductpackage.core.database;

import copel.sesproductpackage.core.database.base.EntityLotBase;
import copel.sesproductpackage.core.unit.OriginalDateTime;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

/**
 * 【Entityクラス】 送信者マスタ(SES_AI_M_SENDER)テーブルのLotクラス.
 *
 * @author 鈴木一矢
 */
public class SES_AI_M_SENDERLot extends EntityLotBase<SES_AI_M_SENDER> {
  /** 全件SELECT文. */
  private static final String SELECT_ALL_SQL =
      "SELECT from_id, from_name, company_id, register_date, register_user FROM SES_AI_M_SENDER";

  public SES_AI_M_SENDERLot() {
    super();
  }

  @Override
  public void selectAll(Connection connection) throws SQLException {
    PreparedStatement preparedStatement = connection.prepareStatement(SELECT_ALL_SQL);
    ResultSet resultSet = preparedStatement.executeQuery();
    this.entityLot = new ArrayList<>();
    while (resultSet.next()) {
      this.entityLot.add(mapResultSet(resultSet));
    }
  }

  @Override
  protected SES_AI_M_SENDER mapResultSet(ResultSet resultSet) throws SQLException {
    SES_AI_M_SENDER sesAiMSender = new SES_AI_M_SENDER();
    sesAiMSender.setFromId(resultSet.getString("from_id"));
    sesAiMSender.setFromName(resultSet.getString("from_name"));
    sesAiMSender.setCompanyId(resultSet.getString("company_id"));
    sesAiMSender.setRegisterDate(new OriginalDateTime(resultSet.getString("register_date")));
    sesAiMSender.setRegisterUser(resultSet.getString("register_user"));
    return sesAiMSender;
  }
}
