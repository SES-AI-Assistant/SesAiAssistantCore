package copel.sesproductpackage.core.database;

import copel.sesproductpackage.core.database.base.EntityBase;
import copel.sesproductpackage.core.database.converter.OriginalDateTimeConverter;
import copel.sesproductpackage.core.unit.OriginalDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Id;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.Table;
import java.sql.Connection;
import java.sql.SQLException;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * 【Entityクラス】 テナント情報マスタ(SES_AI_M_TENANT)テーブル.
 *
 * <p>JPA マッピング対応。テナント情報の登録、取得、更新、削除機能を提供します。
 *
 * @author Copel Co., Ltd.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Entity
@Table(name = "SES_AI_M_TENANT")
public class SES_AI_M_TENANT extends EntityBase {

  /** static EntityManager フィールド */
  private static EntityManager entityManager;

  /** 【PK】 テナントID / tenant_id */
  @Id
  @Column(name = "tenant_id")
  private String tenantId;

  /** テナント名 / tenant_name */
  @Column(name = "tenant_name")
  private String tenantName;

  /** ステータス区分 / tenant_status_cd */
  @Column(name = "tenant_status_cd")
  private String tenantStatusCd;

  /** 登録日時 / register_date */
  @Column(name = "register_date")
  @Convert(converter = OriginalDateTimeConverter.class)
  private OriginalDateTime registerDate;

  /** 登録ユーザー / register_user */
  @Column(name = "register_user")
  private String registerUser;

  /**
   * EntityManager を設定します.
   *
   * @param em EntityManager
   */
  public static void setEntityManager(EntityManager em) {
    entityManager = em;
  }

  /**
   * コンストラクタ.
   *
   * @param tenantId テナントID
   */
  public SES_AI_M_TENANT(String tenantId) {
    super(tenantId);
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
    if (this.tenantId == null) {
      return;
    }
    if (entityManager == null) {
      throw new SQLException("EntityManager not initialized");
    }
    try {
      SES_AI_M_TENANT found = entityManager.find(SES_AI_M_TENANT.class, this.tenantId);
      if (found != null) {
        this.tenantId = found.tenantId;
        this.tenantName = found.tenantName;
        this.tenantStatusCd = found.tenantStatusCd;
        this.registerDate = found.registerDate;
        this.registerUser = found.registerUser;
      }
    } catch (PersistenceException e) {
      throw new SQLException("Failed to select: " + e.getMessage(), e);
    }
  }

  @Override
  public boolean updateByPk(Connection connection) throws SQLException {
    if (this.tenantId == null) {
      return false;
    }
    if (entityManager == null) {
      throw new SQLException("EntityManager not initialized");
    }
    try {
      SES_AI_M_TENANT merged = entityManager.merge(this);
      entityManager.flush();
      return true;
    } catch (PersistenceException e) {
      throw new SQLException("Failed to update: " + e.getMessage(), e);
    }
  }

  @Override
  public boolean deleteByPk(Connection connection) throws SQLException {
    if (this.tenantId == null) {
      return false;
    }
    if (entityManager == null) {
      throw new SQLException("EntityManager not initialized");
    }
    try {
      SES_AI_M_TENANT toDelete = entityManager.find(SES_AI_M_TENANT.class, this.tenantId);
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
