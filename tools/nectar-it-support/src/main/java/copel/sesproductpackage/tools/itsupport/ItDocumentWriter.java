package copel.sesproductpackage.tools.itsupport;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;

/**
 * 結合テスト（IT）の終了後に、テスト項目書と実行結果のレポートを出力する JUnit のリスナーです.
 *
 * <ul>
 *   <li>テスト項目書（{@code docs/IT1テスト項目書.md}）: テストコードの JavaDoc から作り、Git にコミットする。実行結果と日時を含めないため、
 *       テストコードが変わったときだけ内容が変わる
 *   <li>実行結果のレポート（{@code target/it-results/IT1テスト結果.html}）: ケースごとの結果・失敗の内容・ログを1つの HTML
 *       にまとめ、ブラウザで読めるようにする。GitHub Actions では Artifact に保存する
 *   <li>実行結果の要約（{@code target/it-results/IT1テスト結果.md}）: レポートと同じ内容を Markdown にしたもの。GitHub Actions では
 *       Job Summary に載せ、Artifact をダウンロードせずに実行画面で読めるようにする
 * </ul>
 *
 * <p>GitHub Actions で実行したとき（環境変数 {@code GITHUB_ACTIONS} が true のとき）は、失敗したケースを注釈（{@code ::error}）
 * として標準出力に出し、実行画面の上部と PR の画面に表示させます.
 *
 * <p>システムプロパティ {@code nectar.it.spec.file} が設定されているとき（failsafe での実行時）だけ出力します。UT の実行時は
 * テストのクラスパスにこのリスナーが載っても何もしません.
 *
 * @author Copel Co., Ltd.
 */
public final class ItDocumentWriter implements TestExecutionListener {

  /** テスト項目書の先頭に置く、自動生成であることと再生成の方法の説明です。AI と人が誤って手で修正しないように書く. */
  private static final String SPEC_HEADER =
      """
      ---
      title: IT1テスト項目書（%s）
      generated: true
      do_not_edit: true
      generator: nectar-it-support（SesAiAssistantCore/tools/nectar-it-support）の ItDocumentWriter
      source: %s
      regenerate: mvn clean verify
      ---

      <!--
        このファイルは自動生成です。手動で修正しないでください（AI エージェントも同様）。
        内容を変える場合は、生成元のテストコード（source）のテスト名と JavaDoc を修正し、
        regenerate のコマンドを実行してください。手動の修正は次回の生成で上書きされます。
        GitHub Actions の IT1 ワークフローも、テストコードと違っていれば再生成してコミットします。
      -->

      # IT1テスト項目書

      > [!WARNING]
      > このファイルは `src/it/java` のテストコードから自動で生成しています。手動で修正しないでください。
      > 内容を変える場合はテストコードを修正し、`mvn clean verify` を実行して再生成してください。
      > 実行結果とシナリオごとのログは、GitHub Actions の IT1 ワークフローの Artifact（it1-results）を参照してください。
      """;

  /** 実行結果のレポートの見た目です。外部のファイルに頼らず、ダウンロードした HTML 1つで読めるように埋め込む. */
  private static final String REPORT_STYLE =
      """
      body{font-family:-apple-system,"Hiragino Sans","Noto Sans JP",sans-serif;margin:24px;color:#1f2328;background:#fff}
      h1{font-size:22px}h4{margin:12px 0 4px}
      .summary{display:flex;gap:12px;margin:12px 0 20px}
      .summary div{padding:8px 14px;border-radius:6px;background:#f6f8fa}
      .summary .ng{background:#ffebe9;color:#a40e26}.summary .ok{background:#dafbe1;color:#116329}
      details.case{border:1px solid #d0d7de;border-radius:6px;margin:8px 0;padding:0 12px}
      details.case>summary{cursor:pointer;padding:10px 0}
      .badge{display:inline-block;padding:1px 8px;border-radius:10px;color:#fff;font-size:12px}
      .ok .badge{background:#1a7f37}.ng .badge{background:#cf222e}
      dl{display:grid;grid-template-columns:7em 1fr;gap:4px 12px;margin:4px 0}dt{color:#59636e}dd{margin:0}
      pre{white-space:pre-wrap;word-break:break-all;margin:4px 0;font-size:12px}
      pre.failure{background:#ffebe9;padding:8px;border-radius:6px}
      table.log{border-collapse:collapse;width:100%;font-size:12px;margin-bottom:12px}
      table.log th,table.log td{border:1px solid #d0d7de;padding:3px 6px;vertical-align:top;text-align:left}
      table.log th{background:#f6f8fa}table.log td:nth-child(1){white-space:nowrap}
      tr.WARN td{background:#fff8c5}tr.ERROR td{background:#ffebe9}
      details.raw summary{color:#59636e;cursor:pointer;font-size:11px}
      @media (prefers-color-scheme:dark){body{background:#0d1117;color:#e6edf3}
      .summary div,table.log th{background:#161b22}details.case,table.log th,table.log td{border-color:#30363d}
      tr.WARN td{background:#3b2e00}tr.ERROR td,pre.failure,.summary .ng{background:#3d0d10;color:#ffdcd7}
      .summary .ok{background:#0f2e1a;color:#aff5b4}}
      """;

  /**
   * Job Summary に載せる要約の上限のバイト数です。GitHub の上限（1ステップあたり 1MiB）より少し小さくする.
   *
   * @see <a
   *     href="https://docs.github.com/actions/reference/workflows-and-actions/workflow-commands#adding-a-job-summary">Adding
   *     a job summary</a>
   */
  private static final int SUMMARY_MAX_BYTES = 1_000_000;

  /** テストメソッドごとの結果です。キーは {@link ItCase#key()} と同じです. */
  private final Map<String, String> results = Collections.synchronizedMap(new LinkedHashMap<>());

  /** 失敗したテストメソッドの、失敗の内容です。キーは {@link #results} と同じです. */
  private final Map<String, String> failures = Collections.synchronizedMap(new LinkedHashMap<>());

  /**
   * 終了したテストメソッドの結果と失敗の内容を記録します.
   *
   * @param identifier テストの識別子
   * @param result テストの結果
   */
  @Override
  public void executionFinished(final TestIdentifier identifier, final TestExecutionResult result) {
    if (identifier.getSource().orElse(null) instanceof MethodSource source) {
      String key = source.getClassName() + "#" + source.getMethodName();
      this.results.put(
          key, result.getStatus() == TestExecutionResult.Status.SUCCESSFUL ? "成功" : "失敗");
      result.getThrowable().ifPresent(e -> this.failures.put(key, e.toString()));
    }
  }

  /**
   * スキップしたテストメソッドを記録します.
   *
   * @param identifier テストの識別子
   * @param reason スキップの理由
   */
  @Override
  public void executionSkipped(final TestIdentifier identifier, final String reason) {
    if (identifier.getSource().orElse(null) instanceof MethodSource source) {
      this.results.put(source.getClassName() + "#" + source.getMethodName(), "スキップ");
    }
  }

  /**
   * すべてのテストが終わった後に、テスト項目書と実行結果のレポートを出力します.
   *
   * @param testPlan 実行したテストの計画
   */
  @Override
  public void testPlanExecutionFinished(final TestPlan testPlan) {
    String specFile = System.getProperty("nectar.it.spec.file");
    if (specFile == null) {
      return;
    }
    Path sourceDir = Path.of(System.getProperty("nectar.it.source.dir", "src/it/java"));
    List<ItCase> cases = ItCase.readAll(sourceDir);

    // テスト項目書: 実行の有無に関係なく全ケースを載せる。OS で差分が出ないよう改行は \n に固定する
    StringBuilder spec =
        new StringBuilder(
            SPEC_HEADER.formatted(
                System.getProperty("nectar.it.title", "このリポジトリ"),
                String.join(
                    ", ",
                    cases.stream()
                        .map(c -> "src/it/java/" + c.file().toString().replace('\\', '/'))
                        .distinct()
                        .toList())));
    String heading = null;
    int no = 0;
    for (ItCase itCase : cases) {
      if (!itCase.heading().equals(heading)) {
        heading = itCase.heading();
        spec.append("\n## ")
            .append(heading)
            .append("\n\n| No | テストケース | 確かめること | 前提 | 操作 | 期待結果 |\n")
            .append("| --: | :-- | :-- | :-- | :-- | :-- |\n");
      }
      spec.append(itCase.toMarkdownRow(++no));
    }

    // 実行結果のレポートと要約: 実行したケースだけを載せ、ケースごとにログを添える。
    // 要約はログが多すぎて Job Summary の上限を超える場合に備え、ログを除いたものも作っておく
    Path logDir = Path.of(System.getProperty("nectar.it.log.dir", "target/it-results/logs"));
    List<ItCase> executed = cases.stream().filter(c -> this.results.containsKey(c.key())).toList();
    StringBuilder body = new StringBuilder();
    StringBuilder summaryCases = new StringBuilder();
    StringBuilder summaryCasesWithoutLog = new StringBuilder();
    for (int i = 0; i < executed.size(); i++) {
      ItCase itCase = executed.get(i);
      String result = this.results.get(itCase.key());
      String failure = this.failures.get(itCase.key());
      String html =
          itCase.toHtml(
              i + 1,
              result,
              failure,
              logDir.resolve(
                  ItLogRecorder.logFile(itCase.className(), itCase.methodName(), result)));
      body.append(html);
      summaryCases.append(toSummaryBlock(html));
      summaryCasesWithoutLog.append(toSummaryBlock(itCase.toHtml(i + 1, result, failure, null)));
      if (failure != null && "true".equals(System.getenv("GITHUB_ACTIONS"))) {
        // ワークフローコマンドの書式に合わせ、改行などを置き換える
        // https://docs.github.com/actions/reference/workflows-and-actions/workflow-commands#setting-an-error-message
        System.out.println(
            "::error title="
                + escapeWorkflowCommand(itCase.heading() + " › " + itCase.methodName())
                    .replace(":", "%3A")
                    .replace(",", "%2C")
                + "::"
                + escapeWorkflowCommand(failure));
      }
    }
    long failed = this.results.values().stream().filter("失敗"::equals).count();
    long skipped = this.results.values().stream().filter("スキップ"::equals).count();
    String counts =
        "件数 "
            + this.results.size()
            + "　✅ 成功 "
            + (this.results.size() - failed - skipped)
            + "　❌ 失敗 "
            + failed
            + "　⏭️ スキップ "
            + skipped;
    String commit =
        ItCase.escapeHtml(Objects.requireNonNullElse(System.getenv("GITHUB_SHA"), "手元"));
    Path initLog = logDir.resolve("00_初期化.log");
    String initLogHtml =
        Files.exists(initLog)
            ? "<details class=\"case\"><summary>初期化時のログ"
                + "</summary>"
                + ItCase.logTable(initLog)
                + "</details>"
            : "";
    String report =
        "<!DOCTYPE html><html lang=\"ja\"><head><meta charset=\"UTF-8\">"
            + "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">"
            + "<title>IT1テスト結果</title><style>"
            + REPORT_STYLE
            + "</style></head><body><h1>IT1テスト結果</h1>"
            + "<p>実行日時: "
            + OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
            + "　コミット: "
            + commit
            + "</p><div class=\"summary\"><div>件数 "
            + this.results.size()
            + "</div><div class=\"ok\">成功 "
            + (this.results.size() - failed - skipped)
            + "</div><div class=\""
            + (failed > 0 ? "ng" : "")
            + "\">失敗 "
            + failed
            + "</div><div>スキップ "
            + skipped
            + "</div></div>"
            + body
            + initLogHtml
            + "</body></html>\n";
    String summaryHeader = "## IT1テスト結果\n\n" + counts + "\n\nコミット: " + commit + "\n\n";
    String summary = summaryHeader + summaryCases + toSummaryBlock(initLogHtml);
    if (summary.getBytes(StandardCharsets.UTF_8).length > SUMMARY_MAX_BYTES) {
      summary =
          summaryHeader
              + "> [!NOTE]\n> ログが Job Summary の上限を超えるため省きました。"
              + "ログは Artifact（it1-results）の HTML で読めます。\n\n"
              + summaryCasesWithoutLog;
    }

    try {
      Files.createDirectories(Path.of(specFile).getParent());
      Files.writeString(Path.of(specFile), spec.toString(), StandardCharsets.UTF_8);
      Path reportFile =
          Path.of(System.getProperty("nectar.it.report.file", "target/it-results/IT1テスト結果.html"));
      Files.createDirectories(reportFile.getParent());
      Files.writeString(reportFile, report, StandardCharsets.UTF_8);
      Path summaryFile =
          Path.of(System.getProperty("nectar.it.summary.file", "target/it-results/IT1テスト結果.md"));
      Files.createDirectories(summaryFile.getParent());
      Files.writeString(summaryFile, summary, StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /**
   * HTML の断片を、Markdown の中に置ける1つの HTML のかたまりにします.
   *
   * <p>Markdown では空行で HTML のかたまりが終わり、続きが Markdown として読まれて表示が崩れます。中の改行を文字参照（{@code
   * &#10;}）に置き換えて空行を無くし、{@code <pre>} の中の改行は表示上そのまま残します.
   *
   * @param html HTML の断片
   * @return 1行の HTML と、かたまりを区切る空行。断片が空なら空文字
   */
  private static String toSummaryBlock(final String html) {
    String block = html.strip();
    return block.isEmpty() ? "" : block.replace("\r", "").replace("\n", "&#10;") + "\n\n";
  }

  /**
   * GitHub Actions のワークフローコマンドに載せられるよう、% と改行を置き換えます.
   *
   * @param text 置き換える文字列
   * @return 置き換えた文字列
   */
  private static String escapeWorkflowCommand(final String text) {
    return text.replace("%", "%25").replace("\r", "%0D").replace("\n", "%0A");
  }
}
