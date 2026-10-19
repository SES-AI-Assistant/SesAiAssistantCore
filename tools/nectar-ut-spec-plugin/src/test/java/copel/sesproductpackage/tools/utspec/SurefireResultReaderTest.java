package copel.sesproductpackage.tools.utspec;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.w3c.dom.Element;

/** {@link SurefireResultReader} の単体テスト。 */
class SurefireResultReaderTest {

  /** 一時ディレクトリ。surefire のレポートを書き出すために使う。 */
  @TempDir Path tempDir;

  /** 成功、失敗、スキップの3件を持つレポートの内容。 */
  private static final String REPORT =
      """
            <?xml version="1.0" encoding="UTF-8"?>
            <testsuite name="pkg.ATest$f$正常系" tests="3">
              <testcase name="成功するの場合_1を返す()" classname="pkg.ATest$f$正常系" time="0.01"/>
              <testcase name="失敗するの場合_2を返す()" classname="pkg.ATest$f$正常系" time="0.01">
                <failure message="x" type="AssertionError"/>
              </testcase>
              <testcase name="スキップするの場合_3を返す()" classname="pkg.ATest$f$正常系" time="0.00">
                <skipped/>
              </testcase>
            </testsuite>
            """;

  /**
   * レポートのファイルを一時ディレクトリに書き出し、そのパスを返す。
   *
   * @param fileName ファイル名
   * @param content 内容
   * @return 書き出したパス
   * @throws IOException 書き出しに失敗した場合
   */
  private Path writeReport(String fileName, String content) throws IOException {
    Path report = tempDir.resolve(fileName);
    Files.writeString(report, content, StandardCharsets.UTF_8);
    return report;
  }

  /**
   * 文字列の XML から要素を1つ取り出す。
   *
   * @param xml XML の文字列
   * @return ルートの要素
   * @throws Exception 解析に失敗した場合
   */
  private static Element rootOf(String xml) throws Exception {
    return DocumentBuilderFactory.newInstance()
        .newDocumentBuilder()
        .parse(new org.xml.sax.InputSource(new java.io.StringReader(xml)))
        .getDocumentElement();
  }

  @Nested
  class read {

    @Nested
    class 正常系 {

      /**
       * レポートの各テストケースを、クラス名ごとの結果として読むことを確かめる。 仕様書の結果の列を実行結果から埋める。
       *
       * @given 成功、失敗、スキップの3件を持つ TEST-ATest.xml
       * @when read を実行する
       * @then 成功、失敗、スキップの件数を1件ずつ数える結果を返す
       */
      @Test
      void 三種類の結果がある場合_件数を数える結果を返す() throws IOException {
        writeReport("TEST-pkg.ATest.xml", REPORT);

        SurefireResults results = new SurefireResultReader().read(tempDir);

        assertThat(results.count(TestStatus.PASSED)).isEqualTo(1);
        assertThat(results.count(TestStatus.FAILED)).isEqualTo(1);
        assertThat(results.count(TestStatus.SKIPPED)).isEqualTo(1);
      }

      /**
       * TEST- で始まる xml 以外のファイルは読まないことを確かめる。 他のレポートや無関係なファイルを混ぜない。
       *
       * @given TEST の xml と、無関係な txt ファイル
       * @when read を実行する
       * @then TEST の xml の結果だけを数える結果を返す
       */
      @Test
      void 無関係なファイルがある場合_レポートだけを読んだ結果を返す() throws IOException {
        writeReport("TEST-pkg.ATest.xml", REPORT);
        writeReport("notes.txt", "memo");

        assertThat(new SurefireResultReader().read(tempDir).count(TestStatus.PASSED)).isEqualTo(1);
      }
    }

    @Nested
    class 境界値 {

      /**
       * レポートのディレクトリが無い場合は空の結果を返すことを確かめる。 テストを実行していない場合も仕様書を未実行として出す。
       *
       * @given 存在しないディレクトリ
       * @when read を実行する
       * @then 件数が0の結果を返す
       */
      @Test
      void ディレクトリが無い場合_件数が0の結果を返す() throws IOException {
        assertThat(
                new SurefireResultReader().read(tempDir.resolve("none")).count(TestStatus.PASSED))
            .isZero();
      }
    }
  }

  @Nested
  class isReport {

    @Nested
    class 正常系 {

      /**
       * TEST- で始まる xml をレポートと判定することを確かめる。 読むべきファイルを見分ける。
       *
       * @given ファイル名が TEST-A.xml のパス
       * @when isReport を実行する
       * @then true を返す
       */
      @Test
      void ファイル名が規定の名前の場合_trueを返す() {
        assertThat(new SurefireResultReader().isReport(Path.of("TEST-A.xml"))).isTrue();
      }
    }

    @Nested
    class 異常系 {
      /**
       * 名前を持たないパスはレポートではないと判定することを確かめる。 ルートのパスを読み込み対象にしない。
       *
       * @given 名前を持たないルートのパス
       * @when isReport を実行する
       * @then false を返す
       */
      @Test
      void 名前を持たないパスの場合_falseを返す() {
        assertThat(new SurefireResultReader().isReport(Path.of("/"))).isFalse();
      }

      /**
       * 接尾辞が xml 以外のファイルはレポートと判定しないことを確かめる。 テキストの仕様書を読まない。
       *
       * @given ファイル名が TEST-A.txt のパス
       * @when isReport を実行する
       * @then false を返す
       */
      @Test
      void 接尾辞が違う場合_falseを返す() {
        assertThat(new SurefireResultReader().isReport(Path.of("TEST-A.txt"))).isFalse();
      }

      /**
       * 接頭辞が違うファイルはレポートと判定しないことを確かめる。 無関係な xml を読まない。
       *
       * @given ファイル名が A.xml のパス
       * @when isReport を実行する
       * @then false を返す
       */
      @Test
      void 接頭辞が違う場合_falseを返す() {
        assertThat(new SurefireResultReader().isReport(Path.of("A.xml"))).isFalse();
      }
    }
  }

  @Nested
  class collect {

    @Nested
    class 正常系 {

      /**
       * レポートの各テストケースを、クラス名の対応に追加することを確かめる。 同じクラスの結果をまとめて探せるようにする。
       *
       * @given 3件のテストケースを持つレポート
       * @when collect を空の対応に対して実行する
       * @then クラス名 pkg.ATest$f$正常系 に3件の結果を追加する
       */
      @Test
      void テストケースがある場合_クラス名ごとに3件を登録する() throws IOException {
        Path report = writeReport("TEST-pkg.ATest.xml", REPORT);
        Map<String, List<SurefireResults.Outcome>> outcomes = new HashMap<>();

        new SurefireResultReader().collect(report, outcomes);

        assertThat(outcomes.get("pkg.ATest$f$正常系")).hasSize(3);
      }
    }

    @Nested
    class 異常系 {

      /**
       * 解析できないレポートは入出力の例外として扱うことを確かめる。 壊れたレポートを成功扱いにしない。
       *
       * @given 閉じタグの無い壊れたレポート
       * @when collect を実行する
       * @then IOException を投げる
       */
      @Test
      void レポートが壊れている場合_IOExceptionを投げる() throws IOException {
        Path report = writeReport("TEST-broken.xml", "<testsuite>");

        assertThatThrownBy(() -> new SurefireResultReader().collect(report, new HashMap<>()))
            .isInstanceOf(IOException.class);
      }
    }
  }

  @Nested
  class statusOf {

    @Nested
    class 正常系 {

      /**
       * failure を持つテストケースを失敗と判定することを確かめる。 失敗したテストを仕様書で赤く示す。
       *
       * @given failure の子要素を持つ testcase 要素
       * @when statusOf を実行する
       * @then 失敗を返す
       */
      @Test
      void 失敗の子要素がある場合_失敗を返す() throws Exception {
        Element testcase = rootOf("<testcase><failure/></testcase>");

        assertThat(new SurefireResultReader().statusOf(testcase)).isEqualTo(TestStatus.FAILED);
      }

      /**
       * error を持つテストケースも失敗と判定することを確かめる。 例外で止まったテストも失敗として示す。
       *
       * @given error の子要素を持つ testcase 要素
       * @when statusOf を実行する
       * @then 失敗を返す
       */
      @Test
      void エラーの子要素がある場合_失敗を返す() throws Exception {
        Element testcase = rootOf("<testcase><error/></testcase>");

        assertThat(new SurefireResultReader().statusOf(testcase)).isEqualTo(TestStatus.FAILED);
      }

      /**
       * skipped を持つテストケースをスキップと判定することを確かめる。 無効化されたテストを成功と区別する。
       *
       * @given skipped の子要素を持つ testcase 要素
       * @when statusOf を実行する
       * @then スキップを返す
       */
      @Test
      void スキップの子要素がある場合_スキップを返す() throws Exception {
        Element testcase = rootOf("<testcase><skipped/></testcase>");

        assertThat(new SurefireResultReader().statusOf(testcase)).isEqualTo(TestStatus.SKIPPED);
      }
    }

    @Nested
    class 境界値 {

      /**
       * 子要素の無い testcase 要素は成功と判定することを確かめる。 終わったテストを成功として数える。
       *
       * @given 子要素の無い testcase 要素
       * @when statusOf を実行する
       * @then 成功を返す
       */
      @Test
      void 子要素が無い場合_成功を返す() throws Exception {
        Element testcase = rootOf("<testcase/>");

        assertThat(new SurefireResultReader().statusOf(testcase)).isEqualTo(TestStatus.PASSED);
      }
    }
  }

  @Nested
  class hasChild {

    @Nested
    class 正常系 {

      /**
       * 指定した名前の直下の子要素があれば true を返すことを確かめる。 結果の判定に使う。
       *
       * @given failure の子要素を持つ要素
       * @when hasChild に failure を渡して実行する
       * @then true を返す
       */
      @Test
      void 直下に子要素がある場合_trueを返す() throws Exception {
        assertThat(new SurefireResultReader().hasChild(rootOf("<a><failure/></a>"), "failure"))
            .isTrue();
      }
    }

    @Nested
    class 境界値 {

      /**
       * 子要素が無ければ false を返すことを確かめる。 結果が無いテストを失敗と誤らない。
       *
       * @given 子要素の無い要素
       * @when hasChild に failure を渡して実行する
       * @then false を返す
       */
      @Test
      void 子要素が無い場合_falseを返す() throws Exception {
        assertThat(new SurefireResultReader().hasChild(rootOf("<a/>"), "failure")).isFalse();
      }

      /**
       * 文字列だけの子は要素ではないため無視することを確かめる。 テキストを結果の印として扱わない。
       *
       * @given 文字列だけを含む要素
       * @when hasChild に failure を渡して実行する
       * @then false を返す
       */
      @Test
      void 要素が文字列だけの場合_falseを返す() throws Exception {
        assertThat(new SurefireResultReader().hasChild(rootOf("<a>failure</a>"), "failure"))
            .isFalse();
      }
    }
  }

  @Nested
  class parse {

    @Nested
    class 正常系 {

      /**
       * 有効なレポートを解析してドキュメントを返すことを確かめる。 結果の読み取りの土台にする。
       *
       * @given 有効な XML のレポート
       * @when parse を実行する
       * @then ルート要素の testsuite を含むドキュメントを返す
       */
      @Test
      void レポートが有効な場合_ドキュメントを返す() throws IOException {
        Path report = writeReport("TEST-pkg.ATest.xml", REPORT);

        assertThat(new SurefireResultReader().parse(report).getDocumentElement().getTagName())
            .isEqualTo("testsuite");
      }
    }

    @Nested
    class 異常系 {

      /**
       * DOCTYPE を含むレポートは禁止されるため、入出力の例外として扱うことを確かめる。 外部実体を読ませない。
       *
       * @given DOCTYPE を含むレポート
       * @when parse を実行する
       * @then IOException を投げる
       */
      @Test
      void DOCTYPEを含む場合_IOExceptionを投げる() throws IOException {
        Path report = writeReport("TEST-doctype.xml", "<!DOCTYPE testsuite><testsuite/>");

        assertThatThrownBy(() -> new SurefireResultReader().parse(report))
            .isInstanceOf(IOException.class);
      }
    }
  }
}
