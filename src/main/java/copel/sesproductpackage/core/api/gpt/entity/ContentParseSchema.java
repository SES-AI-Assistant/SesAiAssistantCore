package copel.sesproductpackage.core.api.gpt.entity;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import copel.sesproductpackage.core.api.gpt.schema.ConditionalCase;
import copel.sesproductpackage.core.api.gpt.schema.ConditionalSchema;
import copel.sesproductpackage.core.api.gpt.schema.Schema;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AIによる分類・抽出・要約結果エンティティ.
 *
 * <p>種別に応じて、informations リスト内のフィールドのスキーマが動的に変わります。
 *
 * <ul>
 *   <li>type = PERSON → PersonInfoSchema 形式
 *   <li>type = JOB → JobInfoSchema 形式
 *   <li>type = Unknown → summary は設定されない
 * </ul>
 *
 * @author Copel Co., Ltd.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContentParseSchema {
  @Schema(
      title = "送信者会社名+送信者名",
      description =
          "このメール・文章を送信・執筆した営業担当者や紹介元の会社名 + 名前。「紹介されている要員本人（name）」の名前やイニシャルを設定してはならない。文章内に差出人の名前・会社名が記載されていない場合は必ずnullにすること。",
      maxLength = 30,
      example = "株式会社ABC 田中")
  private String senderName = null;

  @Schema(
      title = "情報リスト",
      description = "本文から案件情報、要員情報、その他の情報を抽出しリスト形式で取得する。それぞれの情報に種別を付与し分類する。",
      required = true,
      itemType = InfoSchema.class)
  private List<InfoSchema> informations = null;

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class InfoSchema {
    @Schema(
        title = "情報種別",
        description =
            "特定の人物を提案する文章であり、「氏名（イニシャル）」「年齢（〇〇歳）」「経験スキル」が記載されている場合は「PERSON」、プロジェクトの参画者を募集する文章であり、「必須スキル」「尚可スキル」「勤務地・常駐先」が記載されている場合は「JOB」、それ以外の場合は「Unknown」とする。広告、宣伝、交流会やイベント案内、URLのみ・ファイル展開のみのための連絡、SES案件・要員の紹介と無関係な連絡なども「Unknown」とする。",
        itemType = InformationType.class,
        required = true)
    private InformationType type = InformationType.Unknown;

    @JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "type",
        include = JsonTypeInfo.As.EXTERNAL_PROPERTY,
        defaultImpl = Object.class,
        visible = true)
    @JsonSubTypes({
      @JsonSubTypes.Type(value = PersonInfoSchema.class, name = "PERSON"),
      @JsonSubTypes.Type(value = JobInfoSchema.class, name = "JOB"),
      @JsonSubTypes.Type(value = Object.class, name = "Unknown")
    })
    @ConditionalSchema({
      @ConditionalCase(enumValue = "PERSON", title = "要員情報", schema = PersonInfoSchema.class),
      @ConditionalCase(enumValue = "JOB", title = "案件情報", schema = JobInfoSchema.class)
    })
    @Schema(description = "情報種別に応じた要約情報の詳細データ。Unknown の場合は出力しないこと")
    private Object summary = null;
  }

  public enum InformationType {
    JOB,
    PERSON,
    Unknown;
  }
}
