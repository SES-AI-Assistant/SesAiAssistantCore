package copel.sesproductpackage.core.util;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;

/**
 * AWS Lambda リクエストハンドラーの共通基底クラス。 自動的に requestId を MDC に設定し、ログ出力時に含める。
 *
 * @param <I> 入力型（SQSEvent、S3Event など）
 * @param <O> 出力型（String など）
 * @author Copel Co., Ltd.
 */
@Slf4j
public abstract class BaseLambdaHandler<I, O> implements RequestHandler<I, O> {

  /**
   * Lambda リクエストハンドラー。 requestId を自動的に MDC に設定してから、具体的な処理を実行する。
   *
   * @param input Lambda イベント入力
   * @param context Lambda コンテキスト
   * @return 処理結果
   */
  @Override
  public final O handleRequest(I input, Context context) {
    String requestId = context.getAwsRequestId();
    MDC.put("requestId", requestId);
    try {
      return handleRequestInternal(input, context);
    } finally {
      MDC.clear();
    }
  }

  /**
   * 具体的な Lambda 処理。 各 Lambda 実装クラスで override する。
   *
   * @param input Lambda イベント入力
   * @param context Lambda コンテキスト
   * @return 処理結果
   */
  protected abstract O handleRequestInternal(I input, Context context);
}
