package copel.sesproductpackage.core.unit;

/**
 * TTL（保存期間）の日数を表す値オブジェクト.
 *
 * <p>設定値が未設定（0）または0以下のときは、既定の日数（7日）を使う。0日のままだと登録直後のレコードが期限切れになるため、既定値をこの値の中で決める。
 * 登録日から期限日を求める計算も同じ値が持つ。
 *
 * @author Copel Co., Ltd.
 */
public final class TtlDays {

  /** 未設定または0以下の設定値のときに使う既定の日数. 0日のままだと全レコードが期限切れになるため1週間を既定にする。 */
  private static final int DEFAULT_DAYS = 7;

  /** TTL の日数. 1以上の値。 */
  private final int days;

  /**
   * コンストラクタ.
   *
   * @param days TTL の日数（1以上）
   */
  private TtlDays(final int days) {
    this.days = days;
  }

  /**
   * 設定値の日数からTTLを作る. 未設定（0）または0以下の場合は既定の日数（7日）にする.
   *
   * @param days 設定値の日数。0以下は未設定として扱う
   * @return TTL の日数を表す値オブジェクト
   */
  public static TtlDays of(final int days) {
    return new TtlDays(days <= 0 ? DEFAULT_DAYS : days);
  }

  /**
   * TTL の日数を返す.
   *
   * @return TTL の日数
   */
  public int value() {
    return this.days;
  }

  /**
   * 登録日にTTL日数を足した期限日を返す.
   *
   * <p>ttl が設定されていない案件は、登録日からこの日数を過ぎたら期限切れとして扱う。 引数の登録日は変更せず、新しい値を返す。
   *
   * @param registerDate 登録日
   * @return 登録日にTTL日数を足した期限日
   */
  public OriginalDateTime expiryAfter(final OriginalDateTime registerDate) {
    OriginalDateTime expiry = new OriginalDateTime(registerDate.toTimestamp());
    expiry.plusDays(this.days);
    return expiry;
  }
}
