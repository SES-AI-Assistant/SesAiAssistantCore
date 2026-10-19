package copel.sesproductpackage.tools.utspec;

import java.util.ArrayList;
import java.util.List;

/**
 * テスト名と JavaDoc のテキストに対する文字の規則（禁止語・括弧・装飾記号）を検査する。
 *
 * <p>括弧の検査では {@code {@link}} と {@code {@code}} の中を対象から外す。 対象の語彙は {@link UtSpecRules} に定義されたものを使う。
 *
 * @author Copel Co., Ltd.
 */
public class TextRuleChecker {

  /** 語彙と判定の定義。 */
  private final UtSpecRules rules;

  /** 既定のルールで検査器を生成する。 */
  public TextRuleChecker() {
    this(new UtSpecRules());
  }

  /**
   * ルールを指定して検査器を生成する。
   *
   * @param rules 語彙と判定の定義
   */
  public TextRuleChecker(UtSpecRules rules) {
    this.rules = rules;
  }

  /**
   * テキストを検査し、禁止語（UT-14）、括弧（UT-15）、装飾記号と絵文字（UT-16）の違反を返す。
   *
   * @param text 検査するテキスト
   * @param file 違反を報告するファイル
   * @param line 違反を報告する行番号
   * @return 検出した違反の一覧。違反がなければ空のリスト
   */
  public List<Violation> check(String text, String file, int line) {
    String plain = rules.removeInlineTags(text);
    List<Violation> violations = new ArrayList<>();
    for (String word : rules.findForbiddenWords(plain)) {
      violations.add(new Violation(file, line, UtRule.UT14, "禁止語「" + word + "」を使っている"));
    }
    for (String bracket : rules.findBrackets(plain)) {
      violations.add(new Violation(file, line, UtRule.UT15, "括弧「" + bracket + "」を使っている"));
    }
    for (String symbol : rules.findDecorations(plain)) {
      violations.add(new Violation(file, line, UtRule.UT16, "絵文字または装飾記号「" + symbol + "」を使っている"));
    }
    return violations;
  }
}
