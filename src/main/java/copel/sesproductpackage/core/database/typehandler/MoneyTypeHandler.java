package copel.sesproductpackage.core.database.typehandler;

import copel.sesproductpackage.core.unit.Money;
import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Money型のMyBatis TypeHandler.
 *
 * <p>PostgreSQL NUMERIC(10,2)とJava Money の双方向変換を行います。 金銭額を安全に扱います。
 *
 * @author Copel Co., Ltd.
 */
public class MoneyTypeHandler extends BaseTypeHandler<Money> {

  private static final Logger log = LoggerFactory.getLogger(MoneyTypeHandler.class);

  /**
   * Java Money → PreparedStatement へバインド (INSERT/UPDATE).
   *
   * <p>Money オブジェクトから金額（BigDecimal）を取得し、PreparedStatement にバインドします。 Money が null や empty の場合は NULL
   * を設定します。
   *
   * @param ps PreparedStatement
   * @param i パラメータインデックス
   * @param parameter Money値
   * @param jdbcType JDBC型（未使用）
   * @throws SQLException SQL操作エラーの場合
   */
  @Override
  public void setNonNullParameter(PreparedStatement ps, int i, Money parameter, JdbcType jdbcType)
      throws SQLException {
    if (parameter == null || parameter.isEmpty()) {
      ps.setNull(i, java.sql.Types.NUMERIC);
      return;
    }

    try {
      BigDecimal amount = parameter.getValue();
      ps.setBigDecimal(i, amount);

      if (log.isDebugEnabled()) {
        log.debug("[TypeHandler] Money.setParameter: amount={}", amount);
      }
    } catch (Exception e) {
      throw new SQLException("Failed to convert Money to NUMERIC", e);
    }
  }

  /**
   * ResultSet → Java Money (SELECT by column name).
   *
   * <p>DB の NUMERIC を Money に変換します。NULL値の場合は null を返します。
   *
   * @param rs ResultSet
   * @param columnName カラム名
   * @return Money値、またはnull
   * @throws SQLException SQL操作エラーの場合
   */
  @Override
  public Money getNullableResult(ResultSet rs, String columnName) throws SQLException {
    BigDecimal amount = rs.getBigDecimal(columnName);
    if (amount == null) {
      return null;
    }

    if (log.isDebugEnabled()) {
      log.debug("[TypeHandler] Money.getNullableResult by columnName: amount={}", amount);
    }

    return new Money(amount);
  }

  /**
   * ResultSet → Java Money (SELECT by column index).
   *
   * <p>DB の NUMERIC を Money に変換します。NULL値の場合は null を返します。
   *
   * @param rs ResultSet
   * @param columnIndex カラムインデックス
   * @return Money値、またはnull
   * @throws SQLException SQL操作エラーの場合
   */
  @Override
  public Money getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
    BigDecimal amount = rs.getBigDecimal(columnIndex);
    if (amount == null) {
      return null;
    }

    if (log.isDebugEnabled()) {
      log.debug("[TypeHandler] Money.getNullableResult by columnIndex: amount={}", amount);
    }

    return new Money(amount);
  }

  /**
   * CallableStatement → Java Money (ストアドプロシージャ).
   *
   * <p>DB の NUMERIC を Money に変換します。NULL値の場合は null を返します。
   *
   * @param cs CallableStatement
   * @param columnIndex カラムインデックス
   * @return Money値、またはnull
   * @throws SQLException SQL操作エラーの場合
   */
  @Override
  public Money getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
    BigDecimal amount = cs.getBigDecimal(columnIndex);
    if (amount == null) {
      return null;
    }

    if (log.isDebugEnabled()) {
      log.debug("[TypeHandler] Money.getNullableResult by CallableStatement: amount={}", amount);
    }

    return new Money(amount);
  }
}
