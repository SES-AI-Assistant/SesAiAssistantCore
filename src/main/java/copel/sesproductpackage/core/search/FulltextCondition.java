package copel.sesproductpackage.core.search;

import copel.sesproductpackage.core.api.gpt.schema.Schema;
import java.util.Objects;
import lombok.NoArgsConstructor;

/**
 * 全文検索 API の 1 条件（operator / keyword / negated）.
 *
 * <p>先頭要素の {@code operator} は解釈に用いない想定です。
 */
@NoArgsConstructor(force = true)
public final class FulltextCondition {
  @Schema(description = "\"AND\" または \"OR\"", required = true, example = "AND")
  private final String operator;

  @Schema(description = "検索キーワード", required = true, example = "Python")
  private final String keyword;

  @Schema(description = "否定フラグ", required = true, example = "false")
  private final boolean negated;

  public FulltextCondition(final String operator, final String keyword, final boolean negated) {
    this.operator = operator;
    this.keyword = keyword;
    this.negated = negated;
  }

  public String getOperator() {
    return operator;
  }

  public String getKeyword() {
    return keyword;
  }

  public boolean isNegated() {
    return negated;
  }

  /** トリム後に空でないキーワードがあれば検索可能とみなす. */
  public boolean hasSearchableKeyword() {
    return keyword != null && !keyword.trim().isEmpty();
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    FulltextCondition that = (FulltextCondition) o;
    return negated == that.negated
        && Objects.equals(operator, that.operator)
        && Objects.equals(keyword, that.keyword);
  }

  @Override
  public int hashCode() {
    return Objects.hash(operator, keyword, negated);
  }
}
