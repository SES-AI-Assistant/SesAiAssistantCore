package copel.sesproductpackage.tools.utspec;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * テストメソッド名が {@code {条件}の場合_{期待結果}} の形になっているかを検査する。
 *
 * <p>区切りは最初の「場合_」とする。条件は「場合」で終わり、「の場合」「ない場合」のどちらも使える。期待結果の部分が許可された語尾に一致するかを、 {@link
 * UtSpecRules#isAllowedExpectedResult(String)} で判定する。
 *
 * @author Copel Co., Ltd.
 */
public class TestNameChecker {

  /** テスト名の形式。条件と期待結果を分ける。 */
  private static final Pattern NAME_PATTERN = Pattern.compile("^(.+?)場合_(.+)$");

  /** 語尾の判定に使う定義。 */
  private final UtSpecRules rules;

  /** 既定のルールで検査器を生成する。 */
  public TestNameChecker() {
    this(new UtSpecRules());
  }

  /**
   * ルールを指定して検査器を生成する。
   *
   * @param rules 語尾の判定に使う定義
   */
  public TestNameChecker(UtSpecRules rules) {
    this.rules = rules;
  }

  /**
   * テスト名の形式（UT-08）と期待結果の語尾（UT-09）を検査する。
   *
   * @param methodName テストメソッド名
   * @param file 違反を報告するファイル
   * @param line 違反を報告する行番号
   * @return 検出した違反の一覧。違反がなければ空のリスト
   */
  public List<Violation> check(String methodName, String file, int line) {
    List<Violation> violations = new ArrayList<>();
    Matcher matcher = NAME_PATTERN.matcher(methodName);
    if (!matcher.matches()) {
      violations.add(
          new Violation(
              file, line, UtRule.UT08, "テスト名「" + methodName + "」は {条件}の場合_{期待結果} の形にしていない"));
    } else if (!rules.isAllowedExpectedResult(matcher.group(2))) {
      violations.add(
          new Violation(file, line, UtRule.UT09, "テスト名「" + methodName + "」の期待結果の語尾が許可された一覧にない"));
    }
    return violations;
  }
}
