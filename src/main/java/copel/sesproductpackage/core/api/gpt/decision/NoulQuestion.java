package copel.sesproductpackage.core.api.gpt.decision;

import java.util.Objects;
import lombok.Getter;

/**
 * Yes/No の確率を求める質問（noul）.
 *
 * <p>判断基準（Yes と No のそれぞれの説明）は任意である。指定する場合は、Yes と No の両方を必ず指定する。
 *
 * @author Copel Co., Ltd.
 */
@Getter
public final class NoulQuestion extends DecisionQuestion<NoulAnswer> {

  /** Yes の判断基準。判断基準を指定しない場合は null。 */
  private final String trueDescription;

  /** No の判断基準。判断基準を指定しない場合は null。 */
  private final String falseDescription;

  /**
   * コンストラクタ.
   *
   * @param key 質問キー
   * @param instructions 指示文
   * @param trueDescription Yes の判断基準。指定しない場合は null
   * @param falseDescription No の判断基準。指定しない場合は null
   */
  private NoulQuestion(
      final String key,
      final String instructions,
      final String trueDescription,
      final String falseDescription) {
    super(key, instructions);
    this.trueDescription = trueDescription;
    this.falseDescription = falseDescription;
  }

  /**
   * 判断基準を指定しない Yes/No の質問を作ります.
   *
   * @param key 質問キー
   * @param instructions 指示文
   * @return 質問
   * @throws IllegalArgumentException 質問キーが制約を満たさない場合
   */
  public static NoulQuestion of(final String key, final String instructions) {
    return new NoulQuestion(key, instructions, null, null);
  }

  /**
   * 判断基準を指定した Yes/No の質問を作ります.
   *
   * @param key 質問キー
   * @param instructions 指示文
   * @param trueDescription Yes の判断基準。null は不可
   * @param falseDescription No の判断基準。null は不可
   * @return 質問
   * @throws IllegalArgumentException 質問キーが制約を満たさない場合
   */
  public static NoulQuestion of(
      final String key,
      final String instructions,
      final String trueDescription,
      final String falseDescription) {
    return new NoulQuestion(
        key,
        instructions,
        Objects.requireNonNull(trueDescription, "Yes の判断基準は null にできません"),
        Objects.requireNonNull(falseDescription, "No の判断基準は null にできません"));
  }

  /**
   * この質問への回答の型として {@link NoulAnswer} を返します.
   *
   * @return {@link NoulAnswer} の型
   */
  @Override
  public Class<NoulAnswer> answerType() {
    return NoulAnswer.class;
  }

  /**
   * 指示文と判断基準の文字数の合計を返します.
   *
   * <p>判断基準を指定しない場合は、指示文の文字数だけを返す。
   *
   * @return 指示文と判断基準の文字数の合計
   */
  @Override
  public int textLength() {
    int criteriaLength =
        trueDescription == null ? 0 : trueDescription.length() + falseDescription.length();
    return getInstructions().length() + criteriaLength;
  }
}
