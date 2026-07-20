package copel.sesproductpackage.core.unit;

import lombok.Data;

/**
 * マッチング評価と最終判定結果を保持するクラス.
 *
 * @author Copel Co., Ltd.
 */
@Data
public class MatchEvaluateResult {
  /** 最終判定結果 (YES または NO). */
  private String match;

  /** 評価テキスト (◎○△×チェックリスト). */
  private String evaluationText;

  /** デフォルトコンストラクタ. */
  public MatchEvaluateResult() {}

  /**
   * コンストラクタ.
   *
   * @param match 最終判定結果
   * @param evaluationText 評価テキスト
   */
  public MatchEvaluateResult(final String match, final String evaluationText) {
    this.match = match;
    this.evaluationText = evaluationText;
  }
}
