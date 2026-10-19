package copel.sesproductpackage.tools.utspec;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithAnnotations;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * JavaParser を使って Java ソースを読み込む。
 *
 * <p>解析の設定（Java 17 の文法）を1か所にまとめ、本体ソースとテストソースで同じ読み方にする。 ファイルの列挙と、アノテーションの名前による検索もここで行う。
 *
 * @author Copel Co., Ltd.
 */
public class JavaSourceReader {

  /** 解析に使う JavaParser。CI と同じ JDK 17 の文法で解析する。 */
  private final JavaParser parser;

  /** Java 17 の文法で解析する読み込み器を生成する。 */
  public JavaSourceReader() {
    ParserConfiguration configuration =
        new ParserConfiguration().setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_17);
    this.parser = new JavaParser(configuration);
  }

  /**
   * Java ファイルを解析する。構文エラーがあれば空を返す。
   *
   * @param file 解析する Java ファイル
   * @return 解析に成功した場合は Compilation Unit、失敗した場合は空
   * @throws UncheckedIOException ファイルを読み込めない場合
   */
  public Optional<CompilationUnit> parse(Path file) {
    try {
      ParseResult<CompilationUnit> result = parser.parse(file);
      return result.isSuccessful() ? result.getResult() : Optional.empty();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /**
   * ディレクトリ配下の Java ファイルを、パスの順に列挙する。ディレクトリが無い場合は空のリストを返す。
   *
   * @param dir 探索するディレクトリ
   * @return Java ファイルのパスの一覧
   * @throws IOException ディレクトリを探索できない場合
   */
  public List<Path> javaFiles(Path dir) throws IOException {
    if (!Files.isDirectory(dir)) {
      return List.of();
    }
    try (Stream<Path> paths = Files.walk(dir)) {
      return paths
          .filter(Files::isRegularFile)
          .filter(path -> path.toString().endsWith(".java"))
          .sorted()
          .toList();
    }
  }

  /**
   * 基準ディレクトリからの相対パスを、区切り文字を / に揃えた文字列で返す。
   *
   * <p>違反の報告と GitHub の行リンクで使うため、OS によらず / 区切りにする。
   *
   * @param basedir 基準となるディレクトリ（リポジトリ直下）
   * @param file 対象のファイル
   * @return / 区切りの相対パス
   */
  public String relativePath(Path basedir, Path file) {
    return basedir.relativize(file).toString().replace('\\', '/');
  }

  /**
   * 指定した単純名のアノテーションを、宣言から探す。
   *
   * <p>完全修飾名（例: org.junit.jupiter.api.Nested）で書かれていても、末尾の単純名で一致させる。
   *
   * @param node アノテーションを持てるノード（型・メソッド・コンストラクタなど）
   * @param simpleName アノテーションの単純名（例: Nested）
   * @return 見つかったアノテーション。無ければ空
   */
  public Optional<AnnotationExpr> findAnnotation(NodeWithAnnotations<?> node, String simpleName) {
    return node.getAnnotations().stream()
        .filter(annotation -> simpleNameOf(annotation).equals(simpleName))
        .findFirst();
  }

  /**
   * アノテーションの単純名（完全修飾名の末尾）を返す。
   *
   * @param annotation 対象のアノテーション
   * @return 単純名
   */
  public String simpleNameOf(AnnotationExpr annotation) {
    String name = annotation.getNameAsString();
    return name.substring(name.lastIndexOf('.') + 1);
  }
}
