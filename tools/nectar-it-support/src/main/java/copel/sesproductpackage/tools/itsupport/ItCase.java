package copel.sesproductpackage.tools.itsupport;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javaparser.ParserConfiguration.LanguageLevel;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.javadoc.Javadoc;
import com.github.javaparser.javadoc.JavadocBlockTag;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * 結合テスト（IT）のソースから読み取った1つのテストケースです.
 *
 * <p>テストケース名と JavaDoc（本文、{@code @given} {@code @when} {@code @then}）を持ち、テスト項目書（Markdown）の1行と、
 * 実行結果のレポート（HTML）の1ケースを組み立てます。UT のテスト仕様書（nectar-ut-spec-plugin）と同じ書き方の JavaDoc を前提にします.
 *
 * @param file ソースファイルの、ソースのフォルダからの相対パス
 * @param className テストクラスのバイナリ名。@Nested のクラスは「パッケージ.外側$内側」の形
 * @param groups 外側から順に並べたクラス名。テスト項目書の見出しに使う
 * @param methodName テストメソッドの名前。テストケース名を兼ねる
 * @param description JavaDoc の本文。何を確かめるかと、その理由
 * @param given 前提
 * @param when 操作
 * @param then 期待結果
 * @author Copel Co., Ltd.
 */
public record ItCase(
    Path file,
    String className,
    List<String> groups,
    String methodName,
    String description,
    String given,
    String when,
    String then) {

  /** ログの JSON を読むための ObjectMapper です. */
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  /**
   * ソースのフォルダにある *IT.java をすべて読み、テストケースをファイル名の順、ファイルの中は記述の順に返します.
   *
   * <p>実行したケースではなくソースから読むため、一部のケースだけを実行してもテスト項目書が欠けません.
   *
   * @param sourceDir 結合テストのソースのフォルダ
   * @return テストケースのリスト
   */
  public static List<ItCase> readAll(final Path sourceDir) {
    StaticJavaParser.getParserConfiguration().setLanguageLevel(LanguageLevel.JAVA_17);
    List<ItCase> cases = new ArrayList<>();
    try (Stream<Path> files = Files.walk(sourceDir)) {
      for (Path file :
          files.filter(f -> f.getFileName().toString().endsWith("IT.java")).sorted().toList()) {
        CompilationUnit unit = StaticJavaParser.parse(file);
        String packagePrefix =
            unit.getPackageDeclaration().map(p -> p.getNameAsString() + ".").orElse("");
        for (MethodDeclaration method : unit.findAll(MethodDeclaration.class)) {
          if (!method.isAnnotationPresent(Test.class)) {
            continue;
          }
          List<String> groups = new ArrayList<>();
          for (Node node = method.getParentNode().orElse(null);
              node != null;
              node = node.getParentNode().orElse(null)) {
            if (node instanceof ClassOrInterfaceDeclaration declaration) {
              groups.add(0, declaration.getNameAsString());
            }
          }
          Javadoc javadoc = method.getJavadoc().orElse(null);
          Map<String, String> tags = new HashMap<>();
          if (javadoc != null) {
            for (JavadocBlockTag tag : javadoc.getBlockTags()) {
              tags.put(tag.getTagName(), oneLine(tag.getContent().toText()));
            }
          }
          cases.add(
              new ItCase(
                  sourceDir.relativize(file),
                  packagePrefix + String.join("$", groups),
                  List.copyOf(groups),
                  method.getNameAsString(),
                  javadoc == null ? "" : oneLine(javadoc.getDescription().toText()),
                  tags.getOrDefault("given", ""),
                  tags.getOrDefault("when", ""),
                  tags.getOrDefault("then", "")));
        }
      }
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    return cases;
  }

  /**
   * HTML の本文に入れられるよう、特別な意味を持つ文字を置き換えます.
   *
   * @param text 置き換える文字列
   * @return 置き換えた文字列
   */
  public static String escapeHtml(final String text) {
    return text.replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;");
  }

  /**
   * テストケースを識別するキーを返します。実行結果の記録と突き合わせるために使います.
   *
   * @return 「クラスのバイナリ名#メソッド名」
   */
  public String key() {
    return this.className + "#" + this.methodName;
  }

  /**
   * テスト項目書の見出しにする、外側から順に並べたクラス名を返します.
   *
   * @return 「LambdaHandlerIT › handleRequest › 正常系」の形の見出し
   */
  public String heading() {
    return String.join(" › ", this.groups);
  }

  /**
   * テスト項目書（Markdown）の表の1行を返します。実行結果や日時は含めず、テストコードが同じなら常に同じ行になります.
   *
   * @param no 通し番号
   * @return 表の1行。末尾に改行を含む
   */
  public String toMarkdownRow(final int no) {
    return "| "
        + no
        + " | "
        + String.join(
            " | ",
            Stream.of(this.methodName, this.description, this.given, this.when, this.then)
                .map(text -> text.replace("|", "\\|"))
                .toList())
        + " |\n";
  }

  /**
   * 実行結果のレポート（HTML）の1ケースを返します。ケースの内容、結果、失敗の内容、そのケースのログの表を1つにまとめます.
   *
   * <p>GitHub Actions の Job Summary にも同じ HTML を載せるため、GitHub が Markdown の中で受け付けるタグだけを使います.
   *
   * @param no 通し番号
   * @param result 結果。成功、失敗、スキップのいずれか
   * @param failure 失敗の内容。失敗していない場合は null
   * @param logFile そのケースのログファイル。無い場合はログの表を出さない
   * @return HTML の断片
   */
  public String toHtml(
      final int no, final String result, final String failure, final Path logFile) {
    StringBuilder html = new StringBuilder();
    // 失敗したケースは開いた状態にし、すぐに原因とログを読めるようにする
    html.append("<details class=\"case ")
        .append("成功".equals(result) ? "ok" : "ng")
        .append("\"")
        .append("成功".equals(result) ? "" : " open")
        .append("><summary><span class=\"badge\">")
        // GitHub の Job Summary は CSS を使えないため、色が無くても結果を見分けられるよう絵文字を添える
        .append(
            switch (result) {
              case "成功" -> "✅ ";
              case "失敗" -> "❌ ";
              default -> "⏭️ ";
            })
        .append(escapeHtml(result))
        .append("</span> ")
        .append(no)
        .append(". ")
        .append(escapeHtml(this.heading()))
        .append(" › <b>")
        .append(escapeHtml(this.methodName))
        .append("</b></summary><dl>")
        .append("<dt>確かめること</dt><dd>")
        .append(escapeHtml(this.description))
        .append("</dd><dt>前提</dt><dd>")
        .append(escapeHtml(this.given))
        .append("</dd><dt>操作</dt><dd>")
        .append(escapeHtml(this.when))
        .append("</dd><dt>期待結果</dt><dd>")
        .append(escapeHtml(this.then))
        .append("</dd></dl>");
    if (failure != null) {
      html.append("<h4>失敗の内容</h4><pre class=\"failure\">")
          .append(escapeHtml(failure))
          .append("</pre>");
    }
    if (logFile != null && Files.exists(logFile)) {
      html.append("<h4>ログ（CloudWatch Logs と同じ形式）</h4>").append(logTable(logFile));
      html.append(sqlBlock(ItLogRecorder.sqlFile(logFile)));
    }
    return html.append("</details>\n").toString();
  }

  /**
   * アプリが DB に実行した SQL（バインド値を埋めたもの）を、HTML の断片にします.
   *
   * @param sqlFile {@link ItLogRecorder} が書き出した SQL のファイル
   * @return HTML の断片。ファイルが無い、または SQL が無い場合は空文字
   */
  private static String sqlBlock(final Path sqlFile) {
    try {
      if (!Files.exists(sqlFile)) {
        return "";
      }
      String sql = Files.readString(sqlFile, StandardCharsets.UTF_8);
      return sql.isBlank()
          ? ""
          : "<h4>実行された SQL（PostgreSQL が受け取ったバインド値つき）</h4><pre>" + escapeHtml(sql) + "</pre>";
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /**
   * LogstashEncoder の JSON が1行に1件ずつ並んだログファイルを、HTML の表にします.
   *
   * <p>表には時刻・レベル・ロガー・メッセージ・requestId を並べ、例外のスタックトレースはメッセージの下に出します。元の JSON
   * の1行は各行の「JSON」から読めます。初期化時のログの表にも使います.
   *
   * @param logFile ログファイル
   * @return HTML の表
   */
  public static String logTable(final Path logFile) {
    StringBuilder html =
        new StringBuilder(
            "<table class=\"log\"><tr><th>時刻</th><th>レベル</th><th>ロガー</th><th>メッセージ</th>"
                + "<th>requestId</th></tr>");
    try {
      for (String line : Files.readAllLines(logFile, StandardCharsets.UTF_8)) {
        if (line.isBlank()) {
          continue;
        }
        JsonNode log = OBJECT_MAPPER.readTree(line);
        String logger = log.path("logger_name").asText();
        html.append("<tr class=\"")
            .append(escapeHtml(log.path("level").asText()))
            .append("\"><td>")
            .append(escapeHtml(log.path("@timestamp").asText()))
            .append("</td><td>")
            .append(escapeHtml(log.path("level").asText()))
            .append("</td><td title=\"")
            .append(escapeHtml(logger))
            .append("\">")
            .append(escapeHtml(logger.substring(logger.lastIndexOf('.') + 1)))
            .append("</td><td>")
            .append(escapeHtml(log.path("message").asText()));
        if (log.has("stack_trace")) {
          html.append("<pre>")
              .append(escapeHtml(log.path("stack_trace").asText()))
              .append("</pre>");
        }
        html.append("<details class=\"raw\"><summary>JSON</summary><pre>")
            .append(escapeHtml(line))
            .append("</pre></details></td><td>")
            .append(escapeHtml(log.path("requestId").asText("")))
            .append("</td></tr>");
      }
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    return html.append("</table>").toString();
  }

  /**
   * JavaDoc の改行と前後の空白を詰め、1行の文字列にします.
   *
   * @param text JavaDoc から読んだ文字列
   * @return 1行の文字列
   */
  private static String oneLine(final String text) {
    return text.replaceAll("\\s*\\R\\s*", "").trim();
  }
}
