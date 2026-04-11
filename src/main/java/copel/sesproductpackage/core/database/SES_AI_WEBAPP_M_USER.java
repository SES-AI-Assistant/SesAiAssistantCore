package copel.sesproductpackage.core.database;

import copel.sesproductpackage.core.database.base.Column;
import copel.sesproductpackage.core.database.base.EntityBase;
import copel.sesproductpackage.core.unit.OriginalDateTime;
import copel.sesproductpackage.core.unit.Permission;
import copel.sesproductpackage.core.unit.Plan;
import copel.sesproductpackage.core.unit.Role;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * システムユーザーマスタテーブルのエンティティ.
 *
 * @author Copel Co., Ltd.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@ToString(callSuper = true)
public class SES_AI_WEBAPP_M_USER extends EntityBase {
  /** INSERTR文. */
  private static final String INSERT_SQL =
      "INSERT INTO SES_AI_WEBAPP_M_USER (user_id, user_name, company_id, role_cd, plan_cd, register_date, register_user) VALUES (?, ?, ?, ?, ?, ?, ?)";

  /** SELECT文. */
  private static final String SELECT_SQL =
      "SELECT user_id, user_name, company_id, role_cd, plan_cd, register_date, register_user FROM SES_AI_WEBAPP_M_USER WHERE user_id = ?";

  /** UPDATE文. */
  private static final String UPDATE_SQL =
      "UPDATE SES_AI_WEBAPP_M_USER SET user_id = ?, user_name = ?, company_id = ?, role_cd = ?, plan_cd = ?, register_date = ?, register_user = ? WHERE user_id = ?";

  /** DELETE文. */
  private static final String DELETE_SQL = "DELETE FROM SES_AI_WEBAPP_M_USER WHERE user_id = ?";

  /** 【PK】 ユーザーID* / user_id */
  @Column(required = true, primary = true, physicalName = "user_id", logicalName = "ユーザーID")
  private String userId;

  /** ユーザー名 / user_name */
  @Column(physicalName = "user_name", logicalName = "ユーザー名")
  private String userName;

  /** 会社ID / company_id */
  @Column(physicalName = "company_id", logicalName = "会社ID")
  private String companyId;

  /** ロール / role_cd */
  @Column(physicalName = "role_cd", logicalName = "ロール")
  private Role role;

  /** プラン / plan_cd */
  @Column(physicalName = "plan_cd", logicalName = "プラン")
  private Plan plan;

  /**
   * このユーザーがシステム利用可能であるかどうかを判定します.
   *
   * @return 利用可能であればtrue、不可であればfalse
   */
  public boolean hasSystemUseAuth() {
    return this.role != null && this.role.isSystemUseAuth();
  }

  /**
   * ユーザーが持つ全ての権限を取得します.
   *
   * @return 権限セット
   */
  public Set<Permission> getPermissions() {
    Set<Permission> permissions = new HashSet<>();
    if (this.role != null) {
      permissions.addAll(this.role.getPermissions());
    }
    if (this.plan != null) {
      permissions.addAll(this.plan.getPermissions());
    }
    if (!isEligibleForRegisterInfoListImport()) {
      permissions.remove(Permission.REGISTER_INFO_LIST_IMPORT);
    }
    return permissions;
  }

  /**
   * {@link Permission#REGISTER_INFO_LIST_IMPORT} を付与する条件（プレミアムプラン以上かつ一般ロール以上）を満たすか.
   *
   * @return 付与してよい場合 true
   */
  private boolean isEligibleForRegisterInfoListImport() {
    if (this.plan == null || this.plan == Plan.FREE) {
      return false;
    }
    if (this.role == null) {
      return false;
    }
    try {
      int roleNum = Integer.parseInt(this.role.getCode(), 10);
      int generalMin = Integer.parseInt(Role.システムユーザー.getCode(), 10);
      return roleNum >= generalMin;
    } catch (NumberFormatException e) {
      return false;
    }
  }

  @Override
  public int insert(Connection connection) throws SQLException {
    if (connection == null) {
      return 0;
    }
    PreparedStatement preparedStatement = connection.prepareStatement(INSERT_SQL);
    preparedStatement.setString(1, this.userId);
    preparedStatement.setString(2, this.userName);
    preparedStatement.setString(3, this.companyId);
    preparedStatement.setString(4, this.role == null ? null : this.role.getCode());
    preparedStatement.setString(5, this.plan == null ? null : this.plan.getCode());
    preparedStatement.setTimestamp(
        6, this.registerDate == null ? null : this.registerDate.toTimestamp());
    preparedStatement.setString(7, this.registerUser);
    return preparedStatement.executeUpdate();
  }

  @Override
  public void selectByPk(Connection connection) throws SQLException {
    if (connection == null || this.userId == null) {
      return;
    }
    PreparedStatement preparedStatement = connection.prepareStatement(SELECT_SQL);
    preparedStatement.setString(1, this.userId);
    ResultSet resultSet = preparedStatement.executeQuery();
    if (resultSet.next()) {
      this.userId = resultSet.getString("user_id");
      this.userName = resultSet.getString("user_name");
      this.companyId = resultSet.getString("company_id");
      this.role = Role.getEnum(resultSet.getString("role_cd"));
      this.plan = Plan.getEnum(resultSet.getString("plan_cd"));
      this.registerDate = new OriginalDateTime(resultSet.getString("register_date"));
      this.registerUser = resultSet.getString("register_user");
    }
  }

  @Override
  public boolean updateByPk(Connection connection) throws SQLException {
    if (connection == null || this.userId == null) {
      return false;
    }
    PreparedStatement preparedStatement = connection.prepareStatement(UPDATE_SQL);
    preparedStatement.setString(1, this.userId);
    preparedStatement.setString(2, this.userName);
    preparedStatement.setString(3, this.companyId);
    preparedStatement.setString(4, this.role == null ? null : this.role.getCode());
    preparedStatement.setString(5, this.plan == null ? null : this.plan.getCode());
    preparedStatement.setTimestamp(
        6, this.registerDate == null ? null : this.registerDate.toTimestamp());
    preparedStatement.setString(7, this.registerUser);
    preparedStatement.setString(8, this.userId);
    return preparedStatement.executeUpdate() > 0;
  }

  @Override
  public boolean deleteByPk(Connection connection) throws SQLException {
    if (connection == null || this.userId == null) {
      return false;
    }
    PreparedStatement preparedStatement = connection.prepareStatement(DELETE_SQL);
    preparedStatement.setString(1, this.userId);
    return preparedStatement.executeUpdate() > 0;
  }
}
