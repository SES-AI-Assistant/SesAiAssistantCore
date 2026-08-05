package copel.sesproductpackage.core.database.dao;

import copel.sesproductpackage.core.database.SES_AI_WEBAPP_M_USER;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SES_AI_WEBAPP_M_USER 用 DAO.
 *
 * <p>Webアプリユーザーマスタの CRUD 操作を担当します。 テナント ID による絞り込みが必須です。 ただし、システム管理者用にテナント ID フィルタなしメソッドも提供します。
 *
 * @author Copel Co., Ltd.
 */
public class SES_AI_WEBAPP_M_UserDAO extends EntityBaseDAO {

  private static final Logger log = LoggerFactory.getLogger(SES_AI_WEBAPP_M_UserDAO.class);

  private static final String MAPPER_CLASS_NAME =
      "copel.sesproductpackage.core.database.mapper.UserMapper";

  /**
   * コンストラクタ.
   *
   * @param sqlSessionFactory SqlSessionFactory インスタンス
   * @param connection DBコネクション
   */
  public SES_AI_WEBAPP_M_UserDAO(SqlSessionFactory sqlSessionFactory, Connection connection) {
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
  public SES_AI_WEBAPP_M_UserDAO(Connection connection) {
    this(SES_AI_WEBAPP_M_USER.getSqlSessionFactory(), connection);
  }

  /**
   * Webアプリユーザーを挿入します.
   *
   * @param entity 挿入対象のSES_AI_WEBAPP_M_USER
   * @return 挿入行数
   * @throws SQLException DB操作エラーの場合
   */
  public int insert(SES_AI_WEBAPP_M_USER entity) throws SQLException {
    if (sqlSessionFactory == null) {
      throw new SQLException("SqlSessionFactory not initialized");
    }

    try (SqlSession session = sqlSessionFactory.openSession()) {
      Object mapper = session.getMapper(Class.forName(MAPPER_CLASS_NAME));
      java.lang.reflect.Method method =
          mapper.getClass().getMethod("insertUser", SES_AI_WEBAPP_M_USER.class);
      int result = (int) method.invoke(mapper, entity);
      session.commit();

      if (log.isDebugEnabled()) {
        log.debug(
            "[UserDAO] Insert completed: userId={}, tenantId={}, result={}",
            entity.getUserId(),
            entity.getTenantId(),
            result);
      }

      return result;
    } catch (Exception e) {
      throw new SQLException("Failed to insert user", e);
    }
  }

  /**
   * Webアプリユーザーを PK とテナント ID で取得します.
   *
   * @param pkValue ユーザーID
   * @param tenantId テナント ID
   * @return 検索結果のSES_AI_WEBAPP_M_USER、またはnull
   * @throws SQLException DB操作エラーの場合
   */
  public SES_AI_WEBAPP_M_USER selectByPk(String pkValue, String tenantId) throws SQLException {
    if (sqlSessionFactory == null) {
      throw new SQLException("SqlSessionFactory not initialized");
    }

    try (SqlSession session = sqlSessionFactory.openSession()) {
      Object mapper = session.getMapper(Class.forName(MAPPER_CLASS_NAME));
      java.lang.reflect.Method method = mapper.getClass().getMethod("selectUserByPk", Map.class);

      Map<String, Object> params = new HashMap<>();
      params.put("userId", pkValue);
      params.put("tenantId", tenantId);

      SES_AI_WEBAPP_M_USER result = (SES_AI_WEBAPP_M_USER) method.invoke(mapper, params);

      if (log.isDebugEnabled()) {
        log.debug(
            "[UserDAO] Select by PK completed: userId={}, tenantId={}, result={}",
            pkValue,
            tenantId,
            result != null ? "found" : "not found");
      }

      return result;
    } catch (Exception e) {
      throw new SQLException("Failed to select user by PK", e);
    }
  }

  /**
   * Webアプリユーザーをテナント ID フィルタなしで取得します（システム管理者用）.
   *
   * @param pkValue ユーザーID
   * @return 検索結果のSES_AI_WEBAPP_M_USER、またはnull
   * @throws SQLException DB操作エラーの場合
   */
  public SES_AI_WEBAPP_M_USER selectByPkWithoutTenantId(String pkValue) throws SQLException {
    if (sqlSessionFactory == null) {
      throw new SQLException("SqlSessionFactory not initialized");
    }

    try (SqlSession session = sqlSessionFactory.openSession()) {
      Object mapper = session.getMapper(Class.forName(MAPPER_CLASS_NAME));
      java.lang.reflect.Method method =
          mapper.getClass().getMethod("selectUserByPkWithoutTenantId", String.class);

      SES_AI_WEBAPP_M_USER result = (SES_AI_WEBAPP_M_USER) method.invoke(mapper, pkValue);

      if (log.isDebugEnabled()) {
        log.debug(
            "[UserDAO] Select by PK (without tenant filter) completed: userId={}, result={}",
            pkValue,
            result != null ? "found" : "not found");
      }

      return result;
    } catch (Exception e) {
      throw new SQLException("Failed to select user by PK without tenant filter", e);
    }
  }

  /**
   * Webアプリユーザーを更新します.
   *
   * @param entity 更新対象のSES_AI_WEBAPP_M_USER
   * @return 更新行数
   * @throws SQLException DB操作エラーの場合
   */
  public int update(SES_AI_WEBAPP_M_USER entity) throws SQLException {
    if (sqlSessionFactory == null) {
      throw new SQLException("SqlSessionFactory not initialized");
    }

    try (SqlSession session = sqlSessionFactory.openSession()) {
      Object mapper = session.getMapper(Class.forName(MAPPER_CLASS_NAME));
      java.lang.reflect.Method method =
          mapper.getClass().getMethod("updateUserByPk", SES_AI_WEBAPP_M_USER.class);
      int result = (int) method.invoke(mapper, entity);
      session.commit();

      if (log.isDebugEnabled()) {
        log.debug(
            "[UserDAO] Update completed: userId={}, tenantId={}, result={}",
            entity.getUserId(),
            entity.getTenantId(),
            result);
      }

      return result;
    } catch (Exception e) {
      throw new SQLException("Failed to update user", e);
    }
  }

  /**
   * Webアプリユーザーをテナント ID フィルタなしで更新します（システム管理者用）.
   *
   * @param entity 更新対象のSES_AI_WEBAPP_M_USER
   * @return 更新行数
   * @throws SQLException DB操作エラーの場合
   */
  public int updateWithoutTenantId(SES_AI_WEBAPP_M_USER entity) throws SQLException {
    if (sqlSessionFactory == null) {
      throw new SQLException("SqlSessionFactory not initialized");
    }

    try (SqlSession session = sqlSessionFactory.openSession()) {
      Object mapper = session.getMapper(Class.forName(MAPPER_CLASS_NAME));
      java.lang.reflect.Method method =
          mapper.getClass().getMethod("updateUserByPkWithoutTenantId", SES_AI_WEBAPP_M_USER.class);
      int result = (int) method.invoke(mapper, entity);
      session.commit();

      if (log.isDebugEnabled()) {
        log.debug(
            "[UserDAO] Update (without tenant filter) completed: userId={}, tenantId={}, result={}",
            entity.getUserId(),
            entity.getTenantId(),
            result);
      }

      return result;
    } catch (Exception e) {
      throw new SQLException("Failed to update user without tenant filter", e);
    }
  }

  /**
   * Webアプリユーザーを PK とテナント ID で削除します.
   *
   * @param pkValue ユーザーID
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
      java.lang.reflect.Method method = mapper.getClass().getMethod("deleteUserByPk", Map.class);

      Map<String, Object> params = new HashMap<>();
      params.put("userId", pkValue);
      params.put("tenantId", tenantId);

      int result = (int) method.invoke(mapper, params);
      session.commit();

      if (log.isDebugEnabled()) {
        log.debug(
            "[UserDAO] Delete completed: userId={}, tenantId={}, result={}",
            pkValue,
            tenantId,
            result);
      }

      return result;
    } catch (Exception e) {
      throw new SQLException("Failed to delete user", e);
    }
  }

  /**
   * Webアプリユーザーをテナント ID フィルタなしで削除します（システム管理者用）.
   *
   * @param pkValue ユーザーID
   * @return 削除行数
   * @throws SQLException DB操作エラーの場合
   */
  public int deleteWithoutTenantId(String pkValue) throws SQLException {
    if (sqlSessionFactory == null) {
      throw new SQLException("SqlSessionFactory not initialized");
    }

    try (SqlSession session = sqlSessionFactory.openSession()) {
      Object mapper = session.getMapper(Class.forName(MAPPER_CLASS_NAME));
      java.lang.reflect.Method method =
          mapper.getClass().getMethod("deleteUserByPkWithoutTenantId", String.class);

      int result = (int) method.invoke(mapper, pkValue);
      session.commit();

      if (log.isDebugEnabled()) {
        log.debug(
            "[UserDAO] Delete (without tenant filter) completed: userId={}, result={}",
            pkValue,
            result);
      }

      return result;
    } catch (Exception e) {
      throw new SQLException("Failed to delete user without tenant filter", e);
    }
  }
}
