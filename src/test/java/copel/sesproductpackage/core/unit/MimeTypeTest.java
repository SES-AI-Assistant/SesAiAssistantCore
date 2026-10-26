package copel.sesproductpackage.core.unit;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;

/** {@link MimeType} の単体テスト。 */
class MimeTypeTest {

  @Nested
  class getEnum {

    @Nested
    class 正常系 {

      /**
       * Content-Type の文字列に完全に一致する種類を返すことを確かめる。 大文字と小文字の違いで添付ファイルの種類を取り違えないためである。
       *
       * @given Content-Type と種類が CsvSource の各行の値
       * @when getEnum を Content-Type で実行する
       * @then 対応する種類を返す
       */
      @ParameterizedTest
      @CsvSource({
        "text/plain, TEXT_PLAIN",
        "TEXT/HTML, TEXT_HTML",
        "application/pdf, PDF",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet, EXCEL"
      })
      void ContentTypeが種類と完全に一致する場合_対応する種類を返す(final String contentType, final MimeType expected) {
        assertThat(MimeType.getEnum(contentType)).isEqualTo(expected);
      }

      /**
       * 文字コードが付いた Content-Type から種類を返すことを確かめる。 メールの Content-Type は charset や name が付くのが通常である。
       *
       * @given Content-Type と種類が CsvSource の各行の値
       * @when getEnum を Content-Type で実行する
       * @then 対応する種類を返す
       */
      @ParameterizedTest
      @CsvSource({
        "'text/plain; charset=UTF-8', TEXT_PLAIN",
        "'application/pdf; name=skill.pdf', PDF"
      })
      void ContentTypeに付加情報が付く場合_対応する種類を返す(final String contentType, final MimeType expected) {
        assertThat(MimeType.getEnum(contentType)).isEqualTo(expected);
      }

      /**
       * Content-Type が種類の一部である場合に、その種類を返すことを確かめる。 区切りの前までしか分からない Content-Type でも種類を判定するためである。
       *
       * @given Content-Type が "multipart/alter"
       * @when getEnum を実行する
       * @then MULTIPART_ALTERNATIVE を返す
       */
      @ParameterizedTest
      @CsvSource({"multipart/alter, MULTIPART_ALTERNATIVE"})
      void ContentTypeが種類の一部の場合_対応する種類を返す(final String contentType, final MimeType expected) {
        assertThat(MimeType.getEnum(contentType)).isEqualTo(expected);
      }
    }

    @Nested
    class 異常系 {

      /**
       * 対応する種類が無い Content-Type で INVALID を返すことを確かめる。 未知の種類を添付ファイルや本文として処理しないためである。
       *
       * @given Content-Type が "audio/mpeg"
       * @when getEnum を実行する
       * @then INVALID を返す
       */
      @ParameterizedTest
      @CsvSource({"audio/mpeg"})
      void ContentTypeに対応する種類が無い場合_INVALIDを返す(final String contentType) {
        assertThat(MimeType.getEnum(contentType)).isEqualTo(MimeType.INVALID);
      }

      /**
       * Content-Type が null または空文字の場合に INVALID を返すことを確かめる。 Content-Type の無いパートを処理対象にしないためである。
       *
       * @given Content-Type が null と空文字
       * @when getEnum を実行する
       * @then INVALID を返す
       */
      @ParameterizedTest
      @NullAndEmptySource
      void ContentTypeがnullまたは空文字の場合_INVALIDを返す(final String contentType) {
        assertThat(MimeType.getEnum(contentType)).isEqualTo(MimeType.INVALID);
      }
    }
  }

  @Nested
  class isFile {

    @Nested
    class 正常系 {

      /**
       * EXCEL、WORD、PDF が添付ファイルとして扱われることを確かめる。 スキルシートはこの3種類だけを保存して取り込むためである。
       *
       * @given 種類が EXCEL、WORD、PDF
       * @when isFile を実行する
       * @then true を返す
       */
      @ParameterizedTest
      @EnumSource(
          value = MimeType.class,
          names = {"EXCEL", "WORD", "PDF"})
      void 種類がスキルシートの形式の場合_trueを返す(final MimeType mimeType) {
        assertThat(mimeType.isFile()).isTrue();
      }

      /**
       * スキルシートの形式以外が添付ファイルとして扱われないことを確かめる。 画像や圧縮ファイルは取り込み対象外であるためである。
       *
       * @given 種類が TEXT_PLAIN、IMAGE_PNG、ZIP、INVALID
       * @when isFile を実行する
       * @then false を返す
       */
      @ParameterizedTest
      @EnumSource(
          value = MimeType.class,
          names = {"TEXT_PLAIN", "IMAGE_PNG", "ZIP", "INVALID"})
      void 種類がスキルシートの形式以外の場合_falseを返す(final MimeType mimeType) {
        assertThat(mimeType.isFile()).isFalse();
      }
    }
  }

  @Nested
  class isText {

    @Nested
    class 正常系 {

      /**
       * TEXT_PLAIN と MULTIPART_ALTERNATIVE が本文として扱われることを確かめる。 メールの本文はこの2種類のパートに入るためである。
       *
       * @given 種類が TEXT_PLAIN、MULTIPART_ALTERNATIVE
       * @when isText を実行する
       * @then true を返す
       */
      @ParameterizedTest
      @EnumSource(
          value = MimeType.class,
          names = {"TEXT_PLAIN", "MULTIPART_ALTERNATIVE"})
      void 種類が本文の形式の場合_trueを返す(final MimeType mimeType) {
        assertThat(mimeType.isText()).isTrue();
      }

      /**
       * 本文の形式以外が本文として扱われないことを確かめる。 HTML だけのパートは本文として取り込まないためである。
       *
       * @given 種類が TEXT_HTML、PDF、INVALID
       * @when isText を実行する
       * @then false を返す
       */
      @ParameterizedTest
      @EnumSource(
          value = MimeType.class,
          names = {"TEXT_HTML", "PDF", "INVALID"})
      void 種類が本文の形式以外の場合_falseを返す(final MimeType mimeType) {
        assertThat(mimeType.isText()).isFalse();
      }
    }
  }
}
