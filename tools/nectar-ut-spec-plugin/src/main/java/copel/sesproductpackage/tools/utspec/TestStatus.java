package copel.sesproductpackage.tools.utspec;

/**
 * テストケースの実行結果を表す列挙。surefire のレポートから求め、仕様書の「結果」列に出す。
 *
 * @author Copel Co., Ltd.
 */
public enum TestStatus {

  /** 成功。 */
  PASSED("成功"),
  /** 失敗またはエラー。 */
  FAILED("失敗"),
  /** スキップ。 */
  SKIPPED("スキップ"),
  /** surefire の結果に見つからない（未実行）。 */
  UNKNOWN("未実行");

  /** 仕様書に出す日本語の表示名。 */
  private final String label;

  /**
   * 結果を生成する。
   *
   * @param label 表示名
   */
  TestStatus(String label) {
    this.label = label;
  }

  /**
   * 仕様書に出す日本語の表示名を返す。
   *
   * @return 表示名
   */
  public String label() {
    return label;
  }
}
