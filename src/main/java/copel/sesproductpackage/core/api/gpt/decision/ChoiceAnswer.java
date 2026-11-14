package copel.sesproductpackage.core.api.gpt.decision;

import java.util.Map;
import lombok.Getter;

/**
 * 択一の質問（choice）への回答.
 *
 * <p>選ばれた選択肢は判定 API の決定をそのまま保持する。確率が同率の場合も、ここで選び直さない。 全選択肢の確率は、呼び出し側が並び順などの判断に使う。
 *
 * @author Copel Co., Ltd.
 */
@Getter
public final class ChoiceAnswer implements DecisionAnswer {

  /** 選ばれた選択肢のキー。 */
  private final String choice;

  /** 全選択肢の確率（選択肢のキーから確率へ）。外部から変更できないように複製して保持する。 */
  private final Map<String, Double> probabilities;

  /** 確信度。 */
  private final double confidence;

  /**
   * コンストラクタ.
   *
   * @param choice 選ばれた選択肢のキー
   * @param probabilities 全選択肢の確率
   * @param confidence 確信度
   */
  public ChoiceAnswer(
      final String choice, final Map<String, Double> probabilities, final double confidence) {
    this.choice = choice;
    this.probabilities = Map.copyOf(probabilities);
    this.confidence = confidence;
  }

  /**
   * 全選択肢の確率を返します.
   *
   * <p>内部の値を直接渡すと、呼び出し側が変更できてしまうため、読み出しのたびに複製して返す。
   *
   * @return 全選択肢の確率（選択肢のキーから確率へ）の複製
   */
  public Map<String, Double> getProbabilities() {
    return Map.copyOf(probabilities);
  }
}
