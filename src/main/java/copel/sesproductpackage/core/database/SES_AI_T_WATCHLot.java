package copel.sesproductpackage.core.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import copel.sesproductpackage.core.database.SES_AI_T_WATCH.TargetType;
import copel.sesproductpackage.core.database.base.EntityLotBase;
import copel.sesproductpackage.core.unit.OriginalDateTime;

/**
 * 【Entityクラス】 ウォッチ管理テーブル(SES_AI_T_WATCH)のLotクラス.
 *
 * @author Copel Co., Ltd.
 */
public class SES_AI_T_WATCHLot extends EntityLotBase<SES_AI_T_WATCH> {
  /** 全件SELECT文. */
  private static final String SELECT_ALL_SQL =
      "SELECT user_id, target_id, target_type, memo, register_date, register_user, ttl, tenant_id FROM SES_AI_T_WATCH";

  /** SELECT文(ユーザーIDキー). */
  private static final String SELECT_BY_USER_ID_SQL =
      "SELECT user_id, target_id, target_type, memo, register_date, register_user, ttl, tenant_id FROM SES_AI_T_WATCH WHERE user_id = ?";

  /** SELECT文(対象IDキー). */
  private static final String SELECT_BY_TARGET_ID_SQL =
      "SELECT user_id, target_id, target_type, memo, register_date, register_user, ttl, tenant_id FROM SES_AI_T_WATCH WHERE target_id = ? AND target_type = ?";

  public SES_AI_T_WATCHLot() {
    super();
  }


  @Override
  protected String getSelectAllSql() {
    return SELECT_ALL_SQL;
  }

  @Override
  protected String getSelectSql() {
    return "SELECT user_id, target_id, target_type, memo, register_date, register_user, ttl, tenant_id FROM SES_AI_T_WATCH WHERE ";
  }

  /**
   * ユーザーIDが一致するレコードを全て取得し、このLotに格納します.
   *
   * @param connection DBコネクション
   * @param tenantId テナントID
   * @param userId ユーザーID
   * @throws SQLException
   */
  public void selectByUserId(final Connection connection, final String tenantId, final String userId) throws SQLException {
    this.entityLot = new ArrayList<>();
    List<SES_AI_T_WATCH> results = executeQuery(
        connection,
        SELECT_BY_USER_ID_SQL,
        tenantId,
        this::mapResultSet,
        (stmt, paramIndex) -> {
          stmt.setString(paramIndex, userId);
          return paramIndex + 1;
        }
    );
    this.entityLot.addAll(results);
  }

  /**
   * ユーザーIDが一致するレコードを指定件数取得し、このLotに格納します.
   *
   * @param connection DBコネクション
   * @param tenantId テナントID
   * @param userId ユーザーID
   * @param page ページ番号
   * @param size 1ページあたりの件数
   * @throws SQLException
   */
  public void selectByUserIdPaged(final Connection connection, final String tenantId, final String userId, final int page, final int size)
      throws SQLException {
    if (connection == null || userId == null) {
      return;
    }
    java.util.Map<String, String> query = new java.util.HashMap<>();
    query.put("user_id", userId);
    this.selectByQueryPaged(
        connection,
        tenantId,
        "SELECT user_id, target_id, target_type, memo, register_date, register_user, ttl, tenant_id FROM SES_AI_T_WATCH",
        query,
        true,
        page,
        size);
  }

  /**
   * このLotに引数のIDと一致するIDを持つレコードが存在するかどうかを判定します.
   *
   * @param targetId 対象ID
   * @return 存在すればtrue、しなければfalse
   */
  public boolean containsById(String targetId) {
    if (targetId != null && !targetId.isEmpty()) {
      for (SES_AI_T_WATCH entity : this.entityLot) {
        if (targetId.equals(entity.getTargetId())) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * 対象IDと対象種別が一致するレコードを全て取得し、このLotに格納します.
   *
   * @param connection DBコネクション
   * @param tenantId テナントID
   * @param targetId 対象ID
   * @param targetType 対象種別
   * @throws SQLException
   */
  public void selectByTargetId(final Connection connection, final String tenantId, final String targetId, final TargetType targetType)
      throws SQLException {
    if (connection == null || targetId == null || targetType == null) {
      return;
    }
    PreparedStatement preparedStatement = connection.prepareStatement(SELECT_BY_TARGET_ID_SQL);
    preparedStatement.setString(1, targetId);
    preparedStatement.setString(2, targetType.name());
    ResultSet resultSet = preparedStatement.executeQuery();
    this.entityLot = new ArrayList<>();
    while (resultSet.next()) {
      this.entityLot.add(mapResultSet(resultSet));
    }
  }

  /**
   * 期限切れのレコードをDBから削除します.
   *
   * @param connection DBコネクション
   * @param tenantId テナントID
   * @return 削除したレコード数
   * @throws SQLException
   */
  public int deleteExpired(final Connection connection, final String tenantId) throws SQLException {
    final String deleteExpiredSql = "DELETE FROM SES_AI_T_WATCH WHERE ttl < NOW() AND tenant_id = ?";
    PreparedStatement preparedStatement = connection.prepareStatement(deleteExpiredSql);
    preparedStatement.setString(1, tenantId);
    return preparedStatement.executeUpdate();
  }

  /**
   * 期限切れウォッチデータを削除します（テナントID条件なし、バッチ用）.
   *
   * @param connection DBコネクション
   * @return 削除件数
   * @throws SQLException SQL実行エラー
   */
  public int deleteExpiredWithoutTenantId(final Connection connection) throws SQLException {
    final String deleteExpiredSql = "DELETE FROM SES_AI_T_WATCH WHERE ttl < NOW()";
    PreparedStatement preparedStatement = connection.prepareStatement(deleteExpiredSql);
    return preparedStatement.executeUpdate();
  }

  @Override
  public void selectAll(final Connection connection, final String tenantId) throws SQLException {
    PreparedStatement preparedStatement = connection.prepareStatement(SELECT_ALL_SQL + " WHERE tenant_id = ?");
    preparedStatement.setString(1, tenantId);
    ResultSet resultSet = preparedStatement.executeQuery();
    this.entityLot = new ArrayList<>();
    while (resultSet.next()) {
      this.entityLot.add(mapResultSet(resultSet));
    }
  }

  @Override
  protected SES_AI_T_WATCH mapResultSet(ResultSet resultSet) throws SQLException {
    String tenantId = resultSet.getString("tenant_id");
    SES_AI_T_WATCH sesAiTWatch = new SES_AI_T_WATCH(tenantId);
    sesAiTWatch.setUserId(resultSet.getString("user_id"));
    sesAiTWatch.setTargetId(resultSet.getString("target_id"));
    sesAiTWatch.setTargetType(TargetType.getEnumByName(resultSet.getString("target_type")));
    sesAiTWatch.setMemo(resultSet.getString("memo"));
    sesAiTWatch.setRegisterDate(new OriginalDateTime(resultSet.getString("register_date")));
    sesAiTWatch.setRegisterUser(resultSet.getString("register_user"));
    sesAiTWatch.setTtl(new OriginalDateTime(resultSet.getString("ttl")));
    return sesAiTWatch;
  }
}
