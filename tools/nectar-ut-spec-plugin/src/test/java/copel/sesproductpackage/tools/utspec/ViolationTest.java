package copel.sesproductpackage.tools.utspec;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** {@link Violation} の単体テスト。 */
class ViolationTest {

  @Nested
  class format {

    @Nested
    class 正常系 {

      /**
       * 行番号がある違反をログ用の1行の文字列にすることを確かめる。 ログからファイルと行番号を辿って、修正箇所をすぐに開けるようにする。
       *
       * @given ファイルが "src/A.java"、行番号が 5、ルールが UT01、内容が "欠落" の違反
       * @when format を実行する
       * @then ファイル名、行番号、ルール番号、内容を並べた1行を返す
       */
      @Test
      void 行番号が5の場合_ファイルと行番号を含む1行を返す() {
        Violation violation = new Violation("src/A.java", 5, UtRule.UT01, "欠落");

        assertThat(violation.format()).isEqualTo("src/A.java:5 [UT-01] 欠落");
      }
    }

    @Nested
    class 境界値 {

      /**
       * 行番号が0の違反はファイル名だけを付けることを確かめる。 ファイル単位の違反に存在しない行番号を付けると、誤った場所を案内する。
       *
       * @given ファイルが "target/jacoco.csv"、行番号が 0、ルールが UT02、内容が "欠落" の違反
       * @when format を実行する
       * @then 行番号を付けない1行を返す
       */
      @Test
      void 行番号が0の場合_ファイル名だけを含む1行を返す() {
        Violation violation = new Violation("target/jacoco.csv", 0, UtRule.UT02, "欠落");

        assertThat(violation.format()).isEqualTo("target/jacoco.csv [UT-02] 欠落");
      }
    }
  }
}
