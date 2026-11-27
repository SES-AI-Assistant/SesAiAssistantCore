package copel.sesproductpackage.core.unit;

/**
 * テナントのステータス区分.
 *
 * <p>DB（SES_AI_M_TENANT.tenant_status_cd）にはコード値の文字列で保存する。停止中（{@link #SUSPENDED}）だけを停止として扱い、
 * それ以外のコード値、null、空文字は稼働中（{@link #ACTIVE}）とみなす。 未設定や想定外の値の行で、誤って取込を止めないための判定である。
 *
 * @author Copel Co., Ltd.
 */
public enum TenantStatus {
  /** 稼働中. テナントの取込とマッチングを通常どおり行う。 */
  ACTIVE("01"),
  /** 停止中. テナント単位で取込を止める。 */
  SUSPENDED("02");

  /** コード値. */
  private final String code;

  /**
   * コンストラクタ.
   *
   * @param code コード値.
   */
  TenantStatus(final String code) {
    this.code = code;
  }

  /**
   * コード値からステータス区分を取得します.
   *
   * <p>停止中のコード値の場合だけ {@link #SUSPENDED} を返す。停止中以外のコード値、null、空文字は稼働中として {@link #ACTIVE} を返し、
   * 例外は投げない。想定外の値の行で取込を誤って止めないための防御であり、null を返すことはない。
   *
   * @param code コード値
   * @return ステータス区分。停止中以外は {@link #ACTIVE}
   */
  public static TenantStatus getEnum(final String code) {
    return SUSPENDED.getCode().equals(code) ? SUSPENDED : ACTIVE;
  }

  /**
   * 停止中かを判定します.
   *
   * @return 停止中の場合 true、稼働中の場合 false
   */
  public boolean isSuspended() {
    return this == SUSPENDED;
  }

  /**
   * コード値を返却します.
   *
   * @return コード値
   */
  public String getCode() {
    return this.code;
  }
}
