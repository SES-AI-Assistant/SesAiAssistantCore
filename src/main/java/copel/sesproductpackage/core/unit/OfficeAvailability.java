package copel.sesproductpackage.core.unit;

/**
 * 出社可否（週あたりの出社日数）を表す値オブジェクトです.
 *
 * <p>0 はフルリモート希望、5 は常駐可、それ以外は週の出社日数として表示します。
 *
 * @author Copel Co., Ltd.
 */
public final class OfficeAvailability {

  /** フルリモートを表す出社日数。 */
  private static final int FULL_REMOTE = 0;

  /** 常駐可を表す出社日数。 */
  private static final int RESIDENT = 5;

  /** 週の出社日数。 */
  private final int days;

  /**
   * 週の出社日数を指定して生成します.
   *
   * @param days 週の出社日数
   */
  public OfficeAvailability(final int days) {
    this.days = days;
  }

  /**
   * 出社可否を画面と要約に表示する文字列にします.
   *
   * @return 表示用の文字列
   */
  public String toDisplayString() {
    if (this.days == FULL_REMOTE) {
      return "フルリモート希望";
    } else if (this.days == RESIDENT) {
      return "常駐可";
    }
    return "週" + this.days + "まで可";
  }
}
