package copel.sesproductpackage.core.api.gpt.decision;

import java.util.Objects;
import java.util.regex.Pattern;
import lombok.Getter;

/**
 * 判定 API に送る質問の共通の基底クラス.
 *
 * <p>質問キーは回答を取り出す識別子であり、1回の判定の中で重複させない。 キーは API の制約により {@code [A-Za-z0-9_-]+} の64文字以内に限る。
 * キーの検証は、質問を作る時点で行い、 不正な値を API へ送る前に落とすためにここに置く。
 *
 * @param <A> この質問への回答の型
 * @author Copel Co., Ltd.
 */
@Getter
public abstract sealed class DecisionQuestion<A extends DecisionAnswer>
    permits NoulQuestion, ChoiceQuestion {

  /** 質問キーの文字種の制約。 API が受け付けるのは英数字・アンダースコア・ハイフンだけのため。 */
  private static final Pattern KEY_PATTERN = Pattern.compile("[A-Za-z0-9_-]+");

  /** 質問キーの長さの上限（文字数）。 API の制約（64文字以内）に合わせる。 */
  private static final int MAX_KEY_LENGTH = 64;

  /** 質問キー。回答を取り出す識別子である。 */
  private final String key;

  /** 質問の指示文。 */
  private final String instructions;

  /**
   * コンストラクタ.
   *
   * <p>質問キーの文字種と長さを検証する。ASCII の文字だけで構成されるため、文字数は {@code length()} で数えて問題ない。
   *
   * @param key 質問キー
   * @param instructions 指示文。null は不可
   * @throws IllegalArgumentException 質問キーが制約を満たさない場合
   */
  protected DecisionQuestion(final String key, final String instructions) {
    if (!KEY_PATTERN.matcher(key).matches() || key.length() > MAX_KEY_LENGTH) {
      throw new IllegalArgumentException("質問キーは [A-Za-z0-9_-]+ の64文字以内で指定してください: " + key);
    }
    this.key = key;
    this.instructions = Objects.requireNonNull(instructions, "指示文は null にできません");
  }

  /**
   * この質問への回答の型を返します.
   *
   * @return 回答の型
   */
  public abstract Class<A> answerType();

  /**
   * 指示文と判断基準の文字数の合計を返します.
   *
   * <p>判定 API の入力文字数の見積もりに使う。共有の input は含まない。
   *
   * @return 指示文と判断基準の文字数の合計
   */
  public abstract int textLength();
}
