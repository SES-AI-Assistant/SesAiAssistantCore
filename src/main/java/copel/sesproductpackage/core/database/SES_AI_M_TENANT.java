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
 * 【Entityクラス】 テナント情報マスタ(SES_AI_M_TENANT)テーブル.
 *
 * @author Copel Co., Ltd.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class SES_AI_M_TENANT extends EntityBase {

  public SES_AI_M_TENANT(String tenantId) {
    super(tenantId);
  }

  /** INSERT文. */
  private static final String INSERT_SQL =
      "INSERT INTO SES_AI_M_TENANT (tenant_id, tenant_name, tenant_status_cd, register_date, register_user) VALUES (?, ?, ?, ?, ?)";

  /** SELECT文. */
  private static final String SELECT_SQL =
      "SELECT tenant_id, tenant_name, tenant_status_cd, register_date, register_user FROM SES_AI_M_TENANT WHERE tenant_id = ?";

  /** UPDATE文. */
  private static final String UPDATE_SQL =
      "UPDATE SES_AI_M_TENANT SET tenant_name = ?, tenant_status_cd = ? WHERE tenant_id = ?";

  /** DELETE文. */
  private static final String DELETE_SQL = "DELETE FROM SES_AI_M_TENANT WHERE tenant_id = ?";

  /** テナント名 / tenant_name */
  @Column(physicalName = "tenant_name", logicalName = "テナント名")
  private String tenantName;

  /** ステータス区分 / tenant_status_cd */
  @Column(physicalName = "tenant_status_cd", logicalName = "ステータス区分")
  private String tenantStatusCd;

  @Override
  public int insert(Connection connection) throws SQLException {
    return executeInsertWithoutTenantFilter(
        connection,
        INSERT_SQL,
        (stmt) -> {
          stmt.setString(1, this.tenantId);
          stmt.setString(2, this.tenantName);
          stmt.setString(3, this.tenantStatusCd);
          stmt.setTimestamp(4, this.registerDate == null ? null : this.registerDate.toTimestamp());
          stmt.setString(5, this.registerUser);
        },
        "SES_AI_M_TENANT.insert");
  }

  @Override
  public void selectByPk(Connection connection) throws SQLException {
    if (this.tenantId == null) {
      return;
    }
    executeSelectByPkWithoutTenantFilter(
        connection,
        SELECT_SQL,
        (stmt) -> stmt.setString(1, this.tenantId),
        (rs) -> {
          this.tenantId = rs.getString("tenant_id");
          this.tenantName = rs.getString("tenant_name");
          this.tenantStatusCd = rs.getString("tenant_status_cd");
          this.registerDate = new OriginalDateTime(rs.getString("register_date"));
          this.registerUser = rs.getString("register_user");
        },
        "SES_AI_M_TENANT.selectByPk");
  }

  @Override
  public boolean updateByPk(Connection connection) throws SQLException {
    if (this.tenantId == null) {
      return false;
    }
    return executeUpdateByPkWithoutTenantFilter(
        connection,
        UPDATE_SQL,
        (stmt) -> {
          stmt.setString(1, this.tenantName);
          stmt.setString(2, this.tenantStatusCd);
          stmt.setString(3, this.tenantId);
        },
        "SES_AI_M_TENANT.updateByPk");
  }

  @Override
  public boolean deleteByPk(Connection connection) throws SQLException {
    if (this.tenantId == null) {
      return false;
    }
    return executeDeleteByPkWithoutTenantFilter(
        connection,
        DELETE_SQL,
        (stmt) -> stmt.setString(1, this.tenantId),
        "SES_AI_M_TENANT.deleteByPk");
  }
}
