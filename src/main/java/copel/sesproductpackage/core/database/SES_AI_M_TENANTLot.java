package copel.sesproductpackage.core.database;

import copel.sesproductpackage.core.database.base.EntityLotBase;
import copel.sesproductpackage.core.unit.OriginalDateTime;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * 【Lotクラス】 テナント情報マスタ(SES_AI_M_TENANT)テーブル.
 *
 * @author Copel Co., Ltd.
 */
public class SES_AI_M_TENANTLot extends EntityLotBase<SES_AI_M_TENANT> {
  /** 全件SELECT文. */
  private static final String SELECT_ALL_SQL =
      "SELECT tenant_id, tenant_name, tenant_status_cd, register_date, register_user FROM SES_AI_M_TENANT";

  public SES_AI_M_TENANTLot() {
    super();
  }


  @Override
  protected String getSelectAllSql() {
    return SELECT_ALL_SQL;
  }

  @Override
  protected String getSelectSql() {
    return "SELECT tenant_id, tenant_name, tenant_status_cd, register_date, register_user FROM SES_AI_M_TENANT WHERE ";
  }

  @Override
  public void selectAll(final Connection connection, final String tenantId) throws SQLException {
    this.entityLot = new ArrayList<>();
    List<SES_AI_M_TENANT> results = executeQuery(
        connection,
        SELECT_ALL_SQL,
        tenantId,
        this::mapResultSet,
        (stmt, paramIndex) -> paramIndex
    );
    this.entityLot.addAll(results);
  }

  /**
   * 全レコードを取得する（WithoutTenantFilter - バッチ処理専用）.
   *
   * ⚠️ このメソッドは全テナント対象です。
   * バッチ処理専用。コードレビュー必須。
   *
   * @param connection DBコネクション
   * @throws SQLException
   */
  public void selectAllWithoutTenantFilter(final Connection connection) throws SQLException {
    this.entityLot = new ArrayList<>();
    List<SES_AI_M_TENANT> results = executeQueryWithoutTenantFilter(
        connection,
        SELECT_ALL_SQL,
        this::mapResultSet,
        (stmt, paramIndex) -> paramIndex
    );
    this.entityLot.addAll(results);
  }

  @Override
  protected SES_AI_M_TENANT mapResultSet(ResultSet resultSet) throws SQLException {
    String tenantId = resultSet.getString("tenant_id");
    SES_AI_M_TENANT sesAiMTenant = new SES_AI_M_TENANT(tenantId);
    sesAiMTenant.setTenantName(resultSet.getString("tenant_name"));
    sesAiMTenant.setTenantStatusCd(resultSet.getString("tenant_status_cd"));
    sesAiMTenant.setRegisterDate(new OriginalDateTime(resultSet.getString("register_date")));
    sesAiMTenant.setRegisterUser(resultSet.getString("register_user"));
    return sesAiMTenant;
  }
}
