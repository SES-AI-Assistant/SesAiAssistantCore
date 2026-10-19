package copel.sesproductpackage.tools.utspec;

import java.util.ArrayList;
import java.util.List;

/**
 * テストメソッドの JavaDoc の書式（文の数・文末・枕詞・タグ・文字数）を検査する。
 *
 * <p>検査の対象は次のとおり。
 *
 * <ul>
 *   <li>本文は2文で、1文目は「ことを確かめる。」で終わり、各文は「。」で終わる（UT-10）
 *   <li>本文2文目は枕詞で始まらない（UT-11）
 *   <li>@given / @when / @then はそれぞれ1つだけあり、空でなく、1文である（UT-12）
 *   <li>1文は文字数の上限以内である（UT-13）
 * </ul>
 *
 * @author Copel Co., Ltd.
 */
public class JavadocChecker {

  /** 検査する3つのタグ名。 */
  private static final List<String> REQUIRED_TAGS = List.of("given", "when", "then");

  /** 語彙と判定の定義。 */
  private final UtSpecRules rules;

  /** 既定のルールで検査器を生成する。 */
  public JavadocChecker() {
    this(new UtSpecRules());
  }

  /**
   * ルールを指定して検査器を生成する。
   *
   * @param rules 語彙と判定の定義
   */
  public JavadocChecker(UtSpecRules rules) {
    this.rules = rules;
  }

  /**
   * JavaDoc の書式を検査する。
   *
   * @param spec 分解済みの JavaDoc
   * @param file 違反を報告するファイル
   * @param line 違反を報告する行番号
   * @return 検出した違反の一覧。違反がなければ空のリスト
   */
  public List<Violation> check(JavadocSpec spec, String file, int line) {
    List<Violation> violations = new ArrayList<>();
    checkBody(spec, file, line, violations);
    for (String name : REQUIRED_TAGS) {
      checkTag(spec, name, file, line, violations);
    }
    for (String sentence : spec.sentences()) {
      if (rules.isOverSentenceLimit(sentence)) {
        violations.add(
            new Violation(
                file,
                line,
                UtRule.UT13,
                "1文が" + UtSpecRules.MAX_SENTENCE_LENGTH + "文字を超えている: " + sentence));
      }
    }
    return violations;
  }

  /**
   * 本文の文の数・文末・2文目の枕詞を検査する。
   *
   * @param spec 分解済みの JavaDoc
   * @param file 違反を報告するファイル
   * @param line 違反を報告する行番号
   * @param violations 違反の追加先
   */
  public void checkBody(JavadocSpec spec, String file, int line, List<Violation> violations) {
    if (spec.sentences().size() != 2) {
      violations.add(
          new Violation(
              file, line, UtRule.UT10, "JavaDoc の本文は2文にする（現在 " + spec.sentences().size() + " 文）"));
    } else if (!spec.what().endsWith(UtSpecRules.REQUIRED_SENTENCE_END)) {
      violations.add(
          new Violation(
              file, line, UtRule.UT10, "本文1文目は「" + UtSpecRules.REQUIRED_SENTENCE_END + "」で終える"));
    }
    for (String sentence : spec.sentences()) {
      if (!rules.endsWithSentenceEnd(sentence)) {
        violations.add(new Violation(file, line, UtRule.UT10, "文末に「。」がない: " + sentence));
      }
    }
    if (spec.sentences().size() == 2 && rules.startsWithProhibitedPrefix(spec.why())) {
      violations.add(new Violation(file, line, UtRule.UT11, "本文2文目が枕詞で始まっている"));
    }
    if (spec.sentences().size() == 2 && rules.endsWithProhibitedReason(spec.why())) {
      violations.add(new Violation(file, line, UtRule.UT11, "本文2文目は理由を言い切りで書く（「ため。」で終えない）"));
    }
  }

  /**
   * タグが1つだけあり、空でなく、1文であるかを検査する。
   *
   * @param spec 分解済みの JavaDoc
   * @param name タグ名
   * @param file 違反を報告するファイル
   * @param line 違反を報告する行番号
   * @param violations 違反の追加先
   */
  public void checkTag(
      JavadocSpec spec, String name, String file, int line, List<Violation> violations) {
    String content = spec.tag(name);
    if (spec.tagCount(name) != 1) {
      violations.add(
          new Violation(
              file, line, UtRule.UT12, "@" + name + " は1つだけ書く（現在 " + spec.tagCount(name) + " 個）"));
    } else if (content.isEmpty()) {
      violations.add(new Violation(file, line, UtRule.UT12, "@" + name + " が空"));
    } else if (rules.isOverSentenceLimit(content)) {
      violations.add(
          new Violation(
              file,
              line,
              UtRule.UT13,
              "@" + name + " の内容が" + UtSpecRules.MAX_SENTENCE_LENGTH + "文字を超えている"));
    } else if (content.contains("。")) {
      violations.add(new Violation(file, line, UtRule.UT12, "@" + name + " は1文にする"));
    }
  }
}
