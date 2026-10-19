package copel.sesproductpackage.tools.utspec;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * JaCoCo が出力した CSV レポート（jacoco.csv）を読み込み、{@link JacocoSummary} に集計する。
 *
 * <p>CSV は1行が1クラスを表す。内部クラス（{@code Outer.Inner}）や匿名クラス（{@code Outer.new ...}）は 外側のクラスの行に寄せて、1対1
 * の判定を外側のクラス単位で行えるようにする。
 *
 * @author Copel Co., Ltd.
 */
public class JacocoCsvReader {

  /** CSV のヘッダーで使う列名（パッケージ）。 */
  private static final String COLUMN_PACKAGE = "PACKAGE";

  /** CSV のヘッダーで使う列名（クラス）。 */
  private static final String COLUMN_CLASS = "CLASS";

  /** CSV のヘッダーで使う列名（命令の未カバー数）。 */
  private static final String COLUMN_INSTRUCTION_MISSED = "INSTRUCTION_MISSED";

  /** CSV のヘッダーで使う列名（命令のカバー数）。 */
  private static final String COLUMN_INSTRUCTION_COVERED = "INSTRUCTION_COVERED";

  /** CSV のヘッダーで使う列名（分岐の未カバー数）。 */
  private static final String COLUMN_BRANCH_MISSED = "BRANCH_MISSED";

  /** CSV のヘッダーで使う列名（分岐のカバー数）。 */
  private static final String COLUMN_BRANCH_COVERED = "BRANCH_COVERED";

  /**
   * jacoco.csv を読み込んで集計結果を返す。
   *
   * <p>ヘッダーの列名で値を取り出すため、JaCoCo の列の並びが変わっても壊れない。
   *
   * @param csv jacoco.csv のパス
   * @return 集計結果
   * @throws IOException ファイルを読み込めない場合
   * @throws IllegalStateException jacoco.csv が存在しない場合（先に jacoco:report を実行する必要がある）
   */
  public JacocoSummary read(Path csv) throws IOException {
    if (!Files.isRegularFile(csv)) {
      throw new IllegalStateException("jacoco.csv が見つからない: " + csv + "（先に jacoco:report を実行する）");
    }
    List<String> lines = Files.readAllLines(csv, StandardCharsets.UTF_8);
    if (lines.isEmpty()) {
      return new JacocoSummary(Set.of(), 0, 0, 0, 0);
    }
    List<String> header = List.of(lines.get(0).split(","));
    int packageIndex = header.indexOf(COLUMN_PACKAGE);
    int classIndex = header.indexOf(COLUMN_CLASS);
    int instructionMissedIndex = header.indexOf(COLUMN_INSTRUCTION_MISSED);
    int instructionCoveredIndex = header.indexOf(COLUMN_INSTRUCTION_COVERED);
    int branchMissedIndex = header.indexOf(COLUMN_BRANCH_MISSED);
    int branchCoveredIndex = header.indexOf(COLUMN_BRANCH_COVERED);

    Set<String> counted = new TreeSet<>();
    long instructionCovered = 0;
    long instructionMissed = 0;
    long branchCovered = 0;
    long branchMissed = 0;
    for (String line : lines.subList(1, lines.size())) {
      if (line.isBlank()) {
        continue;
      }
      String[] cols = line.split(",");
      long instrMissedRow = Long.parseLong(cols[instructionMissedIndex]);
      long instrCoveredRow = Long.parseLong(cols[instructionCoveredIndex]);
      instructionMissed += instrMissedRow;
      instructionCovered += instrCoveredRow;
      branchMissed += Long.parseLong(cols[branchMissedIndex]);
      branchCovered += Long.parseLong(cols[branchCoveredIndex]);
      if (instrMissedRow + instrCoveredRow > 0) {
        counted.add(outerName(cols[packageIndex], cols[classIndex]));
      }
    }
    return new JacocoSummary(
        counted, instructionCovered, instructionMissed, branchCovered, branchMissed);
  }

  /**
   * パッケージ名とクラス名の列から、外側のクラスの完全修飾名を求める。
   *
   * <p>クラス名は最初の「.」か「$」より前を外側のクラスとして扱う。内部クラスや匿名クラスは 外側のクラスの1対1 の判定に含めるため、ここで寄せる。
   *
   * @param packageName パッケージ名（ドット区切り。デフォルトパッケージは空）
   * @param className CSV の CLASS 列の値
   * @return 外側のクラスの完全修飾名
   */
  public String outerName(String packageName, String className) {
    String outer = className.split("[.$]", 2)[0];
    return packageName.isEmpty() ? outer : packageName + "." + outer;
  }
}
