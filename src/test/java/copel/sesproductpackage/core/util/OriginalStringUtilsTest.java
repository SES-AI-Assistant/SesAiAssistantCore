package copel.sesproductpackage.core.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

/** {@link OriginalStringUtils} の単体テスト。 */
class OriginalStringUtilsTest {

  /** JSON 変換を確かめるための検証用の値。 */
  public static class 検証用の値 {

    /** ユーザー名。 */
    private final String userName;

    /**
     * 検証用の値を生成する。
     *
     * @param userName ユーザー名
     */
    public 検証用の値(final String userName) {
      this.userName = userName;
    }

    /**
     * ユーザー名を返す。
     *
     * @return ユーザー名
     */
    public String getUserName() {
      return userName;
    }
  }

  /** シリアライズに失敗する検証用の値。ゲッターが例外を投げる。 */
  public static class 変換に失敗する値 {

    /**
     * ゲッターが例外を投げる項目を返す。シリアライズの失敗を起こすために使う。
     *
     * @return 投げられる例外の代わりに実行時例外を投げる
     */
    public String getBroken() {
      throw new IllegalStateException("変換に失敗する");
    }
  }

  @Nested
  class toJson {

    @Nested
    class 正常系 {

      /**
       * オブジェクトを指定すると、スネークケースの JSON 文字列を返すことを確かめる。 外部とやりとりする JSON の形式が変わると相手が読めなくなるので、形式を固定する。
       *
       * @given 項目 userName が "山田" の検証用の値
       * @when toJson を検証用の値で実行する
       * @then {"user_name":"山田"} の JSON 文字列を返す
       */
      @Test
      void オブジェクトを指定した場合_JSON文字列を返す() {
        assertThat(OriginalStringUtils.toJson(new 検証用の値("山田"))).isEqualTo("{\"user_name\":\"山田\"}");
      }
    }

    @Nested
    class 異常系 {

      /**
       * シリアライズに失敗した場合は null を返すことを確かめる。 シリアライズの失敗で例外を投げると呼び出し側の処理が止まるので、null を返す。
       *
       * @given 項目の取得で例外を投げる値
       * @when toJson を値で実行する
       * @then null を返す
       */
      @Test
      void シリアライズに失敗する場合_nullを返す() {
        assertThat(OriginalStringUtils.toJson(new 変換に失敗する値())).isNull();
      }
    }

    @Nested
    class 境界値 {

      /**
       * null を指定すると "null" の文字列を返すことを確かめる。 null を JSON として表さないと送信内容が不明になるので、"null" の文字列を返す。
       *
       * @given 対象のオブジェクトが null
       * @when toJson を null で実行する
       * @then "null" の文字列を返す
       */
      @Test
      void nullを指定した場合_nullの文字列を返す() {
        assertThat(OriginalStringUtils.toJson(null)).isEqualTo("null");
      }
    }
  }

  @Nested
  class isEmpty {

    @Nested
    class 正常系 {

      /**
       * 文字列に値がある場合は空ではないと判定することを確かめる。 値のある文字列を未設定として扱うと有効な入力が捨てられるので、false を返す。
       *
       * @given 文字列が "a"
       * @when isEmpty を実行する
       * @then false を返す
       */
      @Test
      void 文字列がある場合_falseを返す() {
        assertThat(OriginalStringUtils.isEmpty("a")).isFalse();
      }
    }

    @Nested
    class 境界値 {

      /**
       * 未設定、空文字、空白だけ、"null" の文字列は空と判定することを確かめる。 未入力を表す値を個別に扱うと判定が漏れるので、まとめて空として扱う。
       *
       * @given 文字列が null、空文字、空白だけ、または "null"
       * @when isEmpty を実行する
       * @then true を返す
       */
      @NullSource
      @ValueSource(strings = {"", " ", "null"})
      @ParameterizedTest
      void 文字列が未設定や空白やnullの場合_trueを返す(final String str) {
        assertThat(OriginalStringUtils.isEmpty(str)).isTrue();
      }
    }
  }

  @Nested
  class isUnspecified {

    @Nested
    class 正常系 {

      /**
       * 値のある文字列は未設定ではないと判定することを確かめる。 実際の値を未設定として捨てると入力が失われるので、false を返す。
       *
       * @given 文字列が "abc"
       * @when isUnspecified を実行する
       * @then false を返す
       */
      @Test
      void 文字列がある場合_falseを返す() {
        assertThat(OriginalStringUtils.isUnspecified("abc")).isFalse();
      }
    }

    @Nested
    class 境界値 {

      /**
       * 未設定、空文字、大文字小文字や前後の空白を問わない "unknown" は未設定と判定することを確かめる。 生成 AI
       * が返す不明の値を未設定として扱わないと画面に不明と表示されるので、未設定として扱う。
       *
       * @given 文字列が null、空文字、"unknown"、"UNKNOWN"、前後に空白のある " Unknown "
       * @when isUnspecified を実行する
       * @then true を返す
       */
      @NullSource
      @ValueSource(strings = {"", "unknown", "UNKNOWN", " Unknown "})
      @ParameterizedTest
      void 未設定や不明の文字列の場合_trueを返す(final String str) {
        assertThat(OriginalStringUtils.isUnspecified(str)).isTrue();
      }
    }
  }

  @Nested
  class truncateByCodePoints {

    @Nested
    class 正常系 {

      /**
       * 文字数が上限以内の場合は元の文字列を返すことを確かめる。 上限内の値を切ると内容が欠けるので、元の文字列を返す。
       *
       * @given 文字列が "abc"、上限が 3
       * @when truncateByCodePoints を実行する
       * @then 元の "abc" を返す
       */
      @Test
      void 文字数が上限以内の場合_元の文字列を返す() {
        assertThat(OriginalStringUtils.truncateByCodePoints("abc", 3)).isEqualTo("abc");
      }

      /**
       * サロゲートペアを含む文字列を、コードポイント数で切り詰めることを確かめる。 文字の途中で切ると文字化けするので、コードポイント数で切る。
       *
       * @given 文字列が "𠮷𠮷a"、上限が 1
       * @when truncateByCodePoints を実行する
       * @then "𠮷" を返す
       */
      @Test
      void サロゲートペアを含む場合_コードポイント数で切り詰めた文字列を返す() {
        assertThat(OriginalStringUtils.truncateByCodePoints("𠮷𠮷a", 1)).isEqualTo("𠮷");
      }
    }

    @Nested
    class 境界値 {

      /**
       * 上限が 0 の場合は空文字を返すことを確かめる。 上限が 0 のカラムに値が入ると DB で失敗するので、空文字にする。
       *
       * @given 文字列が "abc"、上限が 0
       * @when truncateByCodePoints を実行する
       * @then 空文字を返す
       */
      @Test
      void 上限が0の場合_空文字を返す() {
        assertThat(OriginalStringUtils.truncateByCodePoints("abc", 0)).isEmpty();
      }

      /**
       * 文字列が null の場合は null を返すことを確かめる。 未設定の値を切り詰めると null が別の値に変わるので、null のまま返す。
       *
       * @given 文字列が null、上限が 3
       * @when truncateByCodePoints を実行する
       * @then null を返す
       */
      @Test
      void 文字列がnullの場合_nullを返す() {
        assertThat(OriginalStringUtils.truncateByCodePoints(null, 3)).isNull();
      }
    }
  }
}
