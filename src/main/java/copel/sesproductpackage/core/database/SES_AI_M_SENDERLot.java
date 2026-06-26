package copel.sesproductpackage.core.database;

import copel.sesproductpackage.core.database.base.EntityLotBase;
import copel.sesproductpackage.core.unit.OriginalDateTime;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * 【Entityクラス】 送信者マスタ(SES_AI_M_SENDER)テーブルのLotクラス.
 *
 * @author Copel Co., Ltd.
 */
public class SES_AI_M_SENDERLot extends EntityLotBase<SES_AI_M_SENDER> {
  /** 全件SELECT文. */
  private static final String SELECT_ALL_SQL =
      "SELECT from_id, from_name, register_date, register_user, tenant_id FROM SES_AI_M_SENDER";

  /** SELECT文（WHERE句あり）. */
  private static final String SELECT_SQL =
      "SELECT from_id, from_name, register_date, register_user FROM SES_AI_M_SENDER WHERE ";

  public SES_AI_M_SENDERLot() {
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
    List<SES_AI_M_SENDER> results =
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
    List<SES_AI_M_SENDER> results =
        executeQueryWithoutTenantFilter(
            connection, SELECT_ALL_SQL, this::mapResultSet, (stmt, paramIndex) -> paramIndex);
    this.entityLot.addAll(results);
  }

  @Override
  protected SES_AI_M_SENDER mapResultSet(ResultSet resultSet) throws SQLException {
    String tenantId = resultSet.getString("tenant_id");
    SES_AI_M_SENDER sesAiMSender = new SES_AI_M_SENDER(tenantId);
    sesAiMSender.setFromId(resultSet.getString("from_id"));
    sesAiMSender.setFromName(resultSet.getString("from_name"));
    sesAiMSender.setRegisterDate(new OriginalDateTime(resultSet.getString("register_date")));
    sesAiMSender.setRegisterUser(resultSet.getString("register_user"));
    return sesAiMSender;
  }
}
