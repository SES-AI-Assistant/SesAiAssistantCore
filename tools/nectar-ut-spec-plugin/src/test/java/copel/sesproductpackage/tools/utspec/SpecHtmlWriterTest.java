package copel.sesproductpackage.tools.utspec;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** {@link SpecHtmlWriter} の単体テスト。 */
class SpecHtmlWriterTest {

  /** 一時ディレクトリ。HTML を書き出すために使う。 */
  @TempDir Path tempDir;

  /**
   * テストケースを1件作る。
   *
   * @param status 実行結果
   * @return テストケース
   */
  private static TestCase sampleCase(TestStatus status) {
    return new TestCase(
        "pkg.ATest",
        "f",
        "正常系",
        "件数が3の場合_3を返す",
        "",
        0,
        "src/test/java/pkg/ATest.java",
        10,
        "入力を集計することを確かめる。",
        "理由を書く。",
        "件数が3の入力",
        "f を実行する",
        "3を返す",
        status);
  }

  /**
   * 仕様書の内容を1件作る。
   *
   * @param url リポジトリの URL（null の場合は行リンクを作らない）
   * @param commit コミットハッシュ（null の場合は不明）
   * @return 仕様書の内容
   */
  private static SpecReport sampleReport(String url, String commit) {
    return new SpecReport(
        "repo<&>",
        commit,
        url,
        "2026-10-19 10:00:00",
        1,
        0,
        0,
        new JacocoSummary(Set.of("pkg.A"), 9, 1, 3, 1),
        List.of(new MainSourceIndex.ExclusionEntry("pkg.Gen", "生成")),
        List.of(sampleCase(TestStatus.PASSED)));
  }

  @Nested
  class render {

    @Nested
    class 正常系 {

      /**
       * 仕様書の HTML が全体の構成を持ち、利用者の値をエスケープして出すことを確かめる。 単一の HTML として開けて、値の中の記号で画面を壊さない。
       *
       * @given リポジトリ名に記号を含む仕様書の内容
       * @when render を実行する
       * @then 文書の宣言とスクリプトを含み、リポジトリ名を実体参照で出力する HTML を返す
       */
      @Test
      void 記号を含むリポジトリ名の場合_エスケープしたHTMLを返す() {
        String html = new SpecHtmlWriter().render(sampleReport(null, null));

        assertThat(html)
            .startsWith("<!DOCTYPE html>")
            .contains("<script>")
            .contains("repo&lt;&amp;&gt;")
            .doesNotContain("repo<&>");
      }
    }
  }

  @Nested
  class renderSummary {

    @Nested
    class 正常系 {

      /**
       * サマリにコミットとテストの件数、カバレッジを出すことを確かめる。 仕様書の確認で必要な項目を一覧で見せる。
       *
       * @given コミット "abc123" とカバレッジの値を持つ仕様書の内容
       * @when renderSummary を実行する
       * @then コミット、件数、カバレッジの表示を含む HTML を返す
       */
      @Test
      void コミットがある場合_コミットと件数を含むHTMLを返す() {
        String html = new SpecHtmlWriter().renderSummary(sampleReport(null, "abc123"));

        assertThat(html).contains("abc123").contains("成功 1").contains("75.00%");
      }
    }

    @Nested
    class 境界値 {

      /**
       * コミットが取れない場合は不明と表示することを確かめる。 取れない情報を推測で埋めない。
       *
       * @given コミットが null の仕様書の内容
       * @when renderSummary を実行する
       * @then 不明の表示を含む HTML を返す
       */
      @Test
      void コミットが無い場合_不明の表示を含むHTMLを返す() {
        assertThat(new SpecHtmlWriter().renderSummary(sampleReport(null, null))).contains("不明");
      }
    }
  }

  @Nested
  class renderExclusions {

    @Nested
    class 正常系 {

      /**
       * 除外の対象と理由を表にして出すことを確かめる。 除外の理由を仕様書で確認できるようにする。
       *
       * @given 対象 pkg.Gen と理由 生成 の除外を持つ仕様書の内容
       * @when renderExclusions を実行する
       * @then 対象と理由を含む表の HTML を返す
       */
      @Test
      void 除外がある場合_対象と理由を含む表を返す() {
        String html = new SpecHtmlWriter().renderExclusions(sampleReport(null, null));

        assertThat(html).contains("pkg.Gen").contains("生成");
      }
    }

    @Nested
    class 境界値 {

      /**
       * 除外が無い場合は除外が無いと表示することを確かめる。 空の表を出さない。
       *
       * @given 除外を持たない仕様書の内容
       * @when renderExclusions を実行する
       * @then 除外が無いと示す HTML を返す
       */
      @Test
      void 除外が無い場合_除外はないと示すHTMLを返す() {
        SpecReport report =
            new SpecReport(
                "repo",
                null,
                null,
                "t",
                0,
                0,
                0,
                new JacocoSummary(Set.of(), 0, 0, 0, 0),
                List.of(),
                List.of());

        assertThat(new SpecHtmlWriter().renderExclusions(report)).contains("除外はない");
      }
    }
  }

  @Nested
  class renderFilters {

    @Nested
    class 正常系 {

      /**
       * 種類、結果、クラスの絞り込みの入力欄を出すことを確かめる。 利用者がテストを絞り込めるようにする。
       *
       * @given 任意の仕様書の内容
       * @when renderFilters を実行する
       * @then 種類、結果、クラスの入力欄を含む HTML を返す
       */
      @Test
      void 仕様書の内容が任意の場合_三つの入力欄を含むHTMLを返す() {
        String html = new SpecHtmlWriter().renderFilters();

        assertThat(html).contains("fKind").contains("fResult").contains("fClass");
      }
    }
  }

  @Nested
  class renderCases {

    @Nested
    class 正常系 {

      /**
       * テストケースをクラス、第1階層、種類の順に折りたたみで出すことを確かめる。 仕様書の階層を読みやすくする。
       *
       * @given テストケース1件を持つ仕様書の内容
       * @when renderCases を実行する
       * @then クラス名とテストケース名を含む折りたたみの HTML を返す
       */
      @Test
      void テストケースがある場合_折りたたみの階層を含むHTMLを返す() {
        String html = new SpecHtmlWriter().renderCases(sampleReport(null, null));

        assertThat(html).contains("<details").contains("pkg.ATest").contains("件数が3の場合_3を返す");
      }
    }
  }

  @Nested
  class renderTable {

    @Nested
    class 正常系 {

      /**
       * 1件のテストケースを、絞り込み用の属性を持つ行として出すことを確かめる。 絞り込みの画面で行を隠せるようにする。
       *
       * @given テストケース1件と番号 1
       * @when renderTable を実行する
       * @then 種類と結果の属性を持つ行の HTML を返す
       */
      @Test
      void テストケースが1件の場合_属性を持つ行のHTMLを返す() {
        String html =
            new SpecHtmlWriter()
                .renderTable(
                    sampleReport(null, null),
                    List.of(sampleCase(TestStatus.PASSED)),
                    new int[] {1});

        assertThat(html).contains("data-kind=\"正常系\"").contains("data-result=\"成功\"");
      }
    }
  }

  @Nested
  class codeLink {

    @Nested
    class 正常系 {

      /**
       * URL とコミットがあれば、コミット固定の行リンクを作ることを確かめる。 仕様書から該当行のコードへ移動できるようにする。
       *
       * @given URL とコミットを持つ仕様書の内容と、テストケース
       * @when codeLink を実行する
       * @then コミットと行番号を含むリンクの HTML を返す
       */
      @Test
      void URLとコミットがある場合_行番号付きのリンクを返す() {
        SpecHtmlWriter writer = new SpecHtmlWriter();
        String link =
            writer.codeLink(
                sampleReport("https://github.com/o/repo", "abc123"), sampleCase(TestStatus.PASSED));

        assertThat(link)
            .contains("https://github.com/o/repo/blob/abc123/src/test/java/pkg/ATest.java#L10");
      }
    }

    @Nested
    class 境界値 {

      /**
       * コミットが無い場合は、URL があってもリンクにせず場所だけを出すことを確かめる。 コミットを確定できないリンクを作らない。
       *
       * @given URL はあるが、コミットが null の仕様書の内容
       * @when codeLink を実行する
       * @then リンクを含まず、場所を表す文字列を返す
       */
      @Test
      void コミットが無い場合_リンクを含まない文字列を返す() {
        String link =
            new SpecHtmlWriter()
                .codeLink(
                    sampleReport("https://github.com/o/repo", null), sampleCase(TestStatus.PASSED));

        assertThat(link).doesNotContain("<a ").contains("src/test/java/pkg/ATest.java:10");
      }

      /**
       * URL かコミットが無い場合は、リンクにせず場所だけを出すことを確かめる。 取れない情報でリンクを作らない。
       *
       * @given URL が null の仕様書の内容
       * @when codeLink を実行する
       * @then リンクを含まず、場所を表す文字列を返す
       */
      @Test
      void URLが無い場合_リンクを含まない文字列を返す() {
        String link =
            new SpecHtmlWriter()
                .codeLink(sampleReport(null, "abc123"), sampleCase(TestStatus.PASSED));

        assertThat(link).doesNotContain("<a ").contains("src/test/java/pkg/ATest.java:10");
      }
    }
  }

  @Nested
  class escape {

    @Nested
    class 正常系 {

      /**
       * HTML に特別な意味を持つ記号を実体参照に変えることを確かめる。 利用者の入力で HTML の構造が壊れないようにする。
       *
       * @given 記号 アンパサンド、小なり、大なり、二重引用符、単一引用符を含む文字列
       * @when escape を実行する
       * @then 各記号を実体参照に変えた文字列を返す
       */
      @Test
      void 特別な記号を含む場合_実体参照に変えた文字列を返す() {
        assertThat(new SpecHtmlWriter().escape("&<>\"'")).isEqualTo("&amp;&lt;&gt;&quot;&#39;");
      }
    }

    @Nested
    class 境界値 {

      /**
       * null は空文字として扱うことを確かめる。 値の無い項目で例外を投げない。
       *
       * @given null の文字列
       * @when escape を実行する
       * @then 空文字を返す
       */
      @Test
      void 値がnullの場合_空文字を返す() {
        assertThat(new SpecHtmlWriter().escape(null)).isEmpty();
      }
    }
  }

  @Nested
  class write {

    @Nested
    class 正常系 {

      /**
       * 親ディレクトリが無くても HTML を書き出せることを確かめる。 出力先を事前に作らなくても仕様書を出せるようにする。
       *
       * @given 存在しない親ディレクトリ配下のパスと HTML の文字列
       * @when write を実行する
       * @then 指定したパスに HTML の内容を UTF-8 で書き出す
       */
      @Test
      void 親ディレクトリが無い場合_HTMLをファイルに書き出して内容を返す() throws IOException {
        Path file = tempDir.resolve("out/index.html");

        new SpecHtmlWriter().write(file, "<html>テスト</html>");

        assertThat(Files.readString(file, StandardCharsets.UTF_8)).isEqualTo("<html>テスト</html>");
      }

      /**
       * 親を持たない相対パスにも書き出せることを確かめる。 作業ディレクトリ直下の単純なファイル名でも例外にしない。
       *
       * @given 親ディレクトリを持たない相対パスのファイル名
       * @when write を実行する
       * @then ファイルの内容を読み戻して一致させ、一時ファイルを削除する
       */
      @Test
      void 出力先が親を持たないパスの場合_ファイルの内容を返す() throws IOException {
        Path relative = Path.of("nectar-ut-spec-write-check.html");
        try {
          new SpecHtmlWriter().write(relative, "x");

          assertThat(Files.readString(relative)).isEqualTo("x");
        } finally {
          Files.deleteIfExists(relative);
        }
      }
    }
  }

  @Nested
  class groupCases {

    @Nested
    class 正常系 {

      /**
       * テストケースをクラス、第1階層、種類の順に入れ子にして返すことを確かめる。 折りたたみの階層と同じ形で出す。
       *
       * @given テストケース1件
       * @when groupCases を実行する
       * @then クラス、第1階層、種類の順に1件を含む対応を返す
       */
      @Test
      void テストケースが1件の場合_三段の対応を返す() {
        Map<String, Map<String, Map<String, List<TestCase>>>> tree =
            new SpecHtmlWriter().groupCases(List.of(sampleCase(TestStatus.PASSED)));

        assertThat(tree.get("pkg.ATest").get("f").get("正常系")).hasSize(1);
      }
    }
  }

  @Nested
  class kindCounts {

    @Nested
    class 正常系 {

      /**
       * 種類ごとの件数を、規定の順番ですべての種類を含めて返すことを確かめる。 0件の種類も表に出す。
       *
       * @given 正常系のテストケース1件
       * @when kindCounts を実行する
       * @then 正常系が1で、5種類すべてを規定の順番で含む対応を返す
       */
      @Test
      void 正常系の件数が1の場合_5種類の件数を規定の順番にした値を返す() {
        Map<String, Integer> counts =
            new SpecHtmlWriter().kindCounts(List.of(sampleCase(TestStatus.PASSED)));

        assertThat(counts)
            .containsExactly(
                Map.entry("正常系", 1),
                Map.entry("異常系", 0),
                Map.entry("境界値", 0),
                Map.entry("認可", 0),
                Map.entry("テナント隔離", 0));
      }
    }
  }

  @Nested
  class statusClass {

    @Nested
    class 正常系 {

      /**
       * 成功の結果の CSS のクラス名を返すことを確かめる。 結果の色を見分けられるようにする。
       *
       * @given 実行結果が PASSED
       * @when statusClass を実行する
       * @then "passed" を返す
       */
      @Test
      void 結果が成功の場合_passedを返す() {
        assertThat(new SpecHtmlWriter().statusClass(TestStatus.PASSED)).isEqualTo("passed");
      }

      /**
       * 失敗の結果の CSS のクラス名を返すことを確かめる。 失敗を目立たせる。
       *
       * @given 実行結果が FAILED
       * @when statusClass を実行する
       * @then "failed" を返す
       */
      @Test
      void 結果が失敗の場合_failedを返す() {
        assertThat(new SpecHtmlWriter().statusClass(TestStatus.FAILED)).isEqualTo("failed");
      }

      /**
       * スキップの結果の CSS のクラス名を返すことを確かめる。 スキップを失敗と区別する。
       *
       * @given 実行結果が SKIPPED
       * @when statusClass を実行する
       * @then "skipped" を返す
       */
      @Test
      void 結果がスキップの場合_skippedを返す() {
        assertThat(new SpecHtmlWriter().statusClass(TestStatus.SKIPPED)).isEqualTo("skipped");
      }

      /**
       * 未実行の結果の CSS のクラス名を返すことを確かめる。 未実行を成功と区別する。
       *
       * @given 実行結果が UNKNOWN
       * @when statusClass を実行する
       * @then "unknown" を返す
       */
      @Test
      void 結果が未実行の場合_unknownを返す() {
        assertThat(new SpecHtmlWriter().statusClass(TestStatus.UNKNOWN)).isEqualTo("unknown");
      }
    }
  }

  @Nested
  class row {

    @Nested
    class 正常系 {

      /**
       * 見出しと値から、サマリの表の1行を作ることを確かめる。 サマリの表の形式を揃える。
       *
       * @given 見出し "件数" と値 "1"
       * @when row を実行する
       * @then 見出しと値を表の行の要素に入れた HTML を返す
       */
      @Test
      void 見出しと値がある場合_表の1行のHTMLを返す() {
        assertThat(new SpecHtmlWriter().row("件数", "1"))
            .isEqualTo("<tr><th>件数</th><td>1</td></tr>\n");
      }
    }
  }

  @Nested
  class commitText {

    @Nested
    class 正常系 {

      /**
       * コミットがある場合はエスケープしたハッシュを返すことを確かめる。 表示する値を安全にする。
       *
       * @given コミット "abc123" の仕様書の内容
       * @when commitText を実行する
       * @then "abc123" を返す
       */
      @Test
      void コミットがある場合_ハッシュを返す() {
        assertThat(new SpecHtmlWriter().commitText(sampleReport(null, "abc123")))
            .isEqualTo("abc123");
      }
    }

    @Nested
    class 境界値 {

      /**
       * コミットが無い場合は不明と返すことを確かめる。 取れない情報を推測で埋めない。
       *
       * @given コミットが null の仕様書の内容
       * @when commitText を実行する
       * @then "不明" を返す
       */
      @Test
      void コミットが無い場合_不明を返す() {
        assertThat(new SpecHtmlWriter().commitText(sampleReport(null, null))).isEqualTo("不明");
      }
    }
  }

  @Nested
  class percent {

    @Nested
    class 正常系 {

      /**
       * 百分率を小数2桁の文字列にすることを確かめる。 カバレッジの表示を揃える。
       *
       * @given 値が 99.5
       * @when percent を実行する
       * @then "99.50%" を返す
       */
      @Test
      void 値が小数の場合_小数2桁の百分率を返す() {
        assertThat(new SpecHtmlWriter().percent(99.5)).isEqualTo("99.50%");
      }
    }
  }
}
