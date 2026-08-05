package copel.sesproductpackage.core.database.dao;

import copel.sesproductpackage.core.database.base.EntityBase;
import java.sql.Connection;
import java.sql.SQLException;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * エンティティ用 DAO の基底クラス.
 *
 * <p>SqlSession を使用してMyBatisの CRUD 処理をラップします。 Entity インスタンスから Connection パラメータを受け取り、内部で SqlSession
 * を生成・管理します。
 *
 * @author Copel Co., Ltd.
 */
public abstract class EntityBaseDAO {

  private static final Logger log = LoggerFactory.getLogger(EntityBaseDAO.class);

  /** SqlSessionFactory（MyBatis設定） */
  protected SqlSessionFactory sqlSessionFactory;

  /** DBコネクション */
  protected Connection connection;

  /**
   * コンストラクタ.
   *
   * @param sqlSessionFactory SqlSessionFactory インスタンス
   * @param connection DBコネクション
   */
  protected EntityBaseDAO(SqlSessionFactory sqlSessionFactory, Connection connection) {
    this.sqlSessionFactory = sqlSessionFactory;
    this.connection = connection;
  }

  /**
   * Entity を INSERT します.
   *
   * <p>Mapperインターフェースのinsertメソッドを呼び出し、DBに新規レコードを挿入します。
   *
   * @param entity 挿入対象のEntity
   * @param mapperClass Mapper インターフェースクラス
   * @param mapperMethod Mapper メソッド名（例：insertTenant）
   * @return 挿入行数
   * @throws SQLException DB操作エラーの場合
   */
  protected <T extends EntityBase> int insert(T entity, Class<?> mapperClass, String mapperMethod)
      throws SQLException {
    if (sqlSessionFactory == null) {
      throw new SQLException("SqlSessionFactory not initialized");
    }

    try (SqlSession session = sqlSessionFactory.openSession()) {
      Object mapper = session.getMapper(mapperClass);
      java.lang.reflect.Method method = mapperClass.getMethod(mapperMethod, entity.getClass());
      int result = (int) method.invoke(mapper, entity);
      session.commit();

      if (log.isDebugEnabled()) {
        log.debug(
            "[DAO] Insert completed: entity={}, result={}",
            entity.getClass().getSimpleName(),
            result);
      }

      return result;
    } catch (Exception e) {
      throw new SQLException("Failed to insert entity", e);
    }
  }

  /**
   * Entity を PK 指定で SELECT します（テナント ID フィルタ付き）.
   *
   * <p>テナント ID による絞り込みが必須です。
   *
   * @param pkValue PK値
   * @param tenantId テナント ID
   * @param mapperClass Mapper インターフェースクラス
   * @param mapperMethod Mapper メソッド名（例：selectSenderByPk）
   * @return 検索結果の Entity、またはnull
   * @throws SQLException DB操作エラーの場合
   */
  protected <T> T selectByPk(
      String pkValue, String tenantId, Class<?> mapperClass, String mapperMethod)
      throws SQLException {
    if (sqlSessionFactory == null) {
      throw new SQLException("SqlSessionFactory not initialized");
    }

    try (SqlSession session = sqlSessionFactory.openSession()) {
      Object mapper = session.getMapper(mapperClass);

      // map型パラメータを作成
      java.util.Map<String, Object> params = new java.util.HashMap<>();
      params.put("fromId", pkValue); // または fromGroup, userId, notificationId など
      params.put("tenantId", tenantId);

      java.lang.reflect.Method method = mapperClass.getMethod(mapperMethod, java.util.Map.class);
      @SuppressWarnings("unchecked")
      T result = (T) method.invoke(mapper, params);

      if (log.isDebugEnabled()) {
        log.debug(
            "[DAO] Select by PK completed: mapperClass={}, pkValue={}, result={}",
            mapperClass.getSimpleName(),
            pkValue,
            result != null ? "found" : "not found");
      }

      return result;
    } catch (Exception e) {
      throw new SQLException("Failed to select entity by PK", e);
    }
  }

  /**
   * Entity を UPDATE します（テナント ID フィルタ付き）.
   *
   * @param entity 更新対象の Entity
   * @param mapperClass Mapper インターフェースクラス
   * @param mapperMethod Mapper メソッド名（例：updateSenderByPk）
   * @return 更新行数
   * @throws SQLException DB操作エラーの場合
   */
  protected <T extends EntityBase> int update(T entity, Class<?> mapperClass, String mapperMethod)
      throws SQLException {
    if (sqlSessionFactory == null) {
      throw new SQLException("SqlSessionFactory not initialized");
    }

    try (SqlSession session = sqlSessionFactory.openSession()) {
      Object mapper = session.getMapper(mapperClass);
      java.lang.reflect.Method method = mapperClass.getMethod(mapperMethod, entity.getClass());
      int result = (int) method.invoke(mapper, entity);
      session.commit();

      if (log.isDebugEnabled()) {
        log.debug(
            "[DAO] Update completed: entity={}, result={}",
            entity.getClass().getSimpleName(),
            result);
      }

      return result;
    } catch (Exception e) {
      throw new SQLException("Failed to update entity", e);
    }
  }

  /**
   * Entity を DELETE します（テナント ID フィルタ付き）.
   *
   * @param pkValue PK値
   * @param tenantId テナント ID
   * @param mapperClass Mapper インターフェースクラス
   * @param mapperMethod Mapper メソッド名（例：deleteSenderByPk）
   * @return 削除行数
   * @throws SQLException DB操作エラーの場合
   */
  protected int delete(String pkValue, String tenantId, Class<?> mapperClass, String mapperMethod)
      throws SQLException {
    if (sqlSessionFactory == null) {
      throw new SQLException("SqlSessionFactory not initialized");
    }

    try (SqlSession session = sqlSessionFactory.openSession()) {
      Object mapper = session.getMapper(mapperClass);

      java.util.Map<String, Object> params = new java.util.HashMap<>();
      params.put("fromId", pkValue); // または fromGroup, userId, notificationId など
      params.put("tenantId", tenantId);

      java.lang.reflect.Method method = mapperClass.getMethod(mapperMethod, java.util.Map.class);
      int result = (int) method.invoke(mapper, params);
      session.commit();

      if (log.isDebugEnabled()) {
        log.debug(
            "[DAO] Delete completed: mapperClass={}, pkValue={}, result={}",
            mapperClass.getSimpleName(),
            pkValue,
            result);
      }

      return result;
    } catch (Exception e) {
      throw new SQLException("Failed to delete entity by PK", e);
    }
  }
}
