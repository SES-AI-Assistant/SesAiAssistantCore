package copel.sesproductpackage.core.database.typehandler;

import copel.sesproductpackage.core.unit.OriginalDateTime;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * OriginalDateTime型のMyBatis TypeHandler.
 *
 * <p>PostgreSQL timestamp（UTC）とJava OriginalDateTime（JST）の双方向変換を行います。 タイムゾーン対応により、UTC ↔ JST
 * 変換を自動処理します。
 *
 * @author Copel Co., Ltd.
 */
public class OriginalDateTimeTypeHandler extends BaseTypeHandler<OriginalDateTime> {

  private static final Logger log = LoggerFactory.getLogger(OriginalDateTimeTypeHandler.class);

  /** システムタイムゾーン（Asia/Tokyo） */
  private static final ZoneId SYSTEM_ZONE_ID = ZoneId.of("Asia/Tokyo");

  /** UTC タイムゾーン */
  private static final ZoneId UTC_ZONE_ID = ZoneId.of("UTC");

  /**
   * Java OriginalDateTime → PreparedStatement へバインド (INSERT/UPDATE).
   *
   * <p>OriginalDateTime（JST）をUTC Timestamp に変換してバインドします。
   *
   * @param ps PreparedStatement
   * @param i パラメータインデックス
   * @param parameter OriginalDateTime値
   * @param jdbcType JDBC型（未使用）
   * @throws SQLException SQL操作エラーの場合
   */
  @Override
  public void setNonNullParameter(
      PreparedStatement ps, int i, OriginalDateTime parameter, JdbcType jdbcType)
      throws SQLException {
    if (parameter == null) {
      ps.setNull(i, java.sql.Types.TIMESTAMP);
      return;
    }

    try {
      java.sql.Timestamp dbTimestamp = parameter.toTimestamp();
      ps.setTimestamp(i, dbTimestamp);

      if (log.isDebugEnabled()) {
        log.debug("[TypeHandler] OriginalDateTime.setParameter: dbTimestamp={}", dbTimestamp);
      }
    } catch (Exception e) {
      throw new SQLException("Failed to convert OriginalDateTime to Timestamp", e);
    }
  }

  /**
   * ResultSet → Java OriginalDateTime (SELECT by column name).
   *
   * <p>DB timestamp（UTC）をOriginalDateTime（JST）に変換します。
   *
   * @param rs ResultSet
   * @param columnName カラム名
   * @return OriginalDateTime値、またはnull
   * @throws SQLException SQL操作エラーの場合
   */
  @Override
  public OriginalDateTime getNullableResult(ResultSet rs, String columnName) throws SQLException {
    java.sql.Timestamp dbTimestamp = rs.getTimestamp(columnName);
    if (dbTimestamp == null) {
      return null;
    }

    return convertUtcToJst(dbTimestamp);
  }

  /**
   * ResultSet → Java OriginalDateTime (SELECT by column index).
   *
   * <p>DB timestamp（UTC）をOriginalDateTime（JST）に変換します。
   *
   * @param rs ResultSet
   * @param columnIndex カラムインデックス
   * @return OriginalDateTime値、またはnull
   * @throws SQLException SQL操作エラーの場合
   */
  @Override
  public OriginalDateTime getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
    java.sql.Timestamp dbTimestamp = rs.getTimestamp(columnIndex);
    if (dbTimestamp == null) {
      return null;
    }

    return convertUtcToJst(dbTimestamp);
  }

  /**
   * CallableStatement → Java OriginalDateTime (ストアドプロシージャ).
   *
   * <p>DB timestamp（UTC）をOriginalDateTime（JST）に変換します。
   *
   * @param cs CallableStatement
   * @param columnIndex カラムインデックス
   * @return OriginalDateTime値、またはnull
   * @throws SQLException SQL操作エラーの場合
   */
  @Override
  public OriginalDateTime getNullableResult(CallableStatement cs, int columnIndex)
      throws SQLException {
    java.sql.Timestamp dbTimestamp = cs.getTimestamp(columnIndex);
    if (dbTimestamp == null) {
      return null;
    }

    return convertUtcToJst(dbTimestamp);
  }

  /**
   * UTC timestamp をOriginalDateTime（JST）に変換します.
   *
   * <p>UTCの日時をJSTにタイムゾーン変換します。
   *
   * @param dbTimestamp DB timestamp（UTC）
   * @return OriginalDateTime（JST）
   */
  private OriginalDateTime convertUtcToJst(java.sql.Timestamp dbTimestamp) {
    LocalDateTime utcDateTime = dbTimestamp.toLocalDateTime();
    ZonedDateTime utcZoned = utcDateTime.atZone(UTC_ZONE_ID);
    ZonedDateTime jstZoned = utcZoned.withZoneSameInstant(SYSTEM_ZONE_ID);

    if (log.isDebugEnabled()) {
      log.debug(
          "[TypeHandler] UTC→JST conversion: UTC={}, JST={}",
          utcDateTime,
          jstZoned.toLocalDateTime());
    }

    return new OriginalDateTime(jstZoned.toLocalDateTime().toString());
  }
}
