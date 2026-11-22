package copel.sesproductpackage.core.unit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * スキルシート本文の行の一覧を表すクラス. 読み取りの妨げになる行を取り除いて本文を組み直す。
 *
 * <p>取り除くのは次の3種類だけで、それ以外の行は一切変えない。 (a) 空白だけの行 (b) 表の空の行と、区切り行の2つ目以降の連続 (c) 同じ行の3回目以降の出現。
 *
 * @author Copel Co., Ltd.
 */
public final class TextLines {

  /** 同じ行を残す最大の回数。3回目以降の出現を取り除く。 */
  private static final int MAX_DUPLICATE_COUNT = 2;

  /** 行の一覧。本文の並びどおりに並ぶ。 */
  private final List<TextLine> lines;

  /**
   * コンストラクタ.
   *
   * @param lines 行の一覧
   */
  private TextLines(final List<TextLine> lines) {
    this.lines = lines;
  }

  /**
   * 改行で区切った本文から行の一覧を作ります.
   *
   * <p>改行は LF で区切る。CRLF の場合は行末に CR が残るが、組み直すときに元のまま戻るため問題ない。
   *
   * @param text 本文（null を渡さないこと）
   * @return 行の一覧
   */
  public static TextLines of(final String text) {
    return new TextLines(
        Arrays.stream(text.split("\n", -1)).map(TextLine::new).collect(Collectors.toList()));
  }

  /**
   * 読み取りの妨げになる行を取り除いた、新しい行の一覧を返します.
   *
   * <p>区切り行の「2連続目以降」は、本文の直前の行（元の本文での1行前）も区切り行である場合に取り除く。 間に空行などが挟まる区切り行は、連続していないため残す。
   * 同じ行の出現回数は、取り除いた行を除いて数える。
   *
   * @return 取り除いた後の行の一覧。元の一覧は変えない
   */
  public TextLines withoutNoise() {
    List<TextLine> kept = new ArrayList<>();
    Map<String, Integer> occurrences = new HashMap<>();
    for (int i = 0; i < this.lines.size(); i++) {
      TextLine line = this.lines.get(i);
      boolean consecutiveSeparator =
          i > 0 && line.isTableSeparator() && this.lines.get(i - 1).isTableSeparator();
      if (line.isBlank() || line.isEmptyTableRow() || consecutiveSeparator) {
        continue;
      }
      if (occurrences.merge(line.text(), 1, Integer::sum) > MAX_DUPLICATE_COUNT) {
        continue;
      }
      kept.add(line);
    }
    return new TextLines(kept);
  }

  /**
   * 行を改行で連結して本文に戻します.
   *
   * @return 改行で連結した本文。行が無い場合は空文字
   */
  public String join() {
    return this.lines.stream().map(TextLine::text).collect(Collectors.joining("\n"));
  }
}
