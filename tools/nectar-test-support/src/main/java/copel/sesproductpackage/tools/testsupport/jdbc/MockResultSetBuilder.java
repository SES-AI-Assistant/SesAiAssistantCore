package copel.sesproductpackage.tools.testsupport.jdbc;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

/**
 * 列名と行を与えると、行に沿って値を返す {@link ResultSet} のモックを作るビルダーです。
 *
 * <p>JDBC の結果セットは、行を {@code next()} で1行ずつ進めて、現在の行の値を {@code getXxx} で読む。本ビルダーは、その動きを模倣する。
 * 列は列名と列番号（1から始まる）の両方で読める。列名の大文字と小文字は区別しない。
 *
 * <p>対応するメソッドは次のとおり。{@code next}、{@code wasNull}、{@code close}、{@code getString}、{@code
 * getInt}、{@code getLong}、 {@code getDouble}、{@code getBoolean}、{@code getBigDecimal}、{@code
 * getTimestamp}、{@code getDate}、{@code getObject}、 {@code getMetaData}（{@code
 * getColumnCount}、{@code getColumnLabel}、{@code getColumnName}）。 それ以外のメソッドを呼ぶと {@link
 * UnsupportedOperationException} を投げ、テストで未対応の操作に気付けるようにする。
 *
 * <pre>{@code
 * ResultSet resultSet =
 *     MockResultSetBuilder.builder()
 *         .columns("id", "name")
 *         .row(1L, "山田")
 *         .build();
 * }</pre>
 *
 * @author Copel Co., Ltd.
 */
public final class MockResultSetBuilder {

  /** 列名の一覧。列の順番は {@link #row(Object...)} の値の順番と対応する。 */
  private final List<String> columns = new ArrayList<>();

  /** 行の一覧。各行は {@link #columns} と同じ数の値を持つ。 */
  private final List<List<Object>> rows = new ArrayList<>();

  /** 外部からの直接生成を禁止する。{@link #builder()} を使う。 */
  private MockResultSetBuilder() {}

  /**
   * 新しい空のビルダーを返します。
   *
   * @return 新しいビルダー
   */
  public static MockResultSetBuilder builder() {
    return new MockResultSetBuilder();
  }

  /**
   * 列名を順番どおりに設定します。既に設定した列名は置き換えます。
   *
   * <p>行を追加する前に呼ぶこと。行の値の数は、この列数と一致している必要がある。
   *
   * @param names 列名の順番どおりの一覧
   * @return このビルダー
   */
  public MockResultSetBuilder columns(final String... names) {
    columns.clear();
    columns.addAll(Arrays.asList(names));
    return this;
  }

  /**
   * 行を1件追加します。値の順番は列名の順番と対応します。
   *
   * <p>値の数が列数と一致しない場合は、読み取りの結果がずれるため追加を拒否する。値に null を指定すると、その列は null として読める。
   *
   * @param values 列の順番どおりの値
   * @return このビルダー
   * @throws IllegalArgumentException 値の数が列数と一致しない場合
   */
  public MockResultSetBuilder row(final Object... values) {
    if (values.length != columns.size()) {
      throw new IllegalArgumentException(
          "値の数が列数と一致しません。列数: " + columns.size() + "、値の数: " + values.length);
    }
    rows.add(Arrays.asList(values.clone()));
    return this;
  }

  /**
   * 設定した列と行を持つ {@link ResultSet} のモックを返します。
   *
   * <p>返すモックは、呼び出すたびに新しい読み取り位置を持つ。ビルダーの後の変更は、既に返したモックに影響しない。
   *
   * @return 結果セットのモック
   */
  public ResultSet build() {
    return Mockito.mock(
        ResultSet.class, new Cursor(new ArrayList<>(columns), new ArrayList<>(rows)));
  }

  /**
   * 結果セットのモックの呼び出しに応答し、読み取り位置と直前の値の null 判定を保持する、内部の応答クラスです。
   *
   * <p>読み取り位置は {@code next()} で進む。最初の {@code next()} の前と、最後の行の後では、値を読めないため {@link SQLException}
   * を投げる。 値を1つも読んでいない状態で {@code wasNull()} を呼ぶ場合も、判定できないため {@link SQLException} を投げる。
   */
  private static final class Cursor implements Answer<Object> {

    /** 列名の一覧。 */
    private final List<String> columns;

    /** 行の一覧。 */
    private final List<List<Object>> rows;

    /** 現在の行の位置。最初の {@code next()} の前は -1。 */
    private int position = -1;

    /** 直前に読んだ値が null だったかどうか。まだ読んでいない場合は null。 */
    private Boolean lastNull;

    /**
     * 列名と行を受け取って応答クラスを作ります。
     *
     * @param columns 列名の一覧
     * @param rows 行の一覧
     */
    Cursor(final List<String> columns, final List<List<Object>> rows) {
      this.columns = columns;
      this.rows = rows;
    }

    /**
     * 呼び出されたメソッドに応じて、行に沿った値を返します。
     *
     * <p>値の型は、JDBC の標準的な変換に近い範囲で変換する。例えば数値の列を {@code getString} で読むと文字列になる。 数値の型の範囲を超える値は、JDBC
     * と同じく変換できないため {@link SQLException} を投げる。
     *
     * @param invocation 呼び出しの情報
     * @return 呼び出し結果。void のメソッドは null
     * @throws SQLException 読み取り位置が範囲外、列が存在しない、または変換できない値の場合
     */
    @Override
    public Object answer(final InvocationOnMock invocation) throws SQLException {
      String name = invocation.getMethod().getName();
      Object[] args = invocation.getArguments();
      switch (name) {
        case "next" -> {
          lastNull = null;
          position++;
          return position < rows.size();
        }
        case "wasNull" -> {
          if (lastNull == null) {
            throw new SQLException("値を読む前に wasNull を呼び出しました。");
          }
          return lastNull;
        }
        case "close" -> {
          return null;
        }
        case "toString" -> {
          return "MockResultSet";
        }
        case "getMetaData" -> {
          return Mockito.mock(ResultSetMetaData.class, new Metadata(columns));
        }
        case "getObject" -> {
          return read(args[0]);
        }
        case "getString" -> {
          Object value = read(args[0]);
          return value == null ? null : value.toString();
        }
        case "getInt" -> {
          Object value = read(args[0]);
          return value == null ? 0 : (int) toLong(value, Integer.MIN_VALUE, Integer.MAX_VALUE);
        }
        case "getLong" -> {
          Object value = read(args[0]);
          return value == null ? 0L : toLong(value, Long.MIN_VALUE, Long.MAX_VALUE);
        }
        case "getDouble" -> {
          Object value = read(args[0]);
          if (value == null) {
            return 0.0;
          }
          double number = toBigDecimal(value).doubleValue();
          if (Double.isInfinite(number)) {
            throw new SQLException("倍精度の範囲を超えた値です: " + value);
          }
          return number;
        }
        case "getBoolean" -> {
          Object value = read(args[0]);
          if (value == null) {
            return false;
          }
          if (value instanceof Boolean) {
            return value;
          }
          throw new SQLException("真偽値に変換できない値です: " + value);
        }
        case "getBigDecimal" -> {
          Object value = read(args[0]);
          return value == null ? null : toBigDecimal(value);
        }
        case "getTimestamp" -> {
          Object value = read(args[0]);
          if (value == null || value instanceof Timestamp) {
            return value;
          }
          if (value instanceof LocalDateTime) {
            return Timestamp.valueOf((LocalDateTime) value);
          }
          throw new SQLException("日時に変換できない値です: " + value);
        }
        case "getDate" -> {
          Object value = read(args[0]);
          if (value == null || value instanceof Date) {
            return value;
          }
          if (value instanceof LocalDate) {
            return Date.valueOf((LocalDate) value);
          }
          throw new SQLException("日付に変換できない値です: " + value);
        }
        default -> throw new UnsupportedOperationException("MockResultSet は未対応のメソッドです: " + name);
      }
    }

    /**
     * 列番号または列名で指定された、現在の行の値を読みます。
     *
     * <p>読んだ値の null 判定は {@link #lastNull} に保持し、{@code wasNull()} で参照できるようにする。
     *
     * @param reference 列番号（1から始まる {@link Integer}）または列名（{@link String}）
     * @return 読んだ値。null の場合もある
     * @throws SQLException 現在の行が無い、または列が存在しない場合
     */
    private Object read(final Object reference) throws SQLException {
      if (position < 0 || position >= rows.size()) {
        throw new SQLException("現在の行がありません。next を呼んで行を進めてください。");
      }
      int index = -1;
      if (reference instanceof Integer) {
        index = (Integer) reference - 1;
      } else {
        for (int i = 0; i < columns.size() && index < 0; i++) {
          if (columns.get(i).equalsIgnoreCase(String.valueOf(reference))) {
            index = i;
          }
        }
      }
      if (index < 0 || index >= columns.size()) {
        throw new SQLException("列が存在しないか範囲外です: " + reference);
      }
      Object value = rows.get(position).get(index);
      lastNull = value == null;
      return value;
    }

    /**
     * 値を {@link BigDecimal} に変換します。数値と、数値として読める文字列だけを変換できる。
     *
     * <p>数値の列を int、long、double などで読む場合に、同じ変換を使えるようにする。文字列の数値も変換するのは、JDBC の変換規則に近づけるためである。
     * 数値として読めない文字列は {@link NumberFormatException} ではなく {@link SQLException} に包み直す。呼び出し側の例外の扱いを JDBC
     * と同じにするためである。
     *
     * @param value 変換する値。null は指定しない
     * @return 変換した値
     * @throws SQLException 数値に変換できない値の場合
     */
    private static BigDecimal toBigDecimal(final Object value) throws SQLException {
      if (!(value instanceof Number || value instanceof String)) {
        throw new SQLException("数値に変換できない値です: " + value);
      }
      try {
        return new BigDecimal(value.toString());
      } catch (NumberFormatException e) {
        throw new SQLException("数値として読めない文字列です: " + value, e);
      }
    }

    /**
     * 値を、指定した範囲の整数として読みます。範囲の外の値は {@link SQLException} を投げます。
     *
     * <p>int と long の範囲を超える値を黙って切り捨てると、テストの期待値とずれた値で通ってしまうため、範囲外は拒否する。
     *
     * @param value 変換する値。null は指定しない
     * @param min 許容する最小値
     * @param max 許容する最大値
     * @return 変換した整数の値。小数部分は切り捨てる
     * @throws SQLException 数値に変換できない値、または範囲外の値の場合
     */
    private static long toLong(final Object value, final long min, final long max)
        throws SQLException {
      BigDecimal number = toBigDecimal(value);
      if (number.compareTo(BigDecimal.valueOf(min)) < 0
          || number.compareTo(BigDecimal.valueOf(max)) > 0) {
        throw new SQLException("整数の範囲外の値です: " + value);
      }
      return number.longValue();
    }
  }

  /**
   * 結果セットのメタデータのモックの呼び出しに応答する、内部の応答クラスです。
   *
   * <p>列数と列名を、結果セットの列と同じ順番で返す。対応しないメソッドを呼ぶと {@link UnsupportedOperationException} を投げる。
   */
  private static final class Metadata implements Answer<Object> {

    /** 列名の一覧。 */
    private final List<String> columns;

    /**
     * 列名の一覧を受け取って応答クラスを作ります。
     *
     * @param columns 列名の一覧
     */
    Metadata(final List<String> columns) {
      this.columns = columns;
    }

    /**
     * 呼び出されたメソッドに応じて、列数または列名を返します。
     *
     * @param invocation 呼び出しの情報
     * @return 列数、または列名
     * @throws SQLException 列番号が範囲外の場合
     */
    @Override
    public Object answer(final InvocationOnMock invocation) throws SQLException {
      String name = invocation.getMethod().getName();
      switch (name) {
        case "getColumnCount" -> {
          return columns.size();
        }
        case "getColumnLabel", "getColumnName" -> {
          int index = (Integer) invocation.getArguments()[0] - 1;
          if (index < 0 || index >= columns.size()) {
            throw new SQLException("列番号が範囲外です: " + (index + 1));
          }
          return columns.get(index);
        }
        default -> throw new UnsupportedOperationException(
            "ResultSetMetaData は未対応のメソッドです: " + name);
      }
    }
  }
}
