package copel.sesproductpackage.core.database.dao;

import copel.sesproductpackage.core.database.SES_AI_M_GROUP;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SES_AI_M_GROUP 用 DAO.
 *
 * <p>送信元グループマスタの CRUD 操作を担当します。 テナント ID による絞り込みが必須です。
 *
 * @author Copel Co., Ltd.
 */
public class SES_AI_M_GroupDAO extends EntityBaseDAO {

  private static final Logger log = LoggerFactory.getLogger(SES_AI_M_GroupDAO.class);

  private static final String MAPPER_CLASS_NAME =
      "copel.sesproductpackage.core.database.mapper.GroupMapper";

  /**
   * コンストラクタ.
   *
   * @param sqlSessionFactory SqlSessionFactory インスタンス
   * @param connection DBコネクション
   */
  public SES_AI_M_GroupDAO(SqlSessionFactory sqlSessionFactory, Connection connection) {
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
  public SES_AI_M_GroupDAO(Connection connection) {
    this(SES_AI_M_GROUP.getSqlSessionFactory(), connection);
  }

  /**
   * 送信元グループを挿入します.
   *
   * @param entity 挿入対象のSES_AI_M_GROUP
   * @return 挿入行数
   * @throws SQLException DB操作エラーの場合
   */
  public int insert(SES_AI_M_GROUP entity) throws SQLException {
    if (sqlSessionFactory == null) {
      throw new SQLException("SqlSessionFactory not initialized");
    }

    try (SqlSession session = sqlSessionFactory.openSession()) {
      Object mapper = session.getMapper(Class.forName(MAPPER_CLASS_NAME));
      java.lang.reflect.Method method =
          mapper.getClass().getMethod("insertGroup", SES_AI_M_GROUP.class);
      int result = (int) method.invoke(mapper, entity);
      session.commit();

      if (log.isDebugEnabled()) {
        log.debug(
            "[GroupDAO] Insert completed: fromGroup={}, tenantId={}, result={}",
            entity.getFromGroup(),
            entity.getTenantId(),
            result);
      }

      return result;
    } catch (Exception e) {
      throw new SQLException("Failed to insert group", e);
    }
  }

  /**
   * 送信元グループを PK とテナント ID で取得します.
   *
   * @param pkValue グループID
   * @param tenantId テナント ID
   * @return 検索結果のSES_AI_M_GROUP、またはnull
   * @throws SQLException DB操作エラーの場合
   */
  public SES_AI_M_GROUP selectByPk(String pkValue, String tenantId) throws SQLException {
    if (sqlSessionFactory == null) {
      throw new SQLException("SqlSessionFactory not initialized");
    }

    try (SqlSession session = sqlSessionFactory.openSession()) {
      Object mapper = session.getMapper(Class.forName(MAPPER_CLASS_NAME));
      java.lang.reflect.Method method = mapper.getClass().getMethod("selectGroupByPk", Map.class);

      Map<String, Object> params = new HashMap<>();
      params.put("fromGroup", pkValue);
      params.put("tenantId", tenantId);

      SES_AI_M_GROUP result = (SES_AI_M_GROUP) method.invoke(mapper, params);

      if (log.isDebugEnabled()) {
        log.debug(
            "[GroupDAO] Select by PK completed: fromGroup={}, tenantId={}, result={}",
            pkValue,
            tenantId,
            result != null ? "found" : "not found");
      }

      return result;
    } catch (Exception e) {
      throw new SQLException("Failed to select group by PK", e);
    }
  }

  /**
   * 送信元グループを更新します.
   *
   * @param entity 更新対象のSES_AI_M_GROUP
   * @return 更新行数
   * @throws SQLException DB操作エラーの場合
   */
  public int update(SES_AI_M_GROUP entity) throws SQLException {
    if (sqlSessionFactory == null) {
      throw new SQLException("SqlSessionFactory not initialized");
    }

    try (SqlSession session = sqlSessionFactory.openSession()) {
      Object mapper = session.getMapper(Class.forName(MAPPER_CLASS_NAME));
      java.lang.reflect.Method method =
          mapper.getClass().getMethod("updateGroupByPk", SES_AI_M_GROUP.class);
      int result = (int) method.invoke(mapper, entity);
      session.commit();

      if (log.isDebugEnabled()) {
        log.debug(
            "[GroupDAO] Update completed: fromGroup={}, tenantId={}, result={}",
            entity.getFromGroup(),
            entity.getTenantId(),
            result);
      }

      return result;
    } catch (Exception e) {
      throw new SQLException("Failed to update group", e);
    }
  }

  /**
   * 送信元グループを PK とテナント ID で削除します.
   *
   * @param pkValue グループID
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
      java.lang.reflect.Method method = mapper.getClass().getMethod("deleteGroupByPk", Map.class);

      Map<String, Object> params = new HashMap<>();
      params.put("fromGroup", pkValue);
      params.put("tenantId", tenantId);

      int result = (int) method.invoke(mapper, params);
      session.commit();

      if (log.isDebugEnabled()) {
        log.debug(
            "[GroupDAO] Delete completed: fromGroup={}, tenantId={}, result={}",
            pkValue,
            tenantId,
            result);
      }

      return result;
    } catch (Exception e) {
      throw new SQLException("Failed to delete group", e);
    }
  }
}
