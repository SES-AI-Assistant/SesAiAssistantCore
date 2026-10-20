package copel.sesproductpackage.core.unit;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

/** {@link ExperienceDuration} の単体テスト。 */
class ExperienceDurationTest {

  @Nested
  class isEmpty {

    @Nested
    class 境界値 {

      /**
       * 月数が null の場合は未設定と判定することを確かめる。 記載のない経験期間を 0 として表示すると、経験があると誤認されるので、空として扱う。
       *
       * @given 経験月数が null
       * @when isEmpty を実行する
       * @then true を返す
       */
      @Test
      void 月数がnullの場合_trueを返す() {
        assertThat(new ExperienceDuration(null).isEmpty()).isTrue();
      }

      /**
       * 月数が 0 の場合は未設定と判定することを確かめる。 0 か月の経験は未記載と同じく扱わないと、経験があると誤認されるので、空として扱う。
       *
       * @given 経験月数が 0
       * @when isEmpty を実行する
       * @then true を返す
       */
      @Test
      void 月数が0の場合_trueを返す() {
        assertThat(new ExperienceDuration(0).isEmpty()).isTrue();
      }

      /**
       * 月数が 1 の場合は設定済みと判定することを確かめる。 1 か月は最小の設定済みの値なので、未設定との境界を確かめる必要がある。
       *
       * @given 経験月数が 1
       * @when isEmpty を実行する
       * @then false を返す
       */
      @Test
      void 月数が1の場合_falseを返す() {
        assertThat(new ExperienceDuration(1).isEmpty()).isFalse();
      }
    }
  }

  @Nested
  class toJapaneseFormat {

    @Nested
    class 正常系 {

      /**
       * 月数を年と月の日本語表記に変換することを確かめる。 年と月の組み合わせの表示が変わると、経歴の読み取りやすさが変わるので、形式を固定する。
       *
       * @given 経験月数が 14
       * @when toJapaneseFormat を実行する
       * @then "1年2ヶ月" を返す
       */
      @ParameterizedTest
      @CsvSource({"14, 1年2ヶ月", "12, 1年", "24, 2年", "5, 5ヶ月", "13, 1年1ヶ月"})
      void 経験月数を指定した場合_年と月の日本語表記を返す(final int months, final String expected) {
        assertThat(new ExperienceDuration(months).toJapaneseFormat()).isEqualTo(expected);
      }
    }

    @Nested
    class 境界値 {

      /**
       * 未設定の場合は空文字を返すことを確かめる。 未記載の期間を表示すると不明な値が画面に出るので、空文字にする。
       *
       * @given 経験月数が null
       * @when toJapaneseFormat を実行する
       * @then 空文字を返す
       */
      @NullSource
      @ParameterizedTest
      void 経験月数がnullの場合_空文字を返す(final Integer months) {
        assertThat(new ExperienceDuration(months).toJapaneseFormat()).isEmpty();
      }

      /**
       * 0 以下の月数は未設定として空文字を返すことを確かめる。 負の月数は経験として成立しないので、表示せず空文字にする。
       *
       * @given 経験月数が -1
       * @when toJapaneseFormat を実行する
       * @then 空文字を返す
       */
      @ValueSource(ints = {-1})
      @ParameterizedTest
      void 経験月数が負の場合_空文字を返す(final int months) {
        assertThat(new ExperienceDuration(months).toJapaneseFormat()).isEmpty();
      }
    }
  }

  @Nested
  class toSuffix {

    @Nested
    class 正常系 {

      /**
       * 区切り文字を前に付けた期間の表記を返すことを確かめる。 スキル名と期間の区切りは画面の表示形式に合わせる必要があるので、区切り文字を前に付ける形を固定する。
       *
       * @given 経験月数が 14、区切り文字が ":"
       * @when toSuffix を区切り文字 ":" で実行する
       * @then ":1年2ヶ月" を返す
       */
      @Test
      void 経験月数が14の場合_区切り文字を前に付けた表記を返す() {
        assertThat(new ExperienceDuration(14).toSuffix(":")).isEqualTo(":1年2ヶ月");
      }
    }

    @Nested
    class 境界値 {

      /**
       * 未設定の場合は区切り文字を含めず空文字を返すことを確かめる。 期間がないのに区切り文字だけ残ると表示が崩れるので、区切り文字も含めて空文字にする。
       *
       * @given 経験月数が null、区切り文字が ":"
       * @when toSuffix を区切り文字 ":" で実行する
       * @then 空文字を返す
       */
      @Test
      void 経験月数がnullの場合_空文字を返す() {
        assertThat(new ExperienceDuration(null).toSuffix(":")).isEmpty();
      }
    }
  }
}
