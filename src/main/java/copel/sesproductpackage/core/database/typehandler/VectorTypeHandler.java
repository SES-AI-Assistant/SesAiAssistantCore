package copel.sesproductpackage.core.database.typehandler;

import copel.sesproductpackage.core.unit.Vector;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Vector型のMyBatis TypeHandler.
 *
 * <p>PostgreSQL vector(1536)とJava Vector の双方向変換を行います。 AI埋め込みベクトル（OpenAI Embedding等）を型安全に取り扱います。
 *
 * @author Copel Co., Ltd.
 */
public class VectorTypeHandler extends BaseTypeHandler<Vector> {

  private static final Logger log = LoggerFactory.getLogger(VectorTypeHandler.class);

  /**
   * Java Vector → PreparedStatement へバインド (INSERT/UPDATE).
   *
   * <p>Vector オブジェクトを文字列形式に変換してバインドします。 Vector が null の場合は NULL を設定します。
   *
   * @param ps PreparedStatement
   * @param i パラメータインデックス
   * @param parameter Vector値
   * @param jdbcType JDBC型（未使用）
   * @throws SQLException SQL操作エラーの場合
   */
  @Override
  public void setNonNullParameter(PreparedStatement ps, int i, Vector parameter, JdbcType jdbcType)
      throws SQLException {
    if (parameter == null) {
      ps.setNull(i, java.sql.Types.VARCHAR);
      return;
    }

    try {
      String vectorStr = parameter.toString();
      ps.setString(i, vectorStr);

      if (log.isDebugEnabled()) {
        log.debug("[TypeHandler] Vector.setParameter: vectorStr={}", vectorStr);
      }
    } catch (Exception e) {
      throw new SQLException("Failed to convert Vector to string format", e);
    }
  }

  /**
   * ResultSet → Java Vector (SELECT by column name).
   *
   * <p>文字列形式を Vector オブジェクトに変換します。 NULL値や空の場合は null を返します。 Vector はパースされた後に embedding()
   * メソッドを呼び出してベクトル値を埋め込む必要があります。
   *
   * @param rs ResultSet
   * @param columnName カラム名
   * @return Vector値、またはnull
   * @throws SQLException SQL操作エラーの場合
   */
  @Override
  public Vector getNullableResult(ResultSet rs, String columnName) throws SQLException {
    String vectorStr = rs.getString(columnName);
    if (vectorStr == null || vectorStr.trim().isEmpty()) {
      return null;
    }

    try {
      Vector result = new Vector(null);
      result.setRawString(vectorStr);

      if (log.isDebugEnabled()) {
        log.debug("[TypeHandler] Vector.getNullableResult by columnName: rawString={}", vectorStr);
      }

      return result;
    } catch (Exception e) {
      throw new SQLException("Failed to parse Vector from database: " + vectorStr, e);
    }
  }

  /**
   * ResultSet → Java Vector (SELECT by column index).
   *
   * <p>文字列形式を Vector オブジェクトに変換します。 NULL値や空の場合は null を返します。 Vector はパースされた後に embedding()
   * メソッドを呼び出してベクトル値を埋め込む必要があります。
   *
   * @param rs ResultSet
   * @param columnIndex カラムインデックス
   * @return Vector値、またはnull
   * @throws SQLException SQL操作エラーの場合
   */
  @Override
  public Vector getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
    String vectorStr = rs.getString(columnIndex);
    if (vectorStr == null || vectorStr.trim().isEmpty()) {
      return null;
    }

    try {
      Vector result = new Vector(null);
      result.setRawString(vectorStr);

      if (log.isDebugEnabled()) {
        log.debug("[TypeHandler] Vector.getNullableResult by columnIndex: rawString={}", vectorStr);
      }

      return result;
    } catch (Exception e) {
      throw new SQLException("Failed to parse Vector from database at column " + columnIndex, e);
    }
  }

  /**
   * CallableStatement → Java Vector (ストアドプロシージャ).
   *
   * <p>文字列形式を Vector オブジェクトに変換します。 NULL値や空の場合は null を返します。 Vector はパースされた後に embedding()
   * メソッドを呼び出してベクトル値を埋め込む必要があります。
   *
   * @param cs CallableStatement
   * @param columnIndex カラムインデックス
   * @return Vector値、またはnull
   * @throws SQLException SQL操作エラーの場合
   */
  @Override
  public Vector getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
    String vectorStr = cs.getString(columnIndex);
    if (vectorStr == null || vectorStr.trim().isEmpty()) {
      return null;
    }

    try {
      Vector result = new Vector(null);
      result.setRawString(vectorStr);

      if (log.isDebugEnabled()) {
        log.debug(
            "[TypeHandler] Vector.getNullableResult by CallableStatement: rawString={}", vectorStr);
      }

      return result;
    } catch (Exception e) {
      throw new SQLException(
          "Failed to parse Vector from CallableStatement at index " + columnIndex, e);
    }
  }
}
