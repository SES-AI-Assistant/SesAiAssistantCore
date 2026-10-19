package copel.sesproductpackage.tools.utspec;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** {@link SurefireResults} の単体テスト。 */
class SurefireResultsTest {

  /**
   * 1クラスに2件の結果（通常の1件と、パラメータの2組目）を持つ実行結果を作る。
   *
   * @return 実行結果
   */
  private static SurefireResults sample() {
    return new SurefireResults(
        Map.of(
            "pkg.ATest$f$正常系",
            List.of(
                new SurefireResults.Outcome("件数が3の場合_3を返す()", TestStatus.PASSED),
                new SurefireResults.Outcome("件数がNの場合_Nを返す(int)[2]", TestStatus.FAILED))));
  }

  @Nested
  class コンストラクタ {

    @Nested
    class 正常系 {

      /**
       * 渡した対応をそのまま使って、実行結果を探せることを確かめる。 既定の生成と同じ探索の結果になるようにする。
       *
       * @given クラス名に1件の結果を対応させた実行結果
       * @when 同じクラス名とメソッド名で statusOf を実行する
       * @then 対応させた実行結果を返す
       */
      @Test
      void 対応が設定されている場合_探した結果を返す() {
        SurefireResults results =
            new SurefireResults(
                Map.of("c", List.of(new SurefireResults.Outcome("m()", TestStatus.PASSED))));

        assertThat(results.statusOf("c", "m", 0)).isEqualTo(TestStatus.PASSED);
      }
    }
  }

  @Nested
  class statusOf {

    @Nested
    class 正常系 {

      /**
       * 通常のテストの結果を、クラス名とメソッド名で探すことを確かめる。 テストケースごとの結果を仕様書に付ける。
       *
       * @given 結果が成功として記録されている実行結果
       * @when クラス名、メソッド名、順番 0 で statusOf を実行する
       * @then 成功を返す
       */
      @Test
      void テストが通常の場合_成功の結果を返す() {
        assertThat(sample().statusOf("pkg.ATest$f$正常系", "件数が3の場合_3を返す", 0))
            .isEqualTo(TestStatus.PASSED);
      }

      /**
       * テスト名がメソッド名と完全に一致する結果も探せることを確かめる。 引数の無い表記のテストも結果と結び付ける。
       *
       * @given 名前が m の成功の結果
       * @when メソッド名 m で statusOf を実行する
       * @then 成功を返す
       */
      @Test
      void 名前がメソッド名と同じ場合_成功の結果を返す() {
        SurefireResults results =
            new SurefireResults(
                Map.of("c", List.of(new SurefireResults.Outcome("m", TestStatus.PASSED))));

        assertThat(results.statusOf("c", "m", 0)).isEqualTo(TestStatus.PASSED);
      }

      /**
       * パラメータの2組目の結果を、組の番号で探すことを確かめる。 組ごとに成否を分けて仕様書に出す。
       *
       * @given 2組目の失敗の結果
       * @when 順番 1 で statusOf を実行する
       * @then 失敗を返す
       */
      @Test
      void パラメータの2組目の結果が失敗の場合_失敗の結果を返す() {
        assertThat(sample().statusOf("pkg.ATest$f$正常系", "件数がNの場合_Nを返す", 1))
            .isEqualTo(TestStatus.FAILED);
      }
    }

    @Nested
    class 異常系 {

      /**
       * 結果の無いテストは未実行を返すことを確かめる。 実行されていないテストを成功と誤って表示しない。
       *
       * @given 結果を持たないクラス名
       * @when statusOf を実行する
       * @then 未実行を返す
       */
      @Test
      void クラスの結果が無い場合_未実行を返す() {
        assertThat(sample().statusOf("pkg.Other", "x", 0)).isEqualTo(TestStatus.UNKNOWN);
      }

      /**
       * 組の番号が一致しない結果は対象にせず、未実行を返すことを確かめる。 別の組の結果を取り違えない。
       *
       * @given 1組目の成功の結果だけを持つ実行結果
       * @when 順番 1 で statusOf を実行する
       * @then 未実行を返す
       */
      @Test
      void 組の番号が一致しない場合_未実行を返す() {
        SurefireResults results =
            new SurefireResults(
                Map.of("c", List.of(new SurefireResults.Outcome("m(int)[1]", TestStatus.PASSED))));

        assertThat(results.statusOf("c", "m", 1)).isEqualTo(TestStatus.UNKNOWN);
      }

      /**
       * 同じクラスでも名前が一致しない結果は対象にしないことを確かめる。 名前が前方一致するだけの別のテストと取り違えない。
       *
       * @given メソッド件数 の名前だけが一致する別のテスト
       * @when statusOf を実行する
       * @then 未実行を返す
       */
      @Test
      void 名前が前方一致するだけの場合_未実行を返す() {
        assertThat(sample().statusOf("pkg.ATest$f$正常系", "件数", 0)).isEqualTo(TestStatus.UNKNOWN);
      }
    }
  }

  @Nested
  class rowNumberOf {

    @Nested
    class 正常系 {

      /**
       * 末尾の番号を1から始まる組の番号として読むことを確かめる。 パラメータの組に対応するテストを選ぶ。
       *
       * @given テスト名が m と int の後ろに番号3を付けた文字列
       * @when rowNumberOf を実行する
       * @then 3を返す
       */
      @Test
      void 末尾に番号がある場合_番号を返す() {
        assertThat(sample().rowNumberOf("m(int)[3]")).isEqualTo(3);
      }
    }

    @Nested
    class 境界値 {

      /**
       * 番号の無いテスト名は1を返すことを確かめる。 通常のテストを1組目として扱う。
       *
       * @given テスト名が m の後ろに空の括弧を付けた文字列
       * @when rowNumberOf を実行する
       * @then 1を返す
       */
      @Test
      void 番号が無い場合_1を返す() {
        assertThat(sample().rowNumberOf("m()")).isEqualTo(1);
      }
    }
  }

  @Nested
  class count {

    @Nested
    class 正常系 {

      /**
       * 指定した実行結果の件数を数えることを確かめる。 仕様書のサマリの件数に使う。
       *
       * @given 成功が1件、失敗が1件の実行結果
       * @when 失敗を指定して count を実行する
       * @then 1を返す
       */
      @Test
      void 失敗が1件の場合_1を返す() {
        assertThat(sample().count(TestStatus.FAILED)).isEqualTo(1);
      }
    }

    @Nested
    class 境界値 {

      /**
       * 該当の無い実行結果は0を返すことを確かめる。 スキップが無いテストで、件数の表示を0にする。
       *
       * @given 成功と失敗だけの実行結果
       * @when スキップを指定して count を実行する
       * @then 0を返す
       */
      @Test
      void 該当が無い場合_0を返す() {
        assertThat(sample().count(TestStatus.SKIPPED)).isZero();
      }
    }
  }
}
