package copel.sesproductpackage.core.api.gpt.decision;

/**
 * 判定 API の回答の共通の型.
 *
 * <p>回答の種類は {@link NoulAnswer}（Yes/No の確率）と {@link ChoiceAnswer}（択一の選択）の2つだけである。
 * 型引数の上限に使うことで、{@link DecisionQuestion} と回答の型の対応をコンパイル時に保つ。
 *
 * @author Copel Co., Ltd.
 */
public sealed interface DecisionAnswer permits NoulAnswer, ChoiceAnswer {

  /**
   * 回答の確信度を返します.
   *
   * @return 確信度
   */
  double getConfidence();
}
