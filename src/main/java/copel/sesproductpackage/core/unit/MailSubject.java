package copel.sesproductpackage.core.unit;

import copel.sesproductpackage.core.util.OriginalStringUtils;

/**
 * メール件名（mail_subject）の値オブジェクト.
 *
 * <p>登録先の SES_AI_T_JOB / SES_AI_T_PERSON の mail_subject カラムは VARCHAR(150)
 * のため、151文字（コードポイント）以上の件名は先頭150文字に切り詰めて保存する。切り詰めは登録処理（Register Lambda）でのみ行い、 メール受信側では行わない（設計書「3.
 * mail_subject の登録・更新フロー」）。
 *
 * <p>判定と切り詰めは Core の {@link OriginalStringUtils#truncateByCodePoints(String, int)} に任せる。 PostgreSQL
 * の VARCHAR(n) と同じくコードポイント単位で数えるため、絵文字を1文字として扱い、サロゲートペアの途中で分断しない。
 *
 * @author Copel Co., Ltd.
 */
public final class MailSubject {

  /** mail_subject カラムの最大文字数（VARCHAR(150)、コードポイント数で判定）. */
  public static final int MAX_LENGTH = 150;

  /** 件名の本文。null は「件名なし」を表す. */
  private final String value;

  /**
   * コンストラクタ. 生成は {@link #of(String)} で行う.
   *
   * @param value 件名（null 可）
   */
  private MailSubject(final String value) {
    this.value = value;
  }

  /**
   * 件名の値オブジェクトを作る.
   *
   * @param mailSubject 件名（null 可）
   * @return 件名の値オブジェクト
   */
  public static MailSubject of(final String mailSubject) {
    return new MailSubject(mailSubject);
  }

  /**
   * 保存する件名を返す. 150コードポイントを超える場合は先頭150コードポイントに切り詰める.
   *
   * <p>null はそのまま null を返し、空文字は null に変換せずそのまま返す（from_name 等の既存項目と同じ扱い）。
   *
   * @return 150コードポイント以下に切り詰めた件名。null の場合は null
   */
  public String toStoredValue() {
    return OriginalStringUtils.truncateByCodePoints(this.value, MAX_LENGTH);
  }
}
