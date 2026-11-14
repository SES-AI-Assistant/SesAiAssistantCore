package copel.sesproductpackage.core.api.gpt.decision;

import java.util.Map;

/**
 * 判定 API の判定結果.
 *
 * <p>質問ごとの回答を保持し、{@link #answerOf(DecisionQuestion)} で質問の型に合う回答を取り出す。
 * 回答の型は質問の型で決まるため、取り違えはコンパイル時に防げる。
 *
 * @author Copel Co., Ltd.
 */
public final class DecisionResult {

  /** 質問キーから回答へのマップ。外部から変更できないように複製して保持する。 */
  private final Map<String, DecisionAnswer> answers;

  /**
   * コンストラクタ.
   *
   * @param answers 質問キーから回答へのマップ
   */
  public DecisionResult(final Map<String, DecisionAnswer> answers) {
    this.answers = Map.copyOf(answers);
  }

  /**
   * 質問に対する回答を、質問の型で取り出します.
   *
   * @param <A> 回答の型
   * @param question 回答を取り出す質問
   * @return 質問に対する回答
   * @throws IllegalArgumentException 判定結果に質問キーの回答が無い場合
   */
  public <A extends DecisionAnswer> A answerOf(final DecisionQuestion<A> question) {
    DecisionAnswer answer = answers.get(question.getKey());
    if (answer == null) {
      throw new IllegalArgumentException("判定結果に質問キーの回答がありません: " + question.getKey());
    }
    return question.answerType().cast(answer);
  }
}
