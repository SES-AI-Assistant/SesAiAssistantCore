package copel.sesproductpackage.core.api.gpt;

/** GPT（LLM）のモデルを表すインターフェースです。 */
public interface GptModel {
  /**
   * GPTモデル名を返却します.
   *
   * @return GPTモデル名
   */
  String getModelName();
}
