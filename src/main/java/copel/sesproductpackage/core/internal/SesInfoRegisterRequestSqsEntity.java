package copel.sesproductpackage.core.internal;

import com.amazonaws.regions.Regions;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import copel.sesproductpackage.core.api.aws.SQSEntityBase;
import copel.sesproductpackage.core.api.gpt.entity.ContentParseSchema.InformationType;
import copel.sesproductpackage.core.api.line.LineMessagingAPI;
import copel.sesproductpackage.core.unit.RequestType;
import copel.sesproductpackage.core.util.ObjectMapperFactory;
import copel.sesproductpackage.core.util.Properties;
import copel.sesproductpackage.core.util.SsmParameterKey;
import java.io.IOException;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * SES情報登録Lambda（AwsLambdaSesInfoRegister）へのSQSメッセージリクエストEntity.
 *
 * @author Copel Co., Ltd.
 */
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

  /** スキルシートID(このリクエストがMessageであり、かつファイルが紐づているなら使用される). */
  private String fileId;

  /** ファイル名(このリクエストがFileであるなら使用される). */
  private String fileName;

  /** ファイル内容(このリクエストがFileであるなら使用される). */
  private byte[] fileData;

  /** ウォッチ状態にするか. */
  private boolean isWatching = false;

  /** 画面登録由来の種別（Web API の info_type と同一）。未指定時は null。値は JOB / PERSON のみ有効。 */
  private String infoType;

  /** info_type キーが存在するが JOB/PERSON 以外のとき true（{@link #isValid()} が false になる）. */
  private boolean infoTypeInvalid;

  /** テナントID. */
  private String tenantId;

  /** 登録対象となる最小文字数. */
  private static final int CONTENT_MIN_LENGTH_FOR_CLASSIFICATION =
      Properties.getInt("CONTENT_MIN_LENGTH_FOR_CLASSIFICATION");

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

  @Override
  protected String getMessageBody() {
    try {
      return ObjectMapperFactory.OBJECT_MAPPER.writeValueAsString(this);
    } catch (JsonProcessingException e) {
      e.printStackTrace();
      return null;
    }
  }

  /**
   * 与えられたデータが正しいかどうかを判定します.
   *
   * @return 正常データならtrue、異常データならfalse
   */
  @JsonIgnore
  public boolean isValid() {
    if (this.infoTypeInvalid) {
      return false;
    }
    if (RequestType.LineMessage == this.requestType) {
      return this.fromGroup != null
          && this.fromId != null
          && this.fromName != null
          && this.rawContent != null
          && this.rawContent.length() > CONTENT_MIN_LENGTH_FOR_CLASSIFICATION;
    } else if (RequestType.LineFile == this.requestType) {
      return this.fromGroup != null
          && this.fromId != null
          && this.fromName != null
          && this.fileName != null;
    } else if (RequestType.EmailMessage == this.requestType) {
      return this.fromGroup != null
          && this.fromId != null
          && this.rawContent != null
          && this.rawContent.length() > CONTENT_MIN_LENGTH_FOR_CLASSIFICATION;
    } else if (RequestType.EmailFile == this.requestType) {
      return this.fromGroup != null && this.fromId != null && this.fileName != null;
    } else if (RequestType.OtherMessage == this.requestType) {
      return this.fromGroup != null
          && this.fromId != null
          && this.fromName != null
          && this.rawContent != null
          && this.rawContent.length() > CONTENT_MIN_LENGTH_FOR_CLASSIFICATION;
    } else if (RequestType.OtherFile == this.requestType) {
      return this.fromGroup != null
          && this.fromId != null
          && this.fromName != null
          && this.fileName != null;
    } else if (RequestType.ScreenMessage == this.requestType) {
      return this.fromGroup != null && this.fromId != null && this.rawContent != null;
    } else if (RequestType.ScreenFile == this.requestType) {
      return this.fromGroup != null && this.fromId != null && this.fileName != null;
    }
    return false;
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
   * @throws InterruptedException
   * @throws IOException
   */
  public void downloadFileData(final String lineChannelAccessToken)
      throws IOException, InterruptedException {
    switch (this.requestType) {
      case LineFile:
        LineMessagingAPI client = new LineMessagingAPI(lineChannelAccessToken);
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
