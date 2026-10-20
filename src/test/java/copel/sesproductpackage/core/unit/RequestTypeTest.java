package copel.sesproductpackage.core.unit;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;

/** {@link RequestType} の単体テスト。 */
class RequestTypeTest {

  @Nested
  class getEnum {

    @Nested
    class 正常系 {

      /**
       * コード値から対応するリクエスト種別を取得することを確かめる。 受信元が送るコード値を、種別の列挙値へ対応づける仕様を守る。
       *
       * @given コード値が指定されている
       * @when getEnum をコード値で実行する
       * @then 対応するリクエスト種別を返す
       */
      @ParameterizedTest
      @CsvSource({
        "11, LineMessage",
        "12, LineFile",
        "21, EmailMessage",
        "22, EmailFile",
        "31, ScreenMessage",
        "32, ScreenFile",
        "01, OtherMessage",
        "02, OtherFile"
      })
      void コード値を指定した場合_対応するリクエスト種別を返す(final String code, final RequestType expected) {
        assertThat(RequestType.getEnum(code)).isEqualTo(expected);
      }

      /**
       * 列挙名を指定すると対応するリクエスト種別を取得することを確かめる。 JSON では列挙名で届く場合があるため、列挙名でも取得できることを守る。
       *
       * @given 列挙名が指定されている
       * @when getEnum を列挙名で実行する
       * @then 対応するリクエスト種別を返す
       */
      @ParameterizedTest
      @CsvSource({
        "LineMessage, LineMessage",
        "LineFile, LineFile",
        "EmailMessage, EmailMessage",
        "EmailFile, EmailFile",
        "ScreenMessage, ScreenMessage",
        "ScreenFile, ScreenFile",
        "OtherMessage, OtherMessage",
        "OtherFile, OtherFile"
      })
      void 列挙名を指定した場合_対応するリクエスト種別を返す(final String name, final RequestType expected) {
        assertThat(RequestType.getEnum(name)).isEqualTo(expected);
      }
    }

    @Nested
    class 異常系 {

      /**
       * 未定義のコード値を指定すると OtherMessage を返すことを確かめる。 未知の受信種別を例外にすると受信処理全体が止まるので、その他のメッセージとして扱う。
       *
       * @given コード値が "99"
       * @when getEnum を "99" で実行する
       * @then OtherMessage を返す
       */
      @Test
      void 未定義のコード値の場合_OtherMessageを返す() {
        assertThat(RequestType.getEnum("99")).isEqualTo(RequestType.OtherMessage);
      }
    }

    @Nested
    class 境界値 {

      /**
       * null を指定すると null を返すことを確かめる。 種別が無い場合に既定の種別を入れると誤った分類になるので、null を返す。
       *
       * @given コード値が null
       * @when getEnum を null で実行する
       * @then null を返す
       */
      @NullSource
      @ParameterizedTest
      void コード値がnullの場合_nullを返す(final String code) {
        assertThat(RequestType.getEnum(code)).isNull();
      }
    }
  }
}
