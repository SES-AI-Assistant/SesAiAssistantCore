package copel.sesproductpackage.core.database;

import copel.sesproductpackage.core.database.base.EntityLotBase;
import copel.sesproductpackage.core.unit.OriginalDateTime;
import copel.sesproductpackage.core.unit.Role;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * 【Entityクラス】 システムユーザーマスタ(SES_AI_WEBAPP_M_USER)テーブルのLotクラス.
 *
 * @author Copel Co., Ltd.
 */
public class SES_AI_WEBAPP_M_USERLot extends EntityLotBase<SES_AI_WEBAPP_M_USER> {
  /** 全件SELECT文. */
  private static final String SELECT_ALL_SQL =
      "SELECT user_id, user_name, role_cd, register_date, register_user, tenant_id FROM SES_AI_WEBAPP_M_USER";

  /** SELECT文（WHERE句あり）. */
  private static final String SELECT_SQL =
      "SELECT user_id, user_name, role_cd, register_date, register_user FROM SES_AI_WEBAPP_M_USER WHERE ";

  public SES_AI_WEBAPP_M_USERLot() {
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
    List<SES_AI_WEBAPP_M_USER> results =
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
  public void selectAllWithoutTenantId(final Connection connection) throws SQLException {
    this.entityLot = new ArrayList<>();
    List<SES_AI_WEBAPP_M_USER> results =
        executeQueryWithoutTenantFilter(
            connection, SELECT_ALL_SQL, this::mapResultSet, (stmt, paramIndex) -> paramIndex);
    this.entityLot.addAll(results);
  }

  @Override
  protected SES_AI_WEBAPP_M_USER mapResultSet(ResultSet resultSet) throws SQLException {
    String tenantId = resultSet.getString("tenant_id");
    SES_AI_WEBAPP_M_USER sesAiWebappMUser = new SES_AI_WEBAPP_M_USER(tenantId);
    sesAiWebappMUser.setUserId(resultSet.getString("user_id"));
    sesAiWebappMUser.setUserName(resultSet.getString("user_name"));
    sesAiWebappMUser.setRole(Role.getEnum(resultSet.getString("role_cd")));
    sesAiWebappMUser.setRegisterDate(new OriginalDateTime(resultSet.getString("register_date")));
    sesAiWebappMUser.setRegisterUser(resultSet.getString("register_user"));
    return sesAiWebappMUser;
  }
}
