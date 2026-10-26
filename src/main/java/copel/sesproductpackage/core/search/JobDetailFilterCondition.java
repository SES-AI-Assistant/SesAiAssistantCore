package copel.sesproductpackage.core.search;

import lombok.Getter;
import lombok.Setter;

/**
 * 案件検索の詳細フィルター条件を保持するクラス. 各フィールドが null の場合、そのフィルター条件は適用されません。
 *
 * <p>開始月・エリア・単価の範囲は {@link DetailFilterConditionBase} が持つ。
 *
 * @author Copel Co., Ltd.
 */
@Getter
@Setter
public class JobDetailFilterCondition extends DetailFilterConditionBase {
  /** 出社要件（0～5、0=フルリモート、5=常駐）。この値以下の出社要件を持つ案件を対象. */
  private Integer officeRequirements;

  /** デフォルトコンストラクタ. */
  public JobDetailFilterCondition() {}

  /**
   * この条件から WHERE句（WHERE キーワードなし）とパラメータリストを構築します.
   *
   * <p>単価は値を持たない {@link copel.sesproductpackage.core.unit.Money#empty()} の場合も条件に含めない。値が null のまま
   * SQL のパラメータに渡すと組み立てに失敗するため。
   *
   * @param tableAlias テーブルエイリアス（例: "j"）。null または空の場合はエイリアスなしで構築
   * @return WHERE句とパラメータを含む {@link WhereClauseAndParams} オブジェクト
   * @throws IllegalArgumentException テーブルエイリアスが識別子でない場合
   */
  public WhereClauseAndParams buildWhereClause(final String tableAlias) {
    WhereClauseBuilder builder = new WhereClauseBuilder(tableAlias);

    // 開始月フィルター（startDate 以降）
    appendStartDateCondition(builder);

    // エリアフィルター
    appendAreaCondition(builder);

    // 出社要件フィルター（ユーザーが許容できる出社日数の上限を指定するため officeRequirements 以下を検索対象とする。
    // 例：フルリモート(0)を指定した場合、出社を伴う案件(1以上)を除外する必要がある）
    if (this.officeRequirements != null) {
      builder.and(builder.column("office_requirements") + " <= ?", this.officeRequirements);
    }

    // 最小単価フィルター（minPrice 以上）と最大単価フィルター（maxPrice 以下）
    appendPriceRangeCondition(builder);

    return builder.build();
  }

  /**
   * この条件から WHERE句を構築します（テーブルエイリアスなし）.
   *
   * @return WHERE句とパラメータを含む {@link WhereClauseAndParams} オブジェクト
   */
  public WhereClauseAndParams buildWhereClause() {
    return buildWhereClause(null);
  }
}
