package copel.sesproductpackage.core.unit;

import com.fasterxml.jackson.annotation.JsonCreator;

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

}
