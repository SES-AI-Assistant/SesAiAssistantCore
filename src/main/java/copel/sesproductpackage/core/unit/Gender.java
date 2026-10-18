package copel.sesproductpackage.core.unit;

/**
 * 性別.
 *
 * <p>DB（SES_AI_T_PERSON.gender など）およびAI抽出スキーマでは列挙名（Man / Woman / Unknown）を使用する。 API（リクエスト・レスポンス）では
 * {@link #toJapanese()} / {@link #fromJapanese(String)} により日本語（男性 / 女性 / 不明）で相互変換する。
 *
 * @author Copel Co., Ltd.
 */
public enum Gender {
  /** 男性. 日本語表記は「男性」. */
  Man,
  /** 女性. 日本語表記は「女性」. */
  Woman,
  /** 不明. 日本語表記は「不明」. */
  Unknown;

  /**
   * 性別を日本語表記に変換する.
   *
   * @return 日本語表記（男性 / 女性 / 不明）
   */
  public String toJapanese() {
    return switch (this) {
      case Man -> "男性";
      case Woman -> "女性";
      case Unknown -> "不明";
    };
  }

  /**
   * 日本語表記から性別を逆引きする.
   *
   * <p>日本語の文字列を個別に持つと {@link #toJapanese()} との二重管理になるため、 {@link #values()} を走査して {@link
   * #toJapanese()} の結果と比較する。API で受け付けるのは「男性」「女性」「不明」のみであり、 「Man」などのDB値や null、空文字は受け付けない。
   *
   * @param japanese 日本語表記（男性 / 女性 / 不明）
   * @return 対応する性別
   * @throws IllegalArgumentException 日本語表記として不正な値（null・空文字・DB値を含む）の場合
   */
  public static Gender fromJapanese(final String japanese) {
    for (Gender gender : values()) {
      if (gender.toJapanese().equals(japanese)) {
        return gender;
      }
    }
    throw new IllegalArgumentException("不正な性別の日本語表記です: " + japanese);
  }
}
