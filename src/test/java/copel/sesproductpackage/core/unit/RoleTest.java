package copel.sesproductpackage.core.unit;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.EnumSet;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

/** {@link Role} の単体テスト。 */
class RoleTest {

  @Nested
  class getEnum {

    @Nested
    class 正常系 {

      /**
       * コード値からロールを取得することを確かめる。 DB に保存したロールのコードが読めないと権限の判定が誤るので、コードの対応を固定する。
       *
       * @given コード値が指定されている
       * @when getEnum をコード値で実行する
       * @then 対応するロールを返す
       */
      @ParameterizedTest
      @CsvSource({"00, システム利用不可", "10, システムユーザー", "20, 開発", "30, 運用", "99, システム管理者"})
      void コード値を指定した場合_対応するロールを返す(final String code, final Role expected) {
        assertThat(Role.getEnum(code)).isEqualTo(expected);
      }
    }

    @Nested
    class 境界値 {

      /**
       * 未定義のコード値や null を指定すると null を返すことを確かめる。 未定義のロールに既定の権限を与えると権限が誤って付与されるので、null で判定させる。
       *
       * @given コード値が未定義の値、または null
       * @when getEnum を未定義の値または null で実行する
       * @then null を返す
       */
      @NullSource
      @ValueSource(strings = {"98"})
      @ParameterizedTest
      void 未定義のコード値やnullの場合_nullを返す(final String code) {
        assertThat(Role.getEnum(code)).isNull();
      }
    }
  }

  @Nested
  class isSystemUseAuth {

    @Nested
    class 境界値 {

      /**
       * システム利用不可のロールに利用権限が無いことを確かめる。 利用不可のロールに利用権限を与えると制限が効かなくなるので、false を返す。
       *
       * @given ロールがシステム利用不可
       * @when isSystemUseAuth を実行する
       * @then false を返す
       */
      @Test
      void システム利用不可の場合_falseを返す() {
        assertThat(Role.システム利用不可.isSystemUseAuth()).isFalse();
      }

      /**
       * 利用不可の次に当たるシステムユーザーに利用権限があることを確かめる。 利用権限の境界が崩れると制限の範囲が変わるので、利用不可の次のロールを固定する。
       *
       * @given ロールがシステムユーザー
       * @when isSystemUseAuth を実行する
       * @then true を返す
       */
      @Test
      void システムユーザーの場合_trueを返す() {
        assertThat(Role.システムユーザー.isSystemUseAuth()).isTrue();
      }
    }
  }

  @Nested
  class getCode {

    @Nested
    class 正常系 {

      /**
       * ロールのコード値を取得することを確かめる。 DB に保存するコードが変わると既存データと一致しなくなるので、コードを固定する。
       *
       * @given ロールが運用
       * @when getCode を実行する
       * @then "30" を返す
       */
      @Test
      void 運用の場合_30を返す() {
        assertThat(Role.運用.getCode()).isEqualTo("30");
      }
    }
  }

  @Nested
  class getPermissions {

    @Nested
    class 認可 {

      /**
       * 運用ロールがウォッチ管理と送信者、送信元グループ、ユーザー、テナント設定の管理権限を持つことを確かめる。 運用担当の権限が変わると管理操作の範囲が変わるので、権限の集合を固定する。
       *
       * @given ロールが運用
       * @when getPermissions を実行する
       * @then ウォッチ管理を含む管理系の5件の権限を返す
       */
      @Test
      void 運用の場合_管理系の5件の権限を返す() {
        assertThat(Role.運用.getPermissions())
            .containsExactlyInAnyOrder(
                Permission.MANAGE_WATCH,
                Permission.MANAGE_SENDER,
                Permission.MANAGE_FROM_GROUP,
                Permission.MANAGE_USERS,
                Permission.MANAGE_TENANT_SETTINGS);
      }

      /**
       * システム管理者が全ての権限を持つことを確かめる。 最上位のロールに全権限が揃わないと管理操作ができなくなるので、全権限を固定する。
       *
       * @given ロールがシステム管理者
       * @when getPermissions を実行する
       * @then 全ての権限を返す
       */
      @Test
      void システム管理者の場合_全ての権限を返す() {
        assertThat(Role.システム管理者.getPermissions()).isEqualTo(EnumSet.allOf(Permission.class));
      }

      /**
       * システム利用不可のロールが権限を持たないことを確かめる。 利用不可のロールに権限を与えると制限が効かなくなるので、空の集合を返す。
       *
       * @given ロールがシステム利用不可
       * @when getPermissions を実行する
       * @then 権限を持たない空の集合を返す
       */
      @Test
      void システム利用不可の場合_空の集合を返す() {
        assertThat(Role.システム利用不可.getPermissions()).isEmpty();
      }
    }
  }
}
