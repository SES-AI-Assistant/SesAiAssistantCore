package copel.sesproductpackage.core.database;

import copel.sesproductpackage.core.database.base.Column;
import copel.sesproductpackage.core.database.base.EntityBase;
import copel.sesproductpackage.core.unit.OriginalDateTime;
import copel.sesproductpackage.core.unit.Permission;
import copel.sesproductpackage.core.unit.Plan;
import copel.sesproductpackage.core.unit.Role;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * システムユーザーマスタテーブルのエンティティ.
 *
 * @author Copel Co., Ltd.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class SES_AI_WEBAPP_M_USER extends EntityBase {

  public SES_AI_WEBAPP_M_USER(String tenantId) {
    super(tenantId);
  }

  /** tenantIdをあとから設定できるコンストラクタ（ログイン時のユーザーID検索時など）. */
  public SES_AI_WEBAPP_M_USER() {
    super("_temp_");
  }

  /** INSERTR文. */
  private static final String INSERT_SQL =
      "INSERT INTO SES_AI_WEBAPP_M_USER (user_id, user_name, role_cd, plan_cd, register_date, register_user, tenant_id) VALUES (?, ?, ?, ?, ?, ?, ?)";

  /** SELECT文. */
  private static final String SELECT_SQL =
      "SELECT user_id, user_name, role_cd, plan_cd, register_date, register_user, tenant_id FROM SES_AI_WEBAPP_M_USER WHERE user_id = ? AND tenant_id = ?";

  /** SELECT文（tenant_id条件なし）. */
  private static final String SELECT_WITHOUT_TENANT_ID_SQL =
      "SELECT user_id, user_name, role_cd, plan_cd, register_date, register_user, tenant_id FROM SES_AI_WEBAPP_M_USER WHERE user_id = ?";

  /** UPDATE文. */
  private static final String UPDATE_SQL =
      "UPDATE SES_AI_WEBAPP_M_USER SET user_id = ?, user_name = ?, role_cd = ?, plan_cd = ?, register_date = ?, register_user = ? WHERE user_id = ? AND tenant_id = ?";

  /** DELETE文. */
  private static final String DELETE_SQL =
      "DELETE FROM SES_AI_WEBAPP_M_USER WHERE user_id = ? AND tenant_id = ?";

  /** 【PK】 ユーザーID* / user_id */
  @Column(required = true, primary = true, physicalName = "user_id", logicalName = "ユーザーID")
  private String userId;

  /** ユーザー名 / user_name */
  @Column(physicalName = "user_name", logicalName = "ユーザー名")
  private String userName;

  /** ロール / role_cd */
  @Column(physicalName = "role_cd", logicalName = "ロール")
  private Role role;

  /** プラン / plan_cd */
  @Column(physicalName = "plan_cd", logicalName = "プラン")
  private Plan plan;

  /** テナントID / tenant_id（Phase 1 テナント対応） */
  @Column(physicalName = "tenant_id", logicalName = "テナントID")
  private String tenantId;

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
    return executeInsertWithoutTenantFilter(
        connection,
        INSERT_SQL,
        (stmt) -> {
          stmt.setString(1, this.userId);
          stmt.setString(2, this.userName);
          stmt.setString(3, this.role == null ? null : this.role.getCode());
          stmt.setString(4, this.plan == null ? null : this.plan.getCode());
          stmt.setTimestamp(5, this.registerDate == null ? null : this.registerDate.toTimestamp());
          stmt.setString(6, this.registerUser);
          stmt.setString(7, this.tenantId);
        },
        "SES_AI_WEBAPP_M_USER.insert");
  }

  @Override
  public void selectByPk(Connection connection) throws SQLException {
    if (this.userId == null) {
      return;
    }
    executeSelectByPkWithoutTenantFilter(
        connection,
        SELECT_SQL,
        (stmt) -> {
          stmt.setString(1, this.userId);
          stmt.setString(2, this.tenantId);
        },
        (rs) -> {
          this.userId = rs.getString("user_id");
          this.userName = rs.getString("user_name");
          this.role = Role.getEnum(rs.getString("role_cd"));
          this.plan = Plan.getEnum(rs.getString("plan_cd"));
          this.registerDate = new OriginalDateTime(rs.getString("register_date"));
          this.registerUser = rs.getString("register_user");
          this.tenantId = rs.getString("tenant_id");
        },
        "SES_AI_WEBAPP_M_USER.selectByPk");
  }

  /**
   * ユーザーをユーザーID で取得します（テナントID条件なし、システム管理者用）.
   *
   * @param connection DBコネクション
   * @throws SQLException SQL実行エラー
   */
  public void selectByPkWithoutTenantId(Connection connection) throws SQLException {
    if (this.userId == null) {
      return;
    }
    executeSelectByPkWithoutTenantFilter(
        connection,
        SELECT_WITHOUT_TENANT_ID_SQL,
        (stmt) -> stmt.setString(1, this.userId),
        (rs) -> {
          this.userId = rs.getString("user_id");
          this.userName = rs.getString("user_name");
          this.role = Role.getEnum(rs.getString("role_cd"));
          this.plan = Plan.getEnum(rs.getString("plan_cd"));
          this.registerDate = new OriginalDateTime(rs.getString("register_date"));
          this.registerUser = rs.getString("register_user");
          this.tenantId = rs.getString("tenant_id");
        },
        "SES_AI_WEBAPP_M_USER.selectByPkWithoutTenantId");
  }

  @Override
  public boolean updateByPk(Connection connection) throws SQLException {
    if (this.userId == null) {
      return false;
    }
    return executeUpdateByPkWithoutTenantFilter(
        connection,
        UPDATE_SQL,
        (stmt) -> {
          stmt.setString(1, this.userId);
          stmt.setString(2, this.userName);
          stmt.setString(3, this.role == null ? null : this.role.getCode());
          stmt.setString(4, this.plan == null ? null : this.plan.getCode());
          stmt.setTimestamp(5, this.registerDate == null ? null : this.registerDate.toTimestamp());
          stmt.setString(6, this.registerUser);
          stmt.setString(7, this.userId);
          stmt.setString(8, this.tenantId);
        },
        "SES_AI_WEBAPP_M_USER.updateByPk");
  }

  @Override
  public boolean deleteByPk(Connection connection) throws SQLException {
    if (this.userId == null) {
      return false;
    }
    return executeDeleteByPkWithoutTenantFilter(
        connection,
        DELETE_SQL,
        (stmt) -> {
          stmt.setString(1, this.userId);
          stmt.setString(2, this.tenantId);
        },
        "SES_AI_WEBAPP_M_USER.deleteByPk");
  }
}
