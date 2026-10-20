package copel.sesproductpackage.core.api.aws;

import com.fasterxml.jackson.databind.ObjectMapper;
import copel.sesproductpackage.core.util.ApplicationClock;
import copel.sesproductpackage.core.util.Properties;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
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

  /** グローバルキャッシュ（ARN ごとのシークレット値）. */
  private static final Map<String, Map<String, String>> globalCache = new ConcurrentHashMap<>();

  /** グローバルキャッシュの最終更新時刻（ARN ごと）. */
  private static final Map<String, Long> globalCacheTime = new ConcurrentHashMap<>();

  /** キャッシュ有効期限のデフォルト値（1日）. */
  private static final long DEFAULT_CACHE_TTL_MS = 86400000;

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
   * シークレット情報を取得します. キャッシュが有効な場合はスキップします（TTL: CACHE_TTL_MS プロパティで設定、デフォルト1日）。
   *
   * @throws Exception シークレット取得時のエラー
   */
  public void load() throws Exception {
    long now = ApplicationClock.INSTANCE.clock().millis();
    long cacheTtl = DEFAULT_CACHE_TTL_MS;
    String ttlStr = Properties.get("CACHE_TTL_MS");
    if (!ttlStr.isEmpty()) {
      try {
        cacheTtl = Long.parseLong(ttlStr.trim());
      } catch (NumberFormatException e) {
        log.warn("キャッシュTTLの値が不正です: {}", ttlStr);
      }
    }
    Long lastLoadTime = globalCacheTime.get(this.secretArn);

    // キャッシュの値と取得時刻は同時に保存するため、取得時刻がある場合はキャッシュの値も必ずある
    if (lastLoadTime != null && (now - lastLoadTime) < cacheTtl) {
      this.secretValues.putAll(globalCache.get(this.secretArn));
      log.debug("【SesAiAssitantCore】シークレットキャッシュを使用しました: {}", this.secretArn);
      return;
    }

    try {
      GetSecretValueResponse response = this.client.getSecretValue(r -> r.secretId(this.secretArn));
      String secretValue = response.secretString();

      if (secretValue != null && !secretValue.isEmpty()) {
        ObjectMapper mapper = new ObjectMapper();
        @SuppressWarnings("unchecked")
        Map<String, Object> secretMap = mapper.readValue(secretValue, Map.class);

        for (Map.Entry<String, Object> entry : secretMap.entrySet()) {
          this.secretValues.put(entry.getKey(), String.valueOf(entry.getValue()));
        }

        globalCache.put(this.secretArn, new HashMap<>(this.secretValues));
        globalCacheTime.put(this.secretArn, now);

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

  /** リソースをクローズします. */
  public void close() {
    this.client.close();
  }
}
