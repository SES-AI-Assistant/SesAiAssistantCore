package copel.sesproductpackage.core.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import copel.sesproductpackage.tools.testsupport.clock.FixedClock;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/** {@link OriginalDateTime} の単体テスト。 */
@FixedClock("2026-01-15T12:04:05+09:00")
class OriginalDateTimeTest {

  /**
   * 2026 年 1 月 15 日 12 時 34 分 56 秒の日時を作る。
   *
   * @return テスト用の日時
   */
  private static OriginalDateTime 基準の日時() {
    return new OriginalDateTime(2026, 1, 15, 12, 34, 56);
  }

  @Nested
  class コンストラクタ {

    @Nested
    class 正常系 {

      /**
       * 引数なしで生成すると、時計が示す現在日時で初期化されることを確かめる。 現在日時の取得を時計に統一しないとテストで時刻を固定できないので、時計の値を使う。
       *
       * @given 時計が 2026-01-15T12:04:05+09:00 に固定されている
       * @when 引数なしで生成する
       * @then "2026-01-15 12:04:05" の日時を返す
       */
      @Test
      void 現在時刻の時計で生成した場合_固定した時刻の文字列を返す() {
        assertThat(new OriginalDateTime().toString()).isEqualTo("2026-01-15 12:04:05");
      }
    }
  }

  @Nested
  class コンストラクタ_String {

    @Nested
    class 正常系 {

      /**
       * 対応する書式の日時の文字列を解析することを確かめる。 DB や入力から受け取る複数の書式を同じ日時として扱うことを守る。
       *
       * @given 日時の文字列が対応する書式で書かれている
       * @when 文字列を指定して生成する
       * @then 秒までの日時の文字列を返す
       */
      @ParameterizedTest
      @CsvSource({
        "2026-01-15 12:34:56.123456, 2026-01-15 12:34:56",
        "2026-01-15 12:34:56.1, 2026-01-15 12:34:56",
        "2026-01-15 12:34:56.123, 2026-01-15 12:34:56",
        "2026-01-15 12:34:56, 2026-01-15 12:34:56",
        "2026-01-15 12:34, 2026-01-15 12:34:00",
        "2026-01-15, 2026-01-15 00:00:00",
        "2026-01, 2026-01-01 00:00:00",
        "2026/01/15 12:34:56.123456, 2026-01-15 12:34:56",
        "2026/01/15 12:34:56.1, 2026-01-15 12:34:56",
        "2026/01/15 12:34:56, 2026-01-15 12:34:56",
        "2026/01/15 12:34, 2026-01-15 12:34:00",
        "2026/01/15, 2026-01-15 00:00:00",
        "2026/01, 2026-01-01 00:00:00"
      })
      void 日時の文字列を指定した場合_解析した日時の文字列を返す(final String input, final String expected) {
        assertThat(new OriginalDateTime(input).toString()).isEqualTo(expected);
      }
    }

    @Nested
    class 異常系 {

      /**
       * どの書式にも当てはまらない文字列は空の日時になることを確かめる。 解析できない入力を日時として扱うと誤った日付が保存されるので、空の日時にする。
       *
       * @given 日時の文字列が "abc" または存在しない月の "2026-13-01"
       * @when 文字列を指定して生成する
       * @then 空の日時を返す
       */
      @ValueSource(strings = {"abc", "2026-13-01"})
      @ParameterizedTest
      void 解析できない文字列の場合_空の日時を返す(final String input) {
        assertThat(new OriginalDateTime(input).isEmpty()).isTrue();
      }
    }

    @Nested
    class 境界値 {

      /**
       * 文字列が null の場合は空の日時を返すことを確かめる。 日時の入力が無い場合を未設定として扱わないと誤った日付が保存されるので、空の日時にする。
       *
       * @given 日時の文字列が null
       * @when 文字列を null で指定して生成する
       * @then 空の日時を返す
       */
      @Test
      void 日時の文字列がnullの場合_空の日時を返す() {
        assertThat(new OriginalDateTime((String) null).isEmpty()).isTrue();
      }
    }
  }

  @Nested
  class コンストラクタ_int_int_int_int_int_int {

    @Nested
    class 正常系 {

      /**
       * 年月日時分秒を指定して生成することを確かめる。 数値で指定した日時の値が変わると保存される日時がずれるので、そのまま保持する。
       *
       * @given 年が 2026、月が 1、日が 15、時が 12、分が 34、秒が 56
       * @when 年月日時分秒を指定して生成する
       * @then "2026-01-15 12:34:56" の日時を返す
       */
      @Test
      void 年月日時分秒を指定した場合_その日時の文字列を返す() {
        assertThat(基準の日時().toString()).isEqualTo("2026-01-15 12:34:56");
      }
    }

    @Nested
    class 異常系 {

      /**
       * 存在しない月を指定すると例外を投げることを確かめる。 不正な月を補正すると誤った日付が保存されるので、例外を投げる。
       *
       * @given 月が 13
       * @when 年月日時分秒を指定して生成する
       * @then DateTimeException を投げる
       */
      @Test
      void 月が13の場合_DateTimeExceptionを投げる() {
        assertThatThrownBy(() -> new OriginalDateTime(2026, 13, 1, 0, 0, 0))
            .isInstanceOf(DateTimeException.class);
      }
    }
  }

  @Nested
  class コンストラクタ_Date {

    @Nested
    class 正常系 {

      /**
       * SQL の日付を、その日の 0 時の日時として生成することを確かめる。 DB の日付は時刻を持たないので、その日の 0 時として扱う必要がある。
       *
       * @given SQL の日付が 2026-01-15
       * @when SQL の日付を指定して生成する
       * @then "2026-01-15 00:00:00" の日時を返す
       */
      @Test
      void 日付を指定した場合_その日の0時の日時を返す() {
        assertThat(new OriginalDateTime(Date.valueOf("2026-01-15")).toString())
            .isEqualTo("2026-01-15 00:00:00");
      }
    }

    @Nested
    class 境界値 {

      /**
       * SQL の日付が null の場合は空の日時を返すことを確かめる。 DB の NULL を未設定として扱わないと誤った日付が入るので、空の日時にする。
       *
       * @given SQL の日付が null
       * @when SQL の日付を null で指定して生成する
       * @then 空の日時を返す
       */
      @Test
      void 日付がnullの場合_空の日時を返す() {
        assertThat(new OriginalDateTime((Date) null).isEmpty()).isTrue();
      }
    }
  }

  @Nested
  class コンストラクタ_Timestamp {

    @Nested
    class 正常系 {

      /**
       * SQL のタイムスタンプを日時に変換することを確かめる。 DB のタイムスタンプは秒まで保持されるので、その日時の文字列を返す。
       *
       * @given SQL のタイムスタンプが 2026-01-15 12:34:56
       * @when タイムスタンプを指定して生成する
       * @then "2026-01-15 12:34:56" の日時を返す
       */
      @Test
      void タイムスタンプを指定した場合_その日時の文字列を返す() {
        assertThat(new OriginalDateTime(Timestamp.valueOf("2026-01-15 12:34:56")).toString())
            .isEqualTo("2026-01-15 12:34:56");
      }
    }

    @Nested
    class 境界値 {

      /**
       * SQL のタイムスタンプが null の場合は空の日時を返すことを確かめる。 DB の NULL を未設定として扱わないと誤った日時が入るので、空の日時にする。
       *
       * @given SQL のタイムスタンプが null
       * @when タイムスタンプを null で指定して生成する
       * @then 空の日時を返す
       */
      @Test
      void タイムスタンプがnullの場合_空の日時を返す() {
        assertThat(new OriginalDateTime((Timestamp) null).isEmpty()).isTrue();
      }
    }
  }

  @Nested
  class toString {

    @Nested
    class 境界値 {

      /**
       * 空の日時の文字列表現は null であることを確かめる。 未設定の日時を文字列にすると誤った日時が画面に出るので、null を返す。
       *
       * @given 空の日時
       * @when toString を実行する
       * @then null を返す
       */
      @Test
      void 日時が空の場合_nullを返す() {
        assertThat(new OriginalDateTime((String) null).toString()).isNull();
      }
    }
  }

  @Nested
  class compareTo {

    @Nested
    class 正常系 {

      /**
       * 自身が比較対象より後の日時の場合は 1 を返すことを確かめる。 日時の前後関係を並び順に反映しないと並び替えが誤るので、1 を返す。
       *
       * @given 自身が 2026-01-15 12:34:56、比較対象が 2026-01-14 00:00:00
       * @when compareTo を比較対象で実行する
       * @then 1 を返す
       */
      @Test
      void 自身が後の日時の場合_1を返す() {
        OriginalDateTime earlier = new OriginalDateTime(2026, 1, 14, 0, 0, 0);

        assertThat(基準の日時().compareTo(earlier)).isEqualTo(1);
      }

      /**
       * 自身が比較対象より前の日時の場合は負の値を返すことを確かめる。 日時の前後関係を並び順に反映しないと並び替えが誤るので、負の値を返す。
       *
       * @given 自身が 2026-01-14 00:00:00、比較対象が 2026-01-15 12:34:56
       * @when compareTo を比較対象で実行する
       * @then 負の1を返す
       */
      @Test
      void 自身が前の日時の場合_負の1を返す() {
        OriginalDateTime earlier = new OriginalDateTime(2026, 1, 14, 0, 0, 0);

        assertThat(earlier.compareTo(基準の日時())).isEqualTo(-1);
      }

      /**
       * 同じ日時の場合は 0 を返すことを確かめる。 同じ日時を同じ順位にしないと並び替えが不安定になるので、0 を返す。
       *
       * @given 自身と比較対象が 2026-01-15 12:34:56
       * @when compareTo を比較対象で実行する
       * @then 0 を返す
       */
      @Test
      void 同じ日時の場合_0を返す() {
        assertThat(基準の日時().compareTo(基準の日時())).isZero();
      }
    }

    @Nested
    class 境界値 {

      /**
       * 比較対象が空の日時の場合は、値のある自身を後の日時として 1 を返すことを確かめる。 空の日時を最も古い値として扱うと並び替えの結果が決まるので、比較対象が空の場合は 1 を返す。
       *
       * @given 自身が 2026-01-15 12:34:56、比較対象が空の日時
       * @when compareTo を空の日時で実行する
       * @then 1 を返す
       */
      @Test
      void 比較対象が空の日時の場合_1を返す() {
        assertThat(基準の日時().compareTo(new OriginalDateTime((String) null))).isEqualTo(1);
      }

      /**
       * 両方が空の場合は 0 を返すことを確かめる。 空の日時同士を別々と判定すると並び替えが不安定になるので、0 を返す。
       *
       * @given 自身が空の日時、比較対象が null
       * @when compareTo を null で実行する
       * @then 0 を返す
       */
      @Test
      void 両方が空の場合_0を返す() {
        assertThat(new OriginalDateTime((String) null).compareTo(null)).isZero();
      }

      /**
       * 比較対象が null の場合は、値のある自身を後の日時として 1 を返すことを確かめる。 null を比較で例外にすると処理全体が止まるので、null を最も古い値として 1
       * を返す。
       *
       * @given 自身が 2026-01-15 12:34:56、比較対象が null
       * @when compareTo を null で実行する
       * @then 1 を返す
       */
      @Test
      void 比較対象がnullの場合_1を返す() {
        assertThat(基準の日時().compareTo(null)).isEqualTo(1);
      }

      /**
       * 自身が空の場合は負の値を返すことを確かめる。 空の日時を値のある日時より前に並べないと並び替えの結果が決まらないので、負の値を返す。
       *
       * @given 自身が空の日時、比較対象が 2026-01-15 12:34:56
       * @when compareTo を比較対象で実行する
       * @then 負の1を返す
       */
      @Test
      void 自身が空の場合_負の1を返す() {
        assertThat(new OriginalDateTime((String) null).compareTo(基準の日時())).isEqualTo(-1);
      }
    }
  }

  @Nested
  class isEmpty {

    @Nested
    class 正常系 {

      /**
       * 日時が設定されている場合は空ではないと判定することを確かめる。 設定済みの日時を未設定と誤認すると有効な日時が捨てられるので、false を返す。
       *
       * @given 日時が 2026-01-15 12:34:56
       * @when isEmpty を実行する
       * @then false を返す
       */
      @Test
      void 日時がある場合_falseを返す() {
        assertThat(基準の日時().isEmpty()).isFalse();
      }
    }

    @Nested
    class 境界値 {

      /**
       * 日時が未設定の場合は空と判定することを確かめる。 未設定の日時を空として扱わないと値の欠落を見逃すので、true を返す。
       *
       * @given 日時が未設定
       * @when isEmpty を実行する
       * @then true を返す
       */
      @Test
      void 日時が空の場合_trueを返す() {
        assertThat(new OriginalDateTime((String) null).isEmpty()).isTrue();
      }
    }
  }

  @Nested
  class equals {

    @Nested
    class 正常系 {

      /**
       * 同じインスタンスと比較した場合は等しいと判定することを確かめる。 自分自身との比較を等しくしないと等価の性質が崩れるので、true を返す。
       *
       * @given 同じインスタンス
       * @when equals を自分自身で実行する
       * @then true を返す
       */
      @Test
      void 同じインスタンスの場合_trueを返す() {
        OriginalDateTime dateTime = 基準の日時();

        assertThat(dateTime.equals(dateTime)).isTrue();
      }

      /**
       * 日時が同じ別のインスタンスは等しいと判定することを確かめる。 値が同じ日時を同一と判定しないと重複の検出が漏れるので、true を返す。
       *
       * @given 日時が 2026-01-15 12:34:56 の別のインスタンス
       * @when equals を比較対象で実行する
       * @then true を返す
       */
      @Test
      void 日時が同じ場合_trueを返す() {
        assertThat(基準の日時().equals(基準の日時())).isTrue();
      }
    }

    @Nested
    class 異常系 {

      /**
       * 日時が異なる場合は等しくないと判定することを確かめる。 別の日時を同一と判定すると誤った紐づけが起きるので、false を返す。
       *
       * @given 自身が 2026-01-15 12:34:56、比較対象が 2026-01-16 00:00:00
       * @when equals を比較対象で実行する
       * @then false を返す
       */
      @Test
      void 日時が異なる場合_falseを返す() {
        assertThat(基準の日時().equals(new OriginalDateTime(2026, 1, 16, 0, 0, 0))).isFalse();
      }

      /**
       * 派生クラスのインスタンスは基底クラスと等しくないと判定することを確かめる。 派生クラスを同じ日時と判定すると型の違いを見逃すので、false を返す。
       *
       * @given 日時が同じ派生クラスのインスタンス
       * @when equals を派生クラスのインスタンスで実行する
       * @then false を返す
       */
      @Test
      void 派生クラスのインスタンスの場合_falseを返す() {
        OriginalDateTime subclass = new OriginalDateTime(2026, 1, 15, 12, 34, 56) {};

        assertThat(基準の日時().equals(subclass)).isFalse();
      }
    }

    @Nested
    class 境界値 {

      /**
       * 比較対象が null の場合は等しくないと判定することを確かめる。 null との比較で例外が起きると処理全体が止まるので、false を返す。
       *
       * @given 比較対象が null
       * @when equals を null で実行する
       * @then false を返す
       */
      @Test
      void 比較対象がnullの場合_falseを返す() {
        assertThat(基準の日時().equals(null)).isFalse();
      }

      /**
       * 両方が空の別のインスタンスは等しいと判定することを確かめる。 compareTo で空を最も古い日時として 0 を返すことと揃え、equals と compareTo
       * で判定を食い違わせない。
       *
       * @given 自身と比較対象が別インスタンスの空の日時
       * @when equals を比較対象で実行する
       * @then true を返す
       */
      @Test
      void 両方が空の場合_trueを返す() {
        OriginalDateTime first = new OriginalDateTime((String) null);
        OriginalDateTime second = new OriginalDateTime((String) null);

        assertThat(first.equals(second)).isTrue();
      }

      /**
       * 比較対象だけが空の場合は等しくないと判定することを確かめる。 空の日時と値のある日時を区別しないと欠落を見逃すので、false を返す。
       *
       * @given 自身が 2026-01-15 12:34:56、比較対象が空の日時
       * @when equals を比較対象で実行する
       * @then false を返す
       */
      @Test
      void 比較対象が空の場合_falseを返す() {
        assertThat(基準の日時().equals(new OriginalDateTime((String) null))).isFalse();
      }

      /**
       * 自身だけが空の場合は等しくないと判定することを確かめる。 空の日時と値のある日時を区別しないと欠落を見逃すので、false を返す。
       *
       * @given 自身が空の日時、比較対象が 2026-01-15 12:34:56
       * @when equals を比較対象で実行する
       * @then false を返す
       */
      @Test
      void 自身が空の場合_falseを返す() {
        assertThat(new OriginalDateTime((String) null).equals(基準の日時())).isFalse();
      }
    }
  }

  @Nested
  class hashCode {

    @Nested
    class 正常系 {

      /**
       * 等しい日時は同じハッシュコードを返すことを確かめる。 equals で等しい日時が別のハッシュになると HashSet で重複が残るので、同じハッシュコードにする。
       *
       * @given 日時が 2026-01-15 12:34:56 の2つのインスタンス
       * @when hashCode を実行する
       * @then 同じハッシュコードを返す
       */
      @Test
      void 日時が同じ場合_同じハッシュコードを返す() {
        assertThat(基準の日時().hashCode()).isEqualTo(基準の日時().hashCode());
      }
    }

    @Nested
    class 境界値 {

      /**
       * 空の日時のハッシュコードは 0 を返すことを確かめる。 空の日時で例外が起きると処理全体が止まるので、ハッシュコードを 0 にする。
       *
       * @given 日時が空
       * @when hashCode を実行する
       * @then 0 を返す
       */
      @Test
      void 日時が空の場合_0を返す() {
        assertThat(new OriginalDateTime((String) null).hashCode()).isZero();
      }

      /**
       * 両方が空の場合は同じハッシュコードを返すことを確かめる。 equals で両方が空を等しいとするため、HashMap や HashSet で同一に扱えるように揃える。
       *
       * @given 自身と比較対象が別インスタンスの空の日時
       * @when hashCode を実行する
       * @then 同じハッシュコードを返す
       */
      @Test
      void 両方が空の場合_同じハッシュコードを返す() {
        assertThat(new OriginalDateTime((String) null).hashCode())
            .isEqualTo(new OriginalDateTime((String) null).hashCode());
      }
    }
  }

  @Nested
  class getYYYYMM {

    @Nested
    class 正常系 {

      /**
       * 日時を yyyyMM の形式で返すことを確かめる。 年月の集計キーの形式が変わると既存の集計が一致しなくなるので、形式を固定する。
       *
       * @given 日時が 2026-01-15 12:34:56
       * @when getYYYYMM を実行する
       * @then "202601" を返す
       */
      @Test
      void 日時が2026_01_15の場合_202601を返す() {
        assertThat(基準の日時().getYYYYMM()).isEqualTo("202601");
      }
    }

    @Nested
    class 境界値 {

      /**
       * 日時が空の場合は null を返すことを確かめる。 未設定の日時を集計キーにすると誤った集計が作られるので、null を返す。
       *
       * @given 日時が空
       * @when getYYYYMM を実行する
       * @then null を返す
       */
      @Test
      void 日時が空の場合_nullを返す() {
        assertThat(new OriginalDateTime((String) null).getYYYYMM()).isNull();
      }
    }
  }

  @Nested
  class getYyyyMmDd {

    @Nested
    class 正常系 {

      /**
       * 日時を yyyyMMdd の形式で返すことを確かめる。 日付の連番キーの形式が変わると既存のキーが一致しなくなるので、形式を固定する。
       *
       * @given 日時が 2026-01-15 12:34:56
       * @when getYyyyMmDd を実行する
       * @then "20260115" を返す
       */
      @Test
      void 日時が2026_01_15の場合_20260115を返す() {
        assertThat(基準の日時().getYyyyMmDd()).isEqualTo("20260115");
      }
    }

    @Nested
    class 境界値 {

      /**
       * 日時が空の場合は null を返すことを確かめる。 未設定の日時を日付のキーにすると誤った集計が作られるので、null を返す。
       *
       * @given 日時が空
       * @when getYyyyMmDd を実行する
       * @then null を返す
       */
      @Test
      void 日時が空の場合_nullを返す() {
        assertThat(new OriginalDateTime((String) null).getYyyyMmDd()).isNull();
      }
    }
  }

  @Nested
  class toLocalDateTime {

    @Nested
    class 正常系 {

      /**
       * 日時を LocalDateTime として返すことを確かめる。 標準の日時型へ変換すると他のコードと型を共有できるので、同じ日時を返す。
       *
       * @given 日時が 2026-01-15 12:34:56
       * @when toLocalDateTime を実行する
       * @then 同じ日時の LocalDateTime を返す
       */
      @Test
      void 日時が設定されている場合_同じ日時のLocalDateTimeを返す() {
        assertThat(基準の日時().toLocalDateTime()).isEqualTo(LocalDateTime.of(2026, 1, 15, 12, 34, 56));
      }
    }

    @Nested
    class 境界値 {

      /**
       * 日時が空の場合は null を返すことを確かめる。 未設定の日時を値として扱うと誤った日時の計算が起きるので、null を返す。
       *
       * @given 日時が空
       * @when toLocalDateTime を実行する
       * @then null を返す
       */
      @Test
      void 日時が空の場合_nullを返す() {
        assertThat(new OriginalDateTime((String) null).toLocalDateTime()).isNull();
      }
    }
  }

  @Nested
  class toLocalDate {

    @Nested
    class 正常系 {

      /**
       * 日時の日付部分を LocalDate として返すことを確かめる。 時刻を除いた日付だけが必要な場面があるので、日付のみの値を取り出す。
       *
       * @given 日時が 2026-01-15 12:34:56
       * @when toLocalDate を実行する
       * @then 2026-01-15 の LocalDate を返す
       */
      @Test
      void 日時が設定されている場合_日付のLocalDateを返す() {
        assertThat(基準の日時().toLocalDate()).isEqualTo(LocalDate.of(2026, 1, 15));
      }
    }

    @Nested
    class 境界値 {

      /**
       * 日時が空の場合は null を返すことを確かめる。 未設定の日付を値として扱うと誤った計算が起きるので、null を返す。
       *
       * @given 日時が空
       * @when toLocalDate を実行する
       * @then null を返す
       */
      @Test
      void 日時が空の場合_nullを返す() {
        assertThat(new OriginalDateTime((String) null).toLocalDate()).isNull();
      }
    }
  }

  @Nested
  class toTimestamp {

    @Nested
    class 正常系 {

      /**
       * 日時を SQL のタイムスタンプとして返すことを確かめる。 DB の保存用に変換するので、タイムスタンプの値を一致させる必要がある。
       *
       * @given 日時が 2026-01-15 12:34:56
       * @when toTimestamp を実行する
       * @then 同じ日時のタイムスタンプを返す
       */
      @Test
      void 日時が設定されている場合_同じ日時のタイムスタンプを返す() {
        assertThat(基準の日時().toTimestamp()).isEqualTo(Timestamp.valueOf("2026-01-15 12:34:56"));
      }
    }

    @Nested
    class 境界値 {

      /**
       * 日時が空の場合は null を返すことを確かめる。 未設定の日時を DB に保存すると誤った日時が入るので、null を返す。
       *
       * @given 日時が空
       * @when toTimestamp を実行する
       * @then null を返す
       */
      @Test
      void 日時が空の場合_nullを返す() {
        assertThat(new OriginalDateTime((String) null).toTimestamp()).isNull();
      }
    }
  }

  @Nested
  class betweenDays {

    @Nested
    class 正常系 {

      /**
       * 比較対象が後の日付の場合は、その日数の差を正の値で返すことを確かめる。 日数の差の符号を取り違えると期間の計算が逆になるので、後の日付との差を正の値で固定する。
       *
       * @given 自身が 2026-01-01、比較対象が 2026-01-08
       * @when betweenDays を比較対象で実行する
       * @then 7 を返す
       */
      @Test
      void 比較対象が7日後の場合_7を返す() {
        OriginalDateTime start = new OriginalDateTime(2026, 1, 1, 0, 0, 0);

        assertThat(start.betweenDays(new OriginalDateTime(2026, 1, 8, 0, 0, 0))).isEqualTo(7);
      }

      /**
       * 比較対象が前の日付の場合は、その日数の差を負の値で返すことを確かめる。 前の日付との差の符号を固定しないと期間計算が逆になるので、負の値にする。
       *
       * @given 自身が 2026-01-01、比較対象が 2025-12-25
       * @when betweenDays を比較対象で実行する
       * @then 負の7を返す
       */
      @Test
      void 比較対象が7日前の場合_負の7を返す() {
        OriginalDateTime start = new OriginalDateTime(2026, 1, 1, 0, 0, 0);

        assertThat(start.betweenDays(new OriginalDateTime(2025, 12, 25, 0, 0, 0))).isEqualTo(-7);
      }
    }

    @Nested
    class 境界値 {

      /**
       * 比較対象が null の場合は 0 を返すことを確かめる。 比較できない場合の既定値を変えると呼び出し側の判定が変わるので、0 を固定する。
       *
       * @given 比較対象が null
       * @when betweenDays を null で実行する
       * @then 0 を返す
       */
      @Test
      void 比較対象がnullの場合_0を返す() {
        assertThat(基準の日時().betweenDays(null)).isZero();
      }

      /**
       * 比較対象が空の場合は 0 を返すことを確かめる。 空の日時との差は計算できないので、既定値の 0 を固定する。
       *
       * @given 比較対象が空の日時
       * @when betweenDays を空の日時で実行する
       * @then 0 を返す
       */
      @Test
      void 比較対象が空の場合_0を返す() {
        assertThat(基準の日時().betweenDays(new OriginalDateTime((String) null))).isZero();
      }

      /**
       * 自身が空の場合は 0 を返すことを確かめる。 空の日時からの差を計算すると誤った値になるので、0 を返す。
       *
       * @given 自身が空の日時
       * @when betweenDays を比較対象で実行する
       * @then 0 を返す
       */
      @Test
      void 自身が空の場合_0を返す() {
        assertThat(new OriginalDateTime((String) null).betweenDays(基準の日時())).isZero();
      }
    }
  }

  @Nested
  class plusDays {

    @Nested
    class 正常系 {

      /**
       * 日数を加えると、その日数後の日時になることを確かめる。 日付の加算で時刻が変わると時刻のずれが起きるので、時刻を保つ。
       *
       * @given 日時が 2026-01-15 12:34:56
       * @when plusDays を 3 で実行する
       * @then 2026-01-18 12:34:56 の日時を返す
       */
      @Test
      void 日数を3日加えた場合_3日後の日時を返す() {
        OriginalDateTime dateTime = 基準の日時();

        dateTime.plusDays(3);

        assertThat(dateTime.toString()).isEqualTo("2026-01-18 12:34:56");
      }
    }

    @Nested
    class 境界値 {

      /**
       * 日時が空の場合は何も変えないことを確かめる。 未設定の日時に日数を加えると値が生まれて誤った日時になるので、空のままにする。
       *
       * @given 日時が空
       * @when plusDays を 3 で実行する
       * @then 空のままの日時を返す
       */
      @Test
      void 日時が空の場合_空のままの日時を返す() {
        OriginalDateTime dateTime = new OriginalDateTime((String) null);

        dateTime.plusDays(3);

        assertThat(dateTime.isEmpty()).isTrue();
      }
    }
  }

  @Nested
  class minusMinutes {

    @Nested
    class 正常系 {

      /**
       * 分数を引くと、その分数前の日時になることを確かめる。 分単位の減算が崩れると時刻がずれるので、分の減算を固定する。
       *
       * @given 日時が 2026-01-15 12:34:56
       * @when minusMinutes を 30 で実行する
       * @then 2026-01-15 12:04:56 の日時を返す
       */
      @Test
      void 分数を30分引いた場合_30分前の日時を返す() {
        OriginalDateTime dateTime = 基準の日時();

        dateTime.minusMinutes(30);

        assertThat(dateTime.toString()).isEqualTo("2026-01-15 12:04:56");
      }
    }

    @Nested
    class 境界値 {

      /**
       * 日時が空の場合は何も変えないことを確かめる。 未設定の日時に時刻を引くと値が生まれて誤った日時になるので、空のままにする。
       *
       * @given 日時が空
       * @when minusMinutes を 30 で実行する
       * @then 空のままの日時を返す
       */
      @Test
      void 日時が空の場合_空のままの日時を返す() {
        OriginalDateTime dateTime = new OriginalDateTime((String) null);

        dateTime.minusMinutes(30);

        assertThat(dateTime.isEmpty()).isTrue();
      }
    }
  }

  @Nested
  class fromMonth {

    @Nested
    class 正常系 {

      /**
       * 年と月を指定すると、その月の 1 日 0 時の日時を返すことを確かめる。 月初の基準を崩すと月の集計が変わるので、月初の日時を固定する。
       *
       * @given 年が 2026、月が 3
       * @when fromMonth を年と月で実行する
       * @then "2026-03-01 00:00:00" の日時を返す
       */
      @Test
      void 月を3月に指定した場合_3月1日0時の日時を返す() {
        assertThat(OriginalDateTime.fromMonth(2026, 3).toString()).isEqualTo("2026-03-01 00:00:00");
      }
    }

    @Nested
    class 境界値 {

      /**
       * 月に -1 を指定すると、時計が示す現在日時を返すことを確かめる。 即日を表す月の値を取り違えると誤った日時が作られるので、現在日時を返す。
       *
       * @given 年が 2026、月が -1、時計が 2026-01-15T12:04:05+09:00 に固定されている
       * @when fromMonth を年と月で実行する
       * @then 現在日時の "2026-01-15 12:04:05" を返す
       */
      @Test
      void 月がマイナス1の場合_現在日時を返す() {
        assertThat(OriginalDateTime.fromMonth(2026, -1).toString())
            .isEqualTo("2026-01-15 12:04:05");
      }
    }
  }

  @Nested
  class month {

    @Nested
    class 正常系 {

      /**
       * 日時の月を M月 の形式で返すことを確かめる。 月の表示が変わると通知文の形式が崩れるので、M月の表記を固定する。
       *
       * @given 日時が 2026-01-15 12:34:56
       * @when month を実行する
       * @then "1月" を返す
       */
      @Test
      void 日時が1月の場合_1月を返す() {
        assertThat(基準の日時().month()).isEqualTo("1月");
      }
    }

    @Nested
    class 境界値 {

      /**
       * 日時が空の場合は 未定 を返すことを確かめる。 開始月が無い場合に値を出すと誤った月が通知されるので、未定と表示する。
       *
       * @given 日時が空
       * @when month を実行する
       * @then "未定" を返す
       */
      @Test
      void 日時が空の場合_未定を返す() {
        assertThat(new OriginalDateTime((String) null).month()).isEqualTo("未定");
      }
    }
  }

  @Nested
  class fromYearMonthText {

    @Nested
    class 正常系 {

      /**
       * 年と月の区切りが書かれた文字列の場合に、その年月の日時を返すことを確かめる。 開始年月は稼働時期の比較に使うため、書かれた年月をそのまま使うことを守る。
       *
       * @given 文字列が "2026/4" である
       * @when fromYearMonthText を呼び出す
       * @then 2026年4月の日時を返す
       */
      @Test
      void 年と月の区切りが書かれた場合_その年月の日時を返す() {
        assertThat(OriginalDateTime.fromYearMonthText("2026/4").getYYYYMM()).isEqualTo("202604");
      }
    }

    @Nested
    class 異常系 {

      /**
       * 数字に変換できない文字列の場合に、現在の年月を返すことを確かめる。 数字に変換できない値で例外を投げると登録処理が止まるため、今月を既定値とすることを守る。
       *
       * @given 文字列が "abc/xx" で、現在時刻が2026年10月20日である
       * @when fromYearMonthText を呼び出す
       * @then 現在の年月である 202610 を返す
       */
      @Test
      @FixedClock("2026-10-20T10:00:00+09:00")
      void 数字に変換できない場合_現在の年月を返す() {
        assertThat(OriginalDateTime.fromYearMonthText("abc/xx").getYYYYMM()).isEqualTo("202610");
      }

      /**
       * 月が範囲外の文字列の場合に、DateTimeException を投げることを確かめる。 範囲外の月を現在の年月で補うと、入力の誤りが見えなくなるため、例外として伝えることを守る。
       *
       * @given 文字列が "2026/13" である
       * @when fromYearMonthText を呼び出す
       * @then DateTimeException を投げる
       */
      @Test
      void 月が範囲外の場合_DateTimeExceptionを投げる() {
        assertThatThrownBy(() -> OriginalDateTime.fromYearMonthText("2026/13"))
            .isInstanceOf(DateTimeException.class);
      }
    }

    @Nested
    class 境界値 {

      /**
       * 年と月の区切りが無い文字列の場合に、現在の年月を返すことを確かめる。 解釈できない形式で登録処理を止めないため、今月を既定値とすることを守る。
       *
       * @given 文字列が "2026年4月" で、現在時刻が2026年10月20日である
       * @when fromYearMonthText を呼び出す
       * @then 現在の年月である 202610 を返す
       */
      @Test
      @FixedClock("2026-10-20T10:00:00+09:00")
      void 区切りが無い場合_現在の年月を返す() {
        assertThat(OriginalDateTime.fromYearMonthText("2026年4月").getYYYYMM()).isEqualTo("202610");
      }

      /**
       * 文字列が null の場合に、現在の年月を返すことを確かめる。 開始年月が未設定の要員や案件でも、稼働時期の比較ができるよう今月を既定値とすることを守る。
       *
       * @given 文字列が null で、現在時刻が2026年10月20日である
       * @when fromYearMonthText を呼び出す
       * @then 現在の年月である 202610 を返す
       */
      @Test
      @FixedClock("2026-10-20T10:00:00+09:00")
      void nullの場合_現在の年月を返す() {
        assertThat(OriginalDateTime.fromYearMonthText(null).getYYYYMM()).isEqualTo("202610");
      }
    }
  }
}
