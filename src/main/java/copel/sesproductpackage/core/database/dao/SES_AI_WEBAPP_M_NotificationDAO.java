package copel.sesproductpackage.core.database.dao;

import copel.sesproductpackage.core.database.SES_AI_WEBAPP_M_NOTIFICATION;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SES_AI_WEBAPP_M_NOTIFICATION 用 DAO.
 *
 * <p>プッシュ通知デバイス登録マスタの CRUD 操作を担当します。 テナント ID による絞り込みが必須です。
 *
 * @author Copel Co., Ltd.
 */
public class SES_AI_WEBAPP_M_NotificationDAO extends EntityBaseDAO {

  private static final Logger log = LoggerFactory.getLogger(SES_AI_WEBAPP_M_NotificationDAO.class);

  private static final String MAPPER_CLASS_NAME =
      "copel.sesproductpackage.core.database.mapper.NotificationMapper";

  /**
   * コンストラクタ.
   *
   * @param sqlSessionFactory SqlSessionFactory インスタンス
   * @param connection DBコネクション
   */
  public SES_AI_WEBAPP_M_NotificationDAO(
      SqlSessionFactory sqlSessionFactory, Connection connection) {
    super(sqlSessionFactory, connection);
  }

  /**
   * コンストラクタ（SqlSessionFactory は EntityBase の ThreadLocal から取得）.
   *
   * <p>Entity から DAO を呼び出す際に使用するコンストラクタ。 SqlSessionFactory は {@code
   * EntityBase.getSqlSessionFactory()} で自動取得されます。
   *
   * @param connection DBコネクション
   */
  public SES_AI_WEBAPP_M_NotificationDAO(Connection connection) {
    this(SES_AI_WEBAPP_M_NOTIFICATION.getSqlSessionFactory(), connection);
  }

  /**
   * プッシュ通知デバイス登録を挿入します.
   *
   * @param entity 挿入対象のSES_AI_WEBAPP_M_NOTIFICATION
   * @return 挿入行数
   * @throws SQLException DB操作エラーの場合
   */
  public int insert(SES_AI_WEBAPP_M_NOTIFICATION entity) throws SQLException {
    if (sqlSessionFactory == null) {
      throw new SQLException("SqlSessionFactory not initialized");
    }

    try (SqlSession session = sqlSessionFactory.openSession()) {
      Object mapper = session.getMapper(Class.forName(MAPPER_CLASS_NAME));
      java.lang.reflect.Method method =
          mapper.getClass().getMethod("insertNotification", SES_AI_WEBAPP_M_NOTIFICATION.class);
      int result = (int) method.invoke(mapper, entity);
      session.commit();

      if (log.isDebugEnabled()) {
        log.debug(
            "[NotificationDAO] Insert completed: notificationId={}, tenantId={}, result={}",
            entity.getNotificationId(),
            entity.getTenantId(),
            result);
      }

      return result;
    } catch (Exception e) {
      throw new SQLException("Failed to insert notification", e);
    }
  }

  /**
   * プッシュ通知デバイス登録を PK とテナント ID で取得します.
   *
   * @param pkValue 通知デバイスID
   * @param tenantId テナント ID
   * @return 検索結果のSES_AI_WEBAPP_M_NOTIFICATION、またはnull
   * @throws SQLException DB操作エラーの場合
   */
  public SES_AI_WEBAPP_M_NOTIFICATION selectByPk(String pkValue, String tenantId)
      throws SQLException {
    if (sqlSessionFactory == null) {
      throw new SQLException("SqlSessionFactory not initialized");
    }

    try (SqlSession session = sqlSessionFactory.openSession()) {
      Object mapper = session.getMapper(Class.forName(MAPPER_CLASS_NAME));
      java.lang.reflect.Method method =
          mapper.getClass().getMethod("selectNotificationByPk", Map.class);

      Map<String, Object> params = new HashMap<>();
      params.put("notificationId", pkValue);
      params.put("tenantId", tenantId);

      SES_AI_WEBAPP_M_NOTIFICATION result =
          (SES_AI_WEBAPP_M_NOTIFICATION) method.invoke(mapper, params);

      if (log.isDebugEnabled()) {
        log.debug(
            "[NotificationDAO] Select by PK completed: notificationId={}, tenantId={}, result={}",
            pkValue,
            tenantId,
            result != null ? "found" : "not found");
      }

      return result;
    } catch (Exception e) {
      throw new SQLException("Failed to select notification by PK", e);
    }
  }

  /**
   * プッシュ通知デバイス登録を更新します.
   *
   * @param entity 更新対象のSES_AI_WEBAPP_M_NOTIFICATION
   * @return 更新行数
   * @throws SQLException DB操作エラーの場合
   */
  public int update(SES_AI_WEBAPP_M_NOTIFICATION entity) throws SQLException {
    if (sqlSessionFactory == null) {
      throw new SQLException("SqlSessionFactory not initialized");
    }

    try (SqlSession session = sqlSessionFactory.openSession()) {
      Object mapper = session.getMapper(Class.forName(MAPPER_CLASS_NAME));
      java.lang.reflect.Method method =
          mapper.getClass().getMethod("updateNotificationByPk", SES_AI_WEBAPP_M_NOTIFICATION.class);
      int result = (int) method.invoke(mapper, entity);
      session.commit();

      if (log.isDebugEnabled()) {
        log.debug(
            "[NotificationDAO] Update completed: notificationId={}, tenantId={}, result={}",
            entity.getNotificationId(),
            entity.getTenantId(),
            result);
      }

      return result;
    } catch (Exception e) {
      throw new SQLException("Failed to update notification", e);
    }
  }

  /**
   * プッシュ通知デバイス登録を PK とテナント ID で削除します.
   *
   * @param pkValue 通知デバイスID
   * @param tenantId テナント ID
   * @return 削除行数
   * @throws SQLException DB操作エラーの場合
   */
  public int delete(String pkValue, String tenantId) throws SQLException {
    if (sqlSessionFactory == null) {
      throw new SQLException("SqlSessionFactory not initialized");
    }

    try (SqlSession session = sqlSessionFactory.openSession()) {
      Object mapper = session.getMapper(Class.forName(MAPPER_CLASS_NAME));
      java.lang.reflect.Method method =
          mapper.getClass().getMethod("deleteNotificationByPk", Map.class);

      Map<String, Object> params = new HashMap<>();
      params.put("notificationId", pkValue);
      params.put("tenantId", tenantId);

      int result = (int) method.invoke(mapper, params);
      session.commit();

      if (log.isDebugEnabled()) {
        log.debug(
            "[NotificationDAO] Delete completed: notificationId={}, tenantId={}, result={}",
            pkValue,
            tenantId,
            result);
      }

      return result;
    } catch (Exception e) {
      throw new SQLException("Failed to delete notification", e);
    }
  }
}
