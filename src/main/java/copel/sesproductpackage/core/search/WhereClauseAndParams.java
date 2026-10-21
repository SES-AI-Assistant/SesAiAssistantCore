package copel.sesproductpackage.core.search;

import java.util.List;

/**
 * WHERE 句（WHERE キーワードなし）とプレースホルダ用のパラメータを保持する値オブジェクト.
 *
 * <p>検索条件の組み立て結果を、Lot の SQL 生成に渡すために使う。{@link WhereClauseBuilder} で生成する。
 *
 * @author Copel Co., Ltd.
 */
public final class WhereClauseAndParams {
  /** WHERE キーワードを含まない条件式。条件が無い場合は空文字。 */
  private final String whereClauseWithoutWhereKeyword;

  /** 条件式のプレースホルダに対応する値の並び。 */
  private final List<Object> params;

  /**
   * WHERE 句とパラメータを指定して生成します.
   *
   * @param whereClauseWithoutWhereKeyword WHERE キーワードを含まない条件式
   * @param params プレースホルダに対応する値の並び
   */
  WhereClauseAndParams(final String whereClauseWithoutWhereKeyword, final List<Object> params) {
    this.whereClauseWithoutWhereKeyword = whereClauseWithoutWhereKeyword;
    this.params = List.copyOf(params);
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
   * プレースホルダ用パラメータリストを返します.
   *
   * @return プレースホルダに対応する値の並び
   */
  public List<Object> getParams() {
    return params;
  }

  /**
   * WHERE 句が空かを確認します.
   *
   * @return 条件が無ければ true、条件があれば false
   */
  public boolean isEmpty() {
    return whereClauseWithoutWhereKeyword.isEmpty();
  }
}
