package copel.sesproductpackage.core.unit;

import lombok.extern.slf4j.Slf4j;

/**
 * ウォッチ期間（watch_period）の値オブジェクト. ウォッチ行（SES_AI_T_WATCH）の TTL 日数を決める.
 *
 * <p>ウォッチ期間は Web API（API_040 / API_093）で 1〜14 の整数に検証済みである。登録処理（Register Lambda）では防御的に範囲を再検証し、
 * 範囲外の値は例外にせず既定値（SES_AI_T_JOB_TTL / SES_AI_T_PERSON_TTL）へフォールバックする。例外にすると SQS 受信処理が失敗し
 * 再試行ループに入るため、WARN ログを残したうえで登録処理を継続する（設計書「4. ウォッチ登録と TTL 決定フロー」）。
 *
 * <p>ログの logger 名は本クラスの名前になる。
 *
 * @author Copel Co., Ltd.
 */
@Slf4j
public final class WatchPeriod {

  /** ウォッチ期間の下限（日）. Web API の入力検証（@Min）も本値を参照する。 */
  public static final int MIN_DAYS = 1;

  /** ウォッチ期間の上限（日）. Web API の入力検証（@Max）も本値を参照する。 */
  public static final int MAX_DAYS = 14;

  /** ウォッチ期間の日数。null は「未指定」を表す. */
  private final Integer days;

  /**
   * コンストラクタ. 生成は {@link #of(Integer)} で行う.
   *
   * @param days ウォッチ期間（日数、null 可）
   */
  private WatchPeriod(final Integer days) {
    this.days = days;
  }

  /**
   * ウォッチ期間の値オブジェクトを作る.
   *
   * @param days ウォッチ期間（日数、null 可）
   * @return ウォッチ期間の値オブジェクト
   */
  public static WatchPeriod of(final Integer days) {
    return new WatchPeriod(days);
  }

  /**
   * ウォッチ期間が有効な値（1 以上 14 以下の整数）かを判定する.
   *
   * <p>null は「未指定」を表すため無効（false）として扱う。既定値適用ログの判定にも用いる。
   *
   * @return 有効な値なら true、null または範囲外なら false
   */
  public boolean isValid() {
    return this.days != null && this.days >= MIN_DAYS && this.days <= MAX_DAYS;
  }

  /**
   * ウォッチ行に適用する日数を決定する.
   *
   * <p>判定の結果は次のとおり。
   *
   * <ul>
   *   <li>null（未指定。Email / Line 由来メッセージを含む）: 既定値を適用する（ログは出さない）
   *   <li>1 以上 14 以下: 指定値を適用する
   *   <li>それ以外（防御ケース）: WARN ログを出力し既定値を適用する
   * </ul>
   *
   * @param defaultDays 既定値（日数）。案件は SES_AI_T_JOB_TTL、要員は SES_AI_T_PERSON_TTL
   * @return ウォッチ行の TTL に加算する日数
   */
  public int resolveDays(final int defaultDays) {
    if (this.days == null) {
      return defaultDays;
    }
    if (!isValid()) {
      log.warn(
          "watch_periodが範囲外のため既定値を適用します。watch_period={}, 許容範囲={}〜{}, 既定値={}",
          this.days,
          MIN_DAYS,
          MAX_DAYS,
          defaultDays);
      return defaultDays;
    }
    return this.days;
  }
}
