package copel.sesproductpackage.core.util;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import lombok.extern.slf4j.Slf4j;

/**
 * AWS Lambda リクエストハンドラーの共通基底クラス。
 *
 * @param <I> 入力型（SQSEvent、S3Event など）
 * @param <O> 出力型（String など）
 * @author Copel Co., Ltd.
 */
@Slf4j
public abstract class BaseLambdaHandler<I, O> implements RequestHandler<I, O> {

  /**
   * 具体的な Lambda 処理。 各 Lambda 実装クラスで override する。
   *
   * @param input Lambda イベント入力
   * @param context Lambda コンテキスト
   * @return 処理結果
   */
  protected abstract O handleRequestInternal(I input, Context context);
}
