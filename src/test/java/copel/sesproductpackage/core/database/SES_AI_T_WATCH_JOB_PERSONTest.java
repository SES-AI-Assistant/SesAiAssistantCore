package copel.sesproductpackage.core.database;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import copel.sesproductpackage.core.database.SES_AI_T_WATCH.TargetType;
import java.sql.Connection;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** {@link SES_AI_T_WATCH_JOB_PERSON} の単体テスト。 */
class SES_AI_T_WATCH_JOB_PERSONTest {

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
       * @when SES_AI_T_WATCH_JOB_PERSON を "T001" で生成する
       * @then 指定したテナントIDの "T001" を返す
       */
      @Test
      void テナントIDを指定した場合_指定したテナントIDを返す() {
        assertThat(new SES_AI_T_WATCH_JOB_PERSON(TENANT_ID).getTenantId()).isEqualTo(TENANT_ID);
      }
    }

    @Nested
    class 異常系 {

      /**
       * テナントIDが null の場合に生成を拒否することを確かめる。 テナントの無い結合結果は他テナントの行を含む。
       *
       * @given テナントIDが null
       * @when SES_AI_T_WATCH_JOB_PERSON を null で生成する
       * @then IllegalArgumentExceptionを投げる
       */
      @Test
      void テナントIDがnullの場合_IllegalArgumentExceptionを投げる() {
        assertThatThrownBy(() -> new SES_AI_T_WATCH_JOB_PERSON(null))
            .isInstanceOf(IllegalArgumentException.class);
      }
    }
  }

  @Nested
  class hasJobInfo {

    @Nested
    class 正常系 {

      /**
       * 対象種別が JOB で案件名がある場合に案件情報ありと判定することを確かめる。 案件の詳細を画面に表示するかの判定に使う。
       *
       * @given 対象種別が JOB、案件名が "案件名"
       * @when hasJobInfo を実行する
       * @then trueを返す
       */
      @Test
      void 対象種別がJOBで案件名がある場合_trueを返す() {
        SES_AI_T_WATCH_JOB_PERSON entity = new SES_AI_T_WATCH_JOB_PERSON(TENANT_ID);
        entity.setTargetType(TargetType.JOB);
        entity.setJobTitle("案件名");

        assertThat(entity.hasJobInfo()).isTrue();
      }
    }

    @Nested
    class 境界値 {

      /**
       * 対象種別が JOB でも案件名が null の場合に案件情報なしと判定することを確かめる。 案件が削除されて結合先が無い行を、案件ありと扱わない。
       *
       * @given 対象種別が JOB、案件名が null
       * @when hasJobInfo を実行する
       * @then falseを返す
       */
      @Test
      void 案件名がnullの場合_falseを返す() {
        SES_AI_T_WATCH_JOB_PERSON entity = new SES_AI_T_WATCH_JOB_PERSON(TENANT_ID);
        entity.setTargetType(TargetType.JOB);

        assertThat(entity.hasJobInfo()).isFalse();
      }

      /**
       * 案件名があっても対象種別が PERSON の場合に案件情報なしと判定することを確かめる。 要員のウォッチに案件の情報を混ぜない。
       *
       * @given 対象種別が PERSON、案件名が "案件名"
       * @when hasJobInfo を実行する
       * @then falseを返す
       */
      @Test
      void 対象種別がPERSONの場合_falseを返す() {
        SES_AI_T_WATCH_JOB_PERSON entity = new SES_AI_T_WATCH_JOB_PERSON(TENANT_ID);
        entity.setTargetType(TargetType.PERSON);
        entity.setJobTitle("案件名");

        assertThat(entity.hasJobInfo()).isFalse();
      }
    }
  }

  @Nested
  class hasPersonInfo {

    @Nested
    class 正常系 {

      /**
       * 対象種別が PERSON で要員名がある場合に要員情報ありと判定することを確かめる。 要員の詳細を画面に表示するかの判定に使う。
       *
       * @given 対象種別が PERSON、要員名が "山田太郎"
       * @when hasPersonInfo を実行する
       * @then trueを返す
       */
      @Test
      void 対象種別がPERSONで要員名がある場合_trueを返す() {
        SES_AI_T_WATCH_JOB_PERSON entity = new SES_AI_T_WATCH_JOB_PERSON(TENANT_ID);
        entity.setTargetType(TargetType.PERSON);
        entity.setPersonName("山田太郎");

        assertThat(entity.hasPersonInfo()).isTrue();
      }
    }

    @Nested
    class 境界値 {

      /**
       * 対象種別が PERSON でも要員名が null の場合に要員情報なしと判定することを確かめる。 要員が削除されて結合先が無い行を、要員ありと扱わない。
       *
       * @given 対象種別が PERSON、要員名が null
       * @when hasPersonInfo を実行する
       * @then falseを返す
       */
      @Test
      void 要員名がnullの場合_falseを返す() {
        SES_AI_T_WATCH_JOB_PERSON entity = new SES_AI_T_WATCH_JOB_PERSON(TENANT_ID);
        entity.setTargetType(TargetType.PERSON);

        assertThat(entity.hasPersonInfo()).isFalse();
      }

      /**
       * 要員名があっても対象種別が JOB の場合に要員情報なしと判定することを確かめる。 案件のウォッチに要員の情報を混ぜない。
       *
       * @given 対象種別が JOB、要員名が "山田太郎"
       * @when hasPersonInfo を実行する
       * @then falseを返す
       */
      @Test
      void 対象種別がJOBの場合_falseを返す() {
        SES_AI_T_WATCH_JOB_PERSON entity = new SES_AI_T_WATCH_JOB_PERSON(TENANT_ID);
        entity.setTargetType(TargetType.JOB);
        entity.setPersonName("山田太郎");

        assertThat(entity.hasPersonInfo()).isFalse();
      }
    }
  }

  @Nested
  class insert {

    @Nested
    class 異常系 {

      /**
       * 読み取り専用のため登録を拒否し、例外を投げることを確かめる。 結合の結果を登録すると、ウォッチと案件と要員の対応が壊れる。
       *
       * @given 読み取り専用の結合Entity
       * @when insert を接続で実行する
       * @then UnsupportedOperationExceptionを投げる
       */
      @Test
      void 読み取り専用の場合_UnsupportedOperationExceptionを投げる() {
        assertThatThrownBy(() -> new SES_AI_T_WATCH_JOB_PERSON(TENANT_ID).insert((Connection) null))
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
                () -> new SES_AI_T_WATCH_JOB_PERSON(TENANT_ID).selectByPk((Connection) null))
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
                () -> new SES_AI_T_WATCH_JOB_PERSON(TENANT_ID).updateByPk((Connection) null))
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
                () -> new SES_AI_T_WATCH_JOB_PERSON(TENANT_ID).deleteByPk((Connection) null))
            .isInstanceOf(UnsupportedOperationException.class);
      }
    }
  }
}
