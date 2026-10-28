package copel.sesproductpackage.core.unit;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** {@link Permission} の単体テスト。 */
class PermissionTest {

  @Nested
  class getName {

    @Nested
    class 正常系 {

      /**
       * 権限の名称を取得することを確かめる。 画面に表示する権限名を変えないことを守る。
       *
       * @given 権限の列挙値が指定されている
       * @when getName を実行する
       * @then 権限に対応する日本語の名称を返す
       */
      @ParameterizedTest
      @CsvSource({
        "VIEW_MATCHING_LIST, マッチング一覧閲覧",
        "VIEW_JOB_LIST, 案件一覧閲覧",
        "DOWNLOAD_SKILLSHEET, スキルシートダウンロード",
        "EXEC_AI_MATCHING, AIマッチング実行",
        "REGISTER_INFO_LIST_IMPORT, 一覧から案件・要員登録",
        "MANAGE_WATCH, ウォッチ管理",
        "MANAGE_SENDER, 送信者マスタ管理",
        "MANAGE_FROM_GROUP, 送信元グループマスタ管理",
        "VIEW_USAGE_HISTORY, 利用履歴閲覧",
        "MANAGE_USERS, ユーザー管理",
        "MANAGE_MASTER_DATA, マスターデータ管理",
        "MANAGE_TENANT_SETTINGS, テナント設定管理"
      })
      void 権限の列挙値を指定した場合_日本語の名称を返す(final Permission permission, final String expected) {
        assertThat(permission.getName()).isEqualTo(expected);
      }
    }
  }
}
