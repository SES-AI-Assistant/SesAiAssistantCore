package copel.sesproductpackage.core.util;

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
    String fromProp = System.getProperty("aws.endpointUrl");
    if (fromProp != null && !fromProp.trim().isEmpty()) {
      return fromProp.trim();
    }
    String fromEnv = System.getenv("AWS_ENDPOINT_URL");
    if (fromEnv != null && !fromEnv.trim().isEmpty()) {
      return fromEnv.trim();
    }
    return null;
  }
}
