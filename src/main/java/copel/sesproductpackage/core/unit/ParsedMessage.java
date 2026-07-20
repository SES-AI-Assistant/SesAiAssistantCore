package copel.sesproductpackage.core.unit;

import lombok.Data;

/**
 * Geminiの一発化処理によってパースされたメッセージの解析結果を保持するクラス.
 *
 * @author Copel Co., Ltd.
 */
@Data
public class ParsedMessage {
  /** 案件(JOB) / 要員(PERSON) などの判定タイプ. */
  private String type;

  /** 抽出された要約テキスト. */
  private String summary;

  /** 抽出された単価. */
  private Money unitPrice;

  /** デフォルトコンストラクタ. */
  public ParsedMessage() {}

  /**
   * コンストラクタ.
   *
   * @param type 判定タイプ
   * @param summary 要約テキスト
   * @param unitPrice 単価
   */
  public ParsedMessage(final String type, final String summary, final Money unitPrice) {
    this.type = type;
    this.summary = summary;
    this.unitPrice = unitPrice;
  }
}
