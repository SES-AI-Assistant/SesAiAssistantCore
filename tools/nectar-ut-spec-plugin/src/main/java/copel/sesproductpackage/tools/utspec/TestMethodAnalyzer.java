package copel.sesproductpackage.tools.utspec;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.comments.JavadocComment;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * テストメソッド1つを検査し、仕様書に出すテストケースを作る。
 *
 * <p>テスト名、JavaDoc の書式、禁止語・括弧・装飾記号、@DisplayName の使用を検査する。 パラメータ付きテストは、パラメータ1組ごとにテストケースを作る。
 *
 * @author Copel Co., Ltd.
 */
public class TestMethodAnalyzer {

  /** テスト名の形式と語尾を検査する部品。 */
  private final TestNameChecker nameChecker;

  /** 禁止語・括弧・装飾記号を検査する部品。 */
  private final TextRuleChecker textChecker;

  /** JavaDoc の書式を検査する部品。 */
  private final JavadocChecker javadocChecker;

  /** パラメータ付きテストを展開する部品。 */
  private final ParameterizedRowExpander rowExpander;

  /** アノテーションを探す部品。 */
  private final JavaSourceReader reader;

  /** 既定の部品で解析器を生成する。 */
  public TestMethodAnalyzer() {
    UtSpecRules rules = new UtSpecRules();
    this.nameChecker = new TestNameChecker(rules);
    this.textChecker = new TextRuleChecker(rules);
    this.javadocChecker = new JavadocChecker(rules);
    this.rowExpander = new ParameterizedRowExpander();
    this.reader = new JavaSourceReader();
  }

  /**
   * テストメソッドを検査し、違反とテストケースを返す。
   *
   * @param method テストメソッド
   * @param file テストソースの相対パス
   * @param classFqn テストクラスの完全修飾名
   * @param level1 第1階層の名前
   * @param kind 第2階層の名前（種類）
   * @return 検査結果
   */
  public MethodAnalysis analyze(
      MethodDeclaration method, String file, String classFqn, String level1, String kind) {
    String methodName = method.getNameAsString();
    int line = method.getBegin().map(position -> position.line).orElse(0);
    List<Violation> violations = new ArrayList<>();
    violations.addAll(nameChecker.check(methodName, file, line));
    violations.addAll(textChecker.check(methodName, file, line));
    if (reader.findAnnotation(method, "DisplayName").isPresent()) {
      violations.add(
          new Violation(file, line, UtRule.UT17, "@DisplayName は使わない（テストのクラス名とメソッド名で示す）"));
    }
    JavadocSpec spec = javadocOf(method, file, line, violations);
    List<TestCase> cases = new ArrayList<>();
    List<String> labels = rowExpander.labels(method);
    for (int index = 0; index < labels.size(); index++) {
      cases.add(
          new TestCase(
              classFqn,
              level1,
              kind,
              methodName,
              labels.get(index),
              index,
              file,
              line,
              spec.what(),
              spec.why(),
              spec.tag("given"),
              spec.tag("when"),
              spec.tag("then"),
              TestStatus.UNKNOWN));
    }
    return new MethodAnalysis(violations, cases);
  }

  /**
   * JavaDoc を分解して検査する。JavaDoc が無い場合は違反を1件出し、空の JavaDoc を返す。
   *
   * @param method テストメソッド
   * @param file テストソースの相対パス
   * @param line 行番号
   * @param violations 違反の追加先
   * @return 分解した JavaDoc（無い場合は空）
   */
  public JavadocSpec javadocOf(
      MethodDeclaration method, String file, int line, List<Violation> violations) {
    Optional<JavadocComment> comment = method.getJavadocComment();
    if (comment.isEmpty()) {
      violations.add(new Violation(file, line, UtRule.UT10, "テストメソッドに JavaDoc がない"));
      return JavadocSpec.empty();
    }
    JavadocSpec spec = JavadocSpec.from(comment.get().parse());
    violations.addAll(javadocChecker.check(spec, file, line));
    violations.addAll(textChecker.check(spec.allText(), file, line));
    return spec;
  }

  /**
   * テストメソッドの検査結果。
   *
   * @param violations 検出した違反の一覧
   * @param cases 仕様書に出すテストケースの一覧（パラメータの組の数だけある）
   */
  public record MethodAnalysis(List<Violation> violations, List<TestCase> cases) {
    /** 違反の一覧とテストケースの一覧を防御的にコピーして保持する。検査の結果を後から変えないようにするため。 */
    public MethodAnalysis {
      violations = List.copyOf(violations);
      cases = List.copyOf(cases);
    }
  }
}
