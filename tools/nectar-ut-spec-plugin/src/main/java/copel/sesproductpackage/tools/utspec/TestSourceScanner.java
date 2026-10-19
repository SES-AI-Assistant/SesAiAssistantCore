package copel.sesproductpackage.tools.utspec;

import com.github.javaparser.ast.CompilationUnit;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * src/test/java を走査し、テストクラスごとの検査結果を作る。
 *
 * <p>構文エラーのファイルは UT-00 の違反として報告し、他のファイルの検査は続ける。
 *
 * @author Copel Co., Ltd.
 */
public class TestSourceScanner {

  /** Java ソースを読み込む部品。 */
  private final JavaSourceReader reader;

  /** テストクラスの構造を検査する部品。 */
  private final TestClassAnalyzer analyzer;

  /** 既定の部品で走査器を生成する。 */
  public TestSourceScanner() {
    this(new JavaSourceReader(), new TestClassAnalyzer());
  }

  /**
   * 部品を指定して走査器を生成する。
   *
   * @param reader Java ソースを読み込む部品
   * @param analyzer テストクラスの構造を検査する部品
   */
  public TestSourceScanner(JavaSourceReader reader, TestClassAnalyzer analyzer) {
    this.reader = reader;
    this.analyzer = analyzer;
  }

  /**
   * テストソースのディレクトリを走査して、テストクラスごとの検査結果を返す。
   *
   * @param basedir リポジトリ直下（相対パスの基準）
   * @param testSourceDir src/test/java のディレクトリ
   * @param bodies 完全修飾名から本体クラスへの対応
   * @return テストクラスごとの検査結果
   * @throws IOException ファイルを探索できない場合
   */
  public List<TestClassReport> scan(Path basedir, Path testSourceDir, Map<String, BodyClass> bodies)
      throws IOException {
    List<TestClassReport> reports = new ArrayList<>();
    for (Path file : reader.javaFiles(testSourceDir)) {
      String relativePath = reader.relativePath(basedir, file);
      Optional<CompilationUnit> unit = reader.parse(file);
      if (unit.isEmpty()) {
        List<Violation> violations =
            List.of(new Violation(relativePath, 0, UtRule.UT00, "構文エラーのため解析できない"));
        reports.add(new TestClassReport("", null, relativePath, violations, List.of()));
      } else {
        reports.add(analyzer.analyze(unit.get(), relativePath, bodies));
      }
    }
    return reports;
  }
}
