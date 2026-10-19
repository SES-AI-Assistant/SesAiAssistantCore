package copel.sesproductpackage.core.util;

import java.util.function.UnaryOperator;

/**
 * IT1 / LocalStack 向けに、AWS 互換エンドポイント URL を解決するユーティリティ.
 *
 * <p>優先順位: システムプロパティ {@code aws.endpointUrl} → 環境変数 {@code AWS_ENDPOINT_URL}
 */
public final class AwsEndpointUtil {

  private AwsEndpointUtil() {}

  /**
   * ローカル検証用のエンドポイント URL を返す。未設定なら null。
   *
   * @return トリム済み URL、または null
   */
  public static String resolveEndpointUrl() {
    return resolveEndpointUrl(System::getenv);
  }

  /**
   * 環境変数の読み取りを差し替えて、ローカル検証用のエンドポイント URL を解決します。
   *
   * <p>システムプロパティを環境変数より優先する。環境変数の読み取りを引数で受け取り、テストで値を差し替えられるようにする。
   *
   * @param environment 環境変数を読む関数。本番では {@code System::getenv} を渡す
   * @return トリム済み URL、または null
   */
  public static String resolveEndpointUrl(final UnaryOperator<String> environment) {
    String fromProp = System.getProperty("aws.endpointUrl");
    if (fromProp != null && !fromProp.trim().isEmpty()) {
      return fromProp.trim();
    }
    String fromEnv = environment.apply("AWS_ENDPOINT_URL");
    if (fromEnv != null && !fromEnv.trim().isEmpty()) {
      return fromEnv.trim();
    }
    return null;
  }
}
