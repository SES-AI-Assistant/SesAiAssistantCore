package copel.sesproductpackage.core.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** {@link SsmParameterKey} の単体テスト。 */
class SsmParameterKeyTest {

  @Nested
  class getKey {

    @Nested
    class 正常系 {

      /**
       * キーを指定すると、接頭辞を除いた infrastructure から始まるパスを返すことを確かめる。 Parameter Store
       * から読み込んだ設定値と同じキーで参照できることを守る。
       *
       * @given キーが各種の列挙値
       * @when getKey を実行する
       * @then 対応する infrastructure から始まるパスを返す
       */
      @ParameterizedTest
      @CsvSource({
        "REGISTER_QUEUE_URL, infrastructure/messaging/register-queue/url",
        "VPC_ID, infrastructure/network/vpc/id",
        "TENANT_MONTHLY_INGEST_COUNT_TABLE_ARN, infrastructure/dynamodb/tenant-monthly-ingest-count-table/arn"
      })
      void 列挙値を指定した場合_対応するパスを返す(final SsmParameterKey key, final String expected) {
        assertThat(key.getKey()).isEqualTo(expected);
      }

      /**
       * 全てのキーが infrastructure から始まるパスであることを確かめる。 接頭辞を含むキーが混入すると設定値を参照できなくなるので、パスの形式を保証する。
       *
       * @given 全ての列挙値
       * @when 全ての列挙値の getKey を実行する
       * @then 全てのパスが infrastructure から始まる
       */
      @Test
      void 全てのキーの場合_infrastructureから始まるパスを返す() {
        for (SsmParameterKey key : SsmParameterKey.values()) {
          assertThat(key.getKey()).startsWith("infrastructure/");
        }
      }
    }
  }
}
