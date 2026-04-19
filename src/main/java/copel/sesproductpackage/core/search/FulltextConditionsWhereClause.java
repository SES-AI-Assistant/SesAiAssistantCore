package copel.sesproductpackage.core.search;

import java.util.ArrayList;
import java.util.List;

/**
 * 全文検索用の WHERE 句（LIKE / NOT LIKE）を、UI と同じ OR 区切り・枠内 AND の論理で組み立てる.
 */
public final class FulltextConditionsWhereClause {

  private FulltextConditionsWhereClause() {}

  /**
   * LIKE パターン用に {@code %} と {@code _} をエスケープする（キーワード本体のみ。前後の {@code %} は付与側で付与）.
   */
  public static String escapeLikeKeyword(final String keyword) {
    if (keyword == null) {
      return "";
    }
    return keyword.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
  }

  /**
   * 検索可能なキーワードが 1 件以上あるか.
   */
  public static boolean hasSearchableKeyword(final List<FulltextCondition> conditions) {
    if (conditions == null || conditions.isEmpty()) {
      return false;
    }
    for (FulltextCondition c : conditions) {
      if (c != null && c.hasSearchableKeyword()) {
        return true;
      }
    }
    return false;
  }

  /**
   * {@code conditions} を検証し、WHERE 句（WHERE キーワードなし）とプレースホルダ用パラメータを返す.
   *
   * @param columnExpr 例: {@code raw_content}, {@code p.raw_content}
   * @throws IllegalArgumentException 条件が空、または検索可能キーワードがない場合
   */
  public static Built build(final String columnExpr, final List<FulltextCondition> conditions) {
    if (columnExpr == null || columnExpr.trim().isEmpty()) {
      throw new IllegalArgumentException("columnExpr is required");
    }
    if (!hasSearchableKeyword(conditions)) {
      throw new IllegalArgumentException("conditions must contain at least one non-blank keyword");
    }
    List<List<FulltextCondition>> segments = splitSegments(conditions);
    List<String> params = new ArrayList<>();
    StringBuilder ors = new StringBuilder();
    boolean firstSeg = true;
    for (List<FulltextCondition> seg : segments) {
      StringBuilder segAnd = new StringBuilder();
      boolean firstTerm = true;
      for (FulltextCondition c : seg) {
        if (!c.hasSearchableKeyword()) {
          continue;
        }
        if (!firstTerm) {
          segAnd.append(" AND ");
        }
        firstTerm = false;
        String lit = "%" + escapeLikeKeyword(c.getKeyword().trim()) + "%";
        if (c.isNegated()) {
          segAnd.append('(').append(columnExpr).append(" NOT LIKE ? ESCAPE '\\')");
        } else {
          segAnd.append('(').append(columnExpr).append(" LIKE ? ESCAPE '\\')");
        }
        params.add(lit);
      }
      if (segAnd.isEmpty()) {
        continue;
      }
      if (!firstSeg) {
        ors.append(" OR ");
      }
      firstSeg = false;
      ors.append('(').append(segAnd).append(')');
    }
    return new Built(ors.toString(), params);
  }

  /**
   * 複数カラムに OR で照合する WHERE 句を組み立てる.
   *
   * <p>正のキーワード: {@code (primaryColumn LIKE ? OR nullableColumn LIKE ?)}<br>
   * 否定キーワード: {@code (primaryColumn NOT LIKE ? AND (nullableColumn IS NULL OR nullableColumn NOT LIKE ?))}<br>
   * NULL チェックを加えることで LEFT JOIN 先が NULL の行を NOT 条件で誤除外しない。
   *
   * @param primaryColumn 非 NULL 前提の主カラム（例: {@code p.raw_content}）
   * @param nullableColumns NULL になり得る副カラム（例: {@code s.file_content_summary}）
   * @param conditions 検索条件リスト
   * @throws IllegalArgumentException 条件が空、または検索可能キーワードがない場合
   */
  public static Built buildForMultipleColumns(
      final String primaryColumn,
      final List<String> nullableColumns,
      final List<FulltextCondition> conditions) {
    if (primaryColumn == null || primaryColumn.trim().isEmpty()) {
      throw new IllegalArgumentException("primaryColumn is required");
    }
    if (!hasSearchableKeyword(conditions)) {
      throw new IllegalArgumentException("conditions must contain at least one non-blank keyword");
    }
    List<List<FulltextCondition>> segments = splitSegments(conditions);
    List<String> params = new ArrayList<>();
    StringBuilder ors = new StringBuilder();
    boolean firstSeg = true;
    for (List<FulltextCondition> seg : segments) {
      StringBuilder segAnd = new StringBuilder();
      boolean firstTerm = true;
      for (FulltextCondition c : seg) {
        if (!c.hasSearchableKeyword()) {
          continue;
        }
        if (!firstTerm) {
          segAnd.append(" AND ");
        }
        firstTerm = false;
        String lit = "%" + escapeLikeKeyword(c.getKeyword().trim()) + "%";
        if (c.isNegated()) {
          segAnd.append('(');
          segAnd.append(primaryColumn).append(" NOT LIKE ? ESCAPE '\\'");
          params.add(lit);
          for (String nullable : nullableColumns) {
            segAnd.append(" AND (").append(nullable).append(" IS NULL OR ");
            segAnd.append(nullable).append(" NOT LIKE ? ESCAPE '\\')");
            params.add(lit);
          }
          segAnd.append(')');
        } else {
          segAnd.append('(');
          segAnd.append(primaryColumn).append(" LIKE ? ESCAPE '\\'");
          params.add(lit);
          for (String nullable : nullableColumns) {
            segAnd.append(" OR ").append(nullable).append(" LIKE ? ESCAPE '\\'");
            params.add(lit);
          }
          segAnd.append(')');
        }
      }
      if (segAnd.isEmpty()) {
        continue;
      }
      if (!firstSeg) {
        ors.append(" OR ");
      }
      firstSeg = false;
      ors.append('(').append(segAnd).append(')');
    }
    return new Built(ors.toString(), params);
  }

  static List<List<FulltextCondition>> splitSegments(final List<FulltextCondition> conditions) {
    List<List<FulltextCondition>> segments = new ArrayList<>();
    List<FulltextCondition> current = new ArrayList<>();
    for (int i = 0; i < conditions.size(); i++) {
      FulltextCondition c = conditions.get(i);
      if (c == null) {
        continue;
      }
      if (i == 0) {
        current.add(c);
      } else if ("OR".equalsIgnoreCase(trimOrEmpty(c.getOperator()))) {
        if (!current.isEmpty()) {
          segments.add(new ArrayList<>(current));
        }
        current = new ArrayList<>();
        current.add(c);
      } else {
        current.add(c);
      }
    }
    if (!current.isEmpty()) {
      segments.add(current);
    }
    return segments;
  }

  private static String trimOrEmpty(final String s) {
    return s == null ? "" : s.trim();
  }

  /** {@link #build} の結果. */
  public static final class Built {
    private final String whereClauseWithoutWhereKeyword;
    private final List<String> likeParams;

    Built(final String whereClauseWithoutWhereKeyword, final List<String> likeParams) {
      this.whereClauseWithoutWhereKeyword = whereClauseWithoutWhereKeyword;
      this.likeParams = List.copyOf(likeParams);
    }

    public String getWhereClauseWithoutWhereKeyword() {
      return whereClauseWithoutWhereKeyword;
    }

    public List<String> getLikeParams() {
      return likeParams;
    }
  }
}
