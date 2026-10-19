package copel.sesproductpackage.tools.utspec;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * src/main/java を走査して、本体クラスの索引（{@link MainSourceIndex}）を作る。
 *
 * <p>各型の期待する第1階層の名前と、{@code @CoverageExcludedGenerated} による除外の一覧を集める。 除外には理由（reason
 * 属性の空でない文字列）が必須で、欠けていれば UT-18 の違反にする。 構文エラーのファイルは UT-00 の違反にする。除外の判定は単純名で行うため、アノテーションの定義が Core
 * 本体にあるかどうかに依存しない。
 *
 * @author Copel Co., Ltd.
 */
public class MainSourceScanner {

  /** 除外を示すアノテーションの単純名。 */
  private static final String EXCLUSION_ANNOTATION = "CoverageExcludedGenerated";

  /** 除外の理由を示す属性名。 */
  private static final String REASON_ATTRIBUTE = "reason";

  /** Java ソースを読み込む部品。 */
  private final JavaSourceReader reader;

  /** 期待する第1階層の名前を求める部品。 */
  private final ExpectedNameCalculator nameCalculator;

  /** 既定の部品で走査器を生成する。 */
  public MainSourceScanner() {
    this(new JavaSourceReader(), new ExpectedNameCalculator());
  }

  /**
   * 部品を指定して走査器を生成する。
   *
   * @param reader Java ソースを読み込む部品
   * @param nameCalculator 期待する第1階層の名前を求める部品
   */
  public MainSourceScanner(JavaSourceReader reader, ExpectedNameCalculator nameCalculator) {
    this.reader = reader;
    this.nameCalculator = nameCalculator;
  }

  /**
   * 本体ソースのディレクトリを走査して索引を作る。構文エラーのファイルは違反にして対象から外す。
   *
   * @param basedir リポジトリ直下（相対パスの基準）
   * @param mainSourceDir src/main/java のディレクトリ
   * @return 本体クラスの索引
   * @throws IOException ファイルを探索できない場合
   */
  public MainSourceIndex scan(Path basedir, Path mainSourceDir) throws IOException {
    Map<String, BodyClass> bodies = new TreeMap<>();
    List<MainSourceIndex.ExclusionEntry> exclusions = new ArrayList<>();
    List<Violation> violations = new ArrayList<>();
    for (Path file : reader.javaFiles(mainSourceDir)) {
      String relativePath = reader.relativePath(basedir, file);
      Optional<CompilationUnit> unit = reader.parse(file);
      if (unit.isEmpty()) {
        violations.add(new Violation(relativePath, 0, UtRule.UT00, "本体ソースを構文解析できない"));
        continue;
      }
      String packageName =
          unit.get()
              .getPackageDeclaration()
              .map(declaration -> declaration.getNameAsString())
              .orElse("");
      for (TypeDeclaration<?> type : unit.get().getTypes()) {
        Optional<AnnotationExpr> annotation = reader.findAnnotation(type, EXCLUSION_ANNOTATION);
        BodyClass body =
            new BodyClass(
                packageName,
                type.getNameAsString(),
                relativePath,
                lineOf(type),
                nameCalculator.calculate(type),
                annotation.isPresent());
        bodies.put(body.fqn(), body);
        if (annotation.isPresent()) {
          checkReason(annotation.get(), relativePath, violations);
          exclusions.add(
              new MainSourceIndex.ExclusionEntry(body.fqn(), reasonOf(annotation.get())));
        }
        addMemberExclusions(body.fqn(), type, exclusions, relativePath, violations);
      }
    }
    return new MainSourceIndex(bodies, exclusions, violations);
  }

  /**
   * 型のメンバー（メソッドとコンストラクタ）に付いた除外を、除外の一覧に追加する。理由の欠落は違反にする。
   *
   * @param typeName 型の完全修飾名
   * @param type 型宣言
   * @param exclusions 追加先の除外の一覧
   * @param relativePath 型のソースの相対パス（違反の報告に使う）
   * @param violations 追加先の違反の一覧
   */
  public void addMemberExclusions(
      String typeName,
      TypeDeclaration<?> type,
      List<MainSourceIndex.ExclusionEntry> exclusions,
      String relativePath,
      List<Violation> violations) {
    for (BodyDeclaration<?> member : type.getMembers()) {
      if (!member.isMethodDeclaration() && !member.isConstructorDeclaration()) {
        continue;
      }
      String memberName =
          member.isMethodDeclaration()
              ? member.asMethodDeclaration().getNameAsString()
              : ExpectedNameCalculator.CONSTRUCTOR_NAME;
      Optional<AnnotationExpr> annotation = reader.findAnnotation(member, EXCLUSION_ANNOTATION);
      if (annotation.isPresent()) {
        checkReason(annotation.get(), relativePath, violations);
        exclusions.add(
            new MainSourceIndex.ExclusionEntry(
                typeName + "#" + memberName, reasonOf(annotation.get())));
      }
    }
  }

  /**
   * 宣言に {@code @CoverageExcludedGenerated} が付いていれば、その理由を返す。
   *
   * @param node 型やメンバーの宣言
   * @return 除外されていれば理由（reason 属性が無い場合は空文字）。除外されていなければ空
   */
  public Optional<String> exclusionReason(BodyDeclaration<?> node) {
    return reader.findAnnotation(node, EXCLUSION_ANNOTATION).map(this::reasonOf);
  }

  /**
   * 除外の理由が空でない文字列で書かれているかを検査し、書かれていなければ UT-18 の違反を登録する。
   *
   * <p>reason 属性が無い書き方、空の文字列、空白だけの文字列、単一値の書き方は、いずれも理由が無いとみなす。
   *
   * @param annotation {@code @CoverageExcludedGenerated} のアノテーション
   * @param relativePath 宣言のあるファイルの相対パス
   * @param violations 違反の追加先
   */
  public void checkReason(
      AnnotationExpr annotation, String relativePath, List<Violation> violations) {
    if (reasonOf(annotation).isBlank()) {
      violations.add(
          new Violation(
              relativePath,
              lineOf(annotation),
              UtRule.UT18,
              "除外には reason = \"除外の理由\" を空でない文字列で書く"));
    }
  }

  /**
   * 除外アノテーションの reason 属性の値を返す。通常の書き方で文字列として書かれた値だけを読む。
   *
   * @param annotation {@code @CoverageExcludedGenerated} のアノテーション
   * @return reason の文字列。属性が無い、または文字列でない場合は空文字
   */
  public String reasonOf(AnnotationExpr annotation) {
    if (!(annotation instanceof NormalAnnotationExpr normal)) {
      return "";
    }
    return normal.getPairs().stream()
        .filter(pair -> pair.getNameAsString().equals(REASON_ATTRIBUTE))
        .findFirst()
        .filter(pair -> pair.getValue().isStringLiteralExpr())
        .map(pair -> pair.getValue().asStringLiteralExpr().asString())
        .orElse("");
  }

  /**
   * ノードの開始行を返す。位置が不明な場合は 0 を返す。
   *
   * @param node 対象のノード
   * @return 行番号
   */
  public int lineOf(Node node) {
    return node.getBegin().map(position -> position.line).orElse(0);
  }
}
