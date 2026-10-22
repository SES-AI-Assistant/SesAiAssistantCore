package copel.sesproductpackage.core.search;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 検索条件を AND で連結し、WHERE 句とパラメータを組み立てるビルダーです.
 *
 * <p>テーブルエイリアスを指定すると、列名の前にエイリアスとドットを付けます。
 *
 * <p>エイリアスは SQL に直接連結するため、識別子以外は拒否します。
 *
 * @author Copel Co., Ltd.
 */
public final class WhereClauseBuilder {
  /** 識別子の形式。英字かアンダースコアで始まり、英数字とアンダースコアが続く。テーブルエイリアスと列名で共通に使う。 */
  private static final Pattern IDENTIFIER_PATTERN = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");

  /** 列名に前置する「エイリアス.」。エイリアスが無い場合は空文字。 */
  private final String prefix;

  /** AND で連結した条件式。 */
  private final StringBuilder whereClause = new StringBuilder();

  /** プレースホルダに対応する値の並び。 */
  private final List<Object> params = new ArrayList<>();

  /**
   * テーブルエイリアスを指定して生成します.
   *
   * <p>null または空文字の場合は列名にエイリアスを付けない。
   *
   * @param tableAlias テーブルエイリアス（例: "p"）
   * @throws IllegalArgumentException テーブルエイリアスが識別子でない場合
   */
  public WhereClauseBuilder(final String tableAlias) {
    if (tableAlias == null || tableAlias.isEmpty()) {
      this.prefix = "";
    } else if (isIdentifier(tableAlias)) {
      this.prefix = tableAlias + ".";
    } else {
      throw new IllegalArgumentException("テーブルエイリアスは識別子で指定してください: " + tableAlias);
    }
  }

  /**
   * 値が識別子の形式かを判定します.
   *
   * @param value 判定する値（null を渡さない）
   * @return 識別子の形式なら true、形式が違う場合は false
   */
  static boolean isIdentifier(final String value) {
    return IDENTIFIER_PATTERN.matcher(value).matches();
  }

  /**
   * 列名式が識別子、または「テーブル名.列名」の形式かを判定します.
   *
   * <p>ドットは1つまで許す。Backend が検証の目的で渡す「_」も識別子として扱う。
   *
   * @param value 判定する列名式
   * @return 列名式の形式なら true、null または形式が違う場合は false
   */
  static boolean isColumnExpr(final String value) {
    if (value == null) {
      return false;
    }
    String[] parts = value.split("\\.", -1);
    return parts.length <= 2 && Arrays.stream(parts).allMatch(WhereClauseBuilder::isIdentifier);
  }

  /**
   * テーブルエイリアスを付けた列名を返します.
   *
   * @param name 列名（例: "start_date"）
   * @return エイリアスを前置した列名（例: "p.start_date"）。エイリアスが無い場合は列名そのもの
   */
  public String column(final String name) {
    return prefix + name;
  }

  /**
   * 条件式を AND で連結し、プレースホルダに対応する値を加えます.
   *
   * @param condition 条件式。列名は {@link #column(String)} で取得した名前を使う
   * @param values プレースホルダに渡す値
   * @return このビルダー自身
   */
  public WhereClauseBuilder and(final String condition, final Object... values) {
    if (whereClause.length() > 0) {
      whereClause.append(" AND ");
    }
    whereClause.append(condition);
    params.addAll(Arrays.asList(values));
    return this;
  }

  /**
   * ここまでに連結した条件式とパラメータから、WHERE 句の結果を生成します.
   *
   * @return WHERE キーワードなしの条件式とパラメータを含む {@link WhereClauseAndParams}
   */
  public WhereClauseAndParams build() {
    return new WhereClauseAndParams(whereClause.toString(), params);
  }
}
