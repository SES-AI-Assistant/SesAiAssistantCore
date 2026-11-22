package copel.sesproductpackage.core.unit;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** {@link JapaneseYearMonth} の単体テスト。 */
class JapaneseYearMonthTest {

  @Nested
  class parse {

    @Nested
    class 正常系 {

      /**
       * "2023年4月" を年月として解釈することを確かめる。 AIが抽出した開始年月と終了年月を、月数の計算に使える値にする必要があるためである。
       *
       * @given 文字列が "2023年4月"
       * @when parse を実行する
       * @then 表記が "2023年4月" の年月を返す
       */
      @Test
      void 年月の文字列の場合_年月を返す() {
        assertThat(JapaneseYearMonth.parse("2023年4月").map(JapaneseYearMonth::toJapaneseFormat))
            .contains("2023年4月");
      }

      /**
       * 前後に空白がある年月も解釈することを確かめる。 AIの出力は前後に空白が入ることがあり、その案件を集計から外さないためである。
       *
       * @given 文字列が前後に空白を持つ " 2023年4月 "
       * @when parse を実行する
       * @then 表記が "2023年4月" の年月を返す
       */
      @Test
      void 前後に空白がある場合_空白を除いて解釈した年月を返す() {
        assertThat(JapaneseYearMonth.parse(" 2023年4月 ").map(JapaneseYearMonth::toJapaneseFormat))
            .contains("2023年4月");
      }
    }

    @Nested
    class 異常系 {

      /**
       * null の文字列は年月として解釈できないことを確かめる。 開始年月や終了年月が無い案件を集計から除外するためである。
       *
       * @given 文字列が null
       * @when parse を実行する
       * @then 空を返す
       */
      @Test
      void nullの場合_空を返す() {
        assertThat(JapaneseYearMonth.parse(null)).isEmpty();
      }

      /**
       * 令和のように年号で書かれた年月は解釈できないことを確かめる。 年の値を取り違えると月数が大きくずれるため、解釈できないものは除外する。
       *
       * @given 文字列が "令和5年4月"
       * @when parse を実行する
       * @then 空を返す
       */
      @Test
      void 令和表記の場合_空を返す() {
        assertThat(JapaneseYearMonth.parse("令和5年4月")).isEmpty();
      }

      /**
       * 斜線で書かれた年月は YYYY年M月 の形式ではないため解釈できないことを確かめる。 形式を絞ることで、解釈の違いによる月数のずれを防ぐ。
       *
       * @given 文字列が "2023/4"
       * @when parse を実行する
       * @then 空を返す
       */
      @Test
      void 斜線の表記の場合_空を返す() {
        assertThat(JapaneseYearMonth.parse("2023/4")).isEmpty();
      }
    }

    @Nested
    class 境界値 {

      /**
       * 月が1桁で先頭に0を付けた年月を、その月として解釈することを確かめる。 AIの出力の表記ゆれで案件を落とさないためである。
       *
       * @given 文字列が "2023年01月"
       * @when parse を実行する
       * @then 表記が "2023年1月" の年月を返す
       */
      @Test
      void 月が先頭に0を付けた1桁の場合_その月として解釈した年月を返す() {
        assertThat(JapaneseYearMonth.parse("2023年01月").map(JapaneseYearMonth::toJapaneseFormat))
            .contains("2023年1月");
      }

      /**
       * 月が12の年月を12月として解釈することを確かめる。 1月から12月までの範囲の上端であり、範囲を超える13月との境目になるためである。
       *
       * @given 文字列が "2023年12月"
       * @when parse を実行する
       * @then 表記が "2023年12月" の年月を返す
       */
      @Test
      void 月が12の場合_12月として解釈した年月を返す() {
        assertThat(JapaneseYearMonth.parse("2023年12月").map(JapaneseYearMonth::toJapaneseFormat))
            .contains("2023年12月");
      }

      /**
       * 月が13の年月は解釈できないことを確かめる。 存在しない月の案件を、月数の計算に使わないためである。
       *
       * @given 文字列が "2023年13月"
       * @when parse を実行する
       * @then 空を返す
       */
      @Test
      void 月が13の場合_空を返す() {
        assertThat(JapaneseYearMonth.parse("2023年13月")).isEmpty();
      }

      /**
       * 月が0の年月は解釈できないことを確かめる。 0月は存在しない月のため、月数の計算に使わないためである。
       *
       * @given 文字列が "2023年0月"
       * @when parse を実行する
       * @then 空を返す
       */
      @Test
      void 月が0の場合_空を返す() {
        assertThat(JapaneseYearMonth.parse("2023年0月")).isEmpty();
      }
    }
  }

  @Nested
  class monthIndex {

    @Nested
    class 正常系 {

      /**
       * 1月の通し番号が、年の12倍になることを確かめる。 年をまたぐ月数の計算は、この通し番号の差で求めるためである。
       *
       * @given 年月が "2023年1月"
       * @when monthIndex を実行する
       * @then 2023年の12倍の通し番号を返す
       */
      @Test
      void 年の1月の場合_年の12倍の通し番号を返す() {
        assertThat(JapaneseYearMonth.parse("2023年1月").orElseThrow().monthIndex())
            .isEqualTo(2023 * 12);
      }
    }

    @Nested
    class 境界値 {

      /**
       * 12月の通し番号が、翌年1月の通し番号の1つ手前になることを確かめる。 年の境目で月数が1つずれないようにするためである。
       *
       * @given 年月が "2023年12月"
       * @when monthIndex を実行する
       * @then 2023年の12倍に11を足した通し番号を返す
       */
      @Test
      void 年の12月の場合_年の12倍に11を足した通し番号を返す() {
        assertThat(JapaneseYearMonth.parse("2023年12月").orElseThrow().monthIndex())
            .isEqualTo(2023 * 12 + 11);
      }
    }
  }

  @Nested
  class toJapaneseFormat {

    @Nested
    class 正常系 {

      /**
       * 先頭に0を付けない表記で年月を返すことを確かめる。 要約文の年月の表記を、AIの出力の書き方によらずそろえるためである。
       *
       * @given 年月が "2024年04月" を解釈した値
       * @when toJapaneseFormat を実行する
       * @then 表記が "2024年4月" の文字列を返す
       */
      @Test
      void 月に先頭の0がある入力の場合_0を付けない表記を返す() {
        assertThat(JapaneseYearMonth.parse("2024年04月").orElseThrow().toJapaneseFormat())
            .isEqualTo("2024年4月");
      }
    }
  }

  @Nested
  class compareTo {

    @Nested
    class 境界値 {

      /**
       * 同じ年月どうしの比較は0になることを確かめる。 同じ月の案件を、並べ替えで同じ位置として扱うためである。
       *
       * @given 2つの年月がどちらも "2023年4月"
       * @when compareTo を実行する
       * @then 0を返す
       */
      @Test
      void 同じ年月の場合_0を返す() {
        JapaneseYearMonth first = JapaneseYearMonth.parse("2023年4月").orElseThrow();
        JapaneseYearMonth second = JapaneseYearMonth.parse("2023年4月").orElseThrow();

        assertThat(first.compareTo(second)).isZero();
      }

      /**
       * 年をまたぐ比較で、前の年月が負の値になることを確かめる。 年が変わる境目でも、新しい年月を後と判定するためである。
       *
       * @given 自身が "2023年12月"、比較対象が "2024年1月"
       * @when compareTo を実行する
       * @then 負の値を返す
       */
      @Test
      void 比較対象が後の年月の場合_負の値を返す() {
        JapaneseYearMonth december = JapaneseYearMonth.parse("2023年12月").orElseThrow();
        JapaneseYearMonth january = JapaneseYearMonth.parse("2024年1月").orElseThrow();

        assertThat(december.compareTo(january)).isNegative();
      }

      /**
       * 年をまたぐ比較で、後の年月が正の値になることを確かめる。 終了年月の新しい順に並べるときに、翌年の案件を先に出すためである。
       *
       * @given 自身が "2024年1月"、比較対象が "2023年12月"
       * @when compareTo を実行する
       * @then 正の値を返す
       */
      @Test
      void 比較対象が前の年月の場合_正の値を返す() {
        JapaneseYearMonth january = JapaneseYearMonth.parse("2024年1月").orElseThrow();
        JapaneseYearMonth december = JapaneseYearMonth.parse("2023年12月").orElseThrow();

        assertThat(january.compareTo(december)).isPositive();
      }
    }
  }

  @Nested
  class equals {

    @Nested
    class 正常系 {

      /**
       * 同じ年月の2つの値が等しいと判定することを確かめる。 同じ月の案件を、重複として一つにまとめる判定に使うためである。
       *
       * @given 2つの年月がどちらも "2023年4月" を解釈した値
       * @when equals を実行する
       * @then trueを返す
       */
      @Test
      void 同じ年月の場合_trueを返す() {
        JapaneseYearMonth first = JapaneseYearMonth.parse("2023年4月").orElseThrow();
        JapaneseYearMonth second = JapaneseYearMonth.parse("2023年4月").orElseThrow();

        assertThat(first.equals(second)).isTrue();
      }

      /**
       * 同じ年月を解釈した2つの値のハッシュ値が一致することを確かめる。 等しい年月を集合や辞書で同じものとして扱うためである。
       *
       * @given 2つの年月がどちらも "2023年4月" を解釈した値
       * @when hashCode を実行する
       * @then 2つのハッシュ値を同じ値として返す
       */
      @Test
      void 同じ年月の場合_同じハッシュ値を返す() {
        JapaneseYearMonth first = JapaneseYearMonth.parse("2023年4月").orElseThrow();
        JapaneseYearMonth second = JapaneseYearMonth.parse("2023年4月").orElseThrow();

        assertThat(first.hashCode()).isEqualTo(second.hashCode());
      }
    }

    @Nested
    class 異常系 {

      /**
       * 年月の値と別の型の値は等しくないと判定することを確かめる。 文字列と年月を取り違えて比べても、等しいと判定しないためである。
       *
       * @given 年月が "2023年4月" の値と、文字列 "2023年4月"
       * @when equals に文字列を渡して実行する
       * @then falseを返す
       */
      @Test
      void 別の型の場合_falseを返す() {
        JapaneseYearMonth value = JapaneseYearMonth.parse("2023年4月").orElseThrow();

        assertThat(value.equals("2023年4月")).isFalse();
      }
    }

    @Nested
    class 境界値 {

      /**
       * 同じ値の自身と比べた場合に、等しいと判定することを確かめる。 自分自身との比較で判定を外さないためである。
       *
       * @given 年月が "2023年4月" の値を1つ用意する
       * @when equals に同じ値を渡して実行する
       * @then trueを返す
       */
      @Test
      void 同じインスタンスの場合_trueを返す() {
        JapaneseYearMonth value = JapaneseYearMonth.parse("2023年4月").orElseThrow();

        assertThat(value.equals(value)).isTrue();
      }

      /**
       * 年が違う年月は等しくないと判定することを確かめる。 同じ月でも年が違えば別の案件の時期であるためである。
       *
       * @given 年月が "2023年4月" と "2024年4月" の2つの値
       * @when equals を実行する
       * @then falseを返す
       */
      @Test
      void 年が違う場合_falseを返す() {
        JapaneseYearMonth first = JapaneseYearMonth.parse("2023年4月").orElseThrow();
        JapaneseYearMonth second = JapaneseYearMonth.parse("2024年4月").orElseThrow();

        assertThat(first.equals(second)).isFalse();
      }
    }
  }
}
