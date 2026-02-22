package copel.sesproductpackage.core.api.aws;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParameterRequest;
import software.amazon.awssdk.services.ssm.model.GetParameterResponse;

/**
 * 【SES AIアシスタント】 ParameterStore操作クラス.
 *
 * @author 鈴木一矢
 */
public class ParameterStore {
  /** パラメータストアアクセス用クライアント. */
  private final SsmClient ssmClient;

  /**
   * コンストラクタ.
   *
   * @param region リージョン
   */
  public ParameterStore(final Region region) {
    this.ssmClient = SsmClient.builder().region(region).build();
  }

  /**
   * パラメータストアのSecureStringパラメータを取得します.
   *
   * @param key キー
   * @return 値
   */
  public String getParameter(final String key) {
    return this.getParameter(key, true);
  }

  /**
   * パラメータストアのパラメータを取得します.
   *
   * @param key キー
   * @param isSecureString 取得対象がSecureStringであればtrue、そうでなければfalse
   * @return 値
   */
  public String getParameter(final String key, final boolean isSecureString) {
    // パラメータ取得リクエストの作成
    GetParameterRequest parameterRequest =
        GetParameterRequest.builder().name(key).withDecryption(isSecureString).build();

    // パラメータ取得の実行
    GetParameterResponse parameterResponse = this.ssmClient.getParameter(parameterRequest);

    // 結果の返却
    return parameterResponse.parameter().value();
  }
}
