package copel.sesproductpackage.core.database.dao;

import copel.sesproductpackage.core.database.SES_AI_M_SENDER;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SES_AI_M_SENDER 用 DAO.
 *
 * <p>送信者マスタの CRUD 操作を担当します。 テナント ID による絞り込みが必須です。
 *
 * @author Copel Co., Ltd.
 */
public class SES_AI_M_SenderDAO extends EntityBaseDAO {

  private static final Logger log = LoggerFactory.getLogger(SES_AI_M_SenderDAO.class);

  private static final String MAPPER_CLASS_NAME =
      "copel.sesproductpackage.core.database.mapper.SenderMapper";

  /**
   * コンストラクタ.
   *
   * @param sqlSessionFactory SqlSessionFactory インスタンス
   * @param connection DBコネクション
   */
  public SES_AI_M_SenderDAO(SqlSessionFactory sqlSessionFactory, Connection connection) {
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
  public SES_AI_M_SenderDAO(Connection connection) {
    this(SES_AI_M_SENDER.getSqlSessionFactory(), connection);
  }

  /**
   * 送信者を挿入します.
   *
   * @param entity 挿入対象のSES_AI_M_SENDER
   * @return 挿入行数
   * @throws SQLException DB操作エラーの場合
   */
  public int insert(SES_AI_M_SENDER entity) throws SQLException {
    if (sqlSessionFactory == null) {
      throw new SQLException("SqlSessionFactory not initialized");
    }

    try (SqlSession session = sqlSessionFactory.openSession()) {
      Object mapper = session.getMapper(Class.forName(MAPPER_CLASS_NAME));
      java.lang.reflect.Method method =
          mapper.getClass().getMethod("insertSender", SES_AI_M_SENDER.class);
      int result = (int) method.invoke(mapper, entity);
      session.commit();

      if (log.isDebugEnabled()) {
        log.debug(
            "[SenderDAO] Insert completed: fromId={}, tenantId={}, result={}",
            entity.getFromId(),
            entity.getTenantId(),
            result);
      }

      return result;
    } catch (Exception e) {
      throw new SQLException("Failed to insert sender", e);
    }
  }

  /**
   * 送信者を PK とテナント ID で取得します.
   *
   * @param pkValue 送信者ID
   * @param tenantId テナント ID
   * @return 検索結果のSES_AI_M_SENDER、またはnull
   * @throws SQLException DB操作エラーの場合
   */
  public SES_AI_M_SENDER selectByPk(String pkValue, String tenantId) throws SQLException {
    if (sqlSessionFactory == null) {
      throw new SQLException("SqlSessionFactory not initialized");
    }

    try (SqlSession session = sqlSessionFactory.openSession()) {
      Object mapper = session.getMapper(Class.forName(MAPPER_CLASS_NAME));
      java.lang.reflect.Method method = mapper.getClass().getMethod("selectSenderByPk", Map.class);

      Map<String, Object> params = new HashMap<>();
      params.put("fromId", pkValue);
      params.put("tenantId", tenantId);

      SES_AI_M_SENDER result = (SES_AI_M_SENDER) method.invoke(mapper, params);

      if (log.isDebugEnabled()) {
        log.debug(
            "[SenderDAO] Select by PK completed: fromId={}, tenantId={}, result={}",
            pkValue,
            tenantId,
            result != null ? "found" : "not found");
      }

      return result;
    } catch (Exception e) {
      throw new SQLException("Failed to select sender by PK", e);
    }
  }

  /**
   * 送信者を更新します.
   *
   * @param entity 更新対象のSES_AI_M_SENDER
   * @return 更新行数
   * @throws SQLException DB操作エラーの場合
   */
  public int update(SES_AI_M_SENDER entity) throws SQLException {
    if (sqlSessionFactory == null) {
      throw new SQLException("SqlSessionFactory not initialized");
    }

    try (SqlSession session = sqlSessionFactory.openSession()) {
      Object mapper = session.getMapper(Class.forName(MAPPER_CLASS_NAME));
      java.lang.reflect.Method method =
          mapper.getClass().getMethod("updateSenderByPk", SES_AI_M_SENDER.class);
      int result = (int) method.invoke(mapper, entity);
      session.commit();

      if (log.isDebugEnabled()) {
        log.debug(
            "[SenderDAO] Update completed: fromId={}, tenantId={}, result={}",
            entity.getFromId(),
            entity.getTenantId(),
            result);
      }

      return result;
    } catch (Exception e) {
      throw new SQLException("Failed to update sender", e);
    }
  }

  /**
   * 送信者を PK とテナント ID で削除します.
   *
   * @param pkValue 送信者ID
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
      java.lang.reflect.Method method = mapper.getClass().getMethod("deleteSenderByPk", Map.class);

      Map<String, Object> params = new HashMap<>();
      params.put("fromId", pkValue);
      params.put("tenantId", tenantId);

      int result = (int) method.invoke(mapper, params);
      session.commit();

      if (log.isDebugEnabled()) {
        log.debug(
            "[SenderDAO] Delete completed: fromId={}, tenantId={}, result={}",
            pkValue,
            tenantId,
            result);
      }

      return result;
    } catch (Exception e) {
      throw new SQLException("Failed to delete sender", e);
    }
  }

  /**
   * 送信者の存在確認を行います.
   *
   * @param pkValue 送信者ID
   * @param tenantId テナント ID
   * @return 存在する場合 true、しない場合 false
   * @throws SQLException DB操作エラーの場合
   */
  public boolean exists(String pkValue, String tenantId) throws SQLException {
    if (sqlSessionFactory == null) {
      throw new SQLException("SqlSessionFactory not initialized");
    }

    try (SqlSession session = sqlSessionFactory.openSession()) {
      Object mapper = session.getMapper(Class.forName(MAPPER_CLASS_NAME));
      java.lang.reflect.Method method = mapper.getClass().getMethod("checkSenderExists", Map.class);

      Map<String, Object> params = new HashMap<>();
      params.put("fromId", pkValue);
      params.put("tenantId", tenantId);

      Boolean result = (Boolean) method.invoke(mapper, params);

      if (log.isDebugEnabled()) {
        log.debug(
            "[SenderDAO] Exists check completed: fromId={}, tenantId={}, result={}",
            pkValue,
            tenantId,
            result);
      }

      return result != null && result;
    } catch (Exception e) {
      throw new SQLException("Failed to check sender existence", e);
    }
  }
}
