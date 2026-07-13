package copel.sesproductpackage.core.database;

import java.sql.Connection;
import java.sql.SQLException;

import copel.sesproductpackage.core.database.base.Column;
import copel.sesproductpackage.core.database.base.EntityBase;
import copel.sesproductpackage.core.unit.OriginalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * プッシュ通知デバイス登録マスタのエンティティ.
 *
 * @author Copel Co., Ltd.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class SES_AI_WEBAPP_M_NOTIFICATION extends EntityBase {

  public SES_AI_WEBAPP_M_NOTIFICATION(String tenantId) {
    super(tenantId);
  }

  /** INSERT文. */
  private static final String INSERT_SQL =
      "INSERT INTO SES_AI_WEBAPP_M_NOTIFICATION (notification_id, user_id, device_type, device_name, push_notification_endpoint, p256dh, auth, enabled, notify_all_match, register_date, register_user) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

  /** SELECT文. */
  private static final String SELECT_SQL =
      "SELECT notification_id, user_id, device_type, device_name, push_notification_endpoint, p256dh, auth, enabled, notify_all_match, register_date, register_user FROM SES_AI_WEBAPP_M_NOTIFICATION WHERE notification_id = ?";

  /** UPDATE文. */
  private static final String UPDATE_SQL =
      "UPDATE SES_AI_WEBAPP_M_NOTIFICATION SET notification_id = ?, user_id = ?, device_type = ?, device_name = ?, push_notification_endpoint = ?, p256dh = ?, auth = ?, enabled = ?, notify_all_match = ?, register_date = ?, register_user = ? WHERE notification_id = ?";

  /** DELETE文. */
  private static final String DELETE_SQL =
      "DELETE FROM SES_AI_WEBAPP_M_NOTIFICATION WHERE notification_id = ?";

  /** 【PK】 通知デバイスID / notification_id */
  @Column(
      required = true,
      primary = true,
      physicalName = "notification_id",
      logicalName = "通知デバイスID")
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
  @Column(
      required = true,
      physicalName = "push_notification_endpoint",
      logicalName = "Push APIエンドポイント")
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

  /** 全件通知フラグ / notify_all_match */
  @Column(physicalName = "notify_all_match", logicalName = "全件通知フラグ")
  private Boolean notifyAllMatch;

  @Override
  public int insert(Connection connection) throws SQLException {
    return executeInsert(
        connection,
        INSERT_SQL,
        this.tenantId,
        (stmt) -> {
          stmt.setString(1, this.notificationId);
          stmt.setString(2, this.userId);
          stmt.setString(3, this.deviceType);
          stmt.setString(4, this.deviceName);
          stmt.setString(5, this.pushNotificationEndpoint);
          stmt.setString(6, this.p256dh);
          stmt.setString(7, this.auth);
          stmt.setBoolean(8, this.enabled != null && this.enabled);
          stmt.setBoolean(9, this.notifyAllMatch != null && this.notifyAllMatch);
          stmt.setTimestamp(10, this.registerDate == null ? null : this.registerDate.toTimestamp());
          stmt.setString(11, this.registerUser);
        },
        "SES_AI_WEBAPP_M_NOTIFICATION.insert");
  }

  @Override
  public void selectByPk(Connection connection) throws SQLException {
    if (this.notificationId == null) {
      return;
    }
    executeSelectByPk(
        connection,
        SELECT_SQL,
        this.tenantId,
        (stmt) -> stmt.setString(1, this.notificationId),
        (rs) -> {
          this.notificationId = rs.getString("notification_id");
          this.userId = rs.getString("user_id");
          this.deviceType = rs.getString("device_type");
          this.deviceName = rs.getString("device_name");
          this.pushNotificationEndpoint = rs.getString("push_notification_endpoint");
          this.p256dh = rs.getString("p256dh");
          this.auth = rs.getString("auth");
          this.enabled = rs.getBoolean("enabled");
          this.notifyAllMatch = rs.getBoolean("notify_all_match");
          this.registerDate = new OriginalDateTime(rs.getString("register_date"));
          this.registerUser = rs.getString("register_user");
        },
        "SES_AI_WEBAPP_M_NOTIFICATION.selectByPk");
  }

  @Override
  public boolean updateByPk(Connection connection) throws SQLException {
    if (this.notificationId == null) {
      return false;
    }
    return executeUpdateByPk(
        connection,
        UPDATE_SQL,
        this.tenantId,
        (stmt) -> {
          stmt.setString(1, this.notificationId);
          stmt.setString(2, this.userId);
          stmt.setString(3, this.deviceType);
          stmt.setString(4, this.deviceName);
          stmt.setString(5, this.pushNotificationEndpoint);
          stmt.setString(6, this.p256dh);
          stmt.setString(7, this.auth);
          stmt.setBoolean(8, this.enabled != null && this.enabled);
          stmt.setBoolean(9, this.notifyAllMatch != null && this.notifyAllMatch);
          stmt.setTimestamp(10, this.registerDate == null ? null : this.registerDate.toTimestamp());
          stmt.setString(11, this.registerUser);
          stmt.setString(12, this.notificationId);
        },
        "SES_AI_WEBAPP_M_NOTIFICATION.updateByPk");
  }

  @Override
  public boolean deleteByPk(Connection connection) throws SQLException {
    if (this.notificationId == null) {
      return false;
    }
    return executeDeleteByPk(
        connection,
        DELETE_SQL,
        this.tenantId,
        (stmt) -> stmt.setString(1, this.notificationId),
        "SES_AI_WEBAPP_M_NOTIFICATION.deleteByPk");
  }
}
