package copel.sesproductpackage.tools.utspec;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** {@link JavadocChecker} の単体テスト。 */
class JavadocCheckerTest {

  /** 規定どおりの書式の JavaDoc の本文と、3つのタグ。 */
  private static final List<String> VALID_SENTENCES = List.of("入力を集計することを確かめる。", "集計の誤りは業務に直結する。");

  /**
   * 3つのタグがそろった JavaDoc の仕様を作る。
   *
   * @param sentences 本文の文の一覧
   * @return JavaDoc の仕様
   */
  private static JavadocSpec specWith(List<String> sentences) {
    return new JavadocSpec(
        sentences,
        Map.of(
            "given", List.of("前提"),
            "when", List.of("操作"),
            "then", List.of("結果")));
  }

  @Nested
  class コンストラクタ {

    @Nested
    class 正常系 {

      /**
       * 既定のルールで生成した JavadocChecker が規定どおりの書式を通すことを確かめる。 既定の生成で規定の文字数が判定できないと、規定どおりの JavaDoc が違反になる。
       *
       * @given 既定のコンストラクタで生成した JavadocChecker と、規定どおりの書式の JavaDoc
       * @when check を実行する
       * @then 違反を返さない
       */
      @Test
      void 既定の設定で生成された場合_違反のリストを返す() {
        assertThat(new JavadocChecker().check(specWith(VALID_SENTENCES), "A.java", 1)).isEmpty();
      }
    }
  }

  @Nested
  class check {

    @Nested
    class 正常系 {

      /**
       * 規定どおりの書式の JavaDoc が違反を返さないことを確かめる。 規定どおりの書き方を通す。
       *
       * @given 2文の本文と、3つのタグがそろった JavaDoc
       * @when check を実行する
       * @then 違反を返さない
       */
      @Test
      void JavaDocが書式どおりの場合_空のリストを返す() {
        assertThat(new JavadocChecker().check(specWith(VALID_SENTENCES), "A.java", 1)).isEmpty();
      }

      /**
       * 1文が文字数の上限を超えた場合に UT-13 として報告することを確かめる。 長すぎる文で読みにくい仕様書が出ないようにする。
       *
       * @given 文字数が101の文を含む2文の本文
       * @when check を実行する
       * @then UT-13 の違反を1件返す
       */
      @Test
      void 文字数が101の文を含む場合_UT13の違反を返す() {
        String longSentence = "あ".repeat(100) + "。";
        List<Violation> violations =
            new JavadocChecker()
                .check(specWith(List.of("入力を集計することを確かめる。", longSentence)), "A.java", 1);

        assertThat(violations)
            .singleElement()
            .satisfies(violation -> assertThat(violation.rule()).isEqualTo(UtRule.UT13));
      }
    }
  }

  @Nested
  class checkBody {

    @Nested
    class 正常系 {

      /**
       * 本文が2文で1文目が ことを確かめる で終わり、2文目が枕詞で始まらなければ違反を登録しないことを確かめる。 書式どおりの本文を通す。
       *
       * @given 2文の本文で、1文目が ことを確かめる で終わるもの
       * @when checkBody を実行する
       * @then 違反の一覧を変えない
       */
      @Test
      void 本文が書式どおりの場合_何もしない() {
        List<Violation> violations = new ArrayList<>();

        new JavadocChecker().checkBody(specWith(VALID_SENTENCES), "A.java", 1, violations);

        assertThat(violations).isEmpty();
      }

      /**
       * 本文2文目が枕詞で始まる場合は UT-11 を追加することを確かめる。 言い切りで書かせる。
       *
       * @given 2文目が なぜなら業務上必要だから で始まる本文
       * @when checkBody を実行する
       * @then UT-11 の違反を1件登録する
       */
      @Test
      void 二文目が枕詞で始まる場合_UT11の違反を登録する() {
        List<Violation> violations = new ArrayList<>();

        new JavadocChecker()
            .checkBody(
                specWith(List.of("入力を集計することを確かめる。", "なぜなら業務上必要だから。")), "A.java", 1, violations);

        assertThat(violations)
            .singleElement()
            .satisfies(violation -> assertThat(violation.rule()).isEqualTo(UtRule.UT11));
      }
    }

    @Nested
    class 異常系 {
      /**
       * 本文2文目が理由の言い方で終わる場合に UT-11 の違反を登録することを確かめる。 言い切りで書かせるため、末尾の言い方を禁止する。
       *
       * @given 本文2文目が業務上必要なため、末尾が理由の言い方である本文
       * @when checkBody を実行する
       * @then UT-11 の違反を1件登録する
       */
      @Test
      void 本文2文目がためで終わる場合_UT11の違反を登録する() {
        List<Violation> violations = new ArrayList<>();

        new JavadocChecker()
            .checkBody(specWith(List.of("入力を集計することを確かめる。", "業務上必要なため。")), "A.java", 1, violations);

        assertThat(violations)
            .singleElement()
            .satisfies(v -> assertThat(v.rule()).isEqualTo(UtRule.UT11));
      }

      /**
       * 本文が1文しかない場合は UT-10 を追加することを確かめる。 2文の書式を必ず守らせる。
       *
       * @given 1文だけの本文
       * @when checkBody を実行する
       * @then UT-10 の違反を1件登録する
       */
      @Test
      void 本文が1文の場合_UT10の違反を登録する() {
        List<Violation> violations = new ArrayList<>();

        new JavadocChecker().checkBody(specWith(List.of("内容を確かめる。")), "A.java", 1, violations);

        assertThat(violations)
            .singleElement()
            .satisfies(violation -> assertThat(violation.rule()).isEqualTo(UtRule.UT10));
      }

      /**
       * 1文目が規定の文末で終わらない場合に UT-10 を登録することを確かめる。 何を確かめるテストかを統一した言い方で書かせる。
       *
       * @given 1文目が 内容を見る で終わる本文
       * @when checkBody を実行する
       * @then UT-10 の違反を1件登録する
       */
      @Test
      void 一文目の文末が違う場合_UT10の違反を登録する() {
        List<Violation> violations = new ArrayList<>();

        new JavadocChecker()
            .checkBody(specWith(List.of("内容を見る。", "業務上必要である。")), "A.java", 1, violations);

        assertThat(violations)
            .singleElement()
            .satisfies(violation -> assertThat(violation.rule()).isEqualTo(UtRule.UT10));
      }

      /**
       * 句点で終わらない文がある場合は UT-10 を追加することを確かめる。 文が途中で切れた本文を通さない。
       *
       * @given 2文目の末尾に句点が無い本文
       * @when checkBody を実行する
       * @then UT-10 の違反を1件登録する
       */
      @Test
      void 句点で終わらない文がある場合_UT10の違反を登録する() {
        List<Violation> violations = new ArrayList<>();

        new JavadocChecker()
            .checkBody(specWith(List.of("入力を集計することを確かめる。", "業務上必要なため")), "A.java", 1, violations);

        assertThat(violations)
            .singleElement()
            .satisfies(violation -> assertThat(violation.rule()).isEqualTo(UtRule.UT10));
      }
    }
  }

  @Nested
  class checkTag {

    @Nested
    class 正常系 {

      /**
       * 1つだけあり空でない1文のタグは違反を追加しないことを確かめる。 規定どおりのタグを通す。
       *
       * @given タグ given が "前提" の1件だけの JavaDoc
       * @when checkTag に given を渡して実行する
       * @then 違反の一覧を変えない
       */
      @Test
      void タグが1つだけの場合_何もしない() {
        List<Violation> violations = new ArrayList<>();

        new JavadocChecker().checkTag(specWith(VALID_SENTENCES), "given", "A.java", 1, violations);

        assertThat(violations).isEmpty();
      }
    }

    @Nested
    class 異常系 {
      /**
       * タグの内容も1文の文字数の上限で検査することを確かめる。 タグに長い文を書いて仕様書の表が読めなくなることを防ぐ。
       *
       * @given @given の内容が101文字である
       * @when checkTag を given に対して実行する
       * @then UT-13 の違反を1件登録する
       */
      @Test
      void タグの内容が100文字を超える場合_UT13の違反を登録する() {
        List<Violation> violations = new ArrayList<>();
        JavadocSpec spec =
            new JavadocSpec(VALID_SENTENCES, Map.of("given", List.of("あ".repeat(101))));

        new JavadocChecker().checkTag(spec, "given", "A.java", 1, violations);

        assertThat(violations)
            .singleElement()
            .satisfies(v -> assertThat(v.rule()).isEqualTo(UtRule.UT13));
      }

      /**
       * タグが無い場合は UT-12 を追加することを確かめる。 前提や操作を書き漏らさないようにする。
       *
       * @given タグを持たない JavaDoc
       * @when checkTag に then を渡して実行する
       * @then UT-12 の違反を1件登録する
       */
      @Test
      void タグが無い場合_UT12の違反を登録する() {
        List<Violation> violations = new ArrayList<>();

        new JavadocChecker()
            .checkTag(new JavadocSpec(VALID_SENTENCES, Map.of()), "then", "A.java", 1, violations);

        assertThat(violations)
            .singleElement()
            .satisfies(violation -> assertThat(violation.rule()).isEqualTo(UtRule.UT12));
      }

      /**
       * タグが2つある場合は UT-12 を追加することを確かめる。 1つのケースに1つの項目だけを書かせる。
       *
       * @given タグ when が2件ある JavaDoc
       * @when checkTag に when を渡して実行する
       * @then UT-12 の違反を1件登録する
       */
      @Test
      void タグが2つある場合_UT12の違反を登録する() {
        List<Violation> violations = new ArrayList<>();
        JavadocSpec spec =
            new JavadocSpec(
                VALID_SENTENCES,
                Map.of(
                    "given", List.of("前提"), "when", List.of("操作", "別の操作"), "then", List.of("結果")));

        new JavadocChecker().checkTag(spec, "when", "A.java", 1, violations);

        assertThat(violations)
            .singleElement()
            .satisfies(violation -> assertThat(violation.rule()).isEqualTo(UtRule.UT12));
      }

      /**
       * タグの内容が空の場合は UT-12 を追加することを確かめる。 空の前提や期待結果を仕様書に出さない。
       *
       * @given タグ then の内容が空文字の JavaDoc
       * @when checkTag に then を渡して実行する
       * @then UT-12 の違反を1件登録する
       */
      @Test
      void タグの内容が空の場合_UT12の違反を登録する() {
        List<Violation> violations = new ArrayList<>();
        JavadocSpec spec = new JavadocSpec(VALID_SENTENCES, Map.of("then", List.of("")));

        new JavadocChecker().checkTag(spec, "then", "A.java", 1, violations);

        assertThat(violations)
            .singleElement()
            .satisfies(violation -> assertThat(violation.rule()).isEqualTo(UtRule.UT12));
      }

      /**
       * タグの内容に句点が含まれる場合は UT-12 を追加することを確かめる。 タグは1文で書かせる。
       *
       * @given タグ then の内容が2文に分かれた値
       * @when checkTag に then を渡して実行する
       * @then UT-12 の違反を1件登録する
       */
      @Test
      void タグの内容が2文の場合_UT12の違反を登録する() {
        List<Violation> violations = new ArrayList<>();
        JavadocSpec spec = new JavadocSpec(VALID_SENTENCES, Map.of("then", List.of("一つ目。二つ目")));

        new JavadocChecker().checkTag(spec, "then", "A.java", 1, violations);

        assertThat(violations)
            .singleElement()
            .satisfies(violation -> assertThat(violation.rule()).isEqualTo(UtRule.UT12));
      }
    }
  }
}
