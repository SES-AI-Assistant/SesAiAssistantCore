package copel.sesproductpackage.core.unit;

import java.util.Arrays;
import java.util.List;

/**
 * 改行で区切られた複数行のテキストを表す値オブジェクト.
 *
 * <p>経験・必須スキル・尚可スキル・その他条件などは、登録時に改行区切りの文字列として保存される。一覧として扱う箇所が複数あるため、区切り方をここで一箇所にそろえる。
 *
 * @author Copel Co., Ltd.
 */
public final class MultiLineText {
  /** 改行で区切られたテキスト。null の場合は空の一覧として扱う。 */
  private final String text;

  /**
   * 改行区切りのテキストを生成します.
   *
   * @param text 改行で区切られたテキスト（null 可）
   */
  public MultiLineText(final String text) {
    this.text = text;
  }

  /**
   * 改行で区切った一覧を返します.
   *
   * <p>テキストが null の場合は空の一覧を返す。空文字の場合は、空文字1件を含む一覧になる（String#split の動作）。
   *
   * @return 改行で分けた一覧（テキストが null の場合は空の一覧）
   */
  public List<String> toLines() {
    return this.text == null ? Arrays.asList() : Arrays.asList(this.text.split("\n"));
  }
}
