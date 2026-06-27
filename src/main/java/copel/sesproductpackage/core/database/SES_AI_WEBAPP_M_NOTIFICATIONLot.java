package copel.sesproductpackage.core.database;

import copel.sesproductpackage.core.database.base.EntityLotBase;
import copel.sesproductpackage.core.unit.OriginalDateTime;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * プッシュ通知デバイス登録マスタのLotクラス.
 *
 * @author Copel Co., Ltd.
 */
public class SES_AI_WEBAPP_M_NOTIFICATIONLot extends EntityLotBase<SES_AI_WEBAPP_M_NOTIFICATION> {
  /** 全件SELECT文. */
  private static final String SELECT_ALL_SQL =
      "SELECT notification_id, user_id, device_type, device_name, push_notification_endpoint, p256dh, auth, enabled, notify_all_match, register_date, register_user, tenant_id FROM SES_AI_WEBAPP_M_NOTIFICATION";

  /** SELECT文（WHERE句あり）. */
  private static final String SELECT_SQL =
      "SELECT notification_id, user_id, device_type, device_name, push_notification_endpoint, p256dh, auth, enabled, notify_all_match, register_date, register_user, tenant_id FROM SES_AI_WEBAPP_M_NOTIFICATION WHERE ";

  /** SELECT文（user_id検索）. */
  private static final String SELECT_BY_USER_ID_SQL =
      "SELECT notification_id, user_id, device_type, device_name, push_notification_endpoint, p256dh, auth, enabled, notify_all_match, register_date, register_user, tenant_id FROM SES_AI_WEBAPP_M_NOTIFICATION WHERE user_id = ?";

  /** SELECT文（notify_all_match検索）. */
  private static final String SELECT_BY_NOTIFY_ALL_MATCH_SQL =
      "SELECT notification_id, user_id, device_type, device_name, push_notification_endpoint, p256dh, auth, enabled, notify_all_match, register_date, register_user, tenant_id FROM SES_AI_WEBAPP_M_NOTIFICATION WHERE notify_all_match = true";

  public SES_AI_WEBAPP_M_NOTIFICATIONLot() {
    super();
  }

  @Override
  protected String getSelectAllSql() {
    return SELECT_ALL_SQL;
  }

  @Override
  protected String getSelectSql() {
    return SELECT_SQL;
  }

  @Override
  public void selectAll(final Connection connection, final String tenantId) throws SQLException {
    this.entityLot = new ArrayList<>();
    List<SES_AI_WEBAPP_M_NOTIFICATION> results =
        executeQuery(
            connection,
            SELECT_ALL_SQL,
            tenantId,
            this::mapResultSet,
            (stmt, paramIndex) -> paramIndex);
    this.entityLot.addAll(results);
  }

  /**
   * 全レコードを取得する（WithoutTenantFilter - バッチ処理専用）.
   *
   * <p>⚠️ このメソッドは全テナント対象です。 バッチ処理専用。コードレビュー必須。
   *
   * @param connection DBコネクション
   * @throws SQLException
   */
  public void selectAllWithoutTenantFilter(final Connection connection) throws SQLException {
    this.entityLot = new ArrayList<>();
    List<SES_AI_WEBAPP_M_NOTIFICATION> results =
        executeQueryWithoutTenantFilter(
            connection, SELECT_ALL_SQL, this::mapResultSet, (stmt, paramIndex) -> paramIndex);
    this.entityLot.addAll(results);
  }

  /**
   * user_id で デバイス登録情報をすべて取得する.
   *
   * @param connection データベース接続
   * @param tenantId テナントID
   * @param userId ユーザーID
   * @throws SQLException SQL実行時の例外
   */
  public void selectByUserId(
      final Connection connection, final String tenantId, final String userId) throws SQLException {
    if (connection == null || userId == null) {
      this.entityLot = new ArrayList<>();
      return;
    }
    List<SES_AI_WEBAPP_M_NOTIFICATION> results =
        executeQuery(
            connection,
            SELECT_BY_USER_ID_SQL,
            tenantId,
            this::mapResultSet,
            (stmt, paramIndex) -> {
              stmt.setString(paramIndex, userId);
              return paramIndex + 1;
            });
    this.entityLot = results;
  }

  /**
   * notify_all_match = true のデバイス登録情報をすべて取得する.
   *
   * @param connection データベース接続
   * @param tenantId テナントID
   * @throws SQLException SQL実行時の例外
   */
  public void selectByNotifyAllMatch(final Connection connection, final String tenantId)
      throws SQLException {
    List<SES_AI_WEBAPP_M_NOTIFICATION> results =
        executeQuery(
            connection,
            SELECT_BY_NOTIFY_ALL_MATCH_SQL,
            tenantId,
            this::mapResultSet,
            (stmt, paramIndex) -> paramIndex);
    this.entityLot = results;
  }

  @Override
  protected SES_AI_WEBAPP_M_NOTIFICATION mapResultSet(ResultSet resultSet) throws SQLException {
    SES_AI_WEBAPP_M_NOTIFICATION notification = new SES_AI_WEBAPP_M_NOTIFICATION(resultSet.getString("tenant_id"));
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
