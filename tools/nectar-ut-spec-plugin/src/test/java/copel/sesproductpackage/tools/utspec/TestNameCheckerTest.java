package copel.sesproductpackage.tools.utspec;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** {@link TestNameChecker} の単体テスト。 */
class TestNameCheckerTest {

  @Nested
  class コンストラクタ {

    @Nested
    class 正常系 {

      /**
       * 既定のルールで生成した TestNameChecker が規定どおりの形式を通すことを確かめる。 既定の生成で規定の語尾が判定できないと、規定どおりのテスト名まで違反になる。
       *
       * @given 既定のコンストラクタで生成した TestNameChecker
       * @when 形式どおりのテスト名で check を実行する
       * @then 空のリストを返す
       */
      @Test
      void 既定の設定で生成された場合_違反のリストを返す() {
        assertThat(new TestNameChecker().check("入力がある場合_403を返す", "A.java", 1)).isEmpty();
      }
    }
  }

  @Nested
  class check {

    @Nested
    class 正常系 {

      /**
       * 条件と期待結果を"の場合_"で分けた形式の名前は違反にしないことを確かめる。 仕様書のテスト名の書き方を守れていれば通す。
       *
       * @given 条件と期待結果を区切りの語で分けたテスト名
       * @when check を実行する
       * @then 空のリストを返す
       */
      @Test
      void テスト名が形式どおりの場合_空のリストを返す() {
        assertThat(new TestNameChecker().check("DBからユーザーを取得できない場合_503を返す", "A.java", 1)).isEmpty();
      }

      /**
       * 許可された語尾以外の期待結果を UT-09 として報告することを確かめる。 語尾の表記のばらつきを防ぐ。
       *
       * @given 期待結果が一覧にない語尾で終わるテスト名
       * @when check を実行する
       * @then UT-09 の違反を1件返す
       */
      @Test
      void 語尾が一覧にない場合_UT09の違反を返す() {
        assertThat(new TestNameChecker().check("入力がある場合_成功する", "A.java", 7))
            .singleElement()
            .satisfies(
                violation -> {
                  assertThat(violation.rule()).isEqualTo(UtRule.UT09);
                  assertThat(violation.line()).isEqualTo(7);
                });
      }
    }

    @Nested
    class 異常系 {

      /**
       * "の場合_"を含まない名前を UT-08 として報告することを確かめる。 条件と期待結果の分け方を守らせる。
       *
       * @given 区切りの語を含まないテスト名
       * @when check を実行する
       * @then UT-08 の違反を1件返す
       */
      @Test
      void 区切りが無い場合_UT08の違反を返す() {
        assertThat(new TestNameChecker().check("成功する", "A.java", 1))
            .singleElement()
            .satisfies(violation -> assertThat(violation.rule()).isEqualTo(UtRule.UT08));
      }

      /**
       * 条件が空の名前を UT-08 として報告することを確かめる。 条件を書かないテスト名が仕様書に残らないようにする。
       *
       * @given 条件の部分が空のテスト名
       * @when check を実行する
       * @then UT-08 の違反を1件返す
       */
      @Test
      void 条件が空の場合_UT08の違反を返す() {
        assertThat(new TestNameChecker().check("場合_返す", "A.java", 1))
            .singleElement()
            .satisfies(violation -> assertThat(violation.rule()).isEqualTo(UtRule.UT08));
      }
    }
  }
}
