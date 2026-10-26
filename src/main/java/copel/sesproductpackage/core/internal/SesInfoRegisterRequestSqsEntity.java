package copel.sesproductpackage.core.internal;

import com.amazonaws.regions.Regions;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import copel.sesproductpackage.core.api.aws.SQSEntityBase;
import copel.sesproductpackage.core.api.line.LineMessagingApi;
import copel.sesproductpackage.core.unit.InformationType;
import copel.sesproductpackage.core.unit.RequestType;
import copel.sesproductpackage.core.util.ObjectMapperFactory;
import copel.sesproductpackage.core.util.Properties;
import copel.sesproductpackage.core.util.SsmParameterKey;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * SES情報登録Lambda（AwsLambdaSesInfoRegister）へのSQSメッセージリクエストEntity.
 *
 * <p>登録依頼の SQS メッセージの唯一の契約である。送信元（メール・LINE・画面など）によらず、同じキー名・同じ順番・同じ null の扱いの JSON を送る。
 * 画面登録（API_040）が 送っていた JSON と完全に一致する（{@link #getMessageBody()} の JSON 完全一致 UT で保証する）。
 *
 * <p>JSON のキー順は {@link JsonPropertyOrder} で固定する。Jackson は明示的に名前を付けた項目を末尾に回すため、順番を明示しないと契約の順番が崩れる。
 *
 * @author Copel Co., Ltd.
 */
@Slf4j
@Data
@EqualsAndHashCode(callSuper = false)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonPropertyOrder({
  "request_type",
  "from_group",
  "from_id",
  "from_name",
  "raw_content",
  "mail_subject",
  "file_id",
  "file_name",
  "file_data",
  "is_watching",
  "watch_period",
  "info_type",
  "info_type_invalid",
  "tenant_id"
})
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
   * <p>JSON キーは {@code mail_subject}。登録先の SES_AI_T_JOB / SES_AI_T_PERSON の mail_subject
   * に反映される。件名が無い場合はキーを送らない（画面登録の契約に合わせる）。
   */
  @JsonInclude(JsonInclude.Include.NON_NULL)
  private String mailSubject;

  /** スキルシートID(このリクエストがMessageであり、かつファイルが紐づているなら使用される). */
  private String fileId;

  /** ファイル名(このリクエストがFileであるなら使用される). */
  private String fileName;

  /**
   * ファイル内容(このリクエストがFileであるなら使用される).
   *
   * <p>送信元は設定しない。Register Lambda が LINE から取得した内容を保持するために使う。null の場合はキーを送らない。
   */
  @JsonInclude(JsonInclude.Include.NON_NULL)
  private byte[] fileData;

  /**
   * ウォッチ状態にするか.
   *
   * <p>フィールド名は {@code watching} とし、JSON キーは {@code is_watching} にする。フィールド名と JSON キーを別々に持つと、 Lombok
   * の getter（{@code isWatching}）が別の項目として {@code watching} キーを重複して出力してしまうため、フィールドの名前で揃える。
   */
  @JsonProperty("is_watching")
  @JsonAlias("watching")
  private boolean watching = false;

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

  /**
   * info_type キーが存在するが JOB/PERSON 以外のとき true（{@link #isValid()} が false になる）.
   *
   * <p>送信元は設定しない項目のため、送信する JSON には含めない。読み込みだけは従来どおり行う。
   */
  @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
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
    // 項目名と値の対応。種別ごとに必須となる項目名は RequestType が持つ
    Map<String, String> values = new HashMap<>();
    values.put("fromGroup", this.fromGroup);
    values.put("fromId", this.fromId);
    values.put("fromName", this.fromName);
    values.put("rawContent", this.rawContent);
    values.put("fileName", this.fileName);
    String typeName = this.requestType.name();
    for (String name : this.requestType.requiredFieldNames()) {
      if (values.get(name) == null) {
        log.warn("{}: {}がnull", typeName, name);
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
    if (this.requestType.hasBodyText()) {
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
        && this.requestType != null
        && this.requestType.acceptsDirectedJob(this.rawContent);
  }

  /**
   * 画面で PERSON と指定された要員登録か（本文のみまたはスキルシート付き）.
   *
   * @return 画面指定の要員登録ルートなら true
   */
  @JsonIgnore
  public boolean isDirectedCandidateRegistration() {
    return InformationType.PERSON.name().equals(this.infoType)
        && this.requestType != null
        && this.requestType.acceptsDirectedCandidate(this.rawContent);
  }

  /**
   * スキルシートであるかどうかを判定.
   *
   * @return スキルシートであればtrue、それ以外はfalse
   */
  @JsonIgnore
  public boolean isスキルシート() {
    if (this.requestType == null || !this.requestType.isFile()) {
      return false;
    }
    return this.fileName != null && !"".equals(this.fileName);
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
    if (this.requestType.isLineFile()) {
      LineMessagingApi client = new LineMessagingApi(lineChannelAccessToken);
      this.fileData = client.getFile(this.fileId);
    }
  }
}
