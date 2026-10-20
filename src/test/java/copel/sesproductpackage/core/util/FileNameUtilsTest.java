package copel.sesproductpackage.core.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

/** {@link FileNameUtils} の単体テスト。 */
class FileNameUtilsTest {

  /**
   * 文字列の UTF-8 のバイト列を ISO-8859-1 として読んだ文字列を作る。メールのヘッダーで起きる文字化けを再現する。
   *
   * @param text 元の文字列
   * @return 文字化けした文字列
   */
  private static String 文字化け(final String text) {
    return new String(text.getBytes(StandardCharsets.UTF_8), StandardCharsets.ISO_8859_1);
  }

  @Nested
  class decode {

    @Nested
    class 正常系 {

      /**
       * 前後に空白があるファイル名は、空白を除いて返すことを確かめる。 前後の空白を残すとファイル名が一致しなくなるので、空白を除く。
       *
       * @given ファイル名が " report.pdf "
       * @when decode を実行する
       * @then "report.pdf" を返す
       */
      @Test
      void 前後に空白があるファイル名の場合_空白を除いたファイル名を返す() {
        assertThat(FileNameUtils.decode("  report.pdf  ")).isEqualTo("report.pdf");
      }

      /**
       * MIME の Base64 形式で符号化されたファイル名をデコードすることを確かめる。 添付ファイル名が符号のままだと利用者に読めないので、元の日本語へ戻す。
       *
       * @given ファイル名が UTF-8 の Base64 の MIME 形式で "スキル.xlsx" を表す
       * @when decode を実行する
       * @then "スキル.xlsx" を返す
       */
      @Test
      void MIMEのBase64形式の場合_デコードしたファイル名を返す() {
        String encoded =
            "=?UTF-8?B?"
                + Base64.getEncoder().encodeToString("スキル.xlsx".getBytes(StandardCharsets.UTF_8))
                + "?=";

        assertThat(FileNameUtils.decode(encoded)).isEqualTo("スキル.xlsx");
      }

      /**
       * MIME の Quoted-Printable 形式で符号化されたファイル名をデコードすることを確かめる。
       * 空白の代わりの下線を戻さないとファイル名の空白が失われるので、下線を空白に戻す。
       *
       * @given ファイル名が "=?UTF-8?Q?=E3=82=B9=E3=82=AD=E3=83=AB.xlsx?="
       * @when decode を実行する
       * @then "スキル.xlsx" を返す
       */
      @ValueSource(strings = {"=?UTF-8?Q?=E3=82=B9=E3=82=AD=E3=83=AB.xlsx?="})
      @ParameterizedTest
      void MIMEのQuotedPrintable形式の場合_デコードしたファイル名を返す(final String encoded) {
        assertThat(FileNameUtils.decode(encoded)).isEqualTo("スキル.xlsx");
      }

      /**
       * 下線を含む Quoted-Printable 形式のファイル名は、下線を空白に戻すことを確かめる。 下線を空白に戻すと元の名前の下線まで変わるので、空白を含む名前を保つ。
       *
       * @given ファイル名が "=?UTF-8?Q?a_b.txt?="
       * @when decode を実行する
       * @then "a b.txt" を返す
       */
      @Test
      void 下線を含むQuotedPrintable形式の場合_下線を空白に戻したファイル名を返す() {
        assertThat(FileNameUtils.decode("=?UTF-8?Q?a_b.txt?=")).isEqualTo("a b.txt");
      }

      /**
       * RFC 5987 の形式で符号化されたファイル名をデコードすることを確かめる。 Content-Disposition の日本語名が符号のままだと保存先の名前が崩れるので、RFC
       * 5987 を復元する。
       *
       * @given ファイル名が "UTF-8''%E3%82%B9%E3%82%AD%E3%83%AB.xlsx"
       * @when decode を実行する
       * @then "スキル.xlsx" を返す
       */
      @Test
      void RFC5987形式の場合_デコードしたファイル名を返す() {
        assertThat(FileNameUtils.decode("UTF-8''%E3%82%B9%E3%82%AD%E3%83%AB.xlsx"))
            .isEqualTo("スキル.xlsx");
      }

      /**
       * 文字コードを省略した RFC 5987 の形式は UTF-8 としてデコードすることを確かめる。 文字コードの指定が省略された形式を読めないと一部の送信元で失敗するので、UTF-8
       * として扱う。
       *
       * @given ファイル名が "''%41.txt"
       * @when decode を実行する
       * @then "A.txt" を返す
       */
      @Test
      void 文字コードが省略されたRFC5987形式の場合_UTF8としてデコードしたファイル名を返す() {
        assertThat(FileNameUtils.decode("''%41.txt")).isEqualTo("A.txt");
      }

      /**
       * URL エンコードされたファイル名をデコードすることを確かめる。 URL エンコードの名前を戻さないと読めない名前が保存されるので、デコードする。
       *
       * @given ファイル名が "%E3%82%B9.pdf"
       * @when decode を実行する
       * @then "ス.pdf" を返す
       */
      @Test
      void URLエンコードの場合_デコードしたファイル名を返す() {
        assertThat(FileNameUtils.decode("%E3%82%B9.pdf")).isEqualTo("ス.pdf");
      }

      /**
       * 二重に URL エンコードされたファイル名を、2 回のデコードで元に戻すことを確かめる。 二重の符号化を戻さないと読めない名前が残るので、繰り返しデコードする。
       *
       * @given ファイル名が "%25E3%2582%25B9.pdf"
       * @when decode を実行する
       * @then "ス.pdf" を返す
       */
      @Test
      void 二重にURLエンコードされた場合_デコードしたファイル名を返す() {
        assertThat(FileNameUtils.decode("%25E3%2582%25B9.pdf")).isEqualTo("ス.pdf");
      }

      /**
       * 三重に URL エンコードされたファイル名を、3 回のデコードで元に戻すことを確かめる。 三重の符号化を戻さないと読めない名前が残るので、3 回までデコードする。
       *
       * @given ファイル名が "%2525E3%252582%2525B9.pdf"
       * @when decode を実行する
       * @then "ス.pdf" を返す
       */
      @Test
      void 三重にURLエンコードされた場合_デコードしたファイル名を返す() {
        assertThat(FileNameUtils.decode("%2525E3%252582%2525B9.pdf")).isEqualTo("ス.pdf");
      }

      /**
       * 四重に URL エンコードされたファイル名は、3 回までデコードした結果を返すことを確かめる。 回数の上限を変えるとデコード結果が変わるので、3 回の上限を固定する。
       *
       * @given ファイル名が "%252525E3%25252582%252525B9.pdf"
       * @when decode を実行する
       * @then 3 回までデコードした "%E3%82%B9.pdf" を返す
       */
      @Test
      void 四重にURLエンコードされた場合_3回までデコードした結果を返す() {
        assertThat(FileNameUtils.decode("%252525E3%25252582%252525B9.pdf"))
            .isEqualTo("%E3%82%B9.pdf");
      }

      /**
       * UTF-8 が ISO-8859-1 として誤読された文字列を元に戻すことを確かめる。 文字化けした名前を復元しないと利用者に読めないので、元の文字に戻す。
       *
       * @given ファイル名が "スキル.xlsx" の文字化けした文字列
       * @when decode を実行する
       * @then "スキル.xlsx" を返す
       */
      @Test
      void 文字化けした日本語の場合_元の文字列を返す() {
        assertThat(FileNameUtils.decode(文字化け("スキル.xlsx"))).isEqualTo("スキル.xlsx");
      }

      /**
       * 禁止文字を含むファイル名は、禁止文字を下線に置き換えることを確かめる。 保存先で使えない文字が残ると保存に失敗するので、下線に置き換える。
       *
       * @given ファイル名が "a/b:c*d?.txt"
       * @when decode を実行する
       * @then "a_b_c_d_.txt" を返す
       */
      @Test
      void 禁止文字を含む場合_下線に置き換えたファイル名を返す() {
        assertThat(FileNameUtils.decode("a/b:c*d?.txt")).isEqualTo("a_b_c_d_.txt");
      }

      /**
       * 制御文字と二重引用符を下線に置き換えることを確かめる。 制御文字や引用符が残ると保存先で扱えないので、下線に置き換える。
       *
       * @given ファイル名が "a\u0001b\"c.txt"
       * @when decode を実行する
       * @then "a_b_c.txt" を返す
       */
      @Test
      void 制御文字と二重引用符を含む場合_下線に置き換えたファイル名を返す() {
        assertThat(FileNameUtils.decode("a\u0001b\"c.txt")).isEqualTo("a_b_c.txt");
      }

      /**
       * 末尾のドットは取り除くことを確かめる。 末尾のドットが残ると Windows で扱えないので、取り除く。
       *
       * @given ファイル名が "report.pdf.."
       * @when decode を実行する
       * @then "report.pdf" を返す
       */
      @Test
      void 末尾にドットが続く場合_ドットを取り除いたファイル名を返す() {
        assertThat(FileNameUtils.decode("report.pdf..")).isEqualTo("report.pdf");
      }

      /**
       * 日本語以外の文字を含む文字列は、文字化けとみなさずそのまま返すことを確かめる。 日本語以外の文字を書き換えると元の名前が変わるので、そのまま残す。
       *
       * @given ファイル名が "café.pdf"
       * @when decode を実行する
       * @then "café.pdf" を返す
       */
      @Test
      void 日本語以外のアクセント記号を含む場合_元のファイル名を返す() {
        assertThat(FileNameUtils.decode("café.pdf")).isEqualTo("café.pdf");
      }

      /**
       * 符号化の途中で終わる Quoted-Printable 形式は、読めない部分をそのまま残すことを確かめる。 途中で切れた符号で例外が起きると処理全体が止まるので、元の文字を残す。
       *
       * @given ファイル名が "=?UTF-8?Q?a=?=" のように末尾で符号化が途切れている
       * @when decode を実行する
       * @then "a=" を返す
       */
      @Test
      void QuotedPrintableの末尾が途切れている場合_元の文字を残したファイル名を返す() {
        assertThat(FileNameUtils.decode("=?UTF-8?Q?a=?=")).isEqualTo("a=");
      }

      /**
       * 16 進数でない文字を含む Quoted-Printable の符号は、そのまま残すことを確かめる。 不正な符号を変換すると元の名前が変わるので、符号のまま残す。
       *
       * @given ファイル名が "=?UTF-8?Q?=zz?=" または "=?UTF-8?Q?=4z?="
       * @when decode を実行する
       * @then 符号の文字をそのまま残したファイル名を返す
       */
      @CsvSource({"=?UTF-8?Q?=zz?=, =zz", "=?UTF-8?Q?=4z?=, =4z"})
      @ParameterizedTest
      void QuotedPrintableの符号が不正な場合_符号の文字を残したファイル名を返す(final String encoded, final String expected) {
        assertThat(FileNameUtils.decode(encoded)).isEqualTo(expected);
      }

      /**
       * 小文字の b で示された Base64 形式の MIME も、大文字の B と同様にデコードすることを確かめる。
       * 送信元によって符号の英字が小文字になるので、大小を区別せずに復元する。
       *
       * @given ファイル名が UTF-8 の Base64 で "スキル.xlsx" を表し、符号の英字が小文字 b
       * @when decode を実行する
       * @then "スキル.xlsx" を返す
       */
      @Test
      void 小文字のbのBase64形式の場合_デコードしたファイル名を返す() {
        String encoded =
            "=?UTF-8?b?"
                + Base64.getEncoder().encodeToString("スキル.xlsx".getBytes(StandardCharsets.UTF_8))
                + "?=";

        assertThat(FileNameUtils.decode(encoded)).isEqualTo("スキル.xlsx");
      }

      /**
       * 小文字の q で示された Quoted-Printable 形式の MIME も、大文字の Q と同様にデコードすることを確かめる。
       * 送信元によって符号の英字が小文字になるので、大小を区別せずに復元する。
       *
       * @given ファイル名が UTF-8 の Quoted-Printable で "スキル.xlsx" を表し、符号の英字が小文字 q
       * @when decode を実行する
       * @then "スキル.xlsx" を返す
       */
      @Test
      void 小文字のqのQuotedPrintable形式の場合_デコードしたファイル名を返す() {
        assertThat(FileNameUtils.decode("=?UTF-8?q?=E3=82=B9=E3=82=AD=E3=83=AB.xlsx?="))
            .isEqualTo("スキル.xlsx");
      }
    }

    @Nested
    class 異常系 {

      /**
       * Base64 として不正な MIME 形式は、元の文字列を禁止文字の置換だけで返すことを確かめる。 不正な符号で例外が起きると処理全体が止まるので、元の文字列を残す。
       *
       * @given ファイル名が "=?UTF-8?B?!!!?="
       * @when decode を実行する
       * @then "=_UTF-8_B_!!!_=" を返す
       */
      @Test
      void MIMEのBase64が不正な場合_禁止文字を置き換えたファイル名を返す() {
        assertThat(FileNameUtils.decode("=?UTF-8?B?!!!?=")).isEqualTo("=_UTF-8_B_!!!_=");
      }

      /**
       * 文字コードが不明な MIME 形式は、元の文字列を禁止文字の置換だけで返すことを確かめる。 知らない文字コードで例外が起きると処理全体が止まるので、元の文字列を残す。
       *
       * @given ファイル名が "=?BOGUS?B?YQ==?="
       * @when decode を実行する
       * @then "=_BOGUS_B_YQ==_=" を返す
       */
      @Test
      void MIMEの文字コードが不明な場合_禁止文字を置き換えたファイル名を返す() {
        assertThat(FileNameUtils.decode("=?BOGUS?B?YQ==?=")).isEqualTo("=_BOGUS_B_YQ==_=");
      }

      /**
       * RFC 5987 の文字コードが不明な場合は、値の部分をデコードして返すことを確かめる。 文字コードの誤りで値も失うとファイル名が完全に消えるので、値の部分を残す。
       *
       * @given ファイル名が "BOGUS''%41.txt"
       * @when decode を実行する
       * @then "A.txt" を返す
       */
      @Test
      void RFC5987の文字コードが不明な場合_値の部分をデコードしたファイル名を返す() {
        assertThat(FileNameUtils.decode("BOGUS''%41.txt")).isEqualTo("A.txt");
      }

      /**
       * 不正な URL エンコードは、デコードせずに元の文字列を返すことを確かめる。 不正な URL エンコードで例外が起きると処理全体が止まるので、元の名前を残す。
       *
       * @given ファイル名が "%zz.txt"
       * @when decode を実行する
       * @then "%zz.txt" を返す
       */
      @Test
      void 不正なURLエンコードの場合_元のファイル名を返す() {
        assertThat(FileNameUtils.decode("%zz.txt")).isEqualTo("%zz.txt");
      }
    }

    @Nested
    class 境界値 {

      /**
       * ファイル名が null の場合は null を返すことを確かめる。 未設定のファイル名を別の値に変えると後続の判定が誤るので、null のまま返す。
       *
       * @given ファイル名が null
       * @when decode を null で実行する
       * @then null を返す
       */
      @NullSource
      @ParameterizedTest
      void ファイル名がnullの場合_nullを返す(final String rawFileName) {
        assertThat(FileNameUtils.decode(rawFileName)).isNull();
      }

      /**
       * 空白だけのファイル名は空文字を返すことを確かめる。 空白だけの名前を有効なファイル名にすると保存先が無効になるので、空文字にする。
       *
       * @given ファイル名が " "
       * @when decode を実行する
       * @then 空文字を返す
       */
      @Test
      void 空白だけのファイル名の場合_空文字を返す() {
        assertThat(FileNameUtils.decode("   ")).isEmpty();
      }

      /**
       * ドットだけのファイル名はドットを 1 つ残すことを確かめる。 ドットだけの名前から末尾のドットを取り除くと空になるので、1 つのドットを残す。
       *
       * @given ファイル名が "."
       * @when decode を実行する
       * @then "." を返す
       */
      @Test
      void ドットだけのファイル名の場合_ドットを返す() {
        assertThat(FileNameUtils.decode(".")).isEqualTo(".");
      }

      /**
       * 日本語の文字の文字化けは、元の文字を返すことを確かめる。 文字化けの復元が範囲を超えると別の文字になるので、対象の範囲を固定する。
       *
       * @given ファイル名が 1 文字の文字化けした日本語
       * @when decode を文字化けした文字列で実行する
       * @then 元の日本語の文字を返す
       */
      @ValueSource(strings = {"あ", "ア", "漢", "⺀", "㐀", "豈"})
      @ParameterizedTest
      void 日本語の文字の文字化けの場合_元の文字を返す(final String text) {
        assertThat(FileNameUtils.decode(文字化け(text))).isEqualTo(text);
      }

      /**
       * 日本語の記号や範囲外の文字の文字化けは、復元せずそのまま返すことを確かめる。 範囲外の文字を復元すると別の文字に変わるので、そのまま残す。
       *
       * @given ファイル名が 範囲外の記号や文字の文字化けした文字列
       * @when decode を文字化けした文字列で実行する
       * @then 文字化けした文字列をそのまま返す
       */
      @ValueSource(strings = {"、", "䷀", "ｱ"})
      @ParameterizedTest
      void 範囲外の記号の文字化けの場合_文字化けした文字列を返す(final String text) {
        assertThat(FileNameUtils.decode(文字化け(text))).isEqualTo(文字化け(text));
      }

      /**
       * 空文字のファイル名は空文字を返すことを確かめる。 空の入力を別の値に変えると後続の判定が変わるので、空文字を返す。
       *
       * @given ファイル名が空文字
       * @when decode を実行する
       * @then 空文字を返す
       */
      @Test
      void ファイル名が空文字の場合_空文字を返す() {
        assertThat(FileNameUtils.decode("")).isEmpty();
      }
    }
  }
}
