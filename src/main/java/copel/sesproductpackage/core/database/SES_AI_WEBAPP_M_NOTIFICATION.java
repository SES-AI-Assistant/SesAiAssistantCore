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
 * プッシュ通知デバイス登録マスタのエンティティ.
 *
 * <p>JPA マッピング対応。テナント隔離はアプリケーションレベルで実装します。
 *
 * @author Copel Co., Ltd.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Entity
@Table(name = "SES_AI_WEBAPP_M_NOTIFICATION")
public class SES_AI_WEBAPP_M_NOTIFICATION extends EntityBase {

  /** static EntityManager フィールド */
  private static EntityManager entityManager;

  /** 【PK】 通知デバイスID / notification_id */
  @Id
  @Column(name = "notification_id")
  private String notificationId;

  /** ユーザーID / user_id */
  @Column(name = "user_id")
  private String userId;

  /** デバイスタイプ / device_type */
  @Column(name = "device_type")
  private String deviceType;

  /** デバイス名 / device_name */
  @Column(name = "device_name")
  private String deviceName;

  /** Push API endpoint URL / push_notification_endpoint */
  @Column(name = "push_notification_endpoint")
  private String pushNotificationEndpoint;

  /** ECDH public key (base64) / p256dh */
  @Column(name = "p256dh")
  private String p256dh;

  /** HMAC authentication token (base64) / auth */
  @Column(name = "auth")
  private String auth;

  /** 有効フラグ / enabled */
  @Column(name = "enabled")
  private Boolean enabled;

  /** 全件通知フラグ / notify_all_match */
  @Column(name = "notify_all_match")
  private Boolean notifyAllMatch;

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
  public SES_AI_WEBAPP_M_NOTIFICATION(String tenantId) {
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
    if (this.notificationId == null) {
      return;
    }
    if (entityManager == null) {
      throw new SQLException("EntityManager not initialized");
    }
    try {
      SES_AI_WEBAPP_M_NOTIFICATION found =
          entityManager.find(SES_AI_WEBAPP_M_NOTIFICATION.class, this.notificationId);
      if (found != null) {
        this.notificationId = found.notificationId;
        this.userId = found.userId;
        this.deviceType = found.deviceType;
        this.deviceName = found.deviceName;
        this.pushNotificationEndpoint = found.pushNotificationEndpoint;
        this.p256dh = found.p256dh;
        this.auth = found.auth;
        this.enabled = found.enabled;
        this.notifyAllMatch = found.notifyAllMatch;
        this.registerDate = found.registerDate;
        this.registerUser = found.registerUser;
      }
    } catch (PersistenceException e) {
      throw new SQLException("Failed to select: " + e.getMessage(), e);
    }
  }

  @Override
  public boolean updateByPk(Connection connection) throws SQLException {
    if (this.notificationId == null) {
      return false;
    }
    if (entityManager == null) {
      throw new SQLException("EntityManager not initialized");
    }
    try {
      SES_AI_WEBAPP_M_NOTIFICATION merged = entityManager.merge(this);
      entityManager.flush();
      return true;
    } catch (PersistenceException e) {
      throw new SQLException("Failed to update: " + e.getMessage(), e);
    }
  }

  @Override
  public boolean deleteByPk(Connection connection) throws SQLException {
    if (this.notificationId == null) {
      return false;
    }
    if (entityManager == null) {
      throw new SQLException("EntityManager not initialized");
    }
    try {
      SES_AI_WEBAPP_M_NOTIFICATION toDelete =
          entityManager.find(SES_AI_WEBAPP_M_NOTIFICATION.class, this.notificationId);
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
