package copel.sesproductpackage.tools.utspec;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.comments.JavadocComment;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * テストクラス1つの構造を検査する。
 *
 * <p>検査の対象は次のとおり。
 *
 * <ul>
 *   <li>1ファイル1クラスで、名前がファイル名と同じく本体名 + Test であること（UT-03）
 *   <li>テストクラスの JavaDoc に対象クラスへの {@code {@link}} があること（UT-04）
 *   <li>第1階層が本体の対象メソッド・コンストラクタの名前であること（UT-05）
 *   <li>第2階層が規定の種類で、規定の順番に並んでいること（UT-06）
 *   <li>テストメソッドが第2階層の @Nested クラスにだけあること（UT-07）
 * </ul>
 *
 * <p>テストメソッド1つの検査は {@link TestMethodAnalyzer} に任せる。
 *
 * @author Copel Co., Ltd.
 */
public class TestClassAnalyzer {

  /** テストクラス名の接尾辞。本体名 + Test の形式にする。 */
  private static final String TEST_SUFFIX = "Test";

  /** 表示名を指定するアノテーションの単純名（UT-17 の対象）。 */
  private static final String DISPLAY_NAME = "DisplayName";

  /** 階層を表すアノテーションの単純名。 */
  private static final String NESTED = "Nested";

  /** 対象クラスへの {@code {@link}} を探す正規表現。 */
  private static final Pattern LINK_PATTERN = Pattern.compile("\\{@link\\s+([\\w.$]+)");

  /** 語彙と判定の定義。 */
  private final UtSpecRules rules;

  /** アノテーションを探す部品。 */
  private final JavaSourceReader reader;

  /** テストメソッド1つを検査する部品。 */
  private final TestMethodAnalyzer methodAnalyzer;

  /** 既定の部品で解析器を生成する。 */
  public TestClassAnalyzer() {
    this.rules = new UtSpecRules();
    this.reader = new JavaSourceReader();
    this.methodAnalyzer = new TestMethodAnalyzer();
  }

  /**
   * テストクラスの Compilation Unit を検査する。
   *
   * @param unit 解析済みのテストソース
   * @param relativePath リポジトリ直下からのテストソースの相対パス
   * @param bodies 完全修飾名から本体クラスへの対応
   * @return テストクラスの検査結果
   */
  public TestClassReport analyze(
      CompilationUnit unit, String relativePath, Map<String, BodyClass> bodies) {
    List<Violation> violations = new ArrayList<>();
    List<TestCase> cases = new ArrayList<>();
    String packageName =
        unit.getPackageDeclaration().map(declaration -> declaration.getNameAsString()).orElse("");
    List<TypeDeclaration<?>> types = unit.getTypes();
    if (types.size() != 1 || !types.get(0).isClassOrInterfaceDeclaration()) {
      violations.add(new Violation(relativePath, 1, UtRule.UT03, "テストファイルには1つのクラスだけを書く"));
      return new TestClassReport("", null, relativePath, violations, cases);
    }
    ClassOrInterfaceDeclaration top = types.get(0).asClassOrInterfaceDeclaration();
    String name = top.getNameAsString();
    String fqn = qualify(packageName, name);
    String fileBase = fileBaseName(relativePath);
    if (!name.equals(fileBase) || !name.endsWith(TEST_SUFFIX)) {
      violations.add(
          new Violation(
              relativePath, lineOf(top), UtRule.UT03, "テストクラス名はファイル名と同じにし、本体名 + Test にする"));
    }
    String bodySimpleName =
        name.endsWith(TEST_SUFFIX) ? name.substring(0, name.length() - TEST_SUFFIX.length()) : null;
    String bodyFqn = bodySimpleName == null ? null : qualify(packageName, bodySimpleName);
    BodyClass body = bodyFqn == null ? null : bodies.get(bodyFqn);

    checkDisplayName(top, relativePath, violations);
    checkClassLink(top, unit, relativePath, packageName, bodySimpleName, violations);
    for (BodyDeclaration<?> member : top.getMembers()) {
      if (isTestMethod(member)) {
        violations.add(
            new Violation(
                relativePath, lineOf(member), UtRule.UT07, "テストメソッドは第2階層の @Nested クラスに置く"));
      } else if (member.isClassOrInterfaceDeclaration()) {
        ClassOrInterfaceDeclaration child = member.asClassOrInterfaceDeclaration();
        if (isNested(child)) {
          checkLevel1(child, body, fqn, relativePath, violations, cases);
        } else {
          checkForbiddenNesting(child, relativePath, violations);
        }
      }
    }
    return new TestClassReport(fqn, bodyFqn, relativePath, violations, cases);
  }

  /**
   * テストクラスの JavaDoc に、対象クラスへの参照が解決できる形で書かれているかを検査する（UT-04）。
   *
   * @param top テストクラスの宣言
   * @param unit テストソースの Compilation Unit（import の確認に使う）
   * @param relativePath テストソースの相対パス
   * @param packageName テストクラスのパッケージ名
   * @param bodySimpleName 対象クラスの単純名。テストクラス名が本体名 + Test でない場合は null（検査しない）
   * @param violations 違反の追加先
   */
  public void checkClassLink(
      ClassOrInterfaceDeclaration top,
      CompilationUnit unit,
      String relativePath,
      String packageName,
      String bodySimpleName,
      List<Violation> violations) {
    if (bodySimpleName == null) {
      return;
    }
    Optional<JavadocComment> comment = top.getJavadocComment();
    if (comment.isEmpty()
        || !resolvesToBody(comment.get().parse().toText(), unit, packageName, bodySimpleName)) {
      violations.add(
          new Violation(
              relativePath,
              lineOf(top),
              UtRule.UT04,
              "テストクラスの JavaDoc に対象クラス {@link " + bodySimpleName + "} を解決できる形で書いていない"));
    }
  }

  /**
   * 宣言に UT-17 の @DisplayName が付いていれば違反を登録する。クラスの表示名もメソッド名で書かせるため。
   *
   * @param decl テストクラスまたは階層のクラス宣言
   * @param relativePath テストソースの相対パス
   * @param violations 違反の追加先
   */
  public void checkDisplayName(
      ClassOrInterfaceDeclaration decl, String relativePath, List<Violation> violations) {
    if (reader.findAnnotation(decl, DISPLAY_NAME).isPresent()) {
      violations.add(
          new Violation(
              relativePath, lineOf(decl), UtRule.UT17, "@DisplayName は使わない（テストのクラス名とメソッド名で示す）"));
    }
  }

  /**
   * 第1階層（対象メソッド）を検査し、その中の第2階層（種類）を検査する。
   *
   * @param level1 第1階層のクラス宣言
   * @param body 対象の本体クラス。無い場合は null
   * @param classFqn テストクラスの完全修飾名
   * @param relativePath テストソースの相対パス
   * @param violations 違反の追加先
   * @param cases テストケースの追加先
   */
  public void checkLevel1(
      ClassOrInterfaceDeclaration level1,
      BodyClass body,
      String classFqn,
      String relativePath,
      List<Violation> violations,
      List<TestCase> cases) {
    checkDisplayName(level1, relativePath, violations);
    String level1Name = level1.getNameAsString();
    if (body == null || !body.expectedNames().contains(level1Name)) {
      violations.add(
          new Violation(
              relativePath,
              lineOf(level1),
              UtRule.UT05,
              "第1階層「" + level1Name + "」は対象クラスの対象メソッド・コンストラクタに存在しない"));
    }
    int lastKindIndex = -1;
    for (BodyDeclaration<?> member : level1.getMembers()) {
      if (isTestMethod(member)) {
        violations.add(
            new Violation(
                relativePath,
                lineOf(member),
                UtRule.UT07,
                "第1階層にテストメソッドを置けない（第2階層の @Nested クラスに置く）"));
      } else if (member.isClassOrInterfaceDeclaration()) {
        ClassOrInterfaceDeclaration kind = member.asClassOrInterfaceDeclaration();
        if (isNested(kind)) {
          lastKindIndex =
              checkLevel2(
                  kind, level1Name, classFqn, relativePath, lastKindIndex, violations, cases);
        } else {
          checkForbiddenNesting(kind, relativePath, violations);
        }
      }
    }
  }

  /**
   * 第2階層（種類）を検査し、その中のテストメソッドを解析する。
   *
   * @param kind 第2階層のクラス宣言
   * @param level1Name 第1階層の名前
   * @param classFqn テストクラスの完全修飾名
   * @param relativePath テストソースの相対パス
   * @param lastKindIndex 直前の兄弟の種類の順番。最初なら -1
   * @param violations 違反の追加先
   * @param cases テストケースの追加先
   * @return この種類の順番（規定外の名前の場合は直前の値のまま）
   */
  public int checkLevel2(
      ClassOrInterfaceDeclaration kind,
      String level1Name,
      String classFqn,
      String relativePath,
      int lastKindIndex,
      List<Violation> violations,
      List<TestCase> cases) {
    checkDisplayName(kind, relativePath, violations);
    String kindName = kind.getNameAsString();
    int index = rules.kindIndex(kindName);
    int nextIndex = lastKindIndex;
    if (index < 0) {
      violations.add(
          new Violation(
              relativePath,
              lineOf(kind),
              UtRule.UT06,
              "第2階層「" + kindName + "」は正常系、異常系、境界値、認可、テナント隔離のいずれかにする"));
    } else if (index <= lastKindIndex) {
      violations.add(
          new Violation(
              relativePath, lineOf(kind), UtRule.UT06, "第2階層は正常系、異常系、境界値、認可、テナント隔離の順に書く"));
    } else {
      nextIndex = index;
    }
    for (BodyDeclaration<?> member : kind.getMembers()) {
      if (isTestMethod(member)) {
        MethodDeclaration method = member.asMethodDeclaration();
        if (hasUnsupportedTestAnnotation(method)) {
          violations.add(
              new Violation(
                  relativePath,
                  lineOf(method),
                  UtRule.UT07,
                  "未対応のテストアノテーションは使わない（@Test または @ParameterizedTest にする）"));
        } else {
          TestMethodAnalyzer.MethodAnalysis analysis =
              methodAnalyzer.analyze(method, relativePath, classFqn, level1Name, kindName);
          violations.addAll(analysis.violations());
          cases.addAll(analysis.cases());
        }
      } else if (member.isClassOrInterfaceDeclaration()) {
        checkForbiddenNesting(member.asClassOrInterfaceDeclaration(), relativePath, violations);
      }
    }
    return nextIndex;
  }

  /**
   * 階層の外に置かれた @Nested クラスや、@Nested の外にあるテストメソッドを検出する（UT-07）。
   *
   * <p>JUnit は @Nested の連なりの中だけを実行するため、外に置かれたテストは実行されないまま残る。 それを防ぐためにここで違反にする。
   *
   * @param decl 検査する宣言
   * @param relativePath テストソースの相対パス
   * @param violations 違反の追加先
   */
  public void checkForbiddenNesting(
      ClassOrInterfaceDeclaration decl, String relativePath, List<Violation> violations) {
    if (isNested(decl)) {
      violations.add(
          new Violation(relativePath, lineOf(decl), UtRule.UT07, "@Nested は第1階層と第2階層の宣言にだけ付ける"));
    }
    for (BodyDeclaration<?> member : decl.getMembers()) {
      if (isTestMethod(member)) {
        violations.add(
            new Violation(
                relativePath, lineOf(member), UtRule.UT07, "テストメソッドは第2階層の @Nested クラスに置く"));
      } else if (member.isClassOrInterfaceDeclaration()) {
        checkForbiddenNesting(member.asClassOrInterfaceDeclaration(), relativePath, violations);
      }
    }
  }

  /**
   * JavaDoc の参照が、テストクラスから対象クラスを解決できるかを判定する。
   *
   * <p>import の名前の末尾が対象の単純名と同じで、対象の完全修飾名と異なるものがあれば、同名の別クラスを import していると判断する（ワイルドカードや static の
   * import は末尾の名前で区別されるため、同じ扱いにする）。
   *
   * <p>完全修飾名の参照は、対象クラスの完全修飾名と一致する場合だけ認める。単純名の参照は、同じパッケージの 対象クラスを指す場合に認める。ただし、同じ単純名で別のクラスを import
   * している場合は、単純名の参照を 対象クラスに解決できないとして認めない。
   *
   * @param javadocText JavaDoc を文字列にしたもの
   * @param unit テストソースの Compilation Unit
   * @param packageName テストクラスのパッケージ名（対象クラスも同じパッケージにある）
   * @param simpleName 対象クラスの単純名
   * @return 対象クラスを解決できる参照があれば true
   */
  public boolean resolvesToBody(
      String javadocText, CompilationUnit unit, String packageName, String simpleName) {
    String bodyFqn = qualify(packageName, simpleName);
    boolean shadowed =
        unit.getImports().stream()
            .map(imported -> imported.getNameAsString())
            .anyMatch(
                name ->
                    name.substring(name.lastIndexOf('.') + 1).equals(simpleName)
                        && !name.equals(bodyFqn));
    Matcher matcher = LINK_PATTERN.matcher(javadocText);
    while (matcher.find()) {
      String target = matcher.group(1);
      if (target.equals(bodyFqn) || target.equals(simpleName) && !shadowed) {
        return true;
      }
    }
    return false;
  }

  /**
   * 宣言が @Nested を持つかを判定する。
   *
   * @param decl 宣言
   * @return @Nested を持てば true
   */
  public boolean isNested(ClassOrInterfaceDeclaration decl) {
    return reader.findAnnotation(decl, NESTED).isPresent();
  }

  /**
   * メンバーがテストメソッド（テストのアノテーションを持つメソッド）かを判定する。
   *
   * @param member 宣言のメンバー
   * @return テストメソッドなら true
   */
  public boolean isTestMethod(BodyDeclaration<?> member) {
    if (!member.isMethodDeclaration()) {
      return false;
    }
    return member.asMethodDeclaration().getAnnotations().stream()
        .anyMatch(annotation -> rules.isTestAnnotation(reader.simpleNameOf(annotation)));
  }

  /**
   * メソッドが仕様で使わないテストアノテーションを持つかを判定する。
   *
   * @param method メソッド
   * @return 未対応のテストアノテーションを持つなら true
   */
  public boolean hasUnsupportedTestAnnotation(MethodDeclaration method) {
    return method.getAnnotations().stream()
        .map(reader::simpleNameOf)
        .anyMatch(UtSpecRules.UNSUPPORTED_TEST_ANNOTATIONS::contains);
  }

  /**
   * パッケージ名とクラス名から完全修飾名を作る。
   *
   * @param packageName パッケージ名（デフォルトパッケージは空）
   * @param simpleName クラス名
   * @return 完全修飾名
   */
  public String qualify(String packageName, String simpleName) {
    return packageName.isEmpty() ? simpleName : packageName + "." + simpleName;
  }

  /**
   * 相対パスからファイル名（拡張子を除く）を取り出す。
   *
   * @param relativePath / 区切りの相対パス
   * @return 拡張子を除いたファイル名
   */
  public String fileBaseName(String relativePath) {
    String fileName = relativePath.substring(relativePath.lastIndexOf('/') + 1);
    return fileName.endsWith(".java")
        ? fileName.substring(0, fileName.length() - ".java".length())
        : fileName;
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
