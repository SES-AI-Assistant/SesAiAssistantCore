package copel.sesproductpackage.tools.utspec;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** {@link TestCase} の単体テスト。 */
class TestCaseTest {

  /**
   * テストケースを生成する。
   *
   * @param rowLabel パラメータの表示名
   * @param rowIndex パラメータの番号
   * @return テストケース
   */
  private static TestCase sample(String rowLabel, int rowIndex) {
    return new TestCase(
        "pkg.FooTest",
        "run",
        "正常系",
        "run_正常系",
        rowLabel,
        rowIndex,
        "src/test/java/pkg/FooTest.java",
        12,
        "何かを確かめる。",
        "理由を書く。",
        "前提",
        "操作",
        "結果を返す",
        TestStatus.UNKNOWN);
  }

  @Nested
  class surefireClassName {

    @Nested
    class 正常系 {

      /**
       * テストクラス、第1階層、種類をつないだ surefire のクラス名を返すことを確かめる。 surefire のレポートと結び付けるため、クラス名の形式を揃える必要がある。
       *
       * @given 完全修飾名が "pkg.FooTest"、第1階層が "run"、種類が "正常系" のテストケース
       * @when surefireClassName を実行する
       * @then "pkg.FooTest$run$正常系" を返す
       */
      @Test
      void 種類が正常系の場合_内部クラス形式のクラス名を返す() {
        assertThat(sample("", 0).surefireClassName()).isEqualTo("pkg.FooTest$run$正常系");
      }
    }
  }

  @Nested
  class displayName {

    @Nested
    class 正常系 {

      /**
       * パラメータが無い場合はメソッド名だけを表示名にすることを確かめる。 通常のテストの名前に余分な空白や記号を付けない。
       *
       * @given 表示名の空のテストケースで、メソッド名が "run_正常系"
       * @when displayName を実行する
       * @then "run_正常系" を返す
       */
      @Test
      void 表示名が空の場合_メソッド名だけを返す() {
        assertThat(sample("", 0).displayName()).isEqualTo("run_正常系");
      }

      /**
       * パラメータの表示名がある場合はメソッド名の後ろに付けることを確かめる。 同じメソッドの組ごとの行を利用者が見分けられるようにする。
       *
       * @given 表示名が "1組目の入力" で、メソッド名が "run_正常系" のテストケース
       * @when displayName を実行する
       * @then メソッド名の後ろに空白を挟んで表示名を付けた文字列を返す
       */
      @Test
      void 表示名がある場合_メソッド名の後ろに表示名を付けた値を返す() {
        assertThat(sample("[1] 入力値", 0).displayName()).isEqualTo("run_正常系 [1] 入力値");
      }
    }
  }

  @Nested
  class withFile {

    @Nested
    class 正常系 {

      /**
       * ファイルのパスだけを差し替えた新しいテストケースを返すことを確かめる。 リポジトリ直下からのパスに変換して、行リンクを作る。
       *
       * @given 相対パスが "src/test/java/pkg/FooTest.java" のテストケース
       * @when 新しいパス "tools/x/src/test/java/pkg/FooTest.java" で withFile を実行する
       * @then 新しいパスを持ち、他の項目が元のテストケースと同じ値のテストケースを返す
       */
      @Test
      void パスを差し替える場合_パスだけ変えたテストケースを返す() {
        TestCase original = sample("", 0);

        TestCase changed = original.withFile("tools/x/src/test/java/pkg/FooTest.java");

        assertThat(changed.file()).isEqualTo("tools/x/src/test/java/pkg/FooTest.java");
        assertThat(changed.methodName()).isEqualTo(original.methodName());
        assertThat(changed.line()).isEqualTo(original.line());
      }
    }
  }

  @Nested
  class withStatus {

    @Nested
    class 正常系 {

      /**
       * 実行結果だけを差し替えた新しいテストケースを返すことを確かめる。 検査の結果を変えずに、surefire の結果だけを後から付ける。
       *
       * @given 実行結果が UNKNOWN のテストケース
       * @when 実行結果を PASSED にして withStatus を実行する
       * @then 実行結果が PASSED で、他の項目が元のテストケースと同じ値のテストケースを返す
       */
      @Test
      void 実行結果を成功にする場合_結果だけ成功に変えたテストケースを返す() {
        TestCase original = sample("", 0);

        TestCase changed = original.withStatus(TestStatus.PASSED);

        assertThat(changed.status()).isEqualTo(TestStatus.PASSED);
        assertThat(changed.methodName()).isEqualTo(original.methodName());
        assertThat(changed.what()).isEqualTo(original.what());
      }
    }
  }
}
