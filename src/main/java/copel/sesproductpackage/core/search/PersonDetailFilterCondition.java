package copel.sesproductpackage.core.search;

import lombok.Getter;
import lombok.Setter;

/**
 * 要員検索の詳細フィルター条件を保持するクラス. 各フィールドが null の場合、そのフィルター条件は適用されません。
 *
 * <p>開始月・単価の範囲・エリアは {@link DetailFilterConditionBase} が持つ。
 *
 * @author Copel Co., Ltd.
 */
@Getter
@Setter
public class PersonDetailFilterCondition extends DetailFilterConditionBase {
  /** 国籍フィルターの特別な値：「日本以外」を表す. */
  private static final String NATIONALITY_OTHER = "日本以外";

  /** nationality カラムが「日本国籍」を表す際の実際の格納値. */
  private static final String NATIONALITY_JAPAN = "日本";

  /** 年齢の下限。この年齢以上の要員を対象. */
  private Integer minAge;

  /** 年齢の上限。この年齢以下の要員を対象. */
  private Integer maxAge;

  /**
   * 性別（DB値: Man / Woman / Unknown のみ。日本語表記は呼び出し側で Gender.fromJapanese により変換して渡す）。この性別に一致する要員を対象.
   */
  private String gender;

  /**
   * 国籍。nationality カラムの格納値（例："日本"）と完全一致する文字列を指定する。 「日本以外」を検索したい場合のみ特別な値 "日本以外"
   * を指定する（nationalityカラムが "日本" 以外の値を持つ要員が対象になる）.
   */
  private String nationality;

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
   * <p>単価は値を持たない {@link copel.sesproductpackage.core.unit.Money#empty()} の場合も条件に含めない。値が null のまま
   * SQL のパラメータに渡すと組み立てに失敗するため。 「N社先」と所属形態は両方が指定されたときだけ、「N社先形態」の文字列全体で一致させる。
   *
   * @param tableAlias テーブルエイリアス（例: "p"）。null または空の場合はエイリアスなしで構築
   * @return WHERE句とパラメータを含む {@link WhereClauseAndParams} オブジェクト
   * @throws IllegalArgumentException テーブルエイリアスが識別子でない場合
   */
  public WhereClauseAndParams buildWhereClause(final String tableAlias) {
    WhereClauseBuilder builder = new WhereClauseBuilder(tableAlias);

    // 開始月フィルター（startDate 以降）
    appendStartDateCondition(builder);

    // 最小単価フィルター（minPrice 以上）と最大単価フィルター（maxPrice 以下）
    appendPriceRangeCondition(builder);

    // 最小年齢フィルター（minAge 以上）
    if (this.minAge != null) {
      builder.and(builder.column("age") + " >= ?", this.minAge);
    }

    // 最大年齢フィルター（maxAge 以下）
    if (this.maxAge != null) {
      builder.and(builder.column("age") + " <= ?", this.maxAge);
    }

    // 性別フィルター
    if (this.gender != null && !this.gender.isEmpty()) {
      builder.and(builder.column("gender") + " = ?", this.gender);
    }

    // 国籍フィルター（"日本以外" は「nationalityが"日本"ではない」ことを表す特別な値）
    if (this.nationality != null && !this.nationality.isEmpty()) {
      String column = builder.column("nationality");
      if (NATIONALITY_OTHER.equals(this.nationality)) {
        builder.and(column + " IS NOT NULL AND " + column + " <> ?", NATIONALITY_JAPAN);
      } else {
        builder.and(column + " = ?", this.nationality);
      }
    }

    // エリアフィルター
    appendAreaCondition(builder);

    // 出社可能日数フィルター（officeAvailability 以上）
    if (this.officeAvailability != null) {
      builder.and(builder.column("office_availability") + " >= ?", this.officeAvailability);
    }

    // 所属フィルター（organizationは「N社先形態」フォーマット）
    // companiesTierNumber と employmentType の両方が指定されている場合、
    // 「N社先形態」という文字列全体で一致検索
    if (this.companiesTierNumber != null
        && this.employmentType != null
        && !this.employmentType.isEmpty()) {
      String organizationValue = this.companiesTierNumber + "社先" + this.employmentType;
      builder.and(builder.column("organization") + " = ?", organizationValue);
    }

    return builder.build();
  }
}
