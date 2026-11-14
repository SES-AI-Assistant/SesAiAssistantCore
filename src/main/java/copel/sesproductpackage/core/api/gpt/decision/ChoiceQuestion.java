package copel.sesproductpackage.core.api.gpt.decision;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import lombok.Getter;

/**
 * 複数の選択肢から1つを選ぶ質問（choice）.
 *
 * <p>選択肢のキーは回答の識別子で、値は判断基準の説明である（不要なら空文字でよい）。 選択肢の件数は2件以上255件以下に限る。
 *
 * @author Copel Co., Ltd.
 */
@Getter
public final class ChoiceQuestion extends DecisionQuestion<ChoiceAnswer> {

  /** 選択肢の件数の下限。択一にするため2件以上にする。 */
  private static final int MIN_OPTIONS = 2;

  /** 選択肢の件数の上限。API の制約（255件以内）に合わせる。 */
  private static final int MAX_OPTIONS = 255;

  /** 選択肢キーの長さの上限（文字数）。API の制約（128文字以内）に合わせる。 */
  private static final int MAX_OPTION_KEY_LENGTH = 128;

  /**
   * 選択肢キーの形式の制約。
   *
   * <p>先頭と末尾に空白（ASCII と非 ASCII の空白の両方）を置けない。制御文字は位置を問わず置けない。 空でない1文字以上の文字列だけを許す。
   * 512バイト以内の制約は、128文字以内なら UTF-8 で必ず満たされるため、ここでは検査しない。
   */
  private static final Pattern OPTION_KEY_PATTERN =
      Pattern.compile("(?![\\s\\p{Z}])[^\\p{Cc}]+(?<![\\s\\p{Z}])");

  /** 選択肢。キーは選択肢の識別子、値は判断基準の説明で、指定された順序を保つ。 */
  private final Map<String, String> options;

  /**
   * コンストラクタ.
   *
   * @param key 質問キー
   * @param instructions 指示文
   * @param options 選択肢。キーは選択肢の識別子、値は判断基準の説明
   * @throws IllegalArgumentException 選択肢の件数や選択肢キーが制約を満たさない場合
   */
  private ChoiceQuestion(
      final String key, final String instructions, final Map<String, String> options) {
    super(key, instructions);
    if (options.size() < MIN_OPTIONS || options.size() > MAX_OPTIONS) {
      throw new IllegalArgumentException("選択肢は2件以上255件以下で指定してください: " + options.size() + " 件");
    }
    for (Map.Entry<String, String> option : options.entrySet()) {
      String optionKey = option.getKey();
      if (!OPTION_KEY_PATTERN.matcher(optionKey).matches()
          || optionKey.codePointCount(0, optionKey.length()) > MAX_OPTION_KEY_LENGTH) {
        throw new IllegalArgumentException(
            "選択肢キーは1〜128文字で、先頭と末尾に空白を置けず、制御文字を含められません: " + optionKey);
      }
      Objects.requireNonNull(option.getValue(), "選択肢の説明は null にできません");
    }
    this.options = Collections.unmodifiableMap(new LinkedHashMap<>(options));
  }

  /**
   * 選択肢を指定して択一の質問を作ります.
   *
   * @param key 質問キー
   * @param instructions 指示文
   * @param options 選択肢。キーは選択肢の識別子、値は判断基準の説明（不要なら空文字）。null の値は不可
   * @return 質問
   * @throws IllegalArgumentException 質問キー、選択肢の件数、選択肢キーが制約を満たさない場合
   */
  public static ChoiceQuestion of(
      final String key, final String instructions, final Map<String, String> options) {
    return new ChoiceQuestion(key, instructions, options);
  }

  /**
   * この質問への回答の型として {@link ChoiceAnswer} を返します.
   *
   * @return {@link ChoiceAnswer} の型
   */
  @Override
  public Class<ChoiceAnswer> answerType() {
    return ChoiceAnswer.class;
  }

  /**
   * 指示文と選択肢のキー・説明の文字数の合計を返します.
   *
   * @return 指示文と選択肢のキー・説明の文字数の合計
   */
  @Override
  public int textLength() {
    int optionsLength =
        options.entrySet().stream()
            .mapToInt(option -> option.getKey().length() + option.getValue().length())
            .sum();
    return getInstructions().length() + optionsLength;
  }
}
