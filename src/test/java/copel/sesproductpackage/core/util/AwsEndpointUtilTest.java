package copel.sesproductpackage.core.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** {@link AwsEndpointUtil} の単体テスト。 */
class AwsEndpointUtilTest {

  /** テストで設定するシステムプロパティのキー。 */
  private static final String PROPERTY_KEY = "aws.endpointUrl";

  @AfterEach
  void システムプロパティを消す() {
    System.clearProperty(PROPERTY_KEY);
  }

  @Nested
  class resolveEndpointUrl_UnaryOperator {

    @Nested
    class 正常系 {

      /**
       * システムプロパティが設定されている場合は、環境変数より優先して前後の空白を除いた値を返すことを確かめる。
       * 本番で接続先を指定できないと検証環境の接続ができないので、システムプロパティを優先する。
       *
       * @given システムプロパティが " http://localhost:4566 "、環境変数が "http://env"
       * @when resolveEndpointUrl を環境変数の関数つきで実行する
       * @then 前後の空白を除いた "http://localhost:4566" を返す
       */
      @Test
      void システムプロパティが設定されている場合_前後の空白を除いたURLを返す() {
        System.setProperty(PROPERTY_KEY, "  http://localhost:4566  ");

        assertThat(AwsEndpointUtil.resolveEndpointUrl(key -> "http://env"))
            .isEqualTo("http://localhost:4566");
      }

      /**
       * システムプロパティが未設定の場合は環境変数の値を前後の空白を除いて返すことを確かめる。 環境変数だけの指定ができないと CI の設定が使えないので、環境変数でも指定できる。
       *
       * @given システムプロパティが未設定、環境変数が " http://env:4566 "
       * @when resolveEndpointUrl を環境変数の関数つきで実行する
       * @then 前後の空白を除いた "http://env:4566" を返す
       */
      @Test
      void システムプロパティが未設定で環境変数が設定されている場合_環境変数の値を返す() {
        assertThat(AwsEndpointUtil.resolveEndpointUrl(key -> " http://env:4566 "))
            .isEqualTo("http://env:4566");
      }
    }

    @Nested
    class 境界値 {

      /**
       * システムプロパティが空白のみの場合は環境変数の値を返すことを確かめる。 空の指定を有効な値として扱うと接続先が空になるので、環境変数へ戻す。
       *
       * @given システムプロパティが " "、環境変数が "http://env"
       * @when resolveEndpointUrl を環境変数の関数つきで実行する
       * @then 環境変数の "http://env" を返す
       */
      @Test
      void システムプロパティが空白のみの場合_環境変数の値を返す() {
        System.setProperty(PROPERTY_KEY, "   ");

        assertThat(AwsEndpointUtil.resolveEndpointUrl(key -> "http://env")).isEqualTo("http://env");
      }

      /**
       * どちらも未設定の場合は null を返すことを確かめる。 接続先が無い状態を判定できないと誤った接続を試みるので、null を返す。
       *
       * @given システムプロパティと環境変数が未設定
       * @when resolveEndpointUrl を値を返さない環境変数の関数つきで実行する
       * @then null を返す
       */
      @Test
      void どちらも未設定の場合_nullを返す() {
        assertThat(AwsEndpointUtil.resolveEndpointUrl(key -> null)).isNull();
      }

      /**
       * 環境変数が空白のみの場合は null を返すことを確かめる。 空白の接続先を有効と扱うと接続に失敗するので、null を返す。
       *
       * @given システムプロパティが未設定、環境変数が " "
       * @when resolveEndpointUrl を環境変数の関数つきで実行する
       * @then null を返す
       */
      @Test
      void 環境変数が空白のみの場合_nullを返す() {
        assertThat(AwsEndpointUtil.resolveEndpointUrl(key -> "   ")).isNull();
      }
    }
  }

  @Nested
  class resolveEndpointUrl {

    @Nested
    class 正常系 {

      /**
       * 引数なしで呼んだ場合も、システムプロパティを前後の空白を除いて返すことを確かめる。 本番の環境変数を読む入口で解決規則が変わると接続先の選び方が変わるので、規則を固定する。
       *
       * @given システムプロパティが " http://x "
       * @when 引数なしの resolveEndpointUrl を実行する
       * @then 前後の空白を除いた "http://x" を返す
       */
      @Test
      void システムプロパティが設定されている場合_前後の空白を除いたURLを返す() {
        System.setProperty(PROPERTY_KEY, " http://x ");

        assertThat(AwsEndpointUtil.resolveEndpointUrl()).isEqualTo("http://x");
      }
    }
  }
}
