package copel.sesproductpackage.core.unit;

import copel.sesproductpackage.core.api.gpt.GptAnswer;
import copel.sesproductpackage.core.api.gpt.Transformer;
import copel.sesproductpackage.core.util.Properties;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * メッセージ本文を保持し、案件紹介文・要員紹介文・その他の分類を行うクラス.
 * 分類は生成AI（Transformer）を用いて行う。一定文字数未満の本文はAIを呼ばず「その他」とする。
 */
public class Content {

  /** 分類結果が未実施の場合のデフォルト最低文字数. */
  private static final int DEFAULT_MIN_LENGTH_FOR_CLASSIFICATION = 200;

  /** 案件・要員・その他を判定するためのプロンプトのプロパティキー. */
  private static final String PROP_CONTENT_CLASSIFICATION_PROMPT = "CONTENT_CLASSIFICATION_PROMPT";

  /** 分類を行う最低文字数のプロパティキー. この文字数未満はAIを呼ばず「その他」とする. */
  private static final String PROP_CONTENT_MIN_LENGTH = "CONTENT_MIN_LENGTH_FOR_CLASSIFICATION";

  /** 文章が複数要員であるかどうかを判定し分割するプロンプト. */
  private static final String PROP_MULTI_PERSON_PROMPT = "MULTIPLE_PERSONNEL_JUDGMENT_PROMPT";

  /** 文章が複数案件であるかどうかを判定し分割するプロンプト. */
  private static final String PROP_MULTI_JOB_PROMPT = "MULTIPLE_JOB_JUDGMENT_PROMPT";

  /** 分類結果の種別. */
  public enum ContentType {
    /** SES案件の紹介・募集内容. */
    JOB,
    /** SES要員（エンジニア等）の紹介・プロフィール内容. */
    PERSONNEL,
    /** 上記のどちらにも当てはまらない（広告・挨拶・無関係なやりとり等）. */
    OTHER
  }

  /** メッセージ原文. */
  private String rawContent;

  /** 生成AIによる分類結果. classify() 未呼び出し時は null（その他扱い）. */
  private ContentType classificationResult;

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
  }

  /**
   * コンストラクタ.
   *
   * @param rawContent メッセージ原文
   */
  public Content(final String rawContent) {
    this.rawContent = rawContent;
    this.contentList = new ArrayList<>();
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
   * 生成AIを用いて本文を分類する. 一定文字数未満の場合はAIを呼ばず「その他」とする.
   * 分類後は is案件紹介文() / is要員紹介文() が分類結果に基づいて動作する.
   *
   * @param transformer GPTクライアント（Gemini等）
   * @throws IOException 通信エラー時
   * @throws RuntimeException APIエラー時
   */
  public void classify(final Transformer transformer) throws IOException, RuntimeException {
    if (this.isEmpty()) {
      this.classificationResult = ContentType.OTHER;
      return;
    }
    int minLength = DEFAULT_MIN_LENGTH_FOR_CLASSIFICATION;
    String minLenProp = Properties.get(PROP_CONTENT_MIN_LENGTH);
    if (minLenProp != null && !minLenProp.trim().isEmpty()) {
      try {
        minLength = Integer.parseInt(minLenProp.trim());
      } catch (NumberFormatException e) {
        minLength = DEFAULT_MIN_LENGTH_FOR_CLASSIFICATION;
      }
    }
    if (this.rawContent.length() < minLength) {
      this.classificationResult = ContentType.OTHER;
      return;
    }
    String promptTemplate = Properties.get(PROP_CONTENT_CLASSIFICATION_PROMPT);
    if (promptTemplate == null || promptTemplate.isEmpty()) {
      this.classificationResult = ContentType.OTHER;
      return;
    }
    String prompt = promptTemplate + this.rawContent;
    GptAnswer answer = transformer.generate(prompt);
    String answerText = answer != null && answer.getAnswer() != null ? answer.getAnswer().trim() : "";
    if (answerText.isEmpty()) {
      this.classificationResult = ContentType.OTHER;
    } else if (answerText.contains("案件")) {
      this.classificationResult = ContentType.JOB;
    } else if (answerText.contains("要員")) {
      this.classificationResult = ContentType.PERSONNEL;
    } else if (answerText.contains("その他")) {
      this.classificationResult = ContentType.OTHER;
    } else {
      this.classificationResult = ContentType.OTHER;
    }
  }

  /**
   * このメッセージが案件の紹介文であるかどうかを判定します.
   * classify() が呼ばれていて、その結果が「案件」の場合に true.
   *
   * @return 案件紹介文と判定すればtrue、それ以外はfalse
   */
  public boolean is案件紹介文() {
    return !this.isEmpty() && this.classificationResult == ContentType.JOB;
  }

  /**
   * このメッセージが要員の紹介文であるかどうかを判定します.
   * classify() が呼ばれていて、その結果が「要員」の場合に true.
   *
   * @return 要員紹介文と判定すればtrue、それ以外はfalse
   */
  public boolean is要員紹介文() {
    return !this.isEmpty() && this.classificationResult == ContentType.PERSONNEL;
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
   * このメッセージが複数要員または複数案件の情報を持つかどうか判定し、
   * 複数であればこのクラスのリストに結果を持ちます.
   * 事前に classify() で案件 or 要員と判定されている必要がある.
   *
   * @param transformer GPTクライアント
   * @return 複数であればtrue、単一であればfalse
   * @throws IOException 通信エラー時
   * @throws RuntimeException APIエラー時
   */
  public boolean 複数判定処理実行(final Transformer transformer) throws IOException, RuntimeException {
    GptAnswer answer = null;

    final String 複数要員判定プロンプト = Properties.get(PROP_MULTI_PERSON_PROMPT);
    final String 複数案件判定プロンプト = Properties.get(PROP_MULTI_JOB_PROMPT);

    if (this.is要員紹介文()) {
      answer = transformer.generate(複数要員判定プロンプト + this.rawContent);
    } else if (this.is案件紹介文()) {
      answer = transformer.generate(複数案件判定プロンプト + this.rawContent);
    } else {
      return false;
    }

    if (answer != null && answer.length() > 10 && answer.isJsonArrayFormat()) {
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
    if (this.contentList != null && !this.contentList.isEmpty()) {
      return this.contentList.toString();
    }
    return this.rawContent;
  }
}
