package copel.sesproductpackage.tools.utspec;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MemberValuePair;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * パラメータ付きテストを、パラメータ1組ずつの行に展開する。
 *
 * <p>対応するソースは {@code @CsvSource} と {@code @ValueSource} の2つだけ。それ以外のソースは
 * 展開できないため、1行として「未展開のソース」と表示する。{@code @ParameterizedTest} の name 属性が あれば、{@code {index}} と {@code
 * {arguments}} を置き換えて表示名にする。
 *
 * @author Copel Co., Ltd.
 */
public class ParameterizedRowExpander {

  /** パラメータ付きテストのアノテーション名。 */
  private static final String PARAMETERIZED = "ParameterizedTest";

  /** 展開に対応するソースのアノテーション名。 */
  private static final List<String> SUPPORTED_SOURCES = List.of("CsvSource", "ValueSource");

  /** 値を書く属性の名前（CsvSource の value、ValueSource の型ごとの属性の共通の呼び方）。 */
  private static final String VALUE_ATTRIBUTE = "value";

  /** ソースの種類ごとに、値として読む属性の名前。他の属性（delimiter など）は値として扱わない。 */
  private static final Map<String, List<String>> VALUE_ATTRIBUTES =
      Map.of(
          "CsvSource",
          List.of(VALUE_ATTRIBUTE),
          "ValueSource",
          List.of(
              "strings",
              "ints",
              "longs",
              "doubles",
              "floats",
              "shorts",
              "bytes",
              "chars",
              "booleans",
              "classes"));

  /** 表示名の属性名。 */
  private static final String NAME_ATTRIBUTE = "name";

  /** 項目の表示名に使う、パラメータの組の番号の置き換え文字列。 */
  private static final String INDEX_PLACEHOLDER = "{index}";

  /** 項目の表示名に使う、パラメータの値の置き換え文字列。 */
  private static final String ARGUMENTS_PLACEHOLDER = "{arguments}";

  /** アノテーションを探す部品。 */
  private final JavaSourceReader reader;

  /** 既定の読み込み器で展開器を生成する。 */
  public ParameterizedRowExpander() {
    this(new JavaSourceReader());
  }

  /**
   * 読み込み器を指定して展開器を生成する。
   *
   * @param reader アノテーションを探す部品
   */
  public ParameterizedRowExpander(JavaSourceReader reader) {
    this.reader = reader;
  }

  /**
   * テストメソッドの行の表示名の一覧を返す。通常のテストは空文字1つの一覧になる。
   *
   * @param method テストメソッド
   * @return 行の表示名の一覧。パラメータ付きでない場合は {@code [""]}
   */
  public List<String> labels(MethodDeclaration method) {
    Optional<AnnotationExpr> parameterized = reader.findAnnotation(method, PARAMETERIZED);
    if (parameterized.isEmpty()) {
      return List.of("");
    }
    String pattern = nameAttributeOf(parameterized.get());
    List<String> values = new ArrayList<>();
    for (AnnotationExpr annotation : method.getAnnotations()) {
      String sourceName = reader.simpleNameOf(annotation);
      if (sourceName.equals(PARAMETERIZED)) {
        continue;
      }
      if (!SUPPORTED_SOURCES.contains(sourceName)) {
        return List.of("(未展開のソース: " + sourceName + ")");
      }
      values.addAll(valuesOf(annotation));
    }
    if (values.isEmpty()) {
      return List.of("(未展開のソース)");
    }
    List<String> labels = new ArrayList<>();
    for (int index = 1; index <= values.size(); index++) {
      labels.add(label(pattern, index, values.get(index - 1)));
    }
    return labels;
  }

  /**
   * ソースのアノテーションに書かれた値の一覧（1組ずつの文字列）を返す。
   *
   * @param annotation {@code @CsvSource} または {@code @ValueSource}
   * @return 値の文字列の一覧
   */
  public List<String> valuesOf(AnnotationExpr annotation) {
    Optional<Expression> expression = expressionOf(annotation);
    if (expression.isEmpty()) {
      return List.of();
    }
    Expression value = expression.get();
    List<Expression> elements =
        value.isArrayInitializerExpr()
            ? new ArrayList<>(value.asArrayInitializerExpr().getValues())
            : List.of(value);
    List<String> texts = new ArrayList<>();
    for (Expression element : elements) {
      texts.add(textOf(element));
    }
    return texts;
  }

  /**
   * ソースのアノテーションから、値を書いた式を属性名で選んで取り出す。
   *
   * <p>値として読むのは、{@code @CsvSource} の value 属性と、{@code @ValueSource} の型ごとの属性（strings、ints など）だけ。
   * delimiter などの他の属性は値として扱わない。単一値の書き方は、{@code @CsvSource} の value として扱う。
   *
   * @param annotation {@code @CsvSource} または {@code @ValueSource}
   * @return 値の式。該当する属性が無ければ空
   */
  public Optional<Expression> expressionOf(AnnotationExpr annotation) {
    List<String> attributes =
        VALUE_ATTRIBUTES.getOrDefault(reader.simpleNameOf(annotation), List.of());
    if (annotation.isSingleMemberAnnotationExpr()) {
      return attributes.contains(VALUE_ATTRIBUTE)
          ? Optional.of(annotation.asSingleMemberAnnotationExpr().getMemberValue())
          : Optional.empty();
    }
    if (annotation.isNormalAnnotationExpr()) {
      return annotation.asNormalAnnotationExpr().getPairs().stream()
          .filter(pair -> attributes.contains(pair.getNameAsString()))
          .findFirst()
          .map(MemberValuePair::getValue);
    }
    return Optional.empty();
  }

  /**
   * 表示名の書式に従って、1組の表示名を作る。書式が空なら「[番号] 値」の形にする。
   *
   * @param pattern name 属性の書式（空なら既定の形式）
   * @param index 1から始まる組の番号
   * @param value パラメータの値の文字列
   * @return 表示名
   */
  public String label(String pattern, int index, String value) {
    if (pattern.isEmpty()) {
      return "[" + index + "] " + value;
    }
    return pattern
        .replace(INDEX_PLACEHOLDER, String.valueOf(index))
        .replace(ARGUMENTS_PLACEHOLDER, value);
  }

  /**
   * パラメータ付きテストの name 属性の書式を返す。
   *
   * @param annotation {@code @ParameterizedTest} のアノテーション
   * @return name 属性の文字列。無ければ空文字
   */
  public String nameAttributeOf(AnnotationExpr annotation) {
    if (!annotation.isNormalAnnotationExpr()) {
      return "";
    }
    return annotation.asNormalAnnotationExpr().getPairs().stream()
        .filter(pair -> pair.getNameAsString().equals(NAME_ATTRIBUTE))
        .findFirst()
        .filter(pair -> pair.getValue().isStringLiteralExpr())
        .map(pair -> pair.getValue().asStringLiteralExpr().asString())
        .orElse("");
  }

  /**
   * 式を表示用の文字列にする。文字列リテラルは引用符を外し、それ以外はソースの書き方のままにする。
   *
   * @param expression 式
   * @return 表示用の文字列
   */
  public String textOf(Expression expression) {
    if (expression.isStringLiteralExpr()) {
      return expression.asStringLiteralExpr().asString();
    }
    return expression.toString();
  }
}
