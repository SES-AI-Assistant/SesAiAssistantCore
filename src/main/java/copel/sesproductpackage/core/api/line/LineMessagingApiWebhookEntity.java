package copel.sesproductpackage.core.api.line;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import lombok.Getter;

/**
 * LINE Messaging APIからWebhookで送信されてくるリクエストのEntityクラス.
 *
 * <p>LINE Webhook を受信する Lambda と、LINE Messaging API のアダプタである {@link LineMessagingApi} が同じ形式を扱うため、
 * Core の LINE API パッケージに置く。Webhook の JSON を読むだけで、LINE API への通信は行わない。
 *
 * @author Copel Co., Ltd.
 */
public final class LineMessagingApiWebhookEntity {
  /** イベント種別がメッセージであることを表す値。Webhook の events[].type が "message" のときに使う。 */
  private static final String EVENT_TYPE_MESSAGE = "message";

  /** 送信元種別がグループであることを表す値。Webhook の events[].source.type が "group" のときに使う。 */
  private static final String SOURCE_TYPE_GROUP = "group";

  /** メッセージ種別がファイルであることを表す値。Webhook の events[].message.type が "file" のときに使う。 */
  private static final String MESSAGE_TYPE_FILE = "file";

  /** リクエスト種別("message", "file", "join", etc.). */
  private String type;

  /** 送信元種別("user", "group"). */
  private String sourceType;

  /** 送信元ユーザーID. */
  @Getter private String userId;

  /** 送信元グループID. グループ以外から送信された場合は null. */
  @Getter private String groupId;

  /** メッセージ種別("text", "image", "file", etc.). */
  private String messageType;

  /** メッセージ本文. */
  @Getter private String text;

  /** ファイル名. ファイル送信以外の場合は null. */
  @Getter private String fileName;

  /** ファイルID. ファイル送信以外の場合は null. */
  @Getter private String fileId;

  /**
   * JSON文字列を受け取ってパースし、メンバ変数に格納.
   *
   * @param jsonString JSON文字列
   * @throws IllegalArgumentException JSON として解析できない、または events が配列でないか空の場合
   */
  public LineMessagingApiWebhookEntity(final String jsonString) throws IllegalArgumentException {
    try {
      ObjectMapper objectMapper = new ObjectMapper();
      JsonNode rootNode = objectMapper.readTree(jsonString);

      // events が配列でない、または空であれば処理しない
      // オブジェクトの events は size が1以上でも get(0) が null を返し、NullPointerException になる
      JsonNode events = rootNode.path("events");
      if (!events.isArray() || events.isEmpty()) {
        throw new IOException();
      }

      // イベントのタイプ (message, follow, join など)
      this.type = events.get(0).path("type").asText();

      // 送信元の情報 (user or group)
      JsonNode sourceNode = events.get(0).path("source");
      this.sourceType = sourceNode.path("type").asText();
      this.userId = sourceNode.path("userId").asText();
      this.groupId = sourceNode.has("groupId") ? sourceNode.get("groupId").asText() : null;

      // メッセージの種類
      if (this.isMessage()) {
        this.messageType = events.get(0).path("message").path("type").asText();
        this.text = events.get(0).path("message").path("text").asText();
        // ファイル送信なら、ファイルIDとファイル名を取得する.
        if (this.isFile()) {
          this.fileId = events.get(0).path("message").path("id").asText();
          this.fileName = events.get(0).path("message").path("fileName").asText();
        }
      }
    } catch (IOException e) {
      throw new IllegalArgumentException("Invalid JSON format");
    }
  }

  /**
   * Webhookのリクエストが正しいフォーマットかチェック.
   *
   * @return 正しければtrue
   */
  public boolean isValid() {
    // JsonNode#asText は項目が無い場合も空文字を返すため、null にはならない
    return !this.type.isEmpty() && !this.sourceType.isEmpty();
  }

  /**
   * メッセージイベントか判定.
   *
   * @return メッセージならtrue
   */
  public boolean isMessage() {
    return EVENT_TYPE_MESSAGE.equals(this.type);
  }

  /**
   * ファイルが送信されたリクエストか判定.
   *
   * @return ファイル送信ならtrue
   */
  public boolean isFile() {
    return isMessage() && MESSAGE_TYPE_FILE.equals(this.messageType);
  }

  /**
   * グループ内で受信したメッセージか判定.
   *
   * @return グループならtrue
   */
  public boolean isGroup() {
    return SOURCE_TYPE_GROUP.equals(this.sourceType)
        && this.groupId != null
        && !this.groupId.isEmpty();
  }
}
