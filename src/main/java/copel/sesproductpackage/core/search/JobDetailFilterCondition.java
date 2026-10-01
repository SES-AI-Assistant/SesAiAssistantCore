package copel.sesproductpackage.core.search;

import copel.sesproductpackage.core.unit.Area;
import copel.sesproductpackage.core.unit.Money;
import copel.sesproductpackage.core.unit.OriginalDateTime;
import java.util.ArrayList;
import java.util.List;
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

  /** 出社要件（0～5）。この値以上の出社要件を持つ案件を対象. */
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
   * @param tableAlias テーブルエイリアス（例: "j"）。null の場合はエイリアスなしで構築
   * @return WHERE句とパラメータを含む WhereClauseAndParams オブジェクト
   */
  public WhereClauseAndParams buildWhereClause(final String tableAlias) {
    StringBuilder whereClause = new StringBuilder();
    List<Object> params = new ArrayList<>();

    String prefix = tableAlias != null && !tableAlias.isEmpty() ? tableAlias + "." : "";

    // 開始月フィルター（startDate 以降）
    if (this.startDate != null) {
      if (whereClause.length() > 0) {
        whereClause.append(" AND ");
      }
      whereClause.append(prefix).append("start_date >= ?");
      params.add(this.startDate);
    }

    // エリアフィルター
    if (this.area != null) {
      if (whereClause.length() > 0) {
        whereClause.append(" AND ");
      }
      whereClause.append(prefix).append("area = ?");
      params.add(this.area);
    }

    // 出社要件フィルター（officeRequirements 以上）
    if (this.officeRequirements != null) {
      if (whereClause.length() > 0) {
        whereClause.append(" AND ");
      }
      whereClause.append(prefix).append("office_requirements >= ?");
      params.add(this.officeRequirements);
    }

    // 最小単価フィルター（minPrice 以上）
    if (this.minPrice != null) {
      if (whereClause.length() > 0) {
        whereClause.append(" AND ");
      }
      whereClause.append(prefix).append("unit_price >= ?");
      params.add(this.minPrice.getValue());
    }

    // 最大単価フィルター（maxPrice 以下）
    if (this.maxPrice != null) {
      if (whereClause.length() > 0) {
        whereClause.append(" AND ");
      }
      whereClause.append(prefix).append("unit_price <= ?");
      params.add(this.maxPrice.getValue());
    }

    return new WhereClauseAndParams(whereClause.toString(), params);
  }

  /**
   * この条件から WHERE句を構築します（テーブルエイリアスなし）.
   *
   * @return WHERE句とパラメータを含む WhereClauseAndParams オブジェクト
   */
  public WhereClauseAndParams buildWhereClause() {
    return buildWhereClause(null);
  }

  /** WHERE句とパラメータを保持する内部クラス. */
  public static final class WhereClauseAndParams {
    private final String whereClauseWithoutWhereKeyword;
    private final List<Object> params;

    WhereClauseAndParams(final String whereClauseWithoutWhereKeyword, final List<Object> params) {
      this.whereClauseWithoutWhereKeyword = whereClauseWithoutWhereKeyword;
      this.params = List.copyOf(params);
    }

    /** WHERE句（WHERE キーワードなし）を返します. */
    public String getWhereClauseWithoutWhereKeyword() {
      return whereClauseWithoutWhereKeyword;
    }

    /** プレースホルダ用パラメータリストを返します. */
    public List<Object> getParams() {
      return params;
    }

    /** WHERE句が空でないかを確認します. */
    public boolean isEmpty() {
      return whereClauseWithoutWhereKeyword == null || whereClauseWithoutWhereKeyword.isEmpty();
    }
  }
}
