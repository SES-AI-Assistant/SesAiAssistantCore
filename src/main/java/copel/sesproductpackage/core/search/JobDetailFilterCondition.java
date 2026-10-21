package copel.sesproductpackage.core.search;

import copel.sesproductpackage.core.unit.Area;
import copel.sesproductpackage.core.unit.Money;
import copel.sesproductpackage.core.unit.OriginalDateTime;
import lombok.Getter;
import lombok.Setter;

/** 案件検索の詳細フィルター条件を保持するクラス. 各フィールドが null の場合、そのフィルター条件は適用されません。 */
@Getter
@Setter
public class JobDetailFilterCondition {
  /** 開始月（yyyy/MM形式）。この日付以降の開始月を持つ案件を対象. */
  private OriginalDateTime startDate;

  /** エリア（地域コード）。このエリアに一致する案件を対象. */
  private Area area;

  /** 出社要件（0～5、0=フルリモート、5=常駐）。この値以下の出社要件を持つ案件を対象. */
  private Integer officeRequirements;

  /** 最小単価。この価格以上の案件を対象. */
  private Money minPrice;

  /** 最大単価。この価格以下の案件を対象. */
  private Money maxPrice;

  /** デフォルトコンストラクタ. */
  public JobDetailFilterCondition() {}

  /**
   * この条件から WHERE句（WHERE キーワードなし）とパラメータリストを構築します.
   *
   * <p>単価は値を持たない {@link Money#empty()} の場合も条件に含めない。値が null のまま SQL のパラメータに渡すと組み立てに失敗するため。
   *
   * @param tableAlias テーブルエイリアス（例: "j"）。null または空の場合はエイリアスなしで構築
   * @return WHERE句とパラメータを含む {@link WhereClauseAndParams} オブジェクト
   * @throws IllegalArgumentException テーブルエイリアスが識別子でない場合
   */
  public WhereClauseAndParams buildWhereClause(final String tableAlias) {
    WhereClauseBuilder builder = new WhereClauseBuilder(tableAlias);

    // 開始月フィルター（startDate 以降）
    if (this.startDate != null) {
      builder.and(builder.column("start_date") + " >= ?", this.startDate);
    }

    // エリアフィルター
    if (this.area != null) {
      builder.and(builder.column("area") + " = ?", this.area);
    }

    // 出社要件フィルター（ユーザーが許容できる出社日数の上限を指定するため officeRequirements 以下を検索対象とする。
    // 例：フルリモート(0)を指定した場合、出社を伴う案件(1以上)を除外する必要がある）
    if (this.officeRequirements != null) {
      builder.and(builder.column("office_requirements") + " <= ?", this.officeRequirements);
    }

    // 最小単価フィルター（minPrice 以上）
    if (this.minPrice != null && this.minPrice.hasValue()) {
      builder.and(builder.column("unit_price") + " >= ?", this.minPrice.getValue());
    }

    // 最大単価フィルター（maxPrice 以下）
    if (this.maxPrice != null && this.maxPrice.hasValue()) {
      builder.and(builder.column("unit_price") + " <= ?", this.maxPrice.getValue());
    }

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
