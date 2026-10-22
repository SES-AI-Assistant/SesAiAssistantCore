package copel.sesproductpackage.core.database;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** {@link SES_AI_T_MATCH_JOB_PERSON} の単体テスト。 */
class SES_AI_T_MATCH_JOB_PERSONTest {

  /** テナントID。 */
  private static final String TENANT_ID = "T001";

  @Nested
  class コンストラクタ {

    @Nested
    class 正常系 {

      /**
       * テナントIDを指定して生成すると、そのテナントIDを保持することを確かめる。 結合の結果も自テナントの行に限る。
       *
       * @given テナントIDが "T001"
       * @when SES_AI_T_MATCH_JOB_PERSON を "T001" で生成する
       * @then 指定したテナントIDの "T001" を返す
       */
      @Test
      void テナントIDを指定した場合_指定したテナントIDを返す() {
        assertThat(new SES_AI_T_MATCH_JOB_PERSON(TENANT_ID).getTenantId()).isEqualTo(TENANT_ID);
      }
    }

    @Nested
    class 異常系 {

      /**
       * テナントIDが null の場合に生成を拒否することを確かめる。 テナントの無い結合結果は他テナントの行を含む。
       *
       * @given テナントIDが null
       * @when SES_AI_T_MATCH_JOB_PERSON を null で生成する
       * @then IllegalArgumentExceptionを投げる
       */
      @Test
      void テナントIDがnullの場合_IllegalArgumentExceptionを投げる() {
        assertThatThrownBy(() -> new SES_AI_T_MATCH_JOB_PERSON(null))
            .isInstanceOf(IllegalArgumentException.class);
      }
    }
  }

  @Nested
  class hasJobId {

    @Nested
    class 正常系 {

      /**
       * 案件IDがある場合に true を返すことを確かめる。 案件名と単価を表示する行かを判定する。
       *
       * @given 案件IDが "J001"
       * @when hasJobId を実行する
       * @then trueを返す
       */
      @Test
      void 案件IDがある場合_trueを返す() {
        SES_AI_T_MATCH_JOB_PERSON entity = new SES_AI_T_MATCH_JOB_PERSON(TENANT_ID);
        entity.setJobId("J001");

        assertThat(entity.hasJobId()).isTrue();
      }
    }

    @Nested
    class 境界値 {

      /**
       * 案件IDが null の場合に false を返すことを確かめる。 案件が削除されて結合先が無い行を、案件ありと扱わない。
       *
       * @given 案件IDが null
       * @when hasJobId を実行する
       * @then falseを返す
       */
      @Test
      void 案件IDがnullの場合_falseを返す() {
        assertThat(new SES_AI_T_MATCH_JOB_PERSON(TENANT_ID).hasJobId()).isFalse();
      }

      /**
       * 案件IDが空文字の場合に false を返すことを確かめる。 空の案件IDは結合先の案件を指さない。
       *
       * @given 案件IDが空文字
       * @when hasJobId を実行する
       * @then falseを返す
       */
      @Test
      void 案件IDが空文字の場合_falseを返す() {
        SES_AI_T_MATCH_JOB_PERSON entity = new SES_AI_T_MATCH_JOB_PERSON(TENANT_ID);
        entity.setJobId("");

        assertThat(entity.hasJobId()).isFalse();
      }
    }
  }

  @Nested
  class hasPersonId {

    @Nested
    class 正常系 {

      /**
       * 要員IDがある場合に true を返すことを確かめる。 要員名と単価を表示する行かを判定する。
       *
       * @given 要員IDが "P001"
       * @when hasPersonId を実行する
       * @then trueを返す
       */
      @Test
      void 要員IDがある場合_trueを返す() {
        SES_AI_T_MATCH_JOB_PERSON entity = new SES_AI_T_MATCH_JOB_PERSON(TENANT_ID);
        entity.setPersonId("P001");

        assertThat(entity.hasPersonId()).isTrue();
      }
    }

    @Nested
    class 境界値 {

      /**
       * 要員IDが null の場合に false を返すことを確かめる。 要員が削除されて結合先が無い行を、要員ありと扱わない。
       *
       * @given 要員IDが null
       * @when hasPersonId を実行する
       * @then falseを返す
       */
      @Test
      void 要員IDがnullの場合_falseを返す() {
        assertThat(new SES_AI_T_MATCH_JOB_PERSON(TENANT_ID).hasPersonId()).isFalse();
      }

      /**
       * 要員IDが空文字の場合に false を返すことを確かめる。 空の要員IDは結合先の要員を指さない。
       *
       * @given 要員IDが空文字
       * @when hasPersonId を実行する
       * @then falseを返す
       */
      @Test
      void 要員IDが空文字の場合_falseを返す() {
        SES_AI_T_MATCH_JOB_PERSON entity = new SES_AI_T_MATCH_JOB_PERSON(TENANT_ID);
        entity.setPersonId("");

        assertThat(entity.hasPersonId()).isFalse();
      }
    }
  }

  @Nested
  class insert {

    @Nested
    class 異常系 {

      /**
       * 読み取り専用のため登録を拒否し、例外を投げることを確かめる。 結合の結果を登録すると、マッチングと案件と要員の対応が壊れる。
       *
       * @given 読み取り専用の結合Entity
       * @when insert を接続で実行する
       * @then UnsupportedOperationExceptionを投げる
       */
      @Test
      void 読み取り専用の場合_UnsupportedOperationExceptionを投げる() {
        assertThatThrownBy(() -> new SES_AI_T_MATCH_JOB_PERSON(TENANT_ID).insert((Connection) null))
            .isInstanceOf(UnsupportedOperationException.class);
      }
    }
  }

  @Nested
  class selectByPk {

    @Nested
    class 異常系 {

      /**
       * 読み取り専用のため主キーによる取得を拒否し、例外を投げることを確かめる。 結合の結果を単体の取得として扱うと、結合先の項目が欠ける。
       *
       * @given 読み取り専用の結合Entity
       * @when selectByPk を接続で実行する
       * @then UnsupportedOperationExceptionを投げる
       */
      @Test
      void 読み取り専用の場合_UnsupportedOperationExceptionを投げる() {
        assertThatThrownBy(
                () -> new SES_AI_T_MATCH_JOB_PERSON(TENANT_ID).selectByPk((Connection) null))
            .isInstanceOf(UnsupportedOperationException.class);
      }
    }
  }

  @Nested
  class updateByPk {

    @Nested
    class 異常系 {

      /**
       * 読み取り専用のため更新を拒否し、例外を投げることを確かめる。 結合の結果を更新すると、結合先の案件や要員の値を誤って書き換える。
       *
       * @given 読み取り専用の結合Entity
       * @when updateByPk を接続で実行する
       * @then UnsupportedOperationExceptionを投げる
       */
      @Test
      void 読み取り専用の場合_UnsupportedOperationExceptionを投げる() {
        assertThatThrownBy(
                () -> new SES_AI_T_MATCH_JOB_PERSON(TENANT_ID).updateByPk((Connection) null))
            .isInstanceOf(UnsupportedOperationException.class);
      }
    }
  }

  @Nested
  class deleteByPk {

    @Nested
    class 異常系 {

      /**
       * 読み取り専用のため削除を拒否し、例外を投げることを確かめる。 結合の結果から削除すると、結合先の案件や要員の行が消えるおそれがある。
       *
       * @given 読み取り専用の結合Entity
       * @when deleteByPk を接続で実行する
       * @then UnsupportedOperationExceptionを投げる
       */
      @Test
      void 読み取り専用の場合_UnsupportedOperationExceptionを投げる() {
        assertThatThrownBy(
                () -> new SES_AI_T_MATCH_JOB_PERSON(TENANT_ID).deleteByPk((Connection) null))
            .isInstanceOf(UnsupportedOperationException.class);
      }
    }
  }
}
