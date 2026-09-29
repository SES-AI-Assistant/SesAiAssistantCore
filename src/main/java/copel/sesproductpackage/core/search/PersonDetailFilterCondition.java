package copel.sesproductpackage.core.search;

import copel.sesproductpackage.core.unit.Area;
import copel.sesproductpackage.core.unit.Money;
import copel.sesproductpackage.core.unit.OriginalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/**
 * 要員検索の詳細フィルター条件を保持するクラス.
 * 各フィールドが null の場合、そのフィルター条件は適用されません。
 */
@Getter
@Setter
public class PersonDetailFilterCondition {
  /** 開始月（yyyy/MM形式）。この日付以降の開始月を持つ要員を対象. */
  private OriginalDateTime startDate;

  /** 最小単価。この価格以上の要員を対象. */
  private Money minPrice;

  /** 最大単価。この価格以下の要員を対象. */
  private Money maxPrice;

  /** 年齢の下限。この年齢以上の要員を対象. */
  private Integer minAge;

  /** 年齢の上限。この年齢以下の要員を対象. */
  private Integer maxAge;

  /** 性別（M=男性、F=女性）。この性別に一致する要員を対象. */
  private String gender;

  /** 国籍（JP=日本、OTHER=日本以外）。この国籍に一致する要員を対象. */
  private String nationality;

  /** エリア（地域コード）。このエリアに一致する要員を対象. */
  private Area area;

  /** 出社可能日数（0～5）。この値以上の出社可用性を持つ要員を対象. */
  private Integer officeAvailability;

  /** N社先の『N』にあたる数字（0=直、1=1社先、2=2社先）。この値に一致する要員を対象. */
  private Integer companiesTierNumber;

  /** 所属形態（正社員、個人事業主等）。この形態に一致する要員を対象. */
  private String employmentType;

  /** デフォルトコンストラクタ. */
  public PersonDetailFilterCondition() {}

  /**
   * この条件から WHERE句（WHERE キーワードなし）とパラメータリストを構築します.
   *
   * @param tableAlias テーブルエイリアス（例: "p", "s"）。null の場合はエイリアスなしで構築
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

    // 最小年齢フィルター（minAge 以上）
    if (this.minAge != null) {
      if (whereClause.length() > 0) {
        whereClause.append(" AND ");
      }
      whereClause.append(prefix).append("age >= ?");
      params.add(this.minAge);
    }

    // 最大年齢フィルター（maxAge 以下）
    if (this.maxAge != null) {
      if (whereClause.length() > 0) {
        whereClause.append(" AND ");
      }
      whereClause.append(prefix).append("age <= ?");
      params.add(this.maxAge);
    }

    // 性別フィルター
    if (this.gender != null && !this.gender.isEmpty()) {
      if (whereClause.length() > 0) {
        whereClause.append(" AND ");
      }
      whereClause.append(prefix).append("gender = ?");
      params.add(this.gender);
    }

    // 国籍フィルター
    if (this.nationality != null && !this.nationality.isEmpty()) {
      if (whereClause.length() > 0) {
        whereClause.append(" AND ");
      }
      whereClause.append(prefix).append("nationality = ?");
      params.add(this.nationality);
    }

    // エリアフィルター
    if (this.area != null) {
      if (whereClause.length() > 0) {
        whereClause.append(" AND ");
      }
      whereClause.append(prefix).append("area = ?");
      params.add(this.area);
    }

    // 出社可能日数フィルター（officeAvailability 以上）
    if (this.officeAvailability != null) {
      if (whereClause.length() > 0) {
        whereClause.append(" AND ");
      }
      whereClause.append(prefix).append("office_availability >= ?");
      params.add(this.officeAvailability);
    }

    // 所属フィルター（organizationは「N社先形態」フォーマット）
    // companiesTierNumber と employmentType の両方が指定されている場合、
    // 「N社先形態」という文字列全体で一致検索
    if (this.companiesTierNumber != null && this.employmentType != null && !this.employmentType.isEmpty()) {
      if (whereClause.length() > 0) {
        whereClause.append(" AND ");
      }
      String organizationValue = this.companiesTierNumber + "社先" + this.employmentType;
      whereClause.append(prefix).append("organization = ?");
      params.add(organizationValue);
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
      return whereClauseWithoutWhereKeyword == null
          || whereClauseWithoutWhereKeyword.isEmpty();
    }
  }
}
