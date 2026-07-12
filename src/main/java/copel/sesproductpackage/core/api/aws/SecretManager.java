package copel.sesproductpackage.core.api.aws;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;

/**
 * AWS Secrets Manager操作クラス.
 *
 * @author Copel Co., Ltd.
 */
@Slf4j
public class SecretManager {
  /** Secrets Manager クライアント. */
  private final SecretsManagerClient client;

  /** 取得したシークレット情報. */
  private final Map<String, String> secretValues = new HashMap<>();

  /** シークレット ARN. */
  private final String secretArn;

  /**
   * コンストラクタ.
   *
   * @param secretArn シークレット ARN
   * @param region AWS リージョン
   */
  public SecretManager(final String secretArn, final Region region) {
    this.secretArn = secretArn;
    this.client =
        SecretsManagerClient.builder()
            .credentialsProvider(DefaultCredentialsProvider.create())
            .region(region)
            .build();
  }

  /**
   * コンストラクタ（デフォルトリージョン ap-northeast-1）.
   *
   * @param secretArn シークレット ARN
   */
  public SecretManager(final String secretArn) {
    this(secretArn, Region.AP_NORTHEAST_1);
  }

  /**
   * シークレット情報を取得します.
   *
   * @throws Exception シークレット取得時のエラー
   */
  public void load() throws Exception {
    try {
      GetSecretValueResponse response =
          this.client.getSecretValue(
              r -> r.secretId(this.secretArn));
      String secretValue = response.secretString();

      if (secretValue != null && !secretValue.isEmpty()) {
        ObjectMapper mapper = new ObjectMapper();
        @SuppressWarnings("unchecked")
        Map<String, Object> secretMap = mapper.readValue(secretValue, Map.class);

        for (Map.Entry<String, Object> entry : secretMap.entrySet()) {
          this.secretValues.put(entry.getKey(), String.valueOf(entry.getValue()));
        }

        log.info("【SesAiAssitantCore】シークレットを読み込みました: {}", this.secretArn);
      } else {
        log.warn("【SesAiAssitantCore】シークレット値が空です: {}", this.secretArn);
      }
    } catch (Exception e) {
      log.error("【SesAiAssitantCore】シークレット取得中にエラーが発生しました: {}", e.getMessage());
      throw e;
    }
  }

  /**
   * シークレット値を取得します.
   *
   * @param key キー
   * @return 値
   */
  public String get(final String key) {
    return this.secretValues.getOrDefault(key, "");
  }

  /**
   * シークレット値を取得します（存在しない場合はnull）.
   *
   * @param key キー
   * @return 値
   */
  public String getOrNull(final String key) {
    return this.secretValues.get(key);
  }

  /**
   * すべてのシークレット値を取得します.
   *
   * @return シークレット値マップ
   */
  public Map<String, String> getAll() {
    return new HashMap<>(this.secretValues);
  }

  /**
   * シークレット情報が読み込まれているか確認します.
   *
   * @return 読み込み済みの場合 true
   */
  public boolean isLoaded() {
    return !this.secretValues.isEmpty();
  }

  /** リソースをクローズします. */
  public void close() {
    if (this.client != null) {
      this.client.close();
    }
  }
}
