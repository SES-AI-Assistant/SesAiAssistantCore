package copel.sesproductpackage.core.unit;

/**
 * マッチング状態区分.
 *
 * @author 鈴木一矢
 */
public enum MatchingStatus {
  アンマッチ("00"),
  サジェスト中("01"),
  提案中("10"),
  進行中("20"),
  一時停止("30"),
  完了_オファー("40"),
  完了_見送り("41"),
  無効("99");

  /** コード値. */
  private String code;

  /**
   * コンストラクタ.
   *
   * @param code コード値.
   */
  MatchingStatus(final String code) {
    this.code = code;
  }

  /**
   * コード値からEnumを取得します.
   *
   * @param code コード値
   * @return マッチング状態区分Enum
   */
  public static MatchingStatus getEnum(final String code) {
    for (final MatchingStatus status : values()) {
      if (status.getCode().equals(code)) {
        return status;
      }
    }
    return null;
  }

  /**
   * コード値を返却します.
   *
   * @return コード値
   */
  public String getCode() {
    return this.code;
  }

  /**
   * コード値をセットする.
   *
   * @param code コード値
   */
  public void setCode(String code) {
    this.code = code;
  }
}
