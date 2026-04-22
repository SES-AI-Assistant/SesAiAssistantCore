package copel.sesproductpackage.core.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * プッシュ通知リクエスト情報を保持する.
 *
 * @author Copel Co., Ltd.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SesAiAssistantWebAppNotifierRequestEntity {
  /** ユーザーID. */
  @JsonProperty("user_id")
  private String userId;

  /** 通知タイトル. */
  @JsonProperty("title")
  private String title;

  /** 通知本文. */
  @JsonProperty("body")
  private String body;

  /** アイコン URL. */
  @JsonProperty("icon")
  private String icon;

  /** バッジ URL. */
  @JsonProperty("badge")
  private String badge;

  /** タグ. */
  @JsonProperty("tag")
  private String tag;
}
