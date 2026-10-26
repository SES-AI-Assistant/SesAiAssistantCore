package copel.sesproductpackage.core.unit;

/**
 * 要員の年齢の値オブジェクト.
 *
 * <p>AI の要約結果では、年齢が読み取れなかった場合の値として 0 以下の数が入る（Core の要員情報スキーマの既定値は -1）。 0 以下は「年齢不明」を表すため、保存時は null
 * に変えて DB に不正な年齢を残さない。
 *
 * @author Copel Co., Ltd.
 */
public final class PersonAge {

  /** 年齢の値。0 以下は年齢不明を表す. */
  private final int value;

  /**
   * コンストラクタ. 生成は {@link #of(int)} で行う.
   *
   * @param value 年齢（0 以下は年齢不明）
   */
  private PersonAge(final int value) {
    this.value = value;
  }

  /**
   * 年齢の値オブジェクトを作る.
   *
   * @param age 年齢（0 以下は年齢不明）
   * @return 年齢の値オブジェクト
   */
  public static PersonAge of(final int age) {
    return new PersonAge(age);
  }

  /**
   * 保存する年齢を返す.
   *
   * @return 1 以上の年齢。年齢不明（0 以下）の場合は null
   */
  public Integer toStoredValue() {
    return this.value > 0 ? this.value : null;
  }
}
