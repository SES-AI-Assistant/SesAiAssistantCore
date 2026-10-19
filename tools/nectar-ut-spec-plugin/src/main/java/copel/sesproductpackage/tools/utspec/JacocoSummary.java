package copel.sesproductpackage.tools.utspec;

import java.util.Set;

/**
 * JaCoCo の CSV レポートを集計した結果を表す値オブジェクト。
 *
 * <p>数えるコードを持つ本体クラスの一覧（1対1 の基準）と、リポジトリ全体の命令・分岐のカバレッジを持つ。
 *
 * @param countedClasses 数えるコードを持つ本体クラスの完全修飾名の集合（内部クラスは外側のクラスに寄せる）
 * @param instructionCovered 命令のカバー数
 * @param instructionMissed 命令の未カバー数
 * @param branchCovered 分岐のカバー数
 * @param branchMissed 分岐の未カバー数
 * @author Copel Co., Ltd.
 */
public record JacocoSummary(
    Set<String> countedClasses,
    long instructionCovered,
    long instructionMissed,
    long branchCovered,
    long branchMissed) {
  /** 数えるコードを持つクラスの集合を防御的にコピーして保持する。外部の変更が集計結果に影響しないようにするため。 */
  public JacocoSummary {
    countedClasses = Set.copyOf(countedClasses);
  }

  /**
   * 命令のカバレッジ率（百分率）を返す。対象が0件の場合は100.0 を返す。
   *
   * @return 命令のカバレッジ率
   */
  public double instructionPercent() {
    return rate(instructionCovered, instructionMissed);
  }

  /**
   * 分岐のカバレッジ率（百分率）を返す。対象が0件の場合は100.0 を返す。
   *
   * @return 分岐のカバレッジ率
   */
  public double branchPercent() {
    return rate(branchCovered, branchMissed);
  }

  /**
   * カバー数と未カバー数から、カバレッジ率（百分率）を求める。
   *
   * @param covered カバー数
   * @param missed 未カバー数
   * @return カバレッジ率。対象が0件の場合は100.0
   */
  public double rate(long covered, long missed) {
    long total = covered + missed;
    if (total == 0) {
      return 100.0;
    }
    return covered * 100.0 / total;
  }
}
