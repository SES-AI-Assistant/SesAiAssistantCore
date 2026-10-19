package copel.sesproductpackage.tools.utspec;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * surefire のレポートから読んだテストの実行結果を、クラス名ごとに保持する。
 *
 * <p>テストケースの実行結果は、クラス名（{@code 外側$第1階層$種類}）とメソッド名、パラメータの組の番号で探す。 パラメータ付きテストの名前は {@code
 * method(引数)[番号]} の形で出るため、番号は末尾の [n] から読む。
 *
 * @author Copel Co., Ltd.
 */
public class SurefireResults {

  /** パラメータ付きテストの組の番号（末尾の [n]）を読む正規表現。 */
  private static final Pattern ROW_PATTERN = Pattern.compile("\\[(\\d+)\\]$");

  /** クラス名から、そのクラスのテストケースの結果の一覧への対応。 */
  private final Map<String, List<Outcome>> outcomesByClass;

  /**
   * 実行結果の対応を指定して生成する。
   *
   * @param outcomesByClass クラス名から、そのクラスのテストケースの結果の一覧への対応
   */
  public SurefireResults(Map<String, List<Outcome>> outcomesByClass) {
    this.outcomesByClass = Map.copyOf(outcomesByClass);
  }

  /**
   * テストケース1件の実行結果を返す。見つからない場合は未実行を返す。
   *
   * @param className surefire のクラス名
   * @param methodName テストメソッドの名前
   * @param rowIndex パラメータ付きテストの0から始まる順番。通常のテストは 0
   * @return 実行結果
   */
  public TestStatus statusOf(String className, String methodName, int rowIndex) {
    for (Outcome outcome : outcomesByClass.getOrDefault(className, List.of())) {
      String name = outcome.name();
      boolean sameMethod = name.equals(methodName) || name.startsWith(methodName + "(");
      if (sameMethod && rowNumberOf(name) == rowIndex + 1) {
        return outcome.status();
      }
    }
    return TestStatus.UNKNOWN;
  }

  /**
   * テスト名の末尾の組の番号（[n]）を、1から始まる番号で返す。番号が無い場合は 1。
   *
   * @param testName surefire のテスト名
   * @return 1から始まる組の番号
   */
  public int rowNumberOf(String testName) {
    Matcher matcher = ROW_PATTERN.matcher(testName);
    return matcher.find() ? Integer.parseInt(matcher.group(1)) : 1;
  }

  /**
   * 指定した実行結果のテストケース数を返す。
   *
   * @param status 数える実行結果
   * @return テストケース数
   */
  public int count(TestStatus status) {
    int total = 0;
    for (List<Outcome> outcomes : outcomesByClass.values()) {
      for (Outcome outcome : outcomes) {
        if (outcome.status() == status) {
          total++;
        }
      }
    }
    return total;
  }

  /**
   * surefire の1件のテストケースの結果。
   *
   * @param name テストの名前（surefire の name 属性）
   * @param status 実行結果
   */
  public record Outcome(String name, TestStatus status) {}
}
