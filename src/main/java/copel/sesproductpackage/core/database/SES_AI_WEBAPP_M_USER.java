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
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Setter;
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

  /** INSERT文. */
  private static final String INSERT_SQL =
      "INSERT INTO SES_AI_WEBAPP_M_USER (user_id, user_name, role_cd, plan_cd, register_date, "
          + "register_user) VALUES (?, ?, ?, ?, ?, ?)";

  /** SELECT文（tenant_id 条件を手書きする。実行テンプレートが TenantFilterSql を通さないため）. */
  private static final String SELECT_SQL =
      "SELECT user_id, user_name, role_cd, plan_cd, register_date, register_user, tenant_id FROM "
          + "SES_AI_WEBAPP_M_USER WHERE user_id = ? AND tenant_id = ?";

  /** SELECT文（tenant_id条件なし）. */
  private static final String SELECT_WITHOUT_TENANT_ID_SQL =
      "SELECT user_id, user_name, role_cd, plan_cd, register_date, register_user, tenant_id FROM "
          + "SES_AI_WEBAPP_M_USER WHERE user_id = ?";

  /** UPDATE文（tenant_id 条件を手書きする。実行テンプレートが TenantFilterSql を通さないため）. */
  private static final String UPDATE_SQL =
      "UPDATE SES_AI_WEBAPP_M_USER SET user_id = ?, user_name = ?, role_cd = ?, plan_cd = ?, "
          + "register_date = ?, register_user = ? WHERE user_id = ? AND tenant_id = ?";

  /** UPDATE文（tenant_id絞込み条件なし、システム管理者用）. */
  private static final String UPDATE_WITHOUT_TENANT_ID_SQL =
      "UPDATE SES_AI_WEBAPP_M_USER SET user_id = ?, user_name = ?, role_cd = ?, plan_cd = ?, "
          + "register_date = ?, register_user = ?, tenant_id = ? WHERE user_id = ?";

  /** DELETE文（tenant_id 条件を手書きする。実行テンプレートが TenantFilterSql を通さないため）. */
  private static final String DELETE_SQL =
      "DELETE FROM SES_AI_WEBAPP_M_USER WHERE user_id = ? AND tenant_id = ?";

  /** DELETE文（tenant_id絞込み条件なし、システム管理者用）. */
  private static final String DELETE_WITHOUT_TENANT_ID_SQL =
      "DELETE FROM SES_AI_WEBAPP_M_USER WHERE user_id = ?";

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

  /**
   * DB から行を取得できたか（登録済みか）を示すフラグ.
   *
   * <p>直近の SELECT（selectByPk / selectByPkWithoutTenantId）で行が見つかった場合 true、見つからなかった場合 false。DB
   * 列ではないため Column は付与しない。
   */
  @Setter(AccessLevel.NONE)
  private boolean registered;

  /**
   * 直近の SELECT で DB から行を取得できたか（登録済みユーザーか）を返します.
   *
   * <p>未登録ユーザー（行なし）と、登録済みでロールが null・定義外のユーザーを区別するために用いる。ロールの値だけでは両者を区別できない（どちらも role が
   * null）ため、行の有無を明示的に保持する。
   *
   * <p>Lot 経由で取得したエンティティは false のまま（単体の SELECT を行っていないため）。
   *
   * @return 行が取得できていれば true、取得できていない・未検索であれば false
   */
  public boolean isRegistered() {
    return this.registered;
  }

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
   * <p>システム利用不可（ロール 00）または未設定（null・定義外）のユーザーは、プランに関係なく空集合を返します。ロール 00
   * は業務機能を一切利用させないロールであり、プレミアムプランのプラン権限で業務機能が開放されてはならないためです。
   *
   * @return 権限セット（システム利用不可の場合は空集合）
   */
  public Set<Permission> getPermissions() {
    if (!hasSystemUseAuth()) {
      return new HashSet<>();
    }
    // hasSystemUseAuth() が true のため、ロールは null ではない
    Set<Permission> permissions = new HashSet<>(this.role.getPermissions());
    if (this.plan != null) {
      permissions.addAll(this.plan.getPermissions());
    }
    if (!isEligibleForRegisterInfoListImport()) {
      permissions.remove(Permission.REGISTER_INFO_LIST_IMPORT);
    }
    return permissions;
  }

  /**
   * INSERT 文と UPDATE 文で共通の列（1番目から6番目）に、項目の値を順に設定します.
   *
   * @param stmt 値を設定する準備文
   * @throws SQLException 値の設定に失敗した場合
   */
  private void bindCommonColumns(final PreparedStatement stmt) throws SQLException {
    stmt.setString(1, this.userId);
    stmt.setString(2, this.userName);
    stmt.setString(3, this.role == null ? null : this.role.getCode());
    stmt.setString(4, this.plan == null ? null : this.plan.getCode());
    stmt.setTimestamp(5, this.registerDate == null ? null : this.registerDate.toTimestamp());
    stmt.setString(6, this.registerUser);
  }

  /**
   * {@link Permission#REGISTER_INFO_LIST_IMPORT} を付与する条件（プレミアムプラン以上）を満たすか.
   *
   * <p>一般ロール以上の条件は {@link #getPermissions()} で満たしている。システム利用不可のロールは権限を返す前に空集合にするため、ここでは判定しない。
   *
   * @return 付与してよい場合 true
   */
  private boolean isEligibleForRegisterInfoListImport() {
    return this.plan != null && this.plan != Plan.FREE;
  }

  @Override
  public int insert(Connection connection) throws SQLException {
    return executeInsertWithoutTenantFilter(
        connection, INSERT_SQL, this::bindCommonColumns, "SES_AI_WEBAPP_M_USER.insert");
  }

  @Override
  public boolean selectByPk(Connection connection) throws SQLException {
    if (this.userId == null) {
      // 検索しないため未登録扱いとする（registered は false のまま）
      this.registered = false;
      return false;
    }
    this.registered =
        executeSelectByPkWithoutTenantFilter(
            connection,
            SELECT_SQL,
            (stmt) -> {
              stmt.setString(1, this.userId);
              stmt.setString(2, this.tenantId);
            },
            this::fillFromResultSet,
            "SES_AI_WEBAPP_M_USER.selectByPk");
    return this.registered;
  }

  /**
   * ユーザーをユーザーID で取得します（テナントID条件なし、システム管理者用）.
   *
   * @param connection DBコネクション
   * @throws SQLException SQL実行エラー
   */
  public void selectByPkWithoutTenantId(Connection connection) throws SQLException {
    if (this.userId == null) {
      // 検索しないため未登録扱いとする（registered は false のまま）
      this.registered = false;
      return;
    }
    this.registered =
        executeSelectByPkWithoutTenantFilter(
            connection,
            SELECT_WITHOUT_TENANT_ID_SQL,
            (stmt) -> stmt.setString(1, this.userId),
            this::fillFromResultSet,
            "SES_AI_WEBAPP_M_USER.selectByPkWithoutTenantId");
  }

  /**
   * 1行分の結果セットの値を、このユーザーの項目へ設定します.
   *
   * <p>ロールとプランは列のコードから列挙値へ変換する。プランのコードが定義外の場合は {@link Plan#getEnum(String)} が FREE を返す。
   *
   * @param rs 1行分の結果セット
   * @throws SQLException 値の読み取りに失敗した場合
   */
  private void fillFromResultSet(final ResultSet rs) throws SQLException {
    this.userId = rs.getString("user_id");
    this.userName = rs.getString("user_name");
    this.role = Role.getEnum(rs.getString("role_cd"));
    this.plan = Plan.getEnum(rs.getString("plan_cd"));
    this.registerDate = new OriginalDateTime(rs.getString("register_date"));
    this.registerUser = rs.getString("register_user");
    this.tenantId = rs.getString("tenant_id");
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
          bindCommonColumns(stmt);
          stmt.setString(7, this.userId);
          stmt.setString(8, this.tenantId);
        },
        "SES_AI_WEBAPP_M_USER.updateByPk");
  }

  /**
   * ユーザーをユーザーID で更新します（テナントID絞り込み条件なし、システム管理者用）. テナントIDで絞込みはしないが、更新対象にはテナントIDは含まれることに注意.
   *
   * @param connection DBコネクション
   * @return 更新に成功した場合 true、失敗した場合 false
   * @throws SQLException SQL実行エラー
   */
  public boolean updateByPkWithoutTenantId(Connection connection) throws SQLException {
    if (this.userId == null) {
      return false;
    }
    return executeUpdateByPkWithoutTenantFilter(
        connection,
        UPDATE_WITHOUT_TENANT_ID_SQL,
        (stmt) -> {
          stmt.setString(1, this.userId);
          stmt.setString(2, this.userName);
          stmt.setString(3, this.role == null ? null : this.role.getCode());
          stmt.setString(4, this.plan == null ? null : this.plan.getCode());
          stmt.setTimestamp(5, this.registerDate == null ? null : this.registerDate.toTimestamp());
          stmt.setString(6, this.registerUser);
          stmt.setString(7, this.tenantId);
          stmt.setString(8, this.userId);
        },
        "SES_AI_WEBAPP_M_USER.updateByPkWithoutTenantId");
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

  /**
   * ユーザーをユーザーIDで削除します（テナントID絞り込み条件なし、システム管理者用）.
   *
   * @param connection DBコネクション
   * @return 削除に成功した場合 true、失敗した場合 false
   * @throws SQLException SQL実行エラー
   */
  public boolean deleteByPkWithoutTenantIdFilter(Connection connection) throws SQLException {
    if (this.userId == null) {
      return false;
    }
    return executeDeleteByPkWithoutTenantFilter(
        connection,
        DELETE_WITHOUT_TENANT_ID_SQL,
        (stmt) -> stmt.setString(1, this.userId),
        "SES_AI_WEBAPP_M_USER.deleteByPkWithoutTenantIdFilter");
  }
}
