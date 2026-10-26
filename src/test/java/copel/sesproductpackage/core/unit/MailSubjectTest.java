package copel.sesproductpackage.core.unit;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** {@link MailSubject} の単体テスト。 */
class MailSubjectTest {

  @Nested
  class toStoredValue {

    @Nested
    class 正常系 {

      /**
       * 上限以下の件名をそのまま返すことを確かめる。 短い件名を加工すると、メールの件名が変わってしまうからである。
       *
       * @given 件名が "件名テスト"
       * @when toStoredValue を実行する
       * @then "件名テスト" をそのまま返す
       */
      @Test
      void 件名が短い場合_件名を返す() {
        assertThat(MailSubject.of("件名テスト").toStoredValue()).isEqualTo("件名テスト");
      }

      /**
       * 件名が無い場合に null をそのまま返すことを確かめる。 件名ヘッダの無いメールを、空文字に変えないからである。
       *
       * @given 件名が null
       * @when toStoredValue を実行する
       * @then null を返す
       */
      @Test
      void 件名がnullの場合_nullを返す() {
        assertThat(MailSubject.of(null).toStoredValue()).isNull();
      }

      /**
       * 空文字の件名を null に変えずに返すことを確かめる。 送信者名と同じく他の項目と同じ扱いにするからである。
       *
       * @given 件名が空文字
       * @when toStoredValue を実行する
       * @then 空文字を返す
       */
      @Test
      void 件名が空文字の場合_空文字を返す() {
        assertThat(MailSubject.of("").toStoredValue()).isEmpty();
      }
    }

    @Nested
    class 境界値 {

      /**
       * 150文字ちょうどの件名を切り詰めないことを確かめる。 カラムに収まる件名の末尾を捨てないからである。
       *
       * @given 件名が "あ" を150個並べた150文字
       * @when toStoredValue を実行する
       * @then 150文字の件名をそのまま返す
       */
      @Test
      void 件名が150文字の場合_件名を返す() {
        String subject = "あ".repeat(150);

        assertThat(MailSubject.of(subject).toStoredValue()).isEqualTo(subject);
      }

      /**
       * 151文字の件名を先頭の150文字に切り詰めることを確かめる。 カラムに収まらない超過分を捨てて、登録の失敗を防ぐからである。
       *
       * @given 件名が "a" を150個の後に "b" を1個つなげた151文字
       * @when toStoredValue を実行する
       * @then 先頭の "a" を150個並べた150文字を返す
       */
      @Test
      void 件名が151文字の場合_先頭150文字を返す() {
        assertThat(MailSubject.of("a".repeat(150) + "b").toStoredValue())
            .isEqualTo("a".repeat(150));
      }

      /**
       * 大幅に超過する件名も先頭の150文字に切り詰めることを確かめる。 長さによらず、カラムに収まる件名にするからである。
       *
       * @given 件名が "い" を500個並べた500文字
       * @when toStoredValue を実行する
       * @then 先頭の "い" を150個並べた150文字を返す
       */
      @Test
      void 件名が500文字の場合_先頭150文字を返す() {
        assertThat(MailSubject.of("い".repeat(500)).toStoredValue()).isEqualTo("い".repeat(150));
      }

      /**
       * 絵文字を含み150コードポイント以下の件名を切り詰めないことを確かめる。 絵文字は UTF-16
       * では2単位だが、データベースは1文字として数えるため、必要以上に短くしないからである。
       *
       * @given 件名が絵文字を150個並べた150コードポイントで UTF-16 では300単位
       * @when toStoredValue を実行する
       * @then 件名をそのまま返す
       */
      @Test
      void 絵文字を含み150コードポイントの場合_件名を返す() {
        String subject = "😀".repeat(150);

        assertThat(MailSubject.of(subject).toStoredValue()).isEqualTo(subject);
      }

      /**
       * 151コードポイントの絵文字の件名を、コードポイント単位で150に切り詰めることを確かめる。 UTF-16 の単位で切ると、絵文字が途中で分断されるからである。
       *
       * @given 件名が絵文字を151個並べた151コードポイント
       * @when toStoredValue を実行する
       * @then 絵文字を150個並べた件名を返す
       */
      @Test
      void 絵文字を含み151コードポイントの場合_絵文字150個の件名を返す() {
        assertThat(MailSubject.of("😀".repeat(151)).toStoredValue()).isEqualTo("😀".repeat(150));
      }

      /**
       * 切り詰める位置が絵文字の途中にあたる場合に、絵文字を分断しないことを確かめる。 分断すると単独のサロゲートが残り、JDBC で文字化けするからである。
       *
       * @given 件名が "a" を149個、絵文字を1個、"b" を1個つなげた151コードポイント
       * @when toStoredValue を実行する
       * @then "a" を149個と絵文字を1個つなげた150コードポイントを返す
       */
      @Test
      void 切り詰め位置が絵文字の途中にあたる場合_絵文字を分断しない件名を返す() {
        assertThat(MailSubject.of("a".repeat(149) + "😀" + "b").toStoredValue())
            .isEqualTo("a".repeat(149) + "😀");
      }
    }
  }
}
