package copel.sesproductpackage.core.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * {@link AwsEndpointUtil} の単体テスト。
 *
 * <p>システムプロパティ aws.endpointUrl はテストごとに退避して、環境に依存しないようにする。環境変数は読み取り関数を差し替えて確かめる。
 *
 * @author Copel Co., Ltd.
 */
class AwsEndpointUtilTest {

  /** エンドポイントを指定するシステムプロパティのキー。 */
  private static final String ENDPOINT_PROPERTY = "aws.endpointUrl";

  /** テストの前に退避する、システムプロパティ aws.endpointUrl の値。 */
  private String originalEndpointUrl;

  /** テストの前に退避し、システムプロパティ aws.endpointUrl を未設定にする。 */
  @BeforeEach
  void setUp() {
    originalEndpointUrl = System.getProperty(ENDPOINT_PROPERTY);
    System.clearProperty(ENDPOINT_PROPERTY);
  }

  /** テストの後に、システムプロパティ aws.endpointUrl を復元する。 */
  @AfterEach
  void tearDown() {
    if (originalEndpointUrl == null) {
      System.clearProperty(ENDPOINT_PROPERTY);
    } else {
      System.setProperty(ENDPOINT_PROPERTY, originalEndpointUrl);
    }
  }

  /** {@link AwsEndpointUtil#resolveEndpointUrl()} のテスト。 */
  @Nested
  class resolveEndpointUrl {

    /** 正常系のテスト。 */
    @Nested
    class 正常系 {

      /**
       * AwsEndpointUtil#resolveEndpointUrl がシステムプロパティの値を前後の空白を除いて返すことを確かめる。
       * 空白を含む値のままエンドポイントに使うと、接続先の URL が不正になるため。
       *
       * @given システムプロパティ aws.endpointUrl が " http://localhost:4566 "
       * @when resolveEndpointUrl を引数なしで呼び出す
       * @then "http://localhost:4566" を返す
       */
      @Test
      void システムプロパティがある場合_トリム済みの値を返す() {
        System.setProperty(ENDPOINT_PROPERTY, "  http://localhost:4566  ");

        assertEquals("http://localhost:4566", AwsEndpointUtil.resolveEndpointUrl());
      }
    }
  }

  /** {@link AwsEndpointUtil#resolveEndpointUrl(java.util.function.UnaryOperator)} のテスト。 */
  @Nested
  class resolveEndpointUrl_UnaryOperator {

    /** 正常系のテスト。 */
    @Nested
    class 正常系 {

      /**
       * AwsEndpointUtil#resolveEndpointUrl が環境変数の値を前後の空白を除いて返すことを確かめる。
       * システムプロパティがないローカル実行では、環境変数のエンドポイントを使うため。
       *
       * @given システムプロパティ aws.endpointUrl が未設定で、環境変数 AWS_ENDPOINT_URL が " http://env:4566 "
       * @when resolveEndpointUrl を環境変数の関数で呼び出す
       * @then "http://env:4566" を返す
       */
      @Test
      void 環境変数だけがある場合_トリム済みの値を返す() {
        String result =
            AwsEndpointUtil.resolveEndpointUrl(
                key -> Map.of("AWS_ENDPOINT_URL", " http://env:4566 ").get(key));

        assertEquals("http://env:4566", result);
      }
    }

    /** 境界値のテスト。 */
    @Nested
    class 境界値 {

      /**
       * AwsEndpointUtil#resolveEndpointUrl がシステムプロパティが空白のとき環境変数を使うことを確かめる。
       * 空白だけのシステムプロパティを採用すると、エンドポイントが空になるため。
       *
       * @given システムプロパティ aws.endpointUrl が空白 " "、環境変数 AWS_ENDPOINT_URL が "http://env:4566"
       * @when resolveEndpointUrl を環境変数の関数で呼び出す
       * @then "http://env:4566" を返す
       */
      @Test
      void システムプロパティが空白の場合_環境変数の値を返す() {
        System.setProperty(ENDPOINT_PROPERTY, "   ");

        String result =
            AwsEndpointUtil.resolveEndpointUrl(
                key -> Map.of("AWS_ENDPOINT_URL", "http://env:4566").get(key));

        assertEquals("http://env:4566", result);
      }

      /**
       * AwsEndpointUtil#resolveEndpointUrl が環境変数が空文字のとき null を返すことを確かめる。
       * 空文字のエンドポイントを使うと接続先が決まらないため、既定の接続先に任せる。
       *
       * @given システムプロパティ aws.endpointUrl が未設定で、環境変数 AWS_ENDPOINT_URL が空文字 ""
       * @when resolveEndpointUrl を環境変数の関数で呼び出す
       * @then null を返す
       */
      @Test
      void 環境変数が空文字の場合_nullを返す() {
        String result =
            AwsEndpointUtil.resolveEndpointUrl(key -> Map.of("AWS_ENDPOINT_URL", "").get(key));

        assertNull(result);
      }

      /**
       * AwsEndpointUtil#resolveEndpointUrl がシステムプロパティも環境変数もないとき null を返すことを確かめる。
       * 未設定は本番の既定の接続先を使う状態のため。
       *
       * @given システムプロパティ aws.endpointUrl が未設定で、環境変数 AWS_ENDPOINT_URL が未設定
       * @when resolveEndpointUrl を環境変数の関数で呼び出す
       * @then null を返す
       */
      @Test
      void 両方が未設定の場合_nullを返す() {
        assertNull(AwsEndpointUtil.resolveEndpointUrl(key -> null));
      }
    }
  }
}
