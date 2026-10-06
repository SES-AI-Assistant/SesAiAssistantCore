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

  /**
   * 抽出された個別情報スキーマ.
   *
   * @author Copel Co., Ltd.
   */
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class InfoSchema {
    @Schema(
        title = "情報種別",
        description =
            "情報種別。メール・文章の主旨に応じて以下のルールで厳格に分類する。\n"
                + "【最重要判定原則: 主述の向き】\n"
                + "メールの主旨が人材の提案・売り込み（PERSON）なのか、案件の参画者募集（JOB）なのかを最優先で判定する。\n"
                + "【架空要員の捏造絶対禁止】: "
                + "特定個人を識別できる具体的な属性（氏名・イニシャル、実年齢、単価等）が本文中に明記されていない場合、"
                + "職種名（「エンジニア」「プログラマー」「PM」「テックリード」等）や会社名・所属名（「〇〇所属」等）を勝手に氏名（name）として捏造して PERSON を出力することは厳禁とする。"
                + "具体的な個人プロファイルのない紹介導入文、打診文、一般的な連絡文は、PERSON ではなく必ず「Unknown」と判定すること。\n"
                + "【PERSON】: 人材の提案・売り込み（エントリー、推薦、ご提案、自社要員展開等）。"
                + "氏名/イニシャル、実年齢、単価、所属形態、稼働開始日、職歴・スキル等の特定個人属性が存在する場合は、"
                + "本文中に案件名・勤務地・必須スキルの記載（引用文やマッチング希望条件）が含まれていても、主部が特定個人の提案である限り絶対にJOBではなく「PERSON」とする。"
                + "返信メール（Re:）でのエントリーの場合も本文中の案件引用に惑わされず「PERSON」として抽出すること（引用された案件をJOBとして抽出してはならない）。\n"
                + "【JOB】: プロジェクト参画者の募集（案件案内、求めるスキル、勤務地等）。"
                + "特定個人のプロファイルが存在せず参画者を募るものは「JOB」とする。"
                + "営業担当者の名前（「担当：田中」「齊藤」「宮澤」等）や、募集条件（「募集人数：1名」「求める要員：30代まで」「見合う要員様がいらっしゃいましたらご提案ください」等）に惑わされてPERSONを出力してはならない。\n"
                + "【混在メールの切り分け】: 1通内に案件募集と要員提案が明確に併記されている場合（アライアンスメルマガ等）は、案件ブロックからJOBを、要員ブロックからPERSONをそれぞれ別個に抽出すること。\n"
                + "【Unknown】: 不在通知、自動返信（休暇・不在等）、退職挨拶、配信停止連絡、送信専用アドレス通知、イベント・交流会案内、またはSES案件・要員の具体的な提案を伴わない一般的な連絡。summaryは出力しないこと。",
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

  /**
   * 情報種別Enum.
   *
   * @author Copel Co., Ltd.
   */
  public enum InformationType {
    /** 案件情報. */
    JOB,
    /** 要員情報. */
    PERSON,
    /** 対象外（不在通知、退職挨拶等）. */
    Unknown;
  }
}
