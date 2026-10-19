package copel.sesproductpackage.tools.utspec;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** {@link UtSpecRules} の単体テスト。 */
class UtSpecRulesTest {

  private final UtSpecRules rules = new UtSpecRules();

  @Nested
  class コンストラクタ {

    @Nested
    class 正常系 {

      /**
       * 既定のルールで生成した定義が禁止語を判定できることを確かめる。 規定の語彙を読み込めないと、すべての検査が素通りになる。
       *
       * @given 既定のコンストラクタで生成したルールの定義
       * @when 禁止語の一つを含む文字列で findForbiddenWords を実行する
       * @then 禁止語の一つだけを含むリストを返す
       */
      @Test
      void 既定の設定で生成された場合_禁止語を判定できる定義を返す() {
        assertThat(new UtSpecRules().findForbiddenWords("正しく動く")).containsExactly("正しく");
      }
    }
  }

  @Nested
  class findForbiddenWords {

    @Nested
    class 正常系 {
      /**
       * 追加された禁止語を検出することを確かめる。 あいまいな語を仕様書に使わせない。
       *
       * @given 追加された禁止語を含む文字列
       * @when findForbiddenWords を実行する
       * @then 追加された禁止語だけを含むリストを返す
       */
      @Test
      void 禁止語の追加分を含む場合_追加分の禁止語を返す() {
        assertThat(rules.findForbiddenWords("正しい結果")).containsExactly("正しい");
      }

      /**
       * 禁止語を含む文字列から、含まれる禁止語を定義の順に取り出すことを確かめる。 禁止語の違反を1件ずつ報告する。
       *
       * @given 禁止語の二つを含む文字列
       * @when findForbiddenWords を実行する
       * @then 禁止語の二つを定義の順に含むリストを返す
       */
      @Test
      void 禁止語が2つ含まれる場合_定義の順に2つを返す() {
        assertThat(rules.findForbiddenWords("うまく適切に処理する")).containsExactly("適切に", "うまく");
      }
    }

    @Nested
    class 境界値 {

      /**
       * 禁止語を含まない文字列では空のリストを返すことを確かめる。 禁止語が無い場合に違反を作らないようにする。
       *
       * @given 禁止語を含まない文字列 "登録する"
       * @when findForbiddenWords を実行する
       * @then 空のリストを返す
       */
      @Test
      void 禁止語が無い場合_空のリストを返す() {
        assertThat(rules.findForbiddenWords("登録する")).isEmpty();
      }
    }
  }

  @Nested
  class findBrackets {

    @Nested
    class 正常系 {

      /**
       * 括弧を含む文字列から、重複を除いた括弧の文字を返すことを確かめる。 同じ括弧が何度も出ても、違反は種類ごとに1件にする。
       *
       * @given 全角の左括弧と右括弧が2回ずつ現れる文字列
       * @when findBrackets を実行する
       * @then 左括弧と右括弧の2件のリストを返す
       */
      @Test
      void 括弧が重複して現れる場合_重複を除いた括弧を返す() {
        assertThat(rules.findBrackets("（a）（b）")).containsExactly("（", "）");
      }
    }
  }

  @Nested
  class findDecorations {

    @Nested
    class 正常系 {

      /**
       * 装飾記号と絵文字を含む文字列から、それぞれを取り出すことを確かめる。 仕様書の文章に装飾記号や絵文字が混ざるのを防ぐ。
       *
       * @given 星印と絵文字を含む文字列
       * @when findDecorations を実行する
       * @then 星印と絵文字の2件のリストを返す
       */
      @Test
      void 星印と絵文字が含まれる場合_取り出した一覧を返す() {
        assertThat(rules.findDecorations("完了★です😀")).containsExactly("★", "😀");
      }
    }
  }

  @Nested
  class isEmojiOrSymbol {

    @Nested
    class 正常系 {

      /**
       * 絵文字のコードポイントが記号として判定されることを確かめる。 絵文字を仕様書に混ぜないようにする。
       *
       * @given 絵文字の U+1F600
       * @when isEmojiOrSymbol を実行する
       * @then true を返す
       */
      @Test
      void 文字が絵文字の場合_trueを返す() {
        assertThat(rules.isEmojiOrSymbol(0x1F600)).isTrue();
      }

      /**
       * 矢印の記号が判定されることを確かめる。 仕様書の文章に矢印で流れを書かせないようにする。
       *
       * @given 矢印の U+2192
       * @when isEmojiOrSymbol を実行する
       * @then true を返す
       */
      @Test
      void 文字が矢印の場合_trueを返す() {
        assertThat(rules.isEmojiOrSymbol(0x2192)).isTrue();
      }

      /**
       * 範囲外のその他の記号も、Unicode の記号の分類で判定することを確かめる。 範囲に含まれない記号を見逃さないようにする。
       *
       * @given 著作権記号の U+00A9
       * @when isEmojiOrSymbol を実行する
       * @then true を返す
       */
      @Test
      void 文字が著作権記号の場合_trueを返す() {
        assertThat(rules.isEmojiOrSymbol(0x00A9)).isTrue();
      }

      /**
       * 変化セレクタや結合文字の範囲が判定されることを確かめる。 絵文字の見た目を作る文字を見逃さないようにする。
       *
       * @given 変化セレクタの U+FE0F
       * @when isEmojiOrSymbol を実行する
       * @then true を返す
       */
      @Test
      void 文字が変化セレクタの場合_trueを返す() {
        assertThat(rules.isEmojiOrSymbol(0xFE0F)).isTrue();
      }

      /**
       * 結合文字の範囲が判定されることを確かめる。 絵文字を組み立てる結合文字を見逃さないようにする。
       *
       * @given ゼロ幅接合子の U+200D
       * @when isEmojiOrSymbol を実行する
       * @then true を返す
       */
      @Test
      void 文字がゼロ幅接合子の場合_trueを返す() {
        assertThat(rules.isEmojiOrSymbol(0x200D)).isTrue();
      }

      /**
       * 幾何学図形の範囲が判定されることを確かめる。 塗りつぶしの図形を仕様書に混ぜないようにする。
       *
       * @given 黒い大きな正方形の U+2B1B
       * @when isEmojiOrSymbol を実行する
       * @then true を返す
       */
      @Test
      void 文字が幾何学図形の場合_trueを返す() {
        assertThat(rules.isEmojiOrSymbol(0x2B1B)).isTrue();
      }

      /**
       * 記号の範囲で、範囲内の図形が判定されることを確かめる。 星印の図形を範囲で拾えるようにする。
       *
       * @given 黒い星の U+2605
       * @when isEmojiOrSymbol を実行する
       * @then true を返す
       */
      @Test
      void 文字が黒い星の場合_trueを返す() {
        assertThat(rules.isEmojiOrSymbol(0x2605)).isTrue();
      }
    }

    @Nested
    class 境界値 {

      /**
       * 日本語の句点は記号として判定しないことを確かめる。 本文の区切りに使う句点まで違反にしないようにする。
       *
       * @given 句点の U+3002
       * @when isEmojiOrSymbol を実行する
       * @then false を返す
       */
      @Test
      void 文字が句点の場合_falseを返す() {
        assertThat(rules.isEmojiOrSymbol(0x3002)).isFalse();
      }

      /**
       * 英字は記号として判定しないことを確かめる。 通常の文字を違反にしないようにする。
       *
       * @given 英字の A
       * @when isEmojiOrSymbol を実行する
       * @then false を返す
       */
      @Test
      void 文字が英字の場合_falseを返す() {
        assertThat(rules.isEmojiOrSymbol('A')).isFalse();
      }

      /**
       * 記号の範囲の上限より後ろの数学記号は範囲外として判定することを確かめる。 範囲の境界を誤らないようにする。
       *
       * @given 範囲の外の数学記号 U+2200
       * @when isEmojiOrSymbol を実行する
       * @then false を返す
       */
      @Test
      void 文字が範囲の外の数学記号の場合_falseを返す() {
        assertThat(rules.isEmojiOrSymbol(0x2200)).isFalse();
      }

      /**
       * 絵文字の範囲の上限を超えた記号も、分類によって判定することを確かめる。 範囲の端の判定を区切る。
       *
       * @given 範囲の上限を超えた U+1FB00 の記号
       * @when isEmojiOrSymbol を実行する
       * @then 記号の分類で判定した結果を返す
       */
      @Test
      void 絵文字の範囲の上限を超えた記号の場合_分類による判定結果を返す() {
        assertThat(rules.isEmojiOrSymbol(0x1FB00)).isTrue();
      }

      /**
       * 範囲の上限を超えた点字の記号が分類によって判定されることを確かめる。 範囲の上端の判定を区切る。
       *
       * @given 範囲の上限を超えた U+2800 の点字空白
       * @when isEmojiOrSymbol を実行する
       * @then 記号の分類で判定した結果を返す
       */
      @Test
      void 範囲の上限を超えた点字の場合_分類による判定結果を返す() {
        assertThat(rules.isEmojiOrSymbol(0x2800)).isTrue();
      }
    }
  }

  @Nested
  class isAllowedExpectedResult {

    @Nested
    class 正常系 {

      /**
       * "〜を返す"の語尾が許可されることを確かめる。 戻り値を確かめるテストの語尾として最も多く使う。
       *
       * @given 期待結果が "403を返す"
       * @when isAllowedExpectedResult を実行する
       * @then true を返す
       */
      @Test
      void 返すで終わる場合_trueを返す() {
        assertThat(rules.isAllowedExpectedResult("403を返す")).isTrue();
      }

      /**
       * "XxxExceptionを投げる"の表記が許可されることを確かめる。 例外の型名で書く表記も許容し、例外の種類を明示できるようにする。
       *
       * @given 期待結果が "IllegalStateExceptionを投げる"
       * @when isAllowedExpectedResult を実行する
       * @then true を返す
       */
      @Test
      void 例外の型名で終わる場合_trueを返す() {
        assertThat(rules.isAllowedExpectedResult("IllegalStateExceptionを投げる")).isTrue();
      }

      /**
       * "何もしない"が単独で許可されることを確かめる。 副作用が無いことを明示するテストで使う。
       *
       * @given 期待結果が "何もしない"
       * @when isAllowedExpectedResult を実行する
       * @then true を返す
       */
      @Test
      void 期待結果が何もしないの場合_trueを返す() {
        assertThat(rules.isAllowedExpectedResult("何もしない")).isTrue();
      }
    }

    @Nested
    class 異常系 {

      /**
       * 一覧にない語尾は許可しないことを確かめる。 語尾の表記がばらつくと仕様書の読み取りにくさが増える。
       *
       * @given 期待結果が "成功する"
       * @when isAllowedExpectedResult を実行する
       * @then false を返す
       */
      @Test
      void 一覧にない語尾の場合_falseを返す() {
        assertThat(rules.isAllowedExpectedResult("成功する")).isFalse();
      }

      /**
       * 語尾の前に何も無い場合は許可しないことを確かめる。 空の期待結果を見逃さないようにする。
       *
       * @given 期待結果が "を返す"
       * @when isAllowedExpectedResult を実行する
       * @then false を返す
       */
      @Test
      void 期待結果が語尾だけの場合_falseを返す() {
        assertThat(rules.isAllowedExpectedResult("を返す")).isFalse();
      }
    }
  }

  @Nested
  class kindIndex {

    @Nested
    class 正常系 {

      /**
       * 種類の順番が規定どおりの0から始まる番号になることを確かめる。 第2階層の順番の判定に使う。
       *
       * @given 種類の名前が "境界値"
       * @when kindIndex を実行する
       * @then 2 を返す
       */
      @Test
      void 種類が境界値の場合_順番の2を返す() {
        assertThat(rules.kindIndex("境界値")).isEqualTo(2);
      }
    }

    @Nested
    class 異常系 {

      /**
       * 規定外の種類の名前は -1 を返すことを確かめる。 規定外の種類を順番の判定から外して違反として扱う。
       *
       * @given 種類の名前が "その他"
       * @when kindIndex を実行する
       * @then -1 を返す
       */
      @Test
      void 種類が規定外の名前の場合_マイナス1を返す() {
        assertThat(rules.kindIndex("その他")).isEqualTo(-1);
      }
    }
  }

  @Nested
  class startsWithProhibitedPrefix {

    @Nested
    class 正常系 {

      /**
       * "なぜ"で始まる文を枕詞として判定することを確かめる。 言い切りで書かせるため、枕詞を禁止する。
       *
       * @given 文が "なぜなら業務で必要だから" で始まる文
       * @when startsWithProhibitedPrefix を実行する
       * @then true を返す
       */
      @Test
      void なぜで始まる場合_trueを返す() {
        assertThat(rules.startsWithProhibitedPrefix("なぜなら業務で必要だから。")).isTrue();
      }
    }

    @Nested
    class 境界値 {

      /**
       * 枕詞を途中に含むだけの文は判定しないことを確かめる。 文中の語で違反を作らないようにする。
       *
       * @given 文が途中に理由の語を含み、文頭には含まない文
       * @when startsWithProhibitedPrefix を実行する
       * @then false を返す
       */
      @Test
      void 途中に理由を含む場合_falseを返す() {
        assertThat(rules.startsWithProhibitedPrefix("業務の理由は明確である。")).isFalse();
      }
    }
  }

  @Nested
  class endsWithProhibitedReason {

    @Nested
    class 正常系 {

      /**
       * 文末が理由の禁止された言い方で終わる場合に true を返すことを確かめる。 言い切りで書かせるため、この言い方を検出する必要がある。
       *
       * @given 文末が業務上必要な言い方で終わる文
       * @when endsWithProhibitedReason を実行する
       * @then true を返す
       */
      @Test
      void 文末がためで終わる場合_trueを返す() {
        assertThat(rules.endsWithProhibitedReason("業務上必要なため。")).isTrue();
      }
    }

    @Nested
    class 境界値 {

      /**
       * 理由を文中に含むだけの文は false を返すことを確かめる。 文中の理由の書き方は禁止しない。
       *
       * @given 文中に理由を述べ、文末が言い切りの文
       * @when endsWithProhibitedReason を実行する
       * @then false を返す
       */
      @Test
      void ためが文中にある場合_falseを返す() {
        assertThat(rules.endsWithProhibitedReason("業務上必要なため、登録する。")).isFalse();
      }
    }
  }

  @Nested
  class isOverSentenceLimit {

    @Nested
    class 境界値 {

      /**
       * 上限の100文字ちょうどの文は違反にしないことを確かめる。 上限を超えた場合だけを違反とする境界を守る。
       *
       * @given 100文字の文
       * @when isOverSentenceLimit を実行する
       * @then false を返す
       */
      @Test
      void 文字数が100の場合_falseを返す() {
        assertThat(rules.isOverSentenceLimit("あ".repeat(100))).isFalse();
      }

      /**
       * 上限の101文字の文は違反にすることを確かめる。 1文の長さの上限を超えた文章を検出する。
       *
       * @given 101文字の文
       * @when isOverSentenceLimit を実行する
       * @then true を返す
       */
      @Test
      void 文字数が101の場合_trueを返す() {
        assertThat(rules.isOverSentenceLimit("あ".repeat(101))).isTrue();
      }
    }
  }

  @Nested
  class isTestAnnotation {

    @Nested
    class 正常系 {

      /**
       * 対応済みのテストアノテーションがテストとして判定されることを確かめる。 テストメソッドを見落とさないようにする。
       *
       * @given アノテーション名が "ParameterizedTest"
       * @when isTestAnnotation を実行する
       * @then true を返す
       */
      @Test
      void 印がパラメータ付きテストの場合_trueを返す() {
        assertThat(rules.isTestAnnotation("ParameterizedTest")).isTrue();
      }

      /**
       * 未対応のテストアノテーションもテストとして判定されることを確かめる。 未対応の種類を使うと違反として報告するため、判定の対象に含める。
       *
       * @given アノテーション名が "RepeatedTest"
       * @when isTestAnnotation を実行する
       * @then true を返す
       */
      @Test
      void 印が未対応のテストの場合_trueを返す() {
        assertThat(rules.isTestAnnotation("RepeatedTest")).isTrue();
      }
    }

    @Nested
    class 異常系 {

      /**
       * テスト以外のアノテーションは判定しないことを確かめる。 Nested の階層のアノテーションをテストメソッドと誤らないようにする。
       *
       * @given アノテーション名が "Nested"
       * @when isTestAnnotation を実行する
       * @then false を返す
       */
      @Test
      void 印が階層のアノテーションの場合_falseを返す() {
        assertThat(rules.isTestAnnotation("Nested")).isFalse();
      }
    }
  }

  @Nested
  class endsWithSentenceEnd {

    @Nested
    class 正常系 {

      /**
       * 句点で終わる文を文末が句点で終わると判定することを確かめる。 各文が句点で終わっているかを検査する。
       *
       * @given 文の末尾が句点の文
       * @when endsWithSentenceEnd を実行する
       * @then true を返す
       */
      @Test
      void 句点で終わる場合_trueを返す() {
        assertThat(rules.endsWithSentenceEnd("内容を確かめる。")).isTrue();
      }
    }

    @Nested
    class 異常系 {

      /**
       * 句点で終わらない文を判定することを確かめる。 文の途中で切れた文章を見逃さないようにする。
       *
       * @given 文が "内容を確かめる"
       * @when endsWithSentenceEnd を実行する
       * @then false を返す
       */
      @Test
      void 句点で終わらない場合_falseを返す() {
        assertThat(rules.endsWithSentenceEnd("内容を確かめる")).isFalse();
      }
    }
  }

  @Nested
  class removeInlineTags {

    @Nested
    class 正常系 {

      /**
       * {@code {@link}} と {@code {@code}} の中身を取り除くことを確かめる。 対象クラスの名前を括弧の違反として数えないようにする。
       *
       * @given 文字列の中に link と code のタグを含むテキスト
       * @when removeInlineTags を実行する
       * @then タグを除いた文字列を返す
       */
      @Test
      void 参照タグを含む場合_タグを除いた文字列を返す() {
        assertThat(rules.removeInlineTags("対象 {@link Foo} と {@code Bar} の確認"))
            .isEqualTo("対象  と  の確認");
      }
    }
  }

  @Nested
  class findCharsIn {

    @Nested
    class 正常系 {

      /**
       * 絵文字の判定を含めた場合に、記号と絵文字の両方を取り出すことを確かめる。 絵文字の判定を必要な検査だけで有効にできるようにする。
       *
       * @given 文字列が星印を挟んだ英字の文字列で、対象の文字の集合が星印
       * @when 絵文字の判定を含めて findCharsIn を実行する
       * @then 星印の1件のリストを返す
       */
      @Test
      void 絵文字の判定を含める場合_対象の文字を返す() {
        assertThat(rules.findCharsIn("A★B", "★", true)).isEqualTo(List.of("★"));
      }

      /**
       * 絵文字の判定を含めない場合は、対象の文字の集合だけで判定することを確かめる。 括弧の検査で記号の範囲を拾わないようにする。
       *
       * @given 文字列が矢印を挟んだ英字の文字列で、対象の文字の集合が全角の括弧
       * @when 絵文字の判定を含めずに findCharsIn を実行する
       * @then 空のリストを返す
       */
      @Test
      void 絵文字の判定を含めない場合_空のリストを返す() {
        assertThat(rules.findCharsIn("A→B", "（）", false)).isEmpty();
      }
    }
  }
}
