package copel.sesproductpackage.tools.utspec;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * テスト仕様書を、外部依存のない単一の HTML ファイルとして出力する。
 *
 * <p>サマリ、除外クラス一覧、絞り込み、テスト仕様（クラス・第1階層・種類の折りたたみ）の順に出す。 利用者の入力に由来する値はすべて {@link #escape(String)}
 * を通してから出力する。
 *
 * @author Copel Co., Ltd.
 */
public class SpecHtmlWriter {

  /** 画面の見た目を決めるスタイル。 */
  private static final String STYLE =
      """
            body { font-family: -apple-system, "Hiragino Sans", "Meiryo", sans-serif;
                   margin: 24px; color: #222; }
            h1 { font-size: 22px; }
            h2 { font-size: 18px; border-bottom: 2px solid #ddd; padding-bottom: 4px;
                 margin-top: 32px; }
            table { border-collapse: collapse; width: 100%; margin: 8px 0;
                    font-size: 13px; }
            th, td { border: 1px solid #ccc; padding: 4px 6px;
                     vertical-align: top; text-align: left; }
            th { background: #f3f3f3; }
            .filters { margin: 12px 0; font-size: 14px; }
            .filters label { margin-right: 16px; }
            details { margin: 6px 0; }
            summary { cursor: pointer; font-weight: bold; padding: 2px 0; }
            .level1 > summary { font-size: 15px; }
            .kind > summary { font-size: 14px; color: #335; }
            .passed { color: #1a7f37; }
            .failed { color: #cf222e; font-weight: bold; }
            .skipped { color: #9a6700; }
            .unknown { color: #666; }
            """;

  /** 絞り込みを行うスクリプト。値は画面の入力だけで、外部からは読み込まない。 */
  private static final String SCRIPT =
      """
            (function () {
              var kind = document.getElementById('fKind');
              var result = document.getElementById('fResult');
              var cls = document.getElementById('fClass');
              var count = document.getElementById('fCount');
              var rows = document.querySelectorAll('tr[data-kind]');
              function apply() {
                var k = kind.value;
                var r = result.value;
                var c = cls.value.toLowerCase();
                var shown = 0;
                rows.forEach(function (tr) {
                  var ok = (k === '' || tr.dataset.kind === k)
                    && (r === '' || tr.dataset.result === r)
                    && (c === '' || tr.dataset.class.toLowerCase().indexOf(c) >= 0);
                  tr.style.display = ok ? '' : 'none';
                  if (ok) { shown++; }
                });
                count.textContent = shown + ' 件表示';
              }
              kind.addEventListener('change', apply);
              result.addEventListener('change', apply);
              cls.addEventListener('input', apply);
              apply();
            })();
            """;

  /**
   * 仕様書の内容を HTML の文字列にする。
   *
   * @param report 仕様書の内容
   * @return HTML の文字列
   */
  public String render(SpecReport report) {
    StringBuilder html = new StringBuilder();
    html.append(
            "<!DOCTYPE html>\n<html lang=\"ja\">\n<head>\n<meta charset=\"UTF-8\">\n<title>テスト仕様書 ")
        .append(escape(report.repositoryName()))
        .append("</title>\n<style>\n")
        .append(STYLE)
        .append("</style>\n</head>\n<body>\n");
    html.append("<h1>テスト仕様書 ").append(escape(report.repositoryName())).append("</h1>\n");
    html.append(renderSummary(report));
    html.append(renderExclusions(report));
    html.append("<h2>テスト仕様</h2>\n");
    html.append(renderFilters());
    html.append(renderCases(report));
    html.append("<script>\n").append(SCRIPT).append("</script>\n</body>\n</html>\n");
    return html.toString();
  }

  /**
   * サマリ（リポジトリ、コミット、実行日時、件数、カバレッジ、種類別の件数）を HTML にする。
   *
   * @param report 仕様書の内容
   * @return サマリの HTML
   */
  public String renderSummary(SpecReport report) {
    int total = report.cases().size();
    StringBuilder html = new StringBuilder("<h2>サマリ</h2>\n<table>\n");
    html.append(row("リポジトリ", escape(report.repositoryName())));
    html.append(row("コミット", commitText(report)));
    html.append(row("実行日時", escape(report.generatedAt())));
    html.append(
        row(
            "テストケース数",
            total
                + " 件（成功 "
                + report.passedCount()
                + " / 失敗 "
                + report.failedCount()
                + " / スキップ "
                + report.skippedCount()
                + "）"));
    html.append(row("カバレッジ（命令）", percent(report.coverage().instructionPercent())));
    html.append(row("カバレッジ（分岐）", percent(report.coverage().branchPercent())));
    html.append("</table>\n<table>\n<tr><th>種類</th><th>件数</th></tr>\n");
    for (Map.Entry<String, Integer> entry : kindCounts(report.cases()).entrySet()) {
      html.append("<tr><td>")
          .append(escape(entry.getKey()))
          .append("</td><td>")
          .append(entry.getValue())
          .append("</td></tr>\n");
    }
    html.append("</table>\n");
    return html.toString();
  }

  /**
   * 除外クラスの一覧を HTML にする。
   *
   * @param report 仕様書の内容
   * @return 除外の一覧の HTML
   */
  public String renderExclusions(SpecReport report) {
    StringBuilder html = new StringBuilder("<h2>除外クラス一覧</h2>\n");
    if (report.exclusions().isEmpty()) {
      return html.append("<p>除外はない。</p>\n").toString();
    }
    html.append("<table>\n<tr><th>対象</th><th>除外理由</th></tr>\n");
    for (MainSourceIndex.ExclusionEntry exclusion : report.exclusions()) {
      html.append("<tr><td>")
          .append(escape(exclusion.target()))
          .append("</td><td>")
          .append(escape(exclusion.reason()))
          .append("</td></tr>\n");
    }
    return html.append("</table>\n").toString();
  }

  /**
   * 絞り込みの入力欄（種類・結果・クラス）を HTML にする。
   *
   * @return 絞り込みの HTML
   */
  public String renderFilters() {
    StringBuilder html = new StringBuilder("<div class=\"filters\">\n");
    html.append("<label>種類 <select id=\"fKind\"><option value=\"\">すべて</option>");
    for (String kind : UtSpecRules.KIND_NAMES) {
      html.append("<option value=\"")
          .append(escape(kind))
          .append("\">")
          .append(escape(kind))
          .append("</option>");
    }
    html.append("</select></label>\n");
    html.append("<label>結果 <select id=\"fResult\"><option value=\"\">すべて</option>");
    for (TestStatus status : TestStatus.values()) {
      html.append("<option value=\"")
          .append(escape(status.label()))
          .append("\">")
          .append(escape(status.label()))
          .append("</option>");
    }
    html.append("</select></label>\n");
    html.append(
        "<label>クラス <input id=\"fClass\" type=\"search\" placeholder=\"クラス名の一部\"></label>\n");
    html.append("<span id=\"fCount\"></span>\n</div>\n");
    return html.toString();
  }

  /**
   * テストケースを、クラス、第1階層、種類の順に折りたたみで表す。
   *
   * @param report 仕様書の内容
   * @return テスト仕様の HTML
   */
  public String renderCases(SpecReport report) {
    Map<String, Map<String, Map<String, List<TestCase>>>> tree = groupCases(report.cases());
    StringBuilder html = new StringBuilder();
    int[] number = {1};
    for (Map.Entry<String, Map<String, Map<String, List<TestCase>>>> classEntry : tree.entrySet()) {
      html.append("<details open class=\"class\"><summary>")
          .append(escape(classEntry.getKey()))
          .append("</summary>\n");
      for (Map.Entry<String, Map<String, List<TestCase>>> level1Entry :
          classEntry.getValue().entrySet()) {
        html.append("<details open class=\"level1\"><summary>")
            .append(escape(level1Entry.getKey()))
            .append("</summary>\n");
        for (Map.Entry<String, List<TestCase>> kindEntry : level1Entry.getValue().entrySet()) {
          html.append("<details open class=\"kind\"><summary>")
              .append(escape(kindEntry.getKey()))
              .append("</summary>\n");
          html.append(renderTable(report, kindEntry.getValue(), number));
          html.append("</details>\n");
        }
        html.append("</details>\n");
      }
      html.append("</details>\n");
    }
    return html.toString();
  }

  /**
   * 1つの種類のテストケースを表にする。
   *
   * @param report 仕様書の内容（コード列のリンクに使う）
   * @param cases 同じ種類のテストケース
   * @param number 行番号を振るための可変の値（先頭要素を使う）
   * @return 表の HTML
   */
  public String renderTable(SpecReport report, List<TestCase> cases, int[] number) {
    StringBuilder html =
        new StringBuilder(
            "<table>\n<tr><th>No</th><th>テストケース名</th>"
                + "<th>何を確かめるか</th><th>理由</th><th>前提</th><th>操作</th><th>期待結果</th>"
                + "<th>結果</th><th>コード</th></tr>\n");
    for (TestCase testCase : cases) {
      html.append("<tr data-kind=\"")
          .append(escape(testCase.kind()))
          .append("\" data-result=\"")
          .append(escape(testCase.status().label()))
          .append("\" data-class=\"")
          .append(escape(testCase.classFqn()))
          .append("\">");
      html.append("<td>").append(number[0]++).append("</td>");
      html.append("<td>").append(escape(testCase.displayName())).append("</td>");
      html.append("<td>").append(escape(testCase.what())).append("</td>");
      html.append("<td>").append(escape(testCase.why())).append("</td>");
      html.append("<td>").append(escape(testCase.given())).append("</td>");
      html.append("<td>").append(escape(testCase.when())).append("</td>");
      html.append("<td>").append(escape(testCase.then())).append("</td>");
      html.append("<td class=\"")
          .append(statusClass(testCase.status()))
          .append("\">")
          .append(escape(testCase.status().label()))
          .append("</td>");
      html.append("<td>").append(codeLink(report, testCase)).append("</td>");
      html.append("</tr>\n");
    }
    return html.append("</table>\n").toString();
  }

  /**
   * テストケースのコード位置へのリンクを作る。リポジトリ URL とコミットが無い場合はリンクなしの文字列にする。
   *
   * @param report 仕様書の内容
   * @param testCase テストケース
   * @return リンクまたは文字列の HTML
   */
  public String codeLink(SpecReport report, TestCase testCase) {
    String location = testCase.file() + ":" + testCase.line();
    if (report.repositoryUrl() == null || report.commitHash() == null) {
      return escape(location);
    }
    String href =
        report.repositoryUrl()
            + "/blob/"
            + report.commitHash()
            + "/"
            + testCase.file()
            + "#L"
            + testCase.line();
    return "<a href=\"" + escape(href) + "\">" + escape(location) + "</a>";
  }

  /**
   * 文字列を HTML に安全な形にエスケープする。
   *
   * @param text 元の文字列（null の場合は空文字として扱う）
   * @return エスケープ後の文字列
   */
  public String escape(String text) {
    if (text == null) {
      return "";
    }
    return text.replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;");
  }

  /**
   * 仕様書の内容を、指定したパスの HTML ファイルとして書き出す。親ディレクトリが無ければ作る。
   *
   * @param file 出力先のパス
   * @param html HTML の文字列
   * @throws IOException ファイルを書き出せない場合
   */
  public void write(Path file, String html) throws IOException {
    Path parent = file.getParent();
    if (parent != null) {
      Files.createDirectories(parent);
    }
    Files.writeString(file, html, StandardCharsets.UTF_8);
  }

  /**
   * テストケースをクラス、第1階層、種類の順にまとめる。挿入の順番を保つ。
   *
   * @param cases テストケース
   * @return クラス → 第1階層 → 種類 → テストケースの一覧
   */
  public Map<String, Map<String, Map<String, List<TestCase>>>> groupCases(List<TestCase> cases) {
    Map<String, Map<String, Map<String, List<TestCase>>>> tree = new LinkedHashMap<>();
    for (TestCase testCase : cases) {
      tree.computeIfAbsent(testCase.classFqn(), key -> new LinkedHashMap<>())
          .computeIfAbsent(testCase.level1(), key -> new LinkedHashMap<>())
          .computeIfAbsent(testCase.kind(), key -> new ArrayList<>())
          .add(testCase);
    }
    return tree;
  }

  /**
   * 種類ごとの件数を、規定の種類の順番で返す。
   *
   * @param cases テストケース
   * @return 種類 → 件数
   */
  public Map<String, Integer> kindCounts(List<TestCase> cases) {
    Map<String, Integer> counts = new LinkedHashMap<>();
    for (String kind : UtSpecRules.KIND_NAMES) {
      counts.put(kind, 0);
    }
    for (TestCase testCase : cases) {
      counts.merge(testCase.kind(), 1, Integer::sum);
    }
    return counts;
  }

  /**
   * 結果の表示用のクラス名を返す。
   *
   * @param status 実行結果
   * @return CSS のクラス名
   */
  public String statusClass(TestStatus status) {
    return switch (status) {
      case PASSED -> "passed";
      case FAILED -> "failed";
      case SKIPPED -> "skipped";
      case UNKNOWN -> "unknown";
    };
  }

  /**
   * サマリの1行（見出しと値）を HTML にする。
   *
   * @param label 見出し
   * @param valueHtml 値（HTML として出す。エスケープ済みであること）
   * @return 表の1行の HTML
   */
  public String row(String label, String valueHtml) {
    return "<tr><th>" + escape(label) + "</th><td>" + valueHtml + "</td></tr>\n";
  }

  /**
   * コミットハッシュの表示を作る。取れない場合は「不明」にする。
   *
   * @param report 仕様書の内容
   * @return コミットの表示（HTML）
   */
  public String commitText(SpecReport report) {
    return report.commitHash() == null ? "不明" : escape(report.commitHash());
  }

  /**
   * 百分率を小数2桁の文字列にする。
   *
   * @param value 百分率
   * @return 「99.50%」の形式の文字列
   */
  public String percent(double value) {
    return String.format(Locale.ROOT, "%.2f%%", value);
  }
}
