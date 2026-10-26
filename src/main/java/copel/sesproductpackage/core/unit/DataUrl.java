package copel.sesproductpackage.core.unit;

/**
 * Data URL（{@code data:...;base64,XXXX} 形式）の文字列を表す小粒クラス.
 *
 * <p>画面は、ファイルの内容を Data URL の形式で送ることがある。Base64 の本体だけを使う処理（復号、Markdown 変換への受け渡し）で、接頭辞を除く処理を共通化する。
 * 接頭辞が無い場合は、値をそのまま本体とみなす（素の Base64 文字列を受け付けるため）。
 *
 * @author Copel Co., Ltd.
 */
public final class DataUrl {

  /** Base64 の本体の直前に付く区切り. Data URL の接頭辞は、この区切りより前にある。 */
  private static final String BASE64_MARKER = "base64,";

  /** Data URL の文字列（接頭辞を含むことがある）. */
  private final String value;

  /**
   * Data URL の文字列を受け取る.
   *
   * @param value Data URL、または Base64 の文字列
   */
  public DataUrl(final String value) {
    this.value = value;
  }

  /**
   * Base64 の本体を返す. 接頭辞（{@code base64,} より前）は取り除く。
   *
   * <p>区切りが含まれない場合は、値をそのまま返す。区切りの後ろだけを使うため、接頭辞に同じ文字列が含まれても本体は壊れない。
   *
   * @return Base64 の本体
   */
  public String base64Payload() {
    int idx = value.indexOf(BASE64_MARKER);
    if (idx < 0) {
      return value;
    }
    return value.substring(idx + BASE64_MARKER.length());
  }
}
