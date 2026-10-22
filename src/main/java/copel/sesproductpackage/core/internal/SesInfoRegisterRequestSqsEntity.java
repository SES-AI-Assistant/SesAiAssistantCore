package copel.sesproductpackage.core.internal;

import com.amazonaws.regions.Regions;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import copel.sesproductpackage.core.api.aws.SQSEntityBase;
import copel.sesproductpackage.core.api.gpt.entity.ContentParseSchema.InformationType;
import copel.sesproductpackage.core.api.line.LineMessagingApi;
import copel.sesproductpackage.core.unit.RequestType;
import copel.sesproductpackage.core.util.ObjectMapperFactory;
import copel.sesproductpackage.core.util.Properties;
import copel.sesproductpackage.core.util.SsmParameterKey;
import java.io.IOException;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * SES情報登録Lambda（AwsLambdaSesInfoRegister）へのSQSメッセージリクエストEntity.
 *
 * @author Copel Co., Ltd.
 */
@Slf4j
@Data
@EqualsAndHashCode(callSuper = false)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@NoArgsConstructor
public final class SesInfoRegisterRequestSqsEntity extends SQSEntityBase {
  /** リクエスト種別 */
  private RequestType requestType;

  /** 送信元グループ. */
  private String fromGroup;

  /** 送信者ID. */
  private String fromId;

  /** 送信者名. */
  private String fromName;

  /** 原文. */
  private String rawContent;

  /**
   * メール件名（メール由来のリクエストのみ設定される。件名ヘッダが無い場合は null）.
   *
   * <p>JSON キーは {@code mail_subject}。登録先の SES_AI_T_JOB / SES_AI_T_PERSON の mail_subject に反映される。
   */
  private String mailSubject;

  /** スキルシートID(このリクエストがMessageであり、かつファイルが紐づているなら使用される). */
  private String fileId;

  /** ファイル名(このリクエストがFileであるなら使用される). */
  private String fileName;

  /** ファイル内容(このリクエストがFileであるなら使用される). */
  private byte[] fileData;

  /** ウォッチ状態にするか. */
  @JsonProperty("is_watching")
  private boolean isWatching = false;

  /**
   * ウォッチ期間（日数）.
   *
   * <p>JSON キーは {@code watch_period}。値は 1〜14 の整数、または未指定（null）。画面登録（API_040）で
   * ユーザーが指定した値をそのまま載せる。範囲外の値の扱い（既定値へのフォールバック）は Register Lambda 側で行う。
   *
   * <p>未指定時は null のまま送信され、Register Lambda が既定値（SES_AI_T_JOB_TTL / SES_AI_T_PERSON_TTL）を適用する。
   */
  @JsonProperty("watch_period")
  private Integer watchPeriod;

  /** 画面登録由来の種別（Web API の info_type と同一）。未指定時は null。値は JOB / PERSON のみ有効。 */
  private String infoType;

  /** info_type キーが存在するが JOB/PERSON 以外のとき true（{@link #isValid()} が false になる）. */
  private boolean infoTypeInvalid;

  /** テナントID. */
  private String tenantId;

  /**
   * メッセージ本文の JSON 変換に使う ObjectMapper.
   *
   * <p>既定は {@link ObjectMapperFactory#OBJECT_MAPPER}。変換の失敗を UT で再現するためだけに、パッケージ内から差し替える。
   */
  static ObjectMapper objectMapper = ObjectMapperFactory.OBJECT_MAPPER;

  /** 登録対象となる最小文字数. 本文がこの文字数以下の場合は Gemini を呼ばず処理対象外とする. */
  private static final int CONTENT_MIN_LENGTH_FOR_CLASSIFICATION = 200;

  /**
   * SQS送信用コンストラクタ（キューURLを自動取得）.
   *
   * @param region リージョン
   */
  public SesInfoRegisterRequestSqsEntity(Regions region) {
    super(region, Properties.get(SsmParameterKey.REGISTER_QUEUE_URL.getKey()));
  }

  /**
   * SQS送信用コンストラクタ（キューURLを指定）.
   *
   * @param region リージョン
   * @param queueUrl SQSのURL
   */
  public SesInfoRegisterRequestSqsEntity(Regions region, String queueUrl) {
    super(region, queueUrl);
  }

  /**
   * このSQSの入力のrawContentに文字列を追加します.
   *
   * @param content 追加する内容
   */
  public void addRawContent(final String content) {
    if (this.rawContent == null) {
      this.rawContent = content;
    } else {
      this.rawContent += content;
    }
  }

  /**
   * このEntityの内容をJSON形式のメッセージBody文字列に変換します.
   *
   * <p>JSON の変換に失敗した場合は握りつぶさず、{@link JsonProcessingException} を呼び出し元へ伝える。
   *
   * @return メッセージBody
   * @throws JsonProcessingException JSON の変換に失敗した場合
   */
  @Override
  protected String getMessageBody() throws JsonProcessingException {
    return objectMapper.writeValueAsString(this);
  }

  /**
   * 与えられたデータが正しいかどうかを判定します.
   *
   * <p>リクエスト種別ごとに必須の項目を確かめる。欠けている項目はログに残す。
   *
   * @return 正常データならtrue、異常データならfalse
   */
  @JsonIgnore
  public boolean isValid() {
    if (this.infoTypeInvalid) {
      log.warn("info_typeが不正な値です");
      return false;
    }
    if (this.requestType == null) {
      log.warn("不正なrequestTypeです: {}", this.requestType);
      return false;
    }
    String typeName = this.requestType.name();
    if (RequestType.LineMessage == this.requestType
        || RequestType.OtherMessage == this.requestType) {
      return hasRequired(
          typeName,
          "fromGroup",
          this.fromGroup,
          "fromId",
          this.fromId,
          "fromName",
          this.fromName,
          "rawContent",
          this.rawContent);
    }
    if (RequestType.LineFile == this.requestType || RequestType.OtherFile == this.requestType) {
      return hasRequired(
          typeName,
          "fromGroup",
          this.fromGroup,
          "fromId",
          this.fromId,
          "fromName",
          this.fromName,
          "fileName",
          this.fileName);
    }
    if (RequestType.EmailMessage == this.requestType
        || RequestType.ScreenMessage == this.requestType) {
      return hasRequired(
          typeName,
          "fromGroup",
          this.fromGroup,
          "fromId",
          this.fromId,
          "rawContent",
          this.rawContent);
    }
    return hasRequired(
        typeName, "fromGroup", this.fromGroup, "fromId", this.fromId, "fileName", this.fileName);
  }

  /**
   * 項目名と値の組がすべて値を持つかを判定し、欠けている項目をログに残します.
   *
   * <p>ログの文言は「{種別}: {項目名}がnull」の形にそろえる。どの項目が欠けたかを運用で追えるようにするため。
   *
   * @param typeName リクエスト種別の名前（ログに出す）
   * @param nameAndValues 項目名と値を交互に並べた配列
   * @return すべての値が null でなければ true、null の項目が1つでもあれば false
   */
  private static boolean hasRequired(final String typeName, final String... nameAndValues) {
    for (int i = 0; i < nameAndValues.length; i += 2) {
      if (nameAndValues[i + 1] == null) {
        log.warn("{}: {}がnull", typeName, nameAndValues[i]);
        return false;
      }
    }
    return true;
  }

  /**
   * 本文が処理対象となる長さかどうかを判定します.
   *
   * <p>件名を含めず本文だけで判定する。メール受信 Lambda が、200文字以下の本文を Register 用の SQS へ送らないために使う.
   *
   * @param body 本文
   * @return 本文が {@code CONTENT_MIN_LENGTH_FOR_CLASSIFICATION}（200文字）を超える場合は true、null
   *     または200文字以下の場合は false
   */
  public static boolean isProcessingTargetBody(final String body) {
    return body != null && body.length() > CONTENT_MIN_LENGTH_FOR_CLASSIFICATION;
  }

  /**
   * このリクエストが処理対象であるかどうかを判定します（コンテンツ長チェック）.
   *
   * <p>本文を持つ種別は、本文が200文字以下なら処理対象外とする。
   *
   * <p>処理対象外の場合は Gemini を呼ばない。
   *
   * <p>種別が null の場合も処理対象外とする。
   *
   * @return 処理対象ならtrue、処理対象外ならfalse
   */
  @JsonIgnore
  public boolean isProcessingTarget() {
    if (this.requestType == null) {
      return false;
    }
    if (RequestType.LineMessage == this.requestType
        || RequestType.EmailMessage == this.requestType
        || RequestType.OtherMessage == this.requestType) {
      return this.rawContent.length() > CONTENT_MIN_LENGTH_FOR_CLASSIFICATION;
    }
    return true;
  }

  /**
   * 画面で JOB と指定された案件登録（ScreenMessage）か.
   *
   * @return 画面指定の案件登録ルートなら true
   */
  @JsonIgnore
  public boolean isDirectedJobRegistration() {
    return InformationType.JOB.name().equals(this.infoType)
        && RequestType.ScreenMessage.equals(this.requestType)
        && !this.rawContent.isEmpty();
  }

  /**
   * 画面で PERSON と指定された要員登録か（本文のみまたはスキルシート付き）.
   *
   * @return 画面指定の要員登録ルートなら true
   */
  @JsonIgnore
  public boolean isDirectedCandidateRegistration() {
    if (!InformationType.PERSON.name().equals(this.infoType)) {
      return false;
    }
    if (RequestType.ScreenMessage.equals(this.requestType)) {
      return !this.rawContent.isEmpty();
    }
    if (RequestType.ScreenFile.equals(this.requestType)) {
      return true;
    }
    return false;
  }

  /**
   * スキルシートであるかどうかを判定.
   *
   * @return スキルシートであればtrue、それ以外はfalse
   */
  @JsonIgnore
  public boolean isスキルシート() {
    if (RequestType.LineFile.equals(this.requestType)
        || RequestType.EmailFile.equals(this.requestType)
        || RequestType.ScreenFile.equals(this.requestType)
        || RequestType.OtherFile.equals(this.requestType)) {
      return this.fileName != null && !"".equals(this.fileName);
    } else {
      return false;
    }
  }

  /**
   * このリクエストで送られてきたファイルのデータをダウンロードしこのオブジェクトに持つ.
   *
   * <p>LINE のファイルだけ LINE Messaging API から取得する。それ以外の種別では何もしない。
   *
   * @param lineChannelAccessToken LINE のチャネルアクセストークン
   * @throws InterruptedException 待機中にスレッドが中断された場合
   * @throws IOException 入出力（ファイル・ストリーム）の操作に失敗した場合
   */
  public void downloadFileData(final String lineChannelAccessToken)
      throws IOException, InterruptedException {
    switch (this.requestType) {
      case LineFile:
        LineMessagingApi client = new LineMessagingApi(lineChannelAccessToken);
        this.fileData = client.getFile(this.fileId);
        break;
      case EmailFile:
        break;
      case ScreenFile:
        break;
      case OtherFile:
        break;
      default:
        break;
    }
  }
}
