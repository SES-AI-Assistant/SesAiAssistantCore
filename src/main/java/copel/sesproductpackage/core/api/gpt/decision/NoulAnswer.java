package copel.sesproductpackage.core.api.gpt.decision;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Yes/No の質問（noul）への回答.
 *
 * <p>判定は確率と閾値の比較で行う。閾値そのものは呼び出し側が業務の基準で決める。
 *
 * @author Copel Co., Ltd.
 */
@Getter
@AllArgsConstructor
public final class NoulAnswer implements DecisionAnswer {

  /** Yes の確率（API の応答は 0.01〜0.99）。 */
  private final double probability;

  /** 確信度。 */
  private final double confidence;

  /**
   * Yes の確率が閾値を超えるかを返します.
   *
   * <p>確率が閾値と等しい場合は Yes としない。確定要件 Q-07 の「0.5 を超えるなら true、0.5 以下は false」に合わせる。閾値は呼び出し側が指定する。
   *
   * @param threshold 閾値
   * @return 確率が閾値より大きければ true
   */
  public boolean isYes(final double threshold) {
    return probability > threshold;
  }
}
