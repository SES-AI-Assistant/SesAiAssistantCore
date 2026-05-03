package copel.sesproductpackage.core.database;

import copel.sesproductpackage.core.database.base.EntityLotBase;
import copel.sesproductpackage.core.unit.OriginalDateTime;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

/**
 * プッシュ通知デバイス登録マスタのLotクラス.
 *
 * @author Copel Co., Ltd.
 */
public class SES_AI_WEBAPP_M_NOTIFICATIONLot extends EntityLotBase<SES_AI_WEBAPP_M_NOTIFICATION> {
  /** 全件SELECT文. */
  private static final String SELECT_ALL_SQL =
      "SELECT notification_id, user_id, device_type, device_name, push_notification_endpoint, p256dh, auth, enabled, notify_all_match, register_date, register_user FROM SES_AI_WEBAPP_M_NOTIFICATION";

  public SES_AI_WEBAPP_M_NOTIFICATIONLot() {
    super();
  }

  @Override
  protected String getSelectAllSql() {
    return SELECT_ALL_SQL;
  }

  @Override
  protected String getSelectSql() {
    return "SELECT notification_id, user_id, device_type, device_name, push_notification_endpoint, p256dh, auth, enabled, notify_all_match, register_date, register_user FROM SES_AI_WEBAPP_M_NOTIFICATION WHERE ";
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

  /**
   * notify_all_match = true のデバイス登録情報をすべて取得する.
   *
   * @param connection データベース接続
   * @throws SQLException SQL実行時の例外
   */
  public void selectByNotifyAllMatch(Connection connection) throws SQLException {
    String sql =
        "SELECT notification_id, user_id, device_type, device_name, push_notification_endpoint, p256dh, auth, enabled, notify_all_match, register_date, register_user FROM SES_AI_WEBAPP_M_NOTIFICATION WHERE notify_all_match = true";
    PreparedStatement preparedStatement = connection.prepareStatement(sql);
    ResultSet resultSet = preparedStatement.executeQuery();
    this.entityLot = new ArrayList<>();
    while (resultSet.next()) {
      this.entityLot.add(mapResultSet(resultSet));
    }
  }

  @Override
  protected SES_AI_WEBAPP_M_NOTIFICATION mapResultSet(ResultSet resultSet) throws SQLException {
    SES_AI_WEBAPP_M_NOTIFICATION notification = new SES_AI_WEBAPP_M_NOTIFICATION();
    notification.setNotificationId(resultSet.getString("notification_id"));
    notification.setUserId(resultSet.getString("user_id"));
    notification.setDeviceType(resultSet.getString("device_type"));
    notification.setDeviceName(resultSet.getString("device_name"));
    notification.setPushNotificationEndpoint(resultSet.getString("push_notification_endpoint"));
    notification.setP256dh(resultSet.getString("p256dh"));
    notification.setAuth(resultSet.getString("auth"));
    notification.setEnabled(resultSet.getBoolean("enabled"));
    notification.setNotifyAllMatch(resultSet.getBoolean("notify_all_match"));
    notification.setRegisterDate(new OriginalDateTime(resultSet.getString("register_date")));
    notification.setRegisterUser(resultSet.getString("register_user"));
    return notification;
  }
}
