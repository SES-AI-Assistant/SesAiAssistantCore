package copel.sesproductpackage.core.api.gpt;

import java.io.IOException;

/**
 * GPT処理リスナー.
 *
 * @author Copel Co., Ltd..
 */
public interface Transformer {
  /**
   * 引数に入力された文字列をエンベディング処理しベクトル値を返却します.
   *
   * @param input 入力
   * @return ベクトル値
   * @throws IOException
   * @throws RuntimeException
   */
  float[] embedding(final String inputString) throws IOException, RuntimeException;

  /**
   * 引数に入力された文字列を質問として、LLMに回答の生成を実行させその回答を返却します.
   *
   * @param prompt プロンプト
   * @return 回答
   * @throws IOException
   * @throws RuntimeException
   */
  GptAnswer generate(final String prompt) throws IOException, RuntimeException;

  /**
   * LLM APIに対してプロンプトを送信し、指定された型に従ってJSONスキーマベースの構造化出力を取得します.
   *
   * <p>JSONスキーマは@Schemaアノテーションとリフレクションから自動生成されます。
   *
   * <p>API使用履歴は自動的にDBに記録されます。
   *
   * <p>使用例：
   *
   * <pre>{@code
   * class PersonResponse {
   *   @Schema(description = "人物の名前", required = true)
   *   private String name;
   *
   *   @Schema(description = "人物の年齢", required = true)
   *   private int age;
   *
   *   @Schema(description = "スキル一覧", itemType = String.class)
   *   private List<String> skills;
   * }
   *
   * Transformer transformer = new Gemini("YOUR_API_KEY");
   * PersonResponse response = transformer.generate(
   *   "田中太郎という30歳の人物。スキルはJavaとPythonです。",
   *   PersonResponse.class
   * );
   * System.out.println(response.getName());     // "田中太郎"
   * System.out.println(response.getAge());      // 30
   * System.out.println(response.getSkills());   // [Java, Python]
   * }</pre>
   *
   * @param <T> 応答型
   * @param prompt LLM APIに送信するプロンプト
   * @param responseType 応答型を指定するClass<T>
   * @return プロンプトに対する応答をresponseType型のインスタンスとして返します
   * @throws IOException ファイルI/Oエラーが発生した場合
   * @throws RuntimeException APIエラー、スキーマ不正、マッピングエラーが発生した場合
   */
  default <T> T generate(final String prompt, final Class<T> responseType)
      throws IOException, RuntimeException {
    throw new UnsupportedOperationException(
        "Structured generation is not supported by " + this.getClass().getSimpleName());
  }
}
