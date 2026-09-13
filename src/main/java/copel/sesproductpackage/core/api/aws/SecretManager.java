package copel.sesproductpackage.core.api.aws;

import com.fasterxml.jackson.databind.ObjectMapper;
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

  private static long getCacheTTL() {
    String ttlStr = Properties.get("cache.ttl.ms");
    if (ttlStr != null && !ttlStr.isEmpty()) {
      try {
        return Long.parseLong(ttlStr.trim());
      } catch (NumberFormatException e) {
        log.warn("キャッシュTTLの値が不正です: {}", ttlStr);
      }
    }
    return DEFAULT_CACHE_TTL_MS;
  }

  /**
   * シークレット情報を取得します.
   * キャッシュが有効な場合はスキップします（TTL: Properties で設定、デフォルト1日）。
   *
   * @throws Exception シークレット取得時のエラー
   */
  public void load() throws Exception {
    long now = System.currentTimeMillis();
    long cacheTTL = getCacheTTL();
    Long lastLoadTime = globalCacheTime.get(this.secretArn);

    if (lastLoadTime != null && (now - lastLoadTime) < cacheTTL) {
      Map<String, String> cachedValues = globalCache.get(this.secretArn);
      if (cachedValues != null) {
        this.secretValues.putAll(cachedValues);
        log.debug("【SesAiAssitantCore】シークレットキャッシュを使用しました: {}", this.secretArn);
        return;
      }
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
