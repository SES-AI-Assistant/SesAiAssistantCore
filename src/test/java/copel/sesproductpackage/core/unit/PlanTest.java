package copel.sesproductpackage.core.unit;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

/** {@link Plan} の単体テスト。 */
class PlanTest {

  @Nested
  class getEnum {

    @Nested
    class 正常系 {

      /**
       * コード値からプランを取得することを確かめる。 DB に保存したプランのコードが読めないと権限が変わるので、コードの対応を固定する。
       *
       * @given コード値が "10"
       * @when getEnum を "10" で実行する
       * @then PREMIUM を返す
       */
      @Test
      void コード値が10の場合_PREMIUMを返す() {
        assertThat(Plan.getEnum("10")).isEqualTo(Plan.PREMIUM);
      }
    }

    @Nested
    class 境界値 {

      /**
       * 未定義のコード値や null を指定すると FREE を返すことを確かめる。 不明なプランを有料の権限で扱うと権限が過剰になるので、FREE にそろえる。
       *
       * @given コード値が未定義の値、または null
       * @when getEnum を未定義の値または null で実行する
       * @then FREE を返す
       */
      @NullSource
      @ValueSource(strings = {"99"})
      @ParameterizedTest
      void 未定義のコード値やnullの場合_FREEを返す(final String code) {
        assertThat(Plan.getEnum(code)).isEqualTo(Plan.FREE);
      }
    }
  }

  @Nested
  class getCode {

    @Nested
    class 正常系 {

      /**
       * プランのコード値を取得することを確かめる。 DB に保存するコードが変わると既存データと一致しなくなるので、コードを固定する。
       *
       * @given プランが PREMIUM
       * @when getCode を実行する
       * @then "10" を返す
       */
      @Test
      void PREMIUMの場合_10を返す() {
        assertThat(Plan.PREMIUM.getCode()).isEqualTo("10");
      }
    }
  }

  @Nested
  class getName {

    @Nested
    class 正常系 {

      /**
       * プランの名称を取得することを確かめる。 画面に表示する名称が変わると利用者に混乱を与えるので、名称を固定する。
       *
       * @given プランが FREE
       * @when getName を実行する
       * @then "フリープラン" を返す
       */
      @Test
      void FREEの場合_フリープランを返す() {
        assertThat(Plan.FREE.getName()).isEqualTo("フリープラン");
      }
    }
  }

  @Nested
  class getPermissions {

    @Nested
    class 認可 {

      /**
       * FREE プランが閲覧系と監視管理の権限だけを持つことを確かめる。 無料プランで使える機能の範囲が変わると権限の境界が崩れるので、権限の集合を固定する。
       *
       * @given プランが FREE
       * @when getPermissions を実行する
       * @then 閲覧系の2件と監視管理の計3件の権限を返す
       */
      @Test
      void FREEの場合_閲覧系と監視管理の3件の権限を返す() {
        assertThat(Plan.FREE.getPermissions())
            .containsExactlyInAnyOrder(
                Permission.VIEW_MATCHING_LIST, Permission.VIEW_JOB_LIST, Permission.MANAGE_WATCH);
      }

      /**
       * PREMIUM プランが管理系を除く6件の権限を持つことを確かめる。 管理系の権限をプランに含めると無料と有料の区別が崩れるので、含まないことを固定する。
       *
       * @given プランが PREMIUM
       * @when getPermissions を実行する
       * @then 管理系を除く6件の権限を返す
       */
      @Test
      void PREMIUMの場合_管理系を除く6件の権限を返す() {
        Set<Permission> permissions = Plan.PREMIUM.getPermissions();

        assertThat(permissions)
            .containsExactlyInAnyOrder(
                Permission.VIEW_MATCHING_LIST,
                Permission.VIEW_JOB_LIST,
                Permission.DOWNLOAD_SKILLSHEET,
                Permission.EXEC_AI_MATCHING,
                Permission.REGISTER_INFO_LIST_IMPORT,
                Permission.MANAGE_WATCH);
      }
    }
  }
}
