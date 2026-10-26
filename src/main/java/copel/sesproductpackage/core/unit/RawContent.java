package copel.sesproductpackage.core.unit;

import copel.sesproductpackage.core.util.OriginalStringUtils;

/**
 * 原文（raw_content）の値オブジェクト.
 *
 * <p>登録先の SES_AI_T_JOB / SES_AI_T_PERSON の raw_content に保存する文字数には上限がある。 JOB と PERSON
 * の登録・更新の両方で同じ切り詰めを使うため、値オブジェクトにまとめている。切り詰めの判定は Core の {@link
 * OriginalStringUtils#truncateByCodePoints(String, int)} に任せ、結果を変えないようにしている。
 *
 * <p>{@link #MAX_LENGTH} は数値の定数だけを公開する。LINE・メールの受信処理は UTF-16 の単位で同じ上限を比べるため、
 * 切り詰めのロジックは共通化せず、上限の数値だけを共有する。
 *
 * <p>{@link #MAX_LENGTH} の数値は、Email・LINE の受信側では UTF-16 単位の substring 比較長としても使う。保存側（本クラスの {@link
 * #toStoredValue()}）は コードポイント単位で切り詰めるため、同じ数値でも数える単位が異なる点に注意する。
 *
 * @author Copel Co., Ltd.
 */
public final class RawContent {

  /** raw_content に保存する最大文字数（コードポイント数）. */
  public static final int MAX_LENGTH = 3000;

  /** 原文の本文。null は「原文なし」を表す. */
  private final String value;

  /**
   * コンストラクタ. 生成は {@link #of(String)} で行う.
   *
   * @param value 原文（null 可）
   */
  private RawContent(final String value) {
    this.value = value;
  }

  /**
   * 原文の値オブジェクトを作る.
   *
   * @param rawContent 原文（null 可）
   * @return 原文の値オブジェクト
   */
  public static RawContent of(final String rawContent) {
    return new RawContent(rawContent);
  }

  /**
   * 保存する原文を返す. 上限を超える場合は先頭から上限の文字数までに切り詰める.
   *
   * <p>判定はコードポイント単位で行うため、絵文字などのサロゲートペアを1文字として数える。
   *
   * @return 3000文字以下ならそのまま、超える場合は先頭3000文字。null の場合は null
   */
  public String toStoredValue() {
    return OriginalStringUtils.truncateByCodePoints(this.value, MAX_LENGTH);
  }

  /**
   * 原文の長さを UTF-16 の単位で返す. 処理対象外のログに、従来どおりの文字数を出すために使う.
   *
   * <p>ここでは切り詰めを行わない。ログの文字数は従来と同じく {@link String#length()} の単位（UTF-16
   * の単位）で数える。切り詰めのコードポイント単位とは別の単位である。
   *
   * @return 原文の長さ。原文が null の場合は 0
   */
  public int utf16Length() {
    return this.value == null ? 0 : this.value.length();
  }
}
