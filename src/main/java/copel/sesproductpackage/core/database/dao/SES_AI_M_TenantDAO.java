package copel.sesproductpackage.core.database.dao;

import copel.sesproductpackage.core.database.SES_AI_M_TENANT;
import java.sql.Connection;
import java.sql.SQLException;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SES_AI_M_TENANT 用 DAO.
 *
 * <p>テナント情報マスタの CRUD 操作を担当します。 MyBatis SqlSession を使用してDB操作を実行します。
 *
 * @author Copel Co., Ltd.
 */
public class SES_AI_M_TenantDAO extends EntityBaseDAO {

  private static final Logger log = LoggerFactory.getLogger(SES_AI_M_TenantDAO.class);

  private static final String MAPPER_CLASS_NAME =
      "copel.sesproductpackage.core.database.mapper.TenantMapper";

  /**
   * コンストラクタ.
   *
   * @param sqlSessionFactory SqlSessionFactory インスタンス
   * @param connection DBコネクション
   */
  public SES_AI_M_TenantDAO(SqlSessionFactory sqlSessionFactory, Connection connection) {
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
  public SES_AI_M_TenantDAO(Connection connection) {
    this(SES_AI_M_TENANT.getSqlSessionFactory(), connection);
  }

  /**
   * テナント情報を挿入します.
   *
   * @param entity 挿入対象のSES_AI_M_TENANT
   * @return 挿入行数
   * @throws SQLException DB操作エラーの場合
   */
  public int insert(SES_AI_M_TENANT entity) throws SQLException {
    if (sqlSessionFactory == null) {
      throw new SQLException("SqlSessionFactory not initialized");
    }

    try (SqlSession session = sqlSessionFactory.openSession()) {
      Object mapper = session.getMapper(Class.forName(MAPPER_CLASS_NAME));
      java.lang.reflect.Method method =
          mapper.getClass().getMethod("insertTenant", SES_AI_M_TENANT.class);
      int result = (int) method.invoke(mapper, entity);
      session.commit();

      if (log.isDebugEnabled()) {
        log.debug(
            "[TenantDAO] Insert completed: tenantId={}, result={}", entity.getTenantId(), result);
      }

      return result;
    } catch (Exception e) {
      throw new SQLException("Failed to insert tenant", e);
    }
  }

  /**
   * テナント情報を PK で取得します.
   *
   * <p>テナントマスタはマルチテナント対応ではないため、tenantId フィルタなしで検索します。
   *
   * @param pkValue テナントID
   * @return 検索結果のSES_AI_M_TENANT、またはnull
   * @throws SQLException DB操作エラーの場合
   */
  public SES_AI_M_TENANT selectByPk(String pkValue) throws SQLException {
    if (sqlSessionFactory == null) {
      throw new SQLException("SqlSessionFactory not initialized");
    }

    try (SqlSession session = sqlSessionFactory.openSession()) {
      Object mapper = session.getMapper(Class.forName(MAPPER_CLASS_NAME));
      java.lang.reflect.Method method =
          mapper.getClass().getMethod("selectTenantByPk", String.class);
      SES_AI_M_TENANT result = (SES_AI_M_TENANT) method.invoke(mapper, pkValue);

      if (log.isDebugEnabled()) {
        log.debug(
            "[TenantDAO] Select by PK completed: tenantId={}, result={}",
            pkValue,
            result != null ? "found" : "not found");
      }

      return result;
    } catch (Exception e) {
      throw new SQLException("Failed to select tenant by PK", e);
    }
  }

  /**
   * テナント情報を更新します.
   *
   * @param entity 更新対象のSES_AI_M_TENANT
   * @return 更新行数
   * @throws SQLException DB操作エラーの場合
   */
  public int update(SES_AI_M_TENANT entity) throws SQLException {
    if (sqlSessionFactory == null) {
      throw new SQLException("SqlSessionFactory not initialized");
    }

    try (SqlSession session = sqlSessionFactory.openSession()) {
      Object mapper = session.getMapper(Class.forName(MAPPER_CLASS_NAME));
      java.lang.reflect.Method method =
          mapper.getClass().getMethod("updateTenantByPk", SES_AI_M_TENANT.class);
      int result = (int) method.invoke(mapper, entity);
      session.commit();

      if (log.isDebugEnabled()) {
        log.debug(
            "[TenantDAO] Update completed: tenantId={}, result={}", entity.getTenantId(), result);
      }

      return result;
    } catch (Exception e) {
      throw new SQLException("Failed to update tenant", e);
    }
  }

  /**
   * テナント情報を PK で削除します.
   *
   * @param pkValue テナントID
   * @return 削除行数
   * @throws SQLException DB操作エラーの場合
   */
  public int delete(String pkValue) throws SQLException {
    if (sqlSessionFactory == null) {
      throw new SQLException("SqlSessionFactory not initialized");
    }

    try (SqlSession session = sqlSessionFactory.openSession()) {
      Object mapper = session.getMapper(Class.forName(MAPPER_CLASS_NAME));
      java.lang.reflect.Method method =
          mapper.getClass().getMethod("deleteTenantByPk", String.class);
      int result = (int) method.invoke(mapper, pkValue);
      session.commit();

      if (log.isDebugEnabled()) {
        log.debug("[TenantDAO] Delete completed: tenantId={}, result={}", pkValue, result);
      }

      return result;
    } catch (Exception e) {
      throw new SQLException("Failed to delete tenant", e);
    }
  }
}
