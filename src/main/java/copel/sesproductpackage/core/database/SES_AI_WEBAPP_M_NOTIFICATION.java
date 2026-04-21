package copel.sesproductpackage.core.database;

import copel.sesproductpackage.core.database.base.Column;
import copel.sesproductpackage.core.database.base.EntityBase;
import copel.sesproductpackage.core.unit.OriginalDateTime;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * プッシュ通知デバイス登録マスタのエンティティ.
 *
 * @author Copel Co., Ltd.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@ToString(callSuper = true)
public class SES_AI_WEBAPP_M_NOTIFICATION extends EntityBase {
  /** INSERT文. */
  private static final String INSERT_SQL =
      "INSERT INTO SES_AI_WEBAPP_M_NOTIFICATION (notification_id, user_id, device_type, device_name, push_notification_endpoint, p256dh, auth, enabled, register_date, register_user) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

  /** SELECT文. */
  private static final String SELECT_SQL =
      "SELECT notification_id, user_id, device_type, device_name, push_notification_endpoint, p256dh, auth, enabled, register_date, register_user FROM SES_AI_WEBAPP_M_NOTIFICATION WHERE notification_id = ?";

  /** UPDATE文. */
  private static final String UPDATE_SQL =
      "UPDATE SES_AI_WEBAPP_M_NOTIFICATION SET notification_id = ?, user_id = ?, device_type = ?, device_name = ?, push_notification_endpoint = ?, p256dh = ?, auth = ?, enabled = ?, register_date = ?, register_user = ? WHERE notification_id = ?";

  /** DELETE文. */
  private static final String DELETE_SQL = "DELETE FROM SES_AI_WEBAPP_M_NOTIFICATION WHERE notification_id = ?";

  /** 【PK】 通知デバイスID / notification_id */
  @Column(required = true, primary = true, physicalName = "notification_id", logicalName = "通知デバイスID")
  private String notificationId;

  /** ユーザーID / user_id */
  @Column(required = true, physicalName = "user_id", logicalName = "ユーザーID")
  private String userId;

  /** デバイスタイプ / device_type */
  @Column(required = true, physicalName = "device_type", logicalName = "デバイスタイプ")
  private String deviceType;

  /** デバイス名 / device_name */
  @Column(physicalName = "device_name", logicalName = "デバイス名")
  private String deviceName;

  /** Push API endpoint URL / push_notification_endpoint */
  @Column(required = true, physicalName = "push_notification_endpoint", logicalName = "Push APIエンドポイント")
  private String pushNotificationEndpoint;

  /** ECDH public key (base64) / p256dh */
  @Column(required = true, physicalName = "p256dh", logicalName = "ECDH公開鍵")
  private String p256dh;

  /** HMAC authentication token (base64) / auth */
  @Column(required = true, physicalName = "auth", logicalName = "HMACトークン")
  private String auth;

  /** 有効フラグ / enabled */
  @Column(physicalName = "enabled", logicalName = "有効フラグ")
  private Boolean enabled;

  @Override
  public int insert(Connection connection) throws SQLException {
    if (connection == null) {
      return 0;
    }
    PreparedStatement preparedStatement = connection.prepareStatement(INSERT_SQL);
    preparedStatement.setString(1, this.notificationId);
    preparedStatement.setString(2, this.userId);
    preparedStatement.setString(3, this.deviceType);
    preparedStatement.setString(4, this.deviceName);
    preparedStatement.setString(5, this.pushNotificationEndpoint);
    preparedStatement.setString(6, this.p256dh);
    preparedStatement.setString(7, this.auth);
    preparedStatement.setBoolean(8, this.enabled != null && this.enabled);
    preparedStatement.setTimestamp(
        9, this.registerDate == null ? null : this.registerDate.toTimestamp());
    preparedStatement.setString(10, this.registerUser);
    return preparedStatement.executeUpdate();
  }

  @Override
  public void selectByPk(Connection connection) throws SQLException {
    if (connection == null || this.notificationId == null) {
      return;
    }
    PreparedStatement preparedStatement = connection.prepareStatement(SELECT_SQL);
    preparedStatement.setString(1, this.notificationId);
    ResultSet resultSet = preparedStatement.executeQuery();
    if (resultSet.next()) {
      this.notificationId = resultSet.getString("notification_id");
      this.userId = resultSet.getString("user_id");
      this.deviceType = resultSet.getString("device_type");
      this.deviceName = resultSet.getString("device_name");
      this.pushNotificationEndpoint = resultSet.getString("push_notification_endpoint");
      this.p256dh = resultSet.getString("p256dh");
      this.auth = resultSet.getString("auth");
      this.enabled = resultSet.getBoolean("enabled");
      this.registerDate = new OriginalDateTime(resultSet.getString("register_date"));
      this.registerUser = resultSet.getString("register_user");
    }
  }

  @Override
  public boolean updateByPk(Connection connection) throws SQLException {
    if (connection == null || this.notificationId == null) {
      return false;
    }
    PreparedStatement preparedStatement = connection.prepareStatement(UPDATE_SQL);
    preparedStatement.setString(1, this.notificationId);
    preparedStatement.setString(2, this.userId);
    preparedStatement.setString(3, this.deviceType);
    preparedStatement.setString(4, this.deviceName);
    preparedStatement.setString(5, this.pushNotificationEndpoint);
    preparedStatement.setString(6, this.p256dh);
    preparedStatement.setString(7, this.auth);
    preparedStatement.setBoolean(8, this.enabled != null && this.enabled);
    preparedStatement.setTimestamp(
        9, this.registerDate == null ? null : this.registerDate.toTimestamp());
    preparedStatement.setString(10, this.registerUser);
    preparedStatement.setString(11, this.notificationId);
    return preparedStatement.executeUpdate() > 0;
  }

  @Override
  public boolean deleteByPk(Connection connection) throws SQLException {
    if (connection == null || this.notificationId == null) {
      return false;
    }
    PreparedStatement preparedStatement = connection.prepareStatement(DELETE_SQL);
    preparedStatement.setString(1, this.notificationId);
    return preparedStatement.executeUpdate() > 0;
  }
}
