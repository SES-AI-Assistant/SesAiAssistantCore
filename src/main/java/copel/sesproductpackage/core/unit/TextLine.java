package copel.sesproductpackage.core.unit;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * スキルシート本文の1行を表す値オブジェクト. 空白だけの行と、Markdown の表の空の行や区切り行を判定する。
 *
 * <p>表の判定は、前後の空白を除いた行の先頭が縦線（|）で始まる行を表の行とみなす。 表の行でも、セルの内容が無ければ読み取りに不要な行として扱う。
 *
 * @author Copel Co., Ltd.
 */
public final class TextLine {

  /** 表の区切り行のセル。「---」「:---」「---:」「:---:」のように、横線と任意の両端の寄せ記号だけからなる。 */
  private static final Pattern SEPARATOR_CELL = Pattern.compile(":?-+:?");

  /** 行の文字列。 */
  private final String text;

  /**
   * コンストラクタ.
   *
   * @param text 行の文字列（改行を含まない）
   */
  public TextLine(final String text) {
    this.text = text;
  }

  /**
   * 行の文字列を返します.
   *
   * @return 行の文字列
   */
  public String text() {
    return this.text;
  }

  /**
   * 空白だけの行かどうかを判定します.
   *
   * @return 空白だけ、または空の行ならtrue
   */
  public boolean isBlank() {
    return this.text.isBlank();
  }

  /**
   * 表の行で、セルの内容がすべて空かどうかを判定します.
   *
   * <p>「| | |」のような行は、表の枠だけで中身が無いため、読み取りの妨げになる行として扱う。
   *
   * @return 表の行でセルがすべて空ならtrue
   */
  public boolean isEmptyTableRow() {
    return isTableRow() && cells().allMatch(String::isBlank);
  }

  /**
   * Markdown の表の区切り行かどうかを判定します.
   *
   * <p>「| --- | :--- |」のように、セルの内容が横線と寄せ記号だけで、内容のあるセルが1つ以上ある行を区切り行とする。
   * 内容のないセルだけの行は区切り行ではなく、空の行として扱う。
   *
   * @return 区切り行ならtrue
   */
  public boolean isTableSeparator() {
    if (!isTableRow()) {
      return false;
    }
    List<String> filledCells = cells().filter(cell -> !cell.isBlank()).toList();
    return !filledCells.isEmpty()
        && filledCells.stream().allMatch(cell -> SEPARATOR_CELL.matcher(cell).matches());
  }

  /**
   * 行が表の行かどうかを判定します.
   *
   * <p>前後の空白を除いた先頭が縦線（|）であれば表の行とみなす。
   *
   * @return 表の行ならtrue
   */
  private boolean isTableRow() {
    return this.text.strip().startsWith("|");
  }

  /**
   * 表の行を、縦線で区切ったセルの並びとして返します.
   *
   * <p>前後の空白を除いてから区切る。行の両端の縦線で生じる空のセルは、空白だけのセルと同じく内容の無いセルとして扱われる。
   *
   * @return セルの並び（前後の空白は含む）
   */
  private Stream<String> cells() {
    return Arrays.stream(this.text.strip().split("\\|", -1)).map(String::strip);
  }
}
