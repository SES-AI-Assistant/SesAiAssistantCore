package copel.sesproductpackage.core.search;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 全文検索用の WHERE 句（LIKE / NOT LIKE）を、UI と同じ OR 区切り・枠内 AND の論理で組み立てる.
 *
 * <p>演算子が OR の条件で区切った区間の中を AND、区間同士を OR で結ぶ。
 *
 * <p>先頭の条件の演算子は解釈しない。AND と OR 以外（null を含む）は AND として扱う。
 *
 * <p>含まない条件では、列が NULL の行は検索語に一致しないとみなして残す。
 *
 * <p>NULL の行を NOT LIKE で除外すると、LEFT JOIN 先が無い行が消えるため。
 *
 * @author Copel Co., Ltd.
 */
public final class FulltextConditionsWhereClause {
  private FulltextConditionsWhereClause() {}

  /**
   * 検索可能なキーワードが1件以上あるかを判定します.
   *
   * @param conditions 検索条件のリスト。null 要素は無視する
   * @return 空でないキーワードを持つ条件が1件以上あれば true、リストが null か空、または該当が無ければ false
   */
  public static boolean hasSearchableKeyword(final List<FulltextCondition> conditions) {
    if (conditions == null || conditions.isEmpty()) {
      return false;
    }
    for (FulltextCondition condition : conditions) {
      if (condition != null && condition.hasSearchableKeyword()) {
        return true;
      }
    }
    return false;
  }

  /**
   * 1つの列に対する WHERE 句（WHERE キーワードなし）とプレースホルダ用パラメータを組み立てます.
   *
   * @param columnExpr 列名式。例: {@code raw_content}、{@code p.raw_content}
   * @param conditions 検索条件のリスト
   * @return WHERE 句とパラメータを含む {@link Built}
   * @throws IllegalArgumentException 列名式が識別子でない場合、または検索可能なキーワードが1件も無い場合
   */
  public static Built build(final String columnExpr, final List<FulltextCondition> conditions) {
    return buildClause(Collections.singletonList(columnExpr), conditions);
  }

  /**
   * 複数の列に OR で照合する WHERE 句（WHERE キーワードなし）とパラメータを組み立てます.
   *
   * <p>正のキーワード: {@code (primary LIKE ? OR nullable LIKE ?)}<br>
   * 否定キーワード: {@code ((primary IS NULL OR primary NOT LIKE ?) AND (nullable IS NULL OR nullable NOT
   * LIKE ?))}
   *
   * @param primaryColumn 主カラムの列名式。例: {@code p.raw_content}
   * @param nullableColumns 副カラムの列名式の一覧。LEFT JOIN で NULL になり得る列を指定する
   * @param conditions 検索条件のリスト
   * @return WHERE 句とパラメータを含む {@link Built}
   * @throws IllegalArgumentException 列名式が識別子でない場合、または検索可能なキーワードが1件も無い場合
   */
  public static Built buildForMultipleColumns(
      final String primaryColumn,
      final List<String> nullableColumns,
      final List<FulltextCondition> conditions) {
    List<String> columns = new ArrayList<>();
    columns.add(primaryColumn);
    columns.addAll(nullableColumns);
    return buildClause(columns, conditions);
  }

  /**
   * 列名式の一覧と検索条件から、WHERE 句とパラメータを組み立てます.
   *
   * <p>列の数だけプレースホルダを作り、同じキーワードを各列に渡す。区間の分け方は {@link #build} と {@link #buildForMultipleColumns} で共通。
   *
   * @param columns 列名式の一覧。先頭から順に並ぶ
   * @param conditions 検索条件のリスト
   * @return WHERE 句とパラメータを含む {@link Built}
   * @throws IllegalArgumentException 列名式が識別子でない場合、または検索可能なキーワードが1件も無い場合
   */
  private static Built buildClause(
      final List<String> columns, final List<FulltextCondition> conditions) {
    for (String column : columns) {
      if (!WhereClauseBuilder.isColumnExpr(column)) {
        throw new IllegalArgumentException("列名式は識別子で指定してください: " + column);
      }
    }
    if (!hasSearchableKeyword(conditions)) {
      throw new IllegalArgumentException("検索可能なキーワードを1件以上指定してください");
    }

    // 演算子が OR の条件から新しい区間を始める。先頭の条件の演算子は解釈しないため i が 0 のときは区切らない
    List<List<FulltextCondition>> segments = new ArrayList<>();
    List<FulltextCondition> current = new ArrayList<>();
    for (int i = 0; i < conditions.size(); i++) {
      FulltextCondition condition = conditions.get(i);
      if (condition == null) {
        continue;
      }
      String operator = condition.getOperator();
      boolean startsSegment = i > 0 && operator != null && "OR".equalsIgnoreCase(operator.trim());
      if (startsSegment) {
        if (!current.isEmpty()) {
          segments.add(current);
        }
        current = new ArrayList<>();
      }
      current.add(condition);
    }
    // 検索可能なキーワードが1件以上あるため、末尾の区間は必ず空でない
    segments.add(current);

    List<String> params = new ArrayList<>();
    StringBuilder orClause = new StringBuilder();
    for (List<FulltextCondition> segment : segments) {
      StringBuilder andClause = new StringBuilder();
      for (FulltextCondition condition : segment) {
        if (!condition.hasSearchableKeyword()) {
          continue;
        }
        if (andClause.length() > 0) {
          andClause.append(" AND ");
        }
        String term;
        if (condition.isNegated()) {
          // 列が NULL の行は「含まない」に入れるため、列ごとに IS NULL を加える
          List<String> notLike = new ArrayList<>();
          for (String column : columns) {
            notLike.add("(" + column + " IS NULL OR " + column + " NOT LIKE ? ESCAPE '\\')");
          }
          term = notLike.size() == 1 ? notLike.get(0) : "(" + String.join(" AND ", notLike) + ")";
        } else {
          List<String> like = new ArrayList<>();
          for (String column : columns) {
            like.add(column + " LIKE ? ESCAPE '\\'");
          }
          term = "(" + String.join(" OR ", like) + ")";
        }
        andClause.append(term);
        // 検索語の LIKE 特殊文字（\ % _）をエスケープし、前後に % を付けた値を列の数だけ渡す
        String keyword = condition.getKeyword().trim();
        String pattern =
            "%" + keyword.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
        params.addAll(Collections.nCopies(columns.size(), pattern));
      }
      if (andClause.length() == 0) {
        continue;
      }
      if (orClause.length() > 0) {
        orClause.append(" OR ");
      }
      orClause.append('(').append(andClause).append(')');
    }
    return new Built("(" + orClause + ")", params);
  }

  /** {@link #build} と {@link #buildForMultipleColumns} の結果. */
  public static final class Built {
    /** WHERE キーワードを含まない条件式。 */
    private final String whereClauseWithoutWhereKeyword;

    /** LIKE 句のプレースホルダに渡す値の並び。 */
    private final List<String> likeParams;

    /**
     * 結果を生成します.
     *
     * @param whereClauseWithoutWhereKeyword WHERE キーワードを含まない条件式
     * @param likeParams LIKE 句のプレースホルダに渡す値の並び
     */
    Built(final String whereClauseWithoutWhereKeyword, final List<String> likeParams) {
      this.whereClauseWithoutWhereKeyword = whereClauseWithoutWhereKeyword;
      this.likeParams = List.copyOf(likeParams);
    }

    /**
     * WHERE 句（WHERE キーワードなし）を返します.
     *
     * @return WHERE キーワードを含まない条件式
     */
    public String getWhereClauseWithoutWhereKeyword() {
      return whereClauseWithoutWhereKeyword;
    }

    /**
     * LIKE 句のプレースホルダに渡す値を返します.
     *
     * @return プレースホルダに渡す値の並び
     */
    public List<String> getLikeParams() {
      return likeParams;
    }
  }
}
