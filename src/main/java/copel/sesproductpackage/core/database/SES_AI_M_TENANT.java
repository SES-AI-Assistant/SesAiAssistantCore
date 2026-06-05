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

  /** 【PK】 テナントID / tenant_id */
  @Column(required = true, primary = true, physicalName = "tenant_id", logicalName = "テナントID")
  private String tenantId;

  /** テナント名 / tenant_name */
  @Column(physicalName = "tenant_name", logicalName = "テナント名")
  private String tenantName;

  /** ステータス区分 / tenant_status_cd */
  @Column(physicalName = "tenant_status_cd", logicalName = "ステータス区分")
  private String tenantStatusCd;

  @Override
  public int insert(Connection connection) throws SQLException {
    if (connection == null) {
      return 0;
    }
    PreparedStatement preparedStatement = connection.prepareStatement(INSERT_SQL);
    preparedStatement.setString(1, this.tenantId);
    preparedStatement.setString(2, this.tenantName);
    preparedStatement.setString(3, this.tenantStatusCd);
    preparedStatement.setTimestamp(
        4, this.registerDate == null ? null : this.registerDate.toTimestamp());
    preparedStatement.setString(5, this.registerUser);
    return preparedStatement.executeUpdate();
  }

  @Override
  public void selectByPk(Connection connection) throws SQLException {
    if (connection == null || this.tenantId == null) {
      return;
    }
    PreparedStatement preparedStatement = connection.prepareStatement(SELECT_SQL);
    preparedStatement.setString(1, this.tenantId);
    ResultSet resultSet = preparedStatement.executeQuery();
    if (resultSet.next()) {
      this.tenantId = resultSet.getString("tenant_id");
      this.tenantName = resultSet.getString("tenant_name");
      this.tenantStatusCd = resultSet.getString("tenant_status_cd");
      this.registerDate = new OriginalDateTime(resultSet.getString("register_date"));
      this.registerUser = resultSet.getString("register_user");
    }
  }

  @Override
  public boolean updateByPk(Connection connection) throws SQLException {
    if (connection == null || this.tenantId == null) {
      return false;
    }
    PreparedStatement preparedStatement = connection.prepareStatement(UPDATE_SQL);
    preparedStatement.setString(1, this.tenantName);
    preparedStatement.setString(2, this.tenantStatusCd);
    preparedStatement.setString(3, this.tenantId);
    return preparedStatement.executeUpdate() > 0;
  }

  @Override
  public boolean deleteByPk(Connection connection) throws SQLException {
    if (connection == null || this.tenantId == null) {
      return false;
    }
    PreparedStatement preparedStatement = connection.prepareStatement(DELETE_SQL);
    preparedStatement.setString(1, this.tenantId);
    return preparedStatement.executeUpdate() > 0;
  }
}
