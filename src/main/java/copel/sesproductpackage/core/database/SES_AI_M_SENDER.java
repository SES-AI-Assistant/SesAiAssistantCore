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
 * 送信者マスタテーブルのエンティティ.
 *
 * <p>JPA マッピング対応。テナント隔離はアプリケーションレベルで実装します。
 *
 * @author Copel Co., Ltd.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Entity
@Table(name = "SES_AI_M_SENDER")
public class SES_AI_M_SENDER extends EntityBase {

  /** static EntityManager フィールド */
  private static EntityManager entityManager;

  /** 【PK】 送信者ID / from_id */
  @Id
  @Column(name = "from_id")
  private String fromId;

  /** 送信者名 / from_name */
  @Column(name = "from_name")
  private String fromName;

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
  public SES_AI_M_SENDER(String tenantId) {
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
    if (this.fromId == null) {
      return;
    }
    if (entityManager == null) {
      throw new SQLException("EntityManager not initialized");
    }
    try {
      SES_AI_M_SENDER found = entityManager.find(SES_AI_M_SENDER.class, this.fromId);
      if (found != null) {
        this.fromId = found.fromId;
        this.fromName = found.fromName;
        this.registerDate = found.registerDate;
        this.registerUser = found.registerUser;
      }
    } catch (PersistenceException e) {
      throw new SQLException("Failed to select: " + e.getMessage(), e);
    }
  }

  @Override
  public boolean updateByPk(Connection connection) throws SQLException {
    if (this.fromId == null) {
      return false;
    }
    if (entityManager == null) {
      throw new SQLException("EntityManager not initialized");
    }
    try {
      SES_AI_M_SENDER merged = entityManager.merge(this);
      entityManager.flush();
      return true;
    } catch (PersistenceException e) {
      throw new SQLException("Failed to update: " + e.getMessage(), e);
    }
  }

  @Override
  public boolean deleteByPk(Connection connection) throws SQLException {
    if (this.fromId == null) {
      return false;
    }
    if (entityManager == null) {
      throw new SQLException("EntityManager not initialized");
    }
    try {
      SES_AI_M_SENDER toDelete = entityManager.find(SES_AI_M_SENDER.class, this.fromId);
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
   * 送信者マスタに該当IDの送信者が存在するかを判定する.
   *
   * @param connection DB接続
   * @return 存在する場合はtrue、存在しない場合またはconnectionがnullの場合はfalse
   * @throws SQLException SQL実行エラー時
   */
  public boolean isExist(Connection connection) throws SQLException {
    if (this.fromId == null) {
      return false;
    }
    if (entityManager == null) {
      throw new SQLException("EntityManager not initialized");
    }
    try {
      SES_AI_M_SENDER found = entityManager.find(SES_AI_M_SENDER.class, this.fromId);
      return found != null;
    } catch (PersistenceException e) {
      throw new SQLException("Failed to check exist: " + e.getMessage(), e);
    }
  }
}
