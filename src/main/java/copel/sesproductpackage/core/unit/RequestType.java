package copel.sesproductpackage.core.unit;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.List;

/**
 * リクエスト種別
 *
 * @author Copel Co., Ltd.
 */
public enum RequestType {
  LineMessage,
  LineFile,
  EmailMessage,
  EmailFile,
  ScreenMessage,
  ScreenFile,
  OtherMessage,
  OtherFile;

  /**
   * 引数のcodeに対応するEnumを返却します.
   *
   * @param code コード値またはEnum名
   * @return RequestType
   */
  @JsonCreator
  public static RequestType getEnum(final String code) {
    if (code == null) {
      return null;
    }
    switch (code) {
      case "11":
      case "LineMessage":
        return LineMessage;
      case "12":
      case "LineFile":
        return LineFile;
      case "21":
      case "EmailMessage":
        return EmailMessage;
      case "22":
      case "EmailFile":
        return EmailFile;
      case "31":
      case "ScreenMessage":
        return ScreenMessage;
      case "32":
      case "ScreenFile":
        return ScreenFile;
      case "01":
      case "OtherMessage":
        return OtherMessage;
      case "02":
      case "OtherFile":
        return OtherFile;
      default:
        return OtherMessage;
    }
  }

  /**
   * ファイルを取り込むリクエスト種別かを判定します.
   *
   * @return LINE・メール・画面・その他のファイルなら true、それ以外は false
   */
  public boolean isFile() {
    return switch (this) {
      case LineFile, EmailFile, ScreenFile, OtherFile -> true;
      default -> false;
    };
  }

  /**
   * 本文のテキストを持つリクエスト種別かを判定します.
   *
   * <p>本文を持つ種別だけ、本文の文字数で処理対象かを判定する。
   *
   * @return LINE・メール・その他のメッセージなら true、それ以外は false
   */
  public boolean hasBodyText() {
    return switch (this) {
      case LineMessage, EmailMessage, OtherMessage -> true;
      default -> false;
    };
  }

  /**
   * LINE のファイルかを判定します。ファイル本体を LINE Messaging API から取得する種別である。
   *
   * @return LINE のファイルなら true、それ以外は false
   */
  public boolean isLineFile() {
    return this == LineFile;
  }

  /**
   * 登録時に必須となる項目名を、判定する順に返します.
   *
   * <p>LINE・その他のメッセージは送信者名と本文が必要である。メールと画面のメッセージは送信者名を必須としない。 ファイルは種別により、送信者名またはファイル名が必要である。
   *
   * @return 必須の項目名の一覧（判定する順）
   */
  public List<String> requiredFieldNames() {
    return switch (this) {
      case LineMessage, OtherMessage -> List.of("fromGroup", "fromId", "fromName", "rawContent");
      case LineFile, OtherFile -> List.of("fromGroup", "fromId", "fromName", "fileName");
      case EmailMessage, ScreenMessage -> List.of("fromGroup", "fromId", "rawContent");
      case EmailFile, ScreenFile -> List.of("fromGroup", "fromId", "fileName");
    };
  }

  /**
   * 画面で JOB と指定された案件登録として、この種別が原文を受け付けるかを判定します.
   *
   * @param rawContent 原文（画面のメッセージでは null 不可）
   * @return 画面のメッセージで原文が空でなければ true、それ以外は false
   */
  public boolean acceptsDirectedJob(final String rawContent) {
    return switch (this) {
      case ScreenMessage -> !rawContent.isEmpty();
      default -> false;
    };
  }

  /**
   * 画面で PERSON と指定された要員登録として、この種別が受け付けるかを判定します.
   *
   * @param rawContent 原文（画面のメッセージでは null 不可。画面のファイルでは使わない）
   * @return 画面のメッセージで原文が空でない場合、または画面のファイルなら true、それ以外は false
   */
  public boolean acceptsDirectedCandidate(final String rawContent) {
    return switch (this) {
      case ScreenMessage -> !rawContent.isEmpty();
      case ScreenFile -> true;
      default -> false;
    };
  }
}
