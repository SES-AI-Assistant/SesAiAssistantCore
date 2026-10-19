package copel.sesproductpackage.tools.utspec;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** {@link TextRuleChecker} の単体テスト。 */
class TextRuleCheckerTest {

  @Nested
  class コンストラクタ {

    @Nested
    class 正常系 {

      /**
       * 既定のルールで生成した TextRuleChecker が禁止語を検出することを確かめる。 既定の生成で規定の語彙が使われないと、検査が効かなくなる。
       *
       * @given 既定のコンストラクタで生成した TextRuleChecker
       * @when 禁止語を含むテキストで check を実行する
       * @then 禁止語の違反を1件返す
       */
      @Test
      void 既定の設定で生成された場合_禁止語の違反を返す() {
        assertThat(new TextRuleChecker().check("うまく動く", "A.java", 1)).hasSize(1);
      }
    }
  }

  @Nested
  class check {

    @Nested
    class 正常系 {

      /**
       * 禁止語の違反を UT-14 として報告することを確かめる。 禁止語を含む記述を修正できるように、違反の番号と内容を出す。
       *
       * @given 禁止語の一つを含むテキスト
       * @when check を実行する
       * @then UT-14 の違反を1件返す
       */
      @Test
      void 禁止語を含む場合_UT14の違反を返す() {
        assertThat(new TextRuleChecker().check("正しく登録する", "A.java", 4))
            .singleElement()
            .satisfies(
                violation -> {
                  assertThat(violation.rule()).isEqualTo(UtRule.UT14);
                  assertThat(violation.line()).isEqualTo(4);
                });
      }

      /**
       * 括弧の違反を UT-15 として報告することを確かめる。 括弧の使用を仕様どおりに止める。
       *
       * @given テキストが全角の左括弧と右括弧で補足を囲んだ文字列
       * @when check を実行する
       * @then UT-15 の違反を2件返す
       */
      @Test
      void 括弧を含む場合_UT15の違反を返す() {
        assertThat(new TextRuleChecker().check("例（補足）", "A.java", 1))
            .extracting(violation -> violation.rule())
            .containsOnly(UtRule.UT15)
            .hasSize(2);
      }

      /**
       * 装飾記号の違反を UT-16 として報告することを確かめる。 仕様書に装飾記号が混ざるのを防ぐ。
       *
       * @given テキストが 星印を含む文字列
       * @when check を実行する
       * @then UT-16 の違反を1件返す
       */
      @Test
      void 装飾記号を含む場合_UT16の違反を返す() {
        assertThat(new TextRuleChecker().check("完了★", "A.java", 1))
            .singleElement()
            .satisfies(violation -> assertThat(violation.rule()).isEqualTo(UtRule.UT16));
      }
    }

    @Nested
    class 境界値 {

      /**
       * {@code {@link}} の中の括弧は違反にしないことを確かめる。 参照先のクラス名の表記で誤って違反にしない。
       *
       * @given テキストが参照タグの中に括弧を含む文字列
       * @when check を実行する
       * @then 空のリストを返す
       */
      @Test
      void 参照タグの中に括弧がある場合_空のリストを返す() {
        assertThat(new TextRuleChecker().check("対象 {@link Foo(1)} の確認", "A.java", 1)).isEmpty();
      }

      /**
       * 違反の無いテキストは空のリストを返すことを確かめる。 通常の記述で違反を作らない。
       *
       * @given 禁止語も括弧も装飾記号も含まない文字列
       * @when check を実行する
       * @then 空のリストを返す
       */
      @Test
      void テキストに違反が無い場合_空のリストを返す() {
        assertThat(new TextRuleChecker().check("登録件数を確かめる", "A.java", 1)).isEmpty();
      }
    }
  }
}
