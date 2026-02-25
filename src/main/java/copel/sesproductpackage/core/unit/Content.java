package copel.sesproductpackage.core.unit;

import copel.sesproductpackage.core.api.gpt.GptAnswer;
import copel.sesproductpackage.core.api.gpt.Transformer;
import copel.sesproductpackage.core.util.Properties;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Content {

  // =========================================================
  // 重みスコアリング定数
  // =========================================================

  /** 強いワードのスコア倍率（案件・要員どちらにもほぼ登場しない専用ワード）. 例: 氏名（要員専用）、エンド直（案件専用） */
  private static final int SCORE_HIGH = 3;

  /** 弱いワードのスコア倍率（両方に登場し得るが傾向として一方に多いワード）. 例: 場所、内容、作業などの共通性の高いワード */
  private static final int SCORE_LOW = 1;

  /** 強い案件特徴ワード. これらは案件紹介文にほぼ固有で、要員紹介文にはほぼ出現しない強い識別子。 */
  private static final String PROP_JOB_FEATURES_HIGH = "JOB_FEATURES_ARRAY_HIGH";

  /** 弱い案件特徴ワード. 案件紹介文に比較的多く出現するが、要員紹介文にも出現し得るワード。 */
  private static final String PROP_JOB_FEATURES_LOW = "JOB_FEATURES_ARRAY_LOW";

  /** 強い要員特徴ワード. これらは要員紹介文にほぼ固有で、案件紹介文にはほぼ出現しない強い識別子。 */
  private static final String PROP_PERSON_FEATURES_HIGH = "PERSONEL_FEATURES_ARRAY_HIGH";

  /** 弱い要員特徴ワード. 要員紹介文に比較的多く出現するが、案件紹介文にも出現し得るワード。 */
  private static final String PROP_PERSON_FEATURES_LOW = "PERSONEL_FEATURES_ARRAY_LOW";

  /** 後方互換用: 全案件特徴ワード（重み分類未設定の場合に使用）. */
  private static final String PROP_JOB_FEATURES = "JOB_FEATURES_ARRAY";

  /** 後方互換用: 全要員特徴ワード（重み分類未設定の場合に使用）. */
  private static final String PROP_PERSON_FEATURES = "PERSONEL_FEATURES_ARRAY";

  /** 判定基準文字数(この文字数以上であれば要員 or 案件情報と判定する). */
  private static final String PROP_CRITERIA = "TARGET_NUMBER_OF_CRITERIA";

  /** 文章が複数要員であるかどうかを判定し分割するプロンプト. */
  private static final String PROP_MULTI_PERSON_PROMPT = "MULTIPLE_PERSONNEL_JUDGMENT_PROMPT";

  /** 文章が複数案件であるかどうかを判定し分割するプロンプト. */
  private static final String PROP_MULTI_JOB_PROMPT = "MULTIPLE_JOB_JUDGMENT_PROMPT";

  // =========================================================
  // インスタンスフィールド
  // =========================================================

  /** メッセージ原文. */
  private String rawContent;

  /** 案件情報の重み付きスコア. */
  private int jobScore;

  /** 要員情報の重み付きスコア. */
  private int personelScore;

  /** 判定基準文字数. */
  private int targetNumberOfCriteria;

  /** 複数の情報が書かれている場合、各情報ごとに分割したリスト. */
  private List<String> contentList;

  /** このコンテンツが複数であるかどうかを持つフラグ. */
  private boolean is複数紹介文;

  // =========================================================
  // コンストラクタ
  // =========================================================

  /** デフォルトコンストラクタ. */
  public Content() {
    this.rawContent = null;
    this.contentList = new ArrayList<>();
    this.targetNumberOfCriteria = parseTargetCriteria();
  }

  /**
   * コンストラクタ.
   *
   * @param rawContent メッセージ原文
   */
  public Content(final String rawContent) {
    this.rawContent = rawContent;
    this.contentList = new ArrayList<>();
    this.targetNumberOfCriteria = parseTargetCriteria();
    this.jobScore =
        calcScore(rawContent, PROP_JOB_FEATURES_HIGH, PROP_JOB_FEATURES_LOW, PROP_JOB_FEATURES);
    this.personelScore =
        calcScore(
            rawContent, PROP_PERSON_FEATURES_HIGH, PROP_PERSON_FEATURES_LOW, PROP_PERSON_FEATURES);
  }

  // =========================================================
  // プライベートメソッド
  // =========================================================

  /**
   * 判定基準文字数を Properties から読み込む.
   *
   * @return 判定基準文字数
   */
  private static int parseTargetCriteria() {
    return Integer.parseInt(Properties.get(PROP_CRITERIA));
  }

  /**
   * テキストに対してワードの重み付きスコアを計算する. 高スコアワード設定が存在する場合はそちらを優先し、 存在しない場合は後方互換として全ワードを低スコアで計算する。
   *
   * @param text 対象テキスト
   * @param highScoreProp 高スコアワードのプロパティキー
   * @param lowScoreProp 低スコアワードのプロパティキー
   * @param fallbackProp 後方互換用プロパティキー
   * @return 重み付きスコア
   */
  private static int calcScore(
      final String text,
      final String highScoreProp,
      final String lowScoreProp,
      final String fallbackProp) {

    int score = 0;
    String highRaw = Properties.get(highScoreProp);
    String lowRaw = Properties.get(lowScoreProp);

    if (highRaw != null && lowRaw != null) {
      // 重み分類あり: 高スコアワードと低スコアワードで別々にカウント
      score += countOccurrences(text, highRaw.split(",")) * SCORE_HIGH;
      score += countOccurrences(text, lowRaw.split(",")) * SCORE_LOW;
    } else {
      // 後方互換: 全ワードを低スコアでカウント
      String fallbackRaw = Properties.get(fallbackProp);
      if (fallbackRaw != null) {
        score += countOccurrences(text, fallbackRaw.split(",")) * SCORE_LOW;
      }
    }
    return score;
  }

  /**
   * テキスト内の各ワードの延べ出現回数を計算する.
   *
   * @param text 対象テキスト
   * @param keywords 検索ワード配列
   * @return 延べ出現回数
   */
  private static int countOccurrences(final String text, final String[] keywords) {
    int count = 0;
    for (final String keyword : keywords) {
      if (keyword.trim().isEmpty()) {
        continue;
      }
      Pattern pattern = Pattern.compile(Pattern.quote(keyword.trim()));
      Matcher matcher = pattern.matcher(text);
      while (matcher.find()) {
        count++;
      }
    }
    return count;
  }

  // =========================================================
  // 公開メソッド
  // =========================================================

  /**
   * 原文が空であるかどうかを判定します.
   *
   * @return 空であればtrue、そうでなければfalse
   */
  public boolean isEmpty() {
    return this.rawContent == null || "".equals(this.rawContent);
  }

  /**
   * このメッセージが案件の紹介文であるかどうかを判定します. 判定基準文字数以上かつ案件スコアが要員スコアよりも高い場合に true を返します。
   *
   * @return 案件紹介文と判定すればtrue、それ以外はfalse
   */
  public boolean is案件紹介文() {
    return !this.isEmpty()
        && this.rawContent.length() >= this.targetNumberOfCriteria
        && this.jobScore > this.personelScore;
  }

  /**
   * このメッセージが要員の紹介文であるかどうかを判定します. 判定基準文字数以上かつ要員スコアが案件スコアよりも高い場合に true を返します。
   *
   * @return 要員紹介文と判定すればtrue、それ以外はfalse
   */
  public boolean is要員紹介文() {
    return !this.isEmpty()
        && this.rawContent.length() >= this.targetNumberOfCriteria
        && this.personelScore > this.jobScore;
  }

  /**
   * このメッセージが複数の紹介文であるかどうかを返します.
   *
   * @return 複数であればtrue、複数でなければfalse
   */
  public boolean is複数紹介文() {
    return this.is複数紹介文;
  }

  /**
   * このメッセージが複数要員または複数案件の情報を持つかどうか判定し、 複数であればこのクラスのリストに結果を持ちます.
   *
   * @param transformer GPTクライアント
   * @return 複数であればtrue、単一であればfalse
   * @throws IOException
   * @throws RuntimeException
   */
  public boolean 複数判定処理実行(final Transformer transformer) throws IOException, RuntimeException {
    GptAnswer answer = null;

    final String 複数要員判定プロンプト = Properties.get(PROP_MULTI_PERSON_PROMPT);
    final String 複数案件判定プロンプト = Properties.get(PROP_MULTI_JOB_PROMPT);

    // 複数の紹介文であれば配列の形で返却され、単一であれば「false」とだけ返すようなプロンプトを実行
    if (this.is要員紹介文()) {
      answer = transformer.generate(複数要員判定プロンプト + this.rawContent);
    } else if (this.is案件紹介文()) {
      answer = transformer.generate(複数案件判定プロンプト + this.rawContent);
    } else {
      return false;
    }

    // 10文字以上の文字列（「false」でない文字列）が返されていれば複数人の情報であると判断
    if (answer.length() > 10 && answer.isJsonArrayFormat()) {
      // 複数紹介文フラグをtrueにし、回答をリスト形式で取得する
      this.is複数紹介文 = true;
      this.contentList = answer.getAsList();
    }

    return this.is複数紹介文;
  }

  /**
   * このクラスのcontentListを返却します.
   *
   * @return contentList
   */
  public List<String> getContentList() {
    return this.contentList;
  }

  @Override
  public String toString() {
    if (!this.contentList.isEmpty()) {
      return this.contentList.toString();
    } else {
      return this.rawContent;
    }
  }
}
