package copel.sesproductpackage.core.database;

import copel.sesproductpackage.core.database.base.EntityBase;
import copel.sesproductpackage.core.database.converter.OriginalDateTimeConverter;
import copel.sesproductpackage.core.unit.OriginalDateTime;
import copel.sesproductpackage.core.unit.Permission;
import copel.sesproductpackage.core.unit.Plan;
import copel.sesproductpackage.core.unit.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Id;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.Table;
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
 * <p>JPA マッピング対応。システム管理者向けユーザー情報を管理します。
 *
 * @author Copel Co., Ltd.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Entity
@Table(name = "SES_AI_WEBAPP_M_USER")
public class SES_AI_WEBAPP_M_USER extends EntityBase {

  /** static EntityManager フィールド */
  private static EntityManager entityManager;

  /** 【PK】 ユーザーID / user_id */
  @Id
  @Column(name = "user_id")
  private String userId;

  /** ユーザー名 / user_name */
  @Column(name = "user_name")
  private String userName;

  /** ロール / role_cd */
  @Column(name = "role_cd")
  private Role role;

  /** プラン / plan_cd */
  @Column(name = "plan_cd")
  private Plan plan;

  /** 登録日時 / register_date */
  @Column(name = "register_date")
  @Convert(converter = OriginalDateTimeConverter.class)
  private OriginalDateTime registerDate;

  /** 登録ユーザー / register_user */
  @Column(name = "register_user")
  private String registerUser;

  /**
   * コンストラクタ.
   *
   * @param tenantId テナントID
   */
  public SES_AI_WEBAPP_M_USER(String tenantId) {
    super(tenantId);
  }

  /**
   * EntityManager を設定します.
   *
   * @param em EntityManager
   */
  public static void setEntityManager(EntityManager em) {
    entityManager = em;
  }

  /** tenantIdをあとから設定できるコンストラクタ（ログイン時のユーザーID検索時など）. */
  public SES_AI_WEBAPP_M_USER() {
    super("_temp_");
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
    if (entityManager == null) {
      throw new SQLException("EntityManager not initialized");
    }
    try {
      entityManager.persist(this);
      entityManager.flush();
      return 1;
    } catch (PersistenceException e) {
      throw new SQLException("Failed to insert: " + e.getMessage(), e);
    }
  }

  @Override
  public void selectByPk(Connection connection) throws SQLException {
    if (this.userId == null) {
      return;
    }
    if (entityManager == null) {
      throw new SQLException("EntityManager not initialized");
    }
    try {
      SES_AI_WEBAPP_M_USER found = entityManager.find(SES_AI_WEBAPP_M_USER.class, this.userId);
      if (found != null) {
        this.userId = found.userId;
        this.userName = found.userName;
        this.role = found.role;
        this.plan = found.plan;
        this.registerDate = found.registerDate;
        this.registerUser = found.registerUser;
        this.tenantId = found.tenantId;
      }
    } catch (PersistenceException e) {
      throw new SQLException("Failed to select: " + e.getMessage(), e);
    }
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
    if (entityManager == null) {
      throw new SQLException("EntityManager not initialized");
    }
    try {
      SES_AI_WEBAPP_M_USER found = entityManager.find(SES_AI_WEBAPP_M_USER.class, this.userId);
      if (found != null) {
        this.userId = found.userId;
        this.userName = found.userName;
        this.role = found.role;
        this.plan = found.plan;
        this.registerDate = found.registerDate;
        this.registerUser = found.registerUser;
        this.tenantId = found.tenantId;
      }
    } catch (PersistenceException e) {
      throw new SQLException("Failed to select: " + e.getMessage(), e);
    }
  }

  @Override
  public boolean updateByPk(Connection connection) throws SQLException {
    if (this.userId == null) {
      return false;
    }
    if (entityManager == null) {
      throw new SQLException("EntityManager not initialized");
    }
    try {
      SES_AI_WEBAPP_M_USER merged = entityManager.merge(this);
      entityManager.flush();
      return true;
    } catch (PersistenceException e) {
      throw new SQLException("Failed to update: " + e.getMessage(), e);
    }
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
    if (entityManager == null) {
      throw new SQLException("EntityManager not initialized");
    }
    try {
      SES_AI_WEBAPP_M_USER merged = entityManager.merge(this);
      entityManager.flush();
      return true;
    } catch (PersistenceException e) {
      throw new SQLException("Failed to update: " + e.getMessage(), e);
    }
  }

  @Override
  public boolean deleteByPk(Connection connection) throws SQLException {
    if (this.userId == null) {
      return false;
    }
    if (entityManager == null) {
      throw new SQLException("EntityManager not initialized");
    }
    try {
      SES_AI_WEBAPP_M_USER toDelete = entityManager.find(SES_AI_WEBAPP_M_USER.class, this.userId);
      if (toDelete != null) {
        entityManager.remove(toDelete);
        entityManager.flush();
        return true;
      }
      return false;
    } catch (PersistenceException e) {
      throw new SQLException("Failed to delete: " + e.getMessage(), e);
    }
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
    if (entityManager == null) {
      throw new SQLException("EntityManager not initialized");
    }
    try {
      SES_AI_WEBAPP_M_USER toDelete = entityManager.find(SES_AI_WEBAPP_M_USER.class, this.userId);
      if (toDelete != null) {
        entityManager.remove(toDelete);
        entityManager.flush();
        return true;
      }
      return false;
    } catch (PersistenceException e) {
      throw new SQLException("Failed to delete: " + e.getMessage(), e);
    }
  }
}
