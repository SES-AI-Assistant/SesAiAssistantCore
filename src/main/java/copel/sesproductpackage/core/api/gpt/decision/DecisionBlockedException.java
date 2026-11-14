package copel.sesproductpackage.core.api.gpt.decision;

/**
 * 判定 API が入力を安全性のフィルタでブロックしたことを表す例外.
 *
 * <p>ブロックは入力の内容に起因するため、同じ入力で再試行しても成功しない。呼び出し側は、この例外を捕まえて個別に扱う。
 *
 * @author Copel Co., Ltd.
 */
public class DecisionBlockedException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  /**
   * コンストラクタ.
   *
   * @param message 例外メッセージ
   */
  public DecisionBlockedException(final String message) {
    super(message);
  }
}
