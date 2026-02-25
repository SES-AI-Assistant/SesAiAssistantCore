package copel.sesproductpackage.core.api.gpt;

import java.io.IOException;

/**
 * 【SES AIアシスタント】 GPT処理リスナー.
 *
 * @author 鈴木一矢.
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
}
