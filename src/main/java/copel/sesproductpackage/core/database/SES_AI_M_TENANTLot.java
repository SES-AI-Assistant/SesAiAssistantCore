package copel.sesproductpackage.core.database;

import copel.sesproductpackage.core.database.base.EntityLotBase;
import copel.sesproductpackage.core.unit.OriginalDateTime;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

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
    PreparedStatement preparedStatement = connection.prepareStatement(SELECT_ALL_SQL + " WHERE tenant_id = ?");
    preparedStatement.setString(1, tenantId);
    ResultSet resultSet = preparedStatement.executeQuery();
    this.entityLot = new ArrayList<>();
    while (resultSet.next()) {
      this.entityLot.add(mapResultSet(resultSet));
    }
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
