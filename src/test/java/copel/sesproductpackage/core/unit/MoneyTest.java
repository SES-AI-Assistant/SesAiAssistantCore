package copel.sesproductpackage.core.unit;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

/** {@link Money} の単体テスト。 */
class MoneyTest {

  @Nested
  class コンストラクタ_BigDecimal {

    @Nested
    class 正常系 {

      /**
       * BigDecimal の円単位の値を指定した場合、その値を保持することを確かめる。 金額を保存して読み直す際に値が変わると画面の金額が違って見えるので、値の保持を固定する。
       *
       * @given 円単位の値が BigDecimal で 800000
       * @when 生成する
       * @then 800000 を保持する金額を返す
       */
      @Test
      void 円単位のBigDecimalを指定した場合_金額の値を返す() {
        assertThat(new Money(new BigDecimal("800000")).getValue())
            .isEqualTo(new BigDecimal("800000"));
      }
    }
  }

  @Nested
  class コンストラクタ_long {

    @Nested
    class 正常系 {

      /**
       * long の円単位の値を指定した場合、BigDecimal に変換して保持することを確かめる。 円の整数で作った金額の型を変えると DB
       * への保存形式が変わるので、BigDecimal への変換を固定する。
       *
       * @given 円単位の値が long で 1000000
       * @when 生成する
       * @then 1000000 を保持する金額を返す
       */
      @Test
      void 円単位のlongを指定した場合_BigDecimalの金額を返す() {
        assertThat(new Money(1_000_000L).getValue()).isEqualTo(new BigDecimal("1000000"));
      }
    }
  }

  @Nested
  class empty {

    @Nested
    class 境界値 {

      /**
       * 空の金額を生成すると未設定の状態になることを確かめる。 抽出に失敗した単価を 0 円と区別できないと誤った金額が保存されるので、未設定の状態を返す。
       *
       * @given 引数を指定しない
       * @when empty を実行する
       * @then 未設定の金額を返す
       */
      @Test
      void 空の金額を生成した場合_未設定の金額を返す() {
        Money money = Money.empty();

        assertThat(money.isEmpty()).isTrue();
        assertThat(money.getValue()).isNull();
      }
    }
  }

  @Nested
  class isEmpty {

    @Nested
    class 境界値 {

      /**
       * 値が未設定の金額は空と判定することを確かめる。 単価が無い状態を判定できないと未入力の単価を金額として扱うので、空の判定を固定する。
       *
       * @given 値が未設定
       * @when isEmpty を実行する
       * @then true を返す
       */
      @Test
      void 値が未設定の場合_trueを返す() {
        assertThat(Money.empty().isEmpty()).isTrue();
      }

      /**
       * 値が設定された金額は空ではないと判定することを確かめる。 設定済みの単価を空と判定すると有効な単価が捨てられるので、値の有無を区別する。
       *
       * @given 値が 1 円
       * @when isEmpty を実行する
       * @then false を返す
       */
      @Test
      void 値が設定されている場合_falseを返す() {
        assertThat(new Money(1L).isEmpty()).isFalse();
      }
    }
  }

  @Nested
  class hasValue {

    @Nested
    class 境界値 {

      /**
       * 値が未設定の金額は値を持たないと判定することを確かめる。 単価が無い状態で値ありと判定すると null の参照で例外が起きるので、空の判定を固定する。
       *
       * @given 値が未設定
       * @when hasValue を実行する
       * @then false を返す
       */
      @Test
      void 値が未設定の場合_falseを返す() {
        assertThat(Money.empty().hasValue()).isFalse();
      }

      /**
       * 値が設定された金額は値を持つと判定することを確かめる。 isEmpty の逆の判定が揃わないと呼び出し側の条件分岐が食い違うので、両者を揃える。
       *
       * @given 値が 1 円
       * @when hasValue を実行する
       * @then true を返す
       */
      @Test
      void 値が設定されている場合_trueを返す() {
        assertThat(new Money(1L).hasValue()).isTrue();
      }
    }
  }

  @Nested
  class isNegotiable {

    @Nested
    class 正常系 {

      /**
       * 仕様値の 999 万円は単価不問と判定することを確かめる。 値のある単価を値ありと判定しないと表示が抜けるので、値の有無の判定を固定する。
       *
       * @given 値が 9990000 円
       * @when isNegotiable を実行する
       * @then true を返す
       */
      @Test
      void 仕様値の999万円の場合_trueを返す() {
        assertThat(Money.NEGOTIABLE_PRICE.isNegotiable()).isTrue();
      }

      /**
       * DB から読んだ小数点以下 2 桁の 999 万円も単価不問と判定することを確かめる。 999 万円は単価不問の印なので、固定単価として扱うと案件の意味が変わってしまう。
       *
       * @given 値が 9990000.00 円
       * @when isNegotiable を実行する
       * @then true を返す
       */
      @Test
      void スケールが異なる999万円の場合_trueを返す() {
        assertThat(new Money(new BigDecimal("9990000.00")).isNegotiable()).isTrue();
      }
    }

    @Nested
    class 異常系 {

      /**
       * 負の金額は応相談として単価不問と判定することを確かめる。 DB から読んだ小数付きの値は、スケールが違っても同じ金額なので、数値で判定する必要がある。
       *
       * @given 値が -1 円
       * @when isNegotiable を実行する
       * @then true を返す
       */
      @Test
      void 負の金額の場合_trueを返す() {
        assertThat(new Money(-1L).isNegotiable()).isTrue();
      }
    }

    @Nested
    class 境界値 {

      /**
       * 単価が未設定の場合は単価不問ではないと判定することを確かめる。 負の単価を固定単価として扱うと金額が誤って大きく見えるので、応相談として扱う。
       *
       * @given 値が未設定
       * @when isNegotiable を実行する
       * @then false を返す
       */
      @Test
      void 値が未設定の場合_falseを返す() {
        assertThat(Money.empty().isNegotiable()).isFalse();
      }

      /**
       * 0 円は応相談として単価不問と判定することを確かめる。 未設定の単価を単価不問として扱うと入力漏れが仕様値に見えてしまうので、false にする。
       *
       * @given 値が 0 円
       * @when isNegotiable を実行する
       * @then true を返す
       */
      @Test
      void 値が0円の場合_trueを返す() {
        assertThat(new Money(0L).isNegotiable()).isTrue();
      }

      /**
       * 999 万円未満の金額は単価不問ではないと判定することを確かめる。 仕様値の直前の金額を単価不問にすると通常の単価が消えてしまうので、境界を確かめる。
       *
       * @given 値が 9989999 円
       * @when isNegotiable を実行する
       * @then false を返す
       */
      @Test
      void 値が999万円未満の場合_falseを返す() {
        assertThat(new Money(9_989_999L).isNegotiable()).isFalse();
      }
    }
  }

  @Nested
  class isConsultation {

    @Nested
    class 異常系 {

      /**
       * 負の金額は応相談と判定することを確かめる。 負の金額は固定単価としての意味を持たないので、応相談として扱う。
       *
       * @given 値が -1 円
       * @when isConsultation を実行する
       * @then true を返す
       */
      @Test
      void 負の金額の場合_trueを返す() {
        assertThat(new Money(-1L).isConsultation()).isTrue();
      }
    }

    @Nested
    class 境界値 {

      /**
       * 0 円は応相談と判定することを確かめる。 0 円は応相談を表す値なので、応相談として扱う必要がある。
       *
       * @given 値が 0 円
       * @when isConsultation を実行する
       * @then true を返す
       */
      @Test
      void 値が0円の場合_trueを返す() {
        assertThat(new Money(0L).isConsultation()).isTrue();
      }

      /**
       * 1 円は応相談ではないと判定することを確かめる。 正の金額を応相談と区別しないと固定単価が応相談に変わってしまうので、1 円は応相談としない。
       *
       * @given 値が 1 円
       * @when isConsultation を実行する
       * @then false を返す
       */
      @Test
      void 値が1円の場合_falseを返す() {
        assertThat(new Money(1L).isConsultation()).isFalse();
      }

      /**
       * 未設定の金額は応相談ではないと判定することを確かめる。 未設定の単価を応相談と混同すると入力漏れが応相談に見えるので、区別する。
       *
       * @given 値が未設定
       * @when isConsultation を実行する
       * @then false を返す
       */
      @Test
      void 値が未設定の場合_falseを返す() {
        assertThat(Money.empty().isConsultation()).isFalse();
      }
    }
  }

  @Nested
  class toSesUnitFromMan {

    @Nested
    class 正常系 {

      /**
       * 10 万円以上 500 万円以下の値を円に換算することを確かめる。 月額単価の換算規則を変えると既存の金額が変わるので、規則を固定する。
       *
       * @given 万円単位の値が 10 以上 500 以下
       * @when toSesUnitFromMan を万円単位の値で実行する
       * @then 1 万倍した円単位の金額を返す
       */
      @ParameterizedTest
      @CsvSource({"10, 100000", "65.5, 655000", "500, 5000000"})
      void 万円の値が10以上500以下の場合_円に換算した値を返す(final String manValue, final long expected) {
        assertThat(Money.toSesUnitFromMan(new BigDecimal(manValue)).toYenValue())
            .isEqualTo(expected);
      }

      /**
       * 1000 以上 10000 未満の値を時給とみなし、160 時間分の円に換算することを確かめる。
       * 時給の入力漏れを月額に直す換算を固定しないと同じ入力の金額が変わるので、範囲と係数を固定する。
       *
       * @given 万円単位の値が 1000 以上 10000 未満
       * @when toSesUnitFromMan を万円単位の値で実行する
       * @then 160 倍した円単位の金額を返す
       */
      @ParameterizedTest
      @CsvSource({"1000, 160000", "8000, 1280000", "9999, 1599840"})
      void 万円の値が1000以上10000未満の場合_160時間分の円に換算した値を返す(final String manValue, final long expected) {
        assertThat(Money.toSesUnitFromMan(new BigDecimal(manValue)).toYenValue())
            .isEqualTo(expected);
      }

      /**
       * 10000 以上 100000 未満の値を日給とみなし、20 日分の円に換算することを確かめる。
       * 日給の入力漏れを月額に直す換算を固定しないと同じ入力の金額が変わるので、範囲と係数を固定する。
       *
       * @given 万円単位の値が 10000 以上 100000 未満
       * @when toSesUnitFromMan を万円単位の値で実行する
       * @then 20 倍した円単位の金額を返す
       */
      @ParameterizedTest
      @CsvSource({"10000, 200000", "50000, 1000000", "99999, 1999980"})
      void 万円の値が10000以上100000未満の場合_20日分の円に換算した値を返す(final String manValue, final long expected) {
        assertThat(Money.toSesUnitFromMan(new BigDecimal(manValue)).toYenValue())
            .isEqualTo(expected);
      }

      /**
       * 1000 万円以上の値を 10 分の 1 にして円に換算することを確かめる。 桁の過剰な入力を救済する換算を固定しないと大きな金額の扱いが変わるので、10 分の 1
       * の換算を固定する。
       *
       * @given 入力値が 1000 万以上
       * @when toSesUnitFromMan を入力値で実行する
       * @then 10 分の 1 の円単位の金額を返す
       */
      @ParameterizedTest
      @CsvSource({"10000000, 1000000"})
      void 入力値が1000万以上の場合_10分の1の円に換算した値を返す(final String input, final long expected) {
        assertThat(Money.toSesUnitFromMan(new BigDecimal(input)).toYenValue()).isEqualTo(expected);
      }

      /**
       * 10 万円以上 500 万円以下の値をそのまま円として扱うことを確かめる。 円で書かれた金額を二重に換算すると桁が大きくずれるので、そのまま採用する区間を固定する。
       *
       * @given 入力値が 10 万以上 500 万以下
       * @when toSesUnitFromMan を入力値で実行する
       * @then 入力値をそのまま円単位の金額として返す
       */
      @ParameterizedTest
      @CsvSource({"100000, 100000", "5000000, 5000000"})
      void 入力値が10万以上500万以下の場合_入力値の円の金額を返す(final String input, final long expected) {
        assertThat(Money.toSesUnitFromMan(new BigDecimal(input)).toYenValue()).isEqualTo(expected);
      }

      /**
       * 10 未満、または 500 超 1000 未満の値を万円として 1 万倍することを確かめる。 規則に当てはまらない小さな値の換算を固定しないと表示が変わるので、既定の換算を固定する。
       *
       * @given 万円単位の値が 9、501、999.5 のいずれか
       * @when toSesUnitFromMan を万円単位の値で実行する
       * @then 1 万倍した円単位の金額を返す
       */
      @ParameterizedTest
      @CsvSource({"9, 90000", "501, 5010000", "999.5, 9995000"})
      void 万円の値が10未満か500超1000未満の場合_万円として1万倍した円を返す(final String manValue, final long expected) {
        assertThat(Money.toSesUnitFromMan(new BigDecimal(manValue)).toYenValue())
            .isEqualTo(expected);
      }

      /**
       * 仕様値の 999 を指定すると、スキル見合いの仕様値の金額を返すことを確かめる。 999 は単価不問の印なので、一般の換算に入れると金額が誤って大きくなる。
       *
       * @given 万円単位の値が 999
       * @when toSesUnitFromMan を万円単位の値で実行する
       * @then スキル見合いの仕様値の金額を返す
       */
      @Test
      void 万円の値が999の場合_スキル見合いの仕様値を返す() {
        assertThat(Money.toSesUnitFromMan(new BigDecimal("999"))).isSameAs(Money.NEGOTIABLE_PRICE);
      }

      /**
       * 仕様値の 9990000 を指定すると、スキル見合いの仕様値の金額を返すことを確かめる。 円で書かれた単価不問の印を一般の金額にすると誤りになるので、同じ仕様値にそろえる。
       *
       * @given 入力値が 9990000
       * @when toSesUnitFromMan を入力値で実行する
       * @then スキル見合いの仕様値の金額を返す
       */
      @Test
      void 入力値が9990000の場合_スキル見合いの仕様値を返す() {
        assertThat(Money.toSesUnitFromMan(new BigDecimal("9990000")))
            .isSameAs(Money.NEGOTIABLE_PRICE);
      }

      /**
       * 500 万円を超える入力値はそのまま円として返すことを確かめる。 500 万円を超える値を既定の換算で扱わないと大きな金額の扱いが変わるので、値を固定する。
       *
       * @given 入力値が 5000001
       * @when toSesUnitFromMan を入力値で実行する
       * @then 入力値をそのまま円単位の金額として返す
       */
      @Test
      void 入力値が500万超の場合_入力値の円の金額を返す() {
        assertThat(Money.toSesUnitFromMan(new BigDecimal("5000001")).toYenValue())
            .isEqualTo(5_000_001L);
      }

      /**
       * 500 万円超 1000 万円未満の値はそのまま円として返すことを確かめる。 規則の隙間になる区間を円の既定の扱いとして固定し、区間の変更に気付けるようにする。
       *
       * @given 入力値が 5000001 または 9999999
       * @when toSesUnitFromMan を入力値で実行する
       * @then 入力値をそのまま円単位の金額として返す
       */
      @CsvSource({"5000001, 5000001", "9999999, 9999999"})
      @ParameterizedTest
      void 入力値が500万超1000万未満の場合_入力値の円の金額を返す(final String input, final long expected) {
        assertThat(Money.toSesUnitFromMan(new BigDecimal(input)).toYenValue()).isEqualTo(expected);
      }
    }

    @Nested
    class 境界値 {

      /**
       * 入力値が null の場合は空の金額を返すことを確かめる。 単価が無い入力を未設定として扱わないと 0 円と誤認されるので、空の金額を返す。
       *
       * @given 万円単位の値が null
       * @when toSesUnitFromMan を null で実行する
       * @then 空の金額を返す
       */
      @NullSource
      @ParameterizedTest
      void 万円の値がnullの場合_空の金額を返す(final BigDecimal manValue) {
        assertThat(Money.toSesUnitFromMan(manValue).isEmpty()).isTrue();
      }

      /**
       * 0 の入力は応相談として 0 円を返すことを確かめる。 0 以下の入力を 0 円として扱わないと負の金額が誤って保存されるので、応相談の 0 円にそろえる。
       *
       * @given 万円単位の値が 0
       * @when toSesUnitFromMan を万円単位の値で実行する
       * @then 0 円の金額を返す
       */
      @ValueSource(strings = {"0", "-1"})
      @ParameterizedTest
      void 万円の値が0以下の場合_0円を返す(final String manValue) {
        assertThat(Money.toSesUnitFromMan(new BigDecimal(manValue)).toYenValue()).isZero();
      }
    }
  }

  @Nested
  class toJapaneseFormat {

    @Nested
    class 正常系 {

      /**
       * 円単位の金額を万円の表記に変換することを確かめる。 画面の表示形式を変えると既存の画面が崩れるので、万円の表記を固定する。
       *
       * @given 値が 1000000 円
       * @when toJapaneseFormat を実行する
       * @then "100万円" を返す
       */
      @Test
      void 百万円の場合_100万円を返す() {
        assertThat(new Money(1_000_000L).toJapaneseFormat()).isEqualTo("100万円");
      }

      /**
       * 小数点以下 3 桁以上は切り捨てて 2 桁までの表記にすることを確かめる。 小数の桁数が変わると表示が揃わないので、切り捨ての桁数を固定する。
       *
       * @given 値が 1234567 円
       * @when toJapaneseFormat を実行する
       * @then "123.45万円" を返す
       */
      @Test
      void 小数を含む場合_小数点以下2桁で切り捨てた表記を返す() {
        assertThat(new Money(1_234_567L).toJapaneseFormat()).isEqualTo("123.45万円");
      }
    }

    @Nested
    class 境界値 {

      /**
       * 未設定の金額は null を返すことを確かめる。 未設定の金額を表示すると誤った金額に見えるので、null を返す。
       *
       * @given 値が未設定
       * @when toJapaneseFormat を実行する
       * @then null を返す
       */
      @Test
      void 値が未設定の場合_nullを返す() {
        assertThat(Money.empty().toJapaneseFormat()).isNull();
      }
    }
  }

  @Nested
  class toPriceLabel {

    @Nested
    class 正常系 {

      /**
       * 通常の金額は万円の表記をラベルとして返すことを確かめる。 通知で単価を表示する文言が変わると通知の内容が崩れるので、万円の表記を固定する。
       *
       * @given 値が 1000000 円
       * @when toPriceLabel を実行する
       * @then "100万円" を返す
       */
      @Test
      void 通常の金額の場合_万円の表記を返す() {
        assertThat(new Money(1_000_000L).toPriceLabel()).isEqualTo("100万円");
      }

      /**
       * 仕様値の 999 万円はスキル見合いのラベルを返すことを確かめる。 999 万円を単価として表示すると実際の単価と誤認されるので、ラベルに置き換える。
       *
       * @given 値が 9990000 円
       * @when toPriceLabel を実行する
       * @then "スキル見合い" を返す
       */
      @Test
      void 仕様値の999万円の場合_スキル見合いを返す() {
        assertThat(Money.NEGOTIABLE_PRICE.toPriceLabel()).isEqualTo("スキル見合い");
      }

      /**
       * 応相談の 0 円は応相談のラベルを返すことを確かめる。 0 円を単価として表示すると無料と誤認されるので、応相談のラベルに置き換える。
       *
       * @given 値が 0 円
       * @when toPriceLabel を実行する
       * @then "応相談" を返す
       */
      @Test
      void 応相談の0円の場合_応相談を返す() {
        assertThat(new Money(0L).toPriceLabel()).isEqualTo("応相談");
      }
    }

    @Nested
    class 境界値 {

      /**
       * 未設定の金額は null を返すことを確かめる。 未設定の単価を表示に出すと誤った金額に見えるので、null を返す。
       *
       * @given 値が未設定
       * @when toPriceLabel を実行する
       * @then null を返す
       */
      @Test
      void 値が未設定の場合_nullを返す() {
        assertThat(Money.empty().toPriceLabel()).isNull();
      }
    }
  }

  @Nested
  class toYenValue {

    @Nested
    class 正常系 {

      /**
       * 円単位の数値を long で返すことを確かめる。 DB 保存や計算で円の整数が必要になるので、円単位の値を返す。
       *
       * @given 値が 1000000 円
       * @when toYenValue を実行する
       * @then 1000000 を返す
       */
      @Test
      void 値が設定されている場合_円単位の値を返す() {
        assertThat(new Money(1_000_000L).toYenValue()).isEqualTo(1_000_000L);
      }
    }

    @Nested
    class 境界値 {

      /**
       * 未設定の金額は 0 を返すことを確かめる。 未設定の金額を 0 円として計算すると計算結果が誤るので、0 を返す。
       *
       * @given 値が未設定
       * @when toYenValue を実行する
       * @then 0 を返す
       */
      @Test
      void 値が未設定の場合_0を返す() {
        assertThat(Money.empty().toYenValue()).isZero();
      }
    }
  }

  @Nested
  class isLessThan {

    @Nested
    class 正常系 {

      /**
       * 自身が比較対象より小さい場合は true を返すことを確かめる。 小さい方の判定が逆になると金額の大小判定が誤るので、大小を固定する。
       *
       * @given 自身が 1 円、比較対象が 2 円
       * @when isLessThan を比較対象で実行する
       * @then true を返す
       */
      @Test
      void 自身が小さい場合_trueを返す() {
        assertThat(new Money(1L).isLessThan(new Money(2L))).isTrue();
      }
    }

    @Nested
    class 境界値 {

      /**
       * 自身と比較対象が等しい場合は false を返すことを確かめる。 未満の判定に等号を含めると同じ金額が未満と判定されるので、等号を含めない。
       *
       * @given 自身が 1 円、比較対象が 1 円
       * @when isLessThan を比較対象で実行する
       * @then false を返す
       */
      @Test
      void 同じ金額の場合_falseを返す() {
        assertThat(new Money(1L).isLessThan(new Money(1L))).isFalse();
      }

      /**
       * 自身が未設定の場合は false を返すことを確かめる。 未設定の金額を比較すると例外が起きるので、比較を安全に false にする。
       *
       * @given 自身が未設定、比較対象が 1 円
       * @when isLessThan を比較対象で実行する
       * @then false を返す
       */
      @Test
      void 自身が未設定の場合_falseを返す() {
        assertThat(Money.empty().isLessThan(new Money(1L))).isFalse();
      }

      /**
       * 比較対象が null の場合は false を返すことを確かめる。 null との比較で例外が起きると処理全体が止まるので、false を返す。
       *
       * @given 自身が 1 円、比較対象が null
       * @when isLessThan を null で実行する
       * @then false を返す
       */
      @Test
      void 比較対象がnullの場合_falseを返す() {
        assertThat(new Money(1L).isLessThan(null)).isFalse();
      }

      /**
       * 比較対象が未設定の場合は false を返すことを確かめる。 未設定の比較対象で例外が起きると処理全体が止まるので、false を返す。
       *
       * @given 自身が 1 円、比較対象が未設定
       * @when isLessThan を未設定の金額で実行する
       * @then false を返す
       */
      @Test
      void 比較対象が未設定の場合_falseを返す() {
        assertThat(new Money(1L).isLessThan(Money.empty())).isFalse();
      }
    }
  }

  @Nested
  class isLessThanOrEqualTo {

    @Nested
    class 正常系 {

      /**
       * 自身が比較対象より小さい場合は true を返すことを確かめる。 小さい方の判定が以下の判定と揃わないと境界の判定が食い違うので、以下の判定を固定する。
       *
       * @given 自身が 1 円、比較対象が 2 円
       * @when isLessThanOrEqualTo を比較対象で実行する
       * @then true を返す
       */
      @Test
      void 自身が小さい場合_trueを返す() {
        assertThat(new Money(1L).isLessThanOrEqualTo(new Money(2L))).isTrue();
      }
    }

    @Nested
    class 境界値 {

      /**
       * 自身と比較対象が等しい場合は true を返すことを確かめる。 以下の判定は等号を含むので、同じ金額を以下と判定する必要がある。
       *
       * @given 自身が 1 円、比較対象が 1 円
       * @when isLessThanOrEqualTo を比較対象で実行する
       * @then true を返す
       */
      @Test
      void 同じ金額の場合_trueを返す() {
        assertThat(new Money(1L).isLessThanOrEqualTo(new Money(1L))).isTrue();
      }

      /**
       * 比較対象が null の場合は false を返すことを確かめる。 null との比較で例外が起きると処理全体が止まるので、false を返す。
       *
       * @given 自身が 1 円、比較対象が null
       * @when isLessThanOrEqualTo を null で実行する
       * @then false を返す
       */
      @Test
      void 比較対象がnullの場合_falseを返す() {
        assertThat(new Money(1L).isLessThanOrEqualTo(null)).isFalse();
      }

      /**
       * 自身が比較対象より大きい場合は false を返すことを確かめる。 大きい金額を以下と判定すると判定の向きが逆になるので、false を固定する。
       *
       * @given 自身が 2 円、比較対象が 1 円
       * @when isLessThanOrEqualTo を比較対象で実行する
       * @then false を返す
       */
      @Test
      void 自身が大きい場合_falseを返す() {
        assertThat(new Money(2L).isLessThanOrEqualTo(new Money(1L))).isFalse();
      }

      /**
       * 自身が未設定の場合は false を返すことを確かめる。 未設定の金額の比較で例外が起きると処理全体が止まるので、false を返す。
       *
       * @given 自身が未設定、比較対象が 1 円
       * @when isLessThanOrEqualTo を比較対象で実行する
       * @then false を返す
       */
      @Test
      void 自身が未設定の場合_falseを返す() {
        assertThat(Money.empty().isLessThanOrEqualTo(new Money(1L))).isFalse();
      }

      /**
       * 比較対象が未設定の場合は false を返すことを確かめる。 未設定の比較対象で例外が起きると処理全体が止まるので、false を返す。
       *
       * @given 自身が 1 円、比較対象が未設定
       * @when isLessThanOrEqualTo を未設定の金額で実行する
       * @then false を返す
       */
      @Test
      void 比較対象が未設定の場合_falseを返す() {
        assertThat(new Money(1L).isLessThanOrEqualTo(Money.empty())).isFalse();
      }
    }
  }

  @Nested
  class compareTo {

    @Nested
    class 正常系 {

      /**
       * 自身が比較対象より大きい場合は正の値を返すことを確かめる。 大小の順序が逆になると並び替えが誤るので、大きい金額は 1 を返す。
       *
       * @given 自身が 2 円、比較対象が 1 円
       * @when compareTo を比較対象で実行する
       * @then 1 を返す
       */
      @Test
      void 自身が大きい場合_1を返す() {
        assertThat(new Money(2L).compareTo(new Money(1L))).isEqualTo(1);
      }

      /**
       * 自身が比較対象より小さい場合は負の値を返すことを確かめる。 小さい金額の順序が逆になると並び替えが誤るので、負の値を固定する。
       *
       * @given 自身が 1 円、比較対象が 2 円
       * @when compareTo を比較対象で実行する
       * @then -1 を返す
       */
      @Test
      void 自身が小さい場合_負の1を返す() {
        assertThat(new Money(1L).compareTo(new Money(2L))).isEqualTo(-1);
      }
    }

    @Nested
    class 境界値 {

      /**
       * 両方が未設定の場合は 0 を返すことを確かめる。 未設定同士の順序が揃わないと並び替えが不安定になるので、0 を返す。
       *
       * @given 自身と比較対象が未設定
       * @when compareTo を未設定の金額で実行する
       * @then 0 を返す
       */
      @Test
      void 両方が未設定の場合_0を返す() {
        assertThat(Money.empty().compareTo(Money.empty())).isZero();
      }

      /**
       * 自身だけが未設定の場合は負の値を返すことを確かめる。 未設定の金額を先頭にそろえないと並び替えの結果が変わるので、負の値を返す。
       *
       * @given 自身が未設定、比較対象が 1 円
       * @when compareTo を比較対象で実行する
       * @then -1 を返す
       */
      @Test
      void 自身だけが未設定の場合_負の1を返す() {
        assertThat(Money.empty().compareTo(new Money(1L))).isEqualTo(-1);
      }

      /**
       * 比較対象だけが未設定の場合は正の値を返すことを確かめる。 値のある金額を後ろにそろえないと並び替えの結果が変わるので、1 を返す。
       *
       * @given 自身が 1 円、比較対象が未設定
       * @when compareTo を比較対象で実行する
       * @then 1 を返す
       */
      @Test
      void 比較対象だけが未設定の場合_1を返す() {
        assertThat(new Money(1L).compareTo(Money.empty())).isEqualTo(1);
      }

      /**
       * 金額が等しい場合は 0 を返すことを確かめる。 同じ金額を同じ順位にしないと並び替えの安定性が損なわれるので、0 を返す。
       *
       * @given 自身と比較対象が 1 円
       * @when compareTo を比較対象で実行する
       * @then 0 を返す
       */
      @Test
      void 同じ金額の場合_0を返す() {
        assertThat(new Money(1L).compareTo(new Money(1L))).isZero();
      }
    }
  }

  @Nested
  class equals {

    @Nested
    class 正常系 {

      /**
       * 同じ金額の場合は等しいと判定することを確かめる。 同じ金額を同一と判定しないと重複の検出が漏れるので、等しいと判定する。
       *
       * @given 自身と比較対象が 100 円
       * @when equals を比較対象で実行する
       * @then true を返す
       */
      @Test
      void 同じ金額の場合_trueを返す() {
        assertThat(new Money(100L).equals(new Money(100L))).isTrue();
      }

      /**
       * 両方が未設定の場合は等しいと判定することを確かめる。 未設定同士を別々と判定すると重複の検出が不安定になるので、等しいと判定する。
       *
       * @given 自身と比較対象が未設定
       * @when equals を未設定の金額で実行する
       * @then true を返す
       */
      @Test
      void 両方が未設定の場合_trueを返す() {
        assertThat(Money.empty().equals(Money.empty())).isTrue();
      }

      /**
       * 値の異なる金額は等しくないと判定することを確かめる。 金額の一致判定が値の比較として働かないと、別の金額を同一として扱ってしまう。
       *
       * @given 自身が 100 円、比較対象が 200 円
       * @when equals を比較対象で実行する
       * @then false を返す
       */
      @Test
      void 値の異なる金額の場合_falseを返す() {
        assertThat(new Money(100L).equals(new Money(200L))).isFalse();
      }
    }

    @Nested
    class 異常系 {

      /**
       * 金額以外の型と比較した場合は等しくないと判定することを確かめる。 型の違う値を金額として等しいと判定すると型の混在を見逃すので、false を返す。
       *
       * @given 比較対象が文字列 "100"
       * @when equals を比較対象で実行する
       * @then false を返す
       */
      @Test
      void 金額以外の型の場合_falseを返す() {
        assertThat(new Money(100L).equals("100")).isFalse();
      }
    }

    @Nested
    class 境界値 {

      /**
       * 片方だけが未設定の場合は等しくないと判定することを確かめる。 未設定と値のある金額を等しいと判定すると入力漏れを見逃すので、区別する。
       *
       * @given 自身が未設定、比較対象が 100 円
       * @when equals を比較対象で実行する
       * @then false を返す
       */
      @Test
      void 片方だけが未設定の場合_falseを返す() {
        assertThat(Money.empty().equals(new Money(100L))).isFalse();
      }

      /**
       * 小数点以下の桁数だけが異なる場合は等しいと判定することを確かめる。 小数の桁数だけの違いを別の金額と判定すると同じ金額が重複して扱われるので、数値として等しいと判定する。
       *
       * @given 自身が 100、比較対象が 100.00
       * @when equals を比較対象で実行する
       * @then true を返す
       */
      @Test
      void 小数点以下の桁数だけが異なる場合_trueを返す() {
        assertThat(new Money(new BigDecimal("100")).equals(new Money(new BigDecimal("100.00"))))
            .isTrue();
      }

      /**
       * 比較対象だけが未設定の場合は等しくないと判定することを確かめる。 値のある金額と未設定を等しいと判定すると入力漏れを見逃すので、区別する。
       *
       * @given 自身が 100 円、比較対象が未設定
       * @when equals を比較対象で実行する
       * @then false を返す
       */
      @Test
      void 比較対象だけが未設定の場合_falseを返す() {
        assertThat(new Money(100L).equals(Money.empty())).isFalse();
      }
    }
  }

  @Nested
  class hashCode {

    @Nested
    class 正常系 {

      /**
       * 等しい金額は同じハッシュコードを返すことを確かめる。 ハッシュを使うコレクションで同じ金額を一つにまとめるので、同じハッシュコードを返す必要がある。
       *
       * @given 自身と比較対象が 100 円
       * @when hashCode を実行する
       * @then 比較対象と同じハッシュコードを返す
       */
      @Test
      void 同じ金額の場合_同じハッシュコードを返す() {
        assertThat(new Money(100L).hashCode()).isEqualTo(new Money(100L).hashCode());
      }

      /**
       * 小数点以下の桁数だけが異なる等しい金額は同じハッシュコードを返すことを確かめる。 equals と整合しないと HashMap や HashSet
       * で同じ金額が別物になるので、同じハッシュコードにする。
       *
       * @given 自身が 100、比較対象が 100.00
       * @when hashCode を実行する
       * @then 同じハッシュコードを返す
       */
      @Test
      void 小数点以下の桁数だけが異なる場合_同じハッシュコードを返す() {
        assertThat(new Money(new java.math.BigDecimal("100")).hashCode())
            .isEqualTo(new Money(new java.math.BigDecimal("100.00")).hashCode());
      }
    }

    @Nested
    class 境界値 {

      /**
       * 未設定の金額のハッシュコードは 0 を返すことを確かめる。 未設定の金額で例外が起きると処理全体が止まるので、ハッシュコードを 0 にする。
       *
       * @given 値が未設定
       * @when hashCode を実行する
       * @then 0 を返す
       */
      @Test
      void 値が未設定の場合_0を返す() {
        assertThat(Money.empty().hashCode()).isZero();
      }
    }
  }

  @Nested
  class toString {

    @Nested
    class 正常系 {

      /**
       * 値のある金額は万円の表記の文字列を返すことを確かめる。 ログや画面で金額を読める形にすると確認が速くなるので、万円の表記を固定する。
       *
       * @given 値が 1000000 円
       * @when toString を実行する
       * @then "100万円" を返す
       */
      @Test
      void 値が設定されている場合_万円の表記を返す() {
        assertThat(new Money(1_000_000L).toString()).isEqualTo("100万円");
      }
    }

    @Nested
    class 境界値 {

      /**
       * 未設定の金額は "empty" の文字列を返すことを確かめる。 未設定の金額をログで区別しないと値の欠落を見逃すので、empty と表示する。
       *
       * @given 値が未設定
       * @when toString を実行する
       * @then "empty" を返す
       */
      @Test
      void 値が未設定の場合_emptyを返す() {
        assertThat(Money.empty().toString()).isEqualTo("empty");
      }
    }
  }

  @Nested
  class multiply {

    @Nested
    class 正常系 {

      /**
       * 金額に指定の数値を掛けた結果を返すことを確かめる。 掛け算の結果が変わると金額の誤りにつながるので、掛けた結果を固定する。
       *
       * @given 値が 1000000 円
       * @when multiply を乗数 1.5 で実行する
       * @then 1500000 円を返す
       */
      @Test
      void 値が設定されている場合_掛けた結果を返す() {
        assertThat(new Money(1_000_000L).multiply(1.5).toYenValue()).isEqualTo(1_500_000L);
      }
    }

    @Nested
    class 境界値 {

      /**
       * 未設定の金額に掛けると未設定の金額を返すことを確かめる。 未設定の金額の計算で例外が起きると処理全体が止まるので、未設定の金額を返す。
       *
       * @given 値が未設定
       * @when multiply を乗数 1.5 で実行する
       * @then 未設定の金額を返す
       */
      @Test
      void 値が未設定の場合_未設定の金額を返す() {
        assertThat(Money.empty().multiply(1.5).isEmpty()).isTrue();
      }
    }
  }

  @Nested
  class toSummaryPriceLine {

    @Nested
    class 正常系 {

      /**
       * 単価不問でない目標単価は、目標単価の金額をそのまま載せた単価行を返すことを確かめる。 指定された単価は、提案時にそのまま見せる金額だからである。
       *
       * @given 目標単価が 1000000 円、単価が 1500000 円、係数が 0.9
       * @when toSummaryPriceLine を単価 1500000 円、係数 0.9 で実行する
       * @then 目標単価の金額の単価行を返す
       */
      @Test
      void 単価不問でない目標単価の場合_目標単価の金額の単価行を返す() {
        String line = new Money(1_000_000L).toSummaryPriceLine(new Money(1_500_000L), 0.9);

        assertThat(line).isEqualTo("■単価: " + new Money(1_000_000L).toJapaneseFormat() + "\n");
      }

      /**
       * 単価不問の目標単価は、単価に係数を掛けた金額の単価行を返すことを確かめる。 単価不問の案件は要員の単価を基準に提案するためである。
       *
       * @given 目標単価が単価不問の 9990000 円、単価が 1500000 円、係数が 0.9
       * @when toSummaryPriceLine を単価 1500000 円、係数 0.9 で実行する
       * @then 単価に係数を掛けた金額の単価行を返す
       */
      @Test
      void 単価不問の目標単価で単価がある場合_係数を掛けた金額の単価行を返す() {
        String line = Money.NEGOTIABLE_PRICE.toSummaryPriceLine(new Money(1_500_000L), 0.9);

        assertThat(line)
            .isEqualTo("■単価: " + new Money(1_500_000L).multiply(0.9).toJapaneseFormat() + "\n");
      }
    }

    @Nested
    class 境界値 {

      /**
       * 目標単価が未設定の場合は単価の行を作らないことを確かめる。 目標単価を指定しない検索では、単価の行を要約に出さないためである。
       *
       * @given 目標単価が未設定
       * @when toSummaryPriceLine を単価 1500000 円、係数 0.9 で実行する
       * @then 空の文字列を返す
       */
      @Test
      void 目標単価が未設定の場合_空の文字列を返す() {
        String line = Money.empty().toSummaryPriceLine(new Money(1_500_000L), 0.9);

        assertThat(line).isEqualTo("");
      }

      /**
       * 単価不問で単価が未設定の場合は単価の行を作らないことを確かめる。 掛ける単価が無いと金額を計算できないためである。
       *
       * @given 目標単価が単価不問の 9990000 円、単価が null
       * @when toSummaryPriceLine を単価 null、係数 0.9 で実行する
       * @then 空の文字列を返す
       */
      @Test
      void 単価不問で単価がnullの場合_空の文字列を返す() {
        String line = Money.NEGOTIABLE_PRICE.toSummaryPriceLine(null, 0.9);

        assertThat(line).isEqualTo("");
      }
    }
  }
}
