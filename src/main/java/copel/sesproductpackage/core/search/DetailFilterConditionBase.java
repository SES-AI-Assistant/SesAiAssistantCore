package copel.sesproductpackage.core.search;

import copel.sesproductpackage.core.unit.Area;
import copel.sesproductpackage.core.unit.Money;
import copel.sesproductpackage.core.unit.OriginalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * 案件と要員の詳細フィルター条件に共通する項目（開始月・エリア・単価の範囲）と、その WHERE 句の組み立てを持つ基底クラス.
 *
 * <p>案件と要員は、開始月・エリア・単価を同じ列名と同じ条件で絞り込む。同じ項目と組み立てを両方のクラスに持つと、条件の変更が片方だけに入ってしまうため、ここにまとめる。
 * 出社条件など各クラス特有の項目はサブクラスで定義する。条件式を追加する順序はサブクラスの buildWhereClause が決めるため、生成される SQL の順序は変わらない。
 *
 * @author Copel Co., Ltd.
 */
@Getter
@Setter
public abstract class DetailFilterConditionBase {
  /** 開始月（yyyy/MM形式）。この日付以降の開始月を持つ案件または要員を対象. */
  private OriginalDateTime startDate;

  /** エリア（地域コード）。このエリアに一致する案件または要員を対象. */
  private Area area;

  /** 最小単価。この価格以上の案件または要員を対象. */
  private Money minPrice;

  /** 最大単価。この価格以下の案件または要員を対象. */
  private Money maxPrice;

  /**
   * 開始月の条件を、ビルダーに AND で追加します.
   *
   * <p>開始月が null の場合は条件を追加しません。
   *
   * @param builder 条件を追加するビルダー（テーブルエイリアスはビルダーが持つ）
   */
  protected void appendStartDateCondition(final WhereClauseBuilder builder) {
    if (this.startDate != null) {
      builder.and(builder.column("start_date") + " >= ?", this.startDate);
    }
  }

  /**
   * エリアの条件を、ビルダーに AND で追加します.
   *
   * <p>エリアが null の場合は条件を追加しません。
   *
   * @param builder 条件を追加するビルダー（テーブルエイリアスはビルダーが持つ）
   */
  protected void appendAreaCondition(final WhereClauseBuilder builder) {
    if (this.area != null) {
      builder.and(builder.column("area") + " = ?", this.area);
    }
  }

  /**
   * 単価の範囲（下限と上限）の条件を、ビルダーに AND で追加します.
   *
   * <p>単価は値を持たない {@link Money#empty()} の場合は条件に含めません。値が null のまま SQL のパラメータに渡すと組み立てに失敗するためです。
   *
   * @param builder 条件を追加するビルダー（テーブルエイリアスはビルダーが持つ）
   */
  protected void appendPriceRangeCondition(final WhereClauseBuilder builder) {
    if (this.minPrice != null && this.minPrice.hasValue()) {
      builder.and(builder.column("unit_price") + " >= ?", this.minPrice.getValue());
    }
    if (this.maxPrice != null && this.maxPrice.hasValue()) {
      builder.and(builder.column("unit_price") + " <= ?", this.maxPrice.getValue());
    }
  }
}
