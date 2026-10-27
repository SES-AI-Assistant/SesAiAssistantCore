package copel.sesproductpackage.core.database;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import copel.sesproductpackage.core.unit.OriginalDateTime;
import copel.sesproductpackage.core.unit.Permission;
import copel.sesproductpackage.core.unit.Plan;
import copel.sesproductpackage.core.unit.Role;
import copel.sesproductpackage.tools.testsupport.db.JdbcMocks;
import copel.sesproductpackage.tools.testsupport.db.MockDbConnection;
import copel.sesproductpackage.tools.testsupport.jdbc.MockResultSetBuilder;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** {@link SES_AI_WEBAPP_M_USER} の単体テスト。 */
class SES_AI_WEBAPP_M_USERTest {

  /** テナントID。 */
  private static final String TENANT_ID = "T001";

  /** ユーザーID。 */
  private static final String USER_ID = "U001";

  /** 登録日時の文字列。 */
  private static final String REGISTER_DATE_TEXT = "2026-01-15 10:00:00";

  /** 取得結果の列。 */
  private static final String[] COLUMNS = {
    "user_id", "user_name", "role_cd", "plan_cd", "register_date", "register_user", "tenant_id"
  };

  /**
   * 準備文のモックを作り、接続の prepareStatement がその準備文を返すように設定する。
   *
   * @param connection 接続のモック
   * @param updated executeUpdate が返す更新件数
   * @return 準備文のモック
   * @throws SQLException 設定中に例外が起きた場合
   */
  private static PreparedStatement stubUpdate(final Connection connection, final int updated)
      throws SQLException {
    PreparedStatement statement = mock(PreparedStatement.class);
    when(statement.executeUpdate()).thenReturn(updated);
    when(connection.prepareStatement(anyString())).thenReturn(statement);
    return statement;
  }

  /**
   * 項目が揃ったユーザーを生成する。ロールは運用、プランは PREMIUM とする。
   *
   * @return 生成したユーザー
   */
  private static SES_AI_WEBAPP_M_USER user() {
    SES_AI_WEBAPP_M_USER user = new SES_AI_WEBAPP_M_USER(TENANT_ID);
    user.setUserId(USER_ID);
    user.setUserName("山田太郎");
    user.setRole(Role.運用);
    user.setPlan(Plan.PREMIUM);
    user.setRegisterDate(new OriginalDateTime(REGISTER_DATE_TEXT));
    user.setRegisterUser("U000");
    return user;
  }

  /**
   * 役割とプランと登録日時が未設定のユーザーを生成する。
   *
   * @return 生成したユーザー
   */
  private static SES_AI_WEBAPP_M_USER userWithoutOptionalFields() {
    SES_AI_WEBAPP_M_USER user = new SES_AI_WEBAPP_M_USER(TENANT_ID);
    user.setUserId(USER_ID);
    return user;
  }

  @Nested
  class コンストラクタ_String {

    @Nested
    class 正常系 {

      /**
       * テナントIDを指定して生成すると、そのテナントIDを保持することを確かめる。 ユーザーを所属テナントの条件で検索する。
       *
       * @given テナントIDが "T001"
       * @when SES_AI_WEBAPP_M_USER を "T001" で生成する
       * @then 指定したテナントIDの "T001" を返す
       */
      @Test
      void テナントIDを指定した場合_指定したテナントIDを返す() {
        assertThat(new SES_AI_WEBAPP_M_USER(TENANT_ID).getTenantId()).isEqualTo(TENANT_ID);
      }
    }

    @Nested
    class 異常系 {

      /**
       * テナントIDが null の場合に生成を拒否することを確かめる。 テナントの無いユーザーは他テナントの権限で扱われる。
       *
       * @given テナントIDが null
       * @when SES_AI_WEBAPP_M_USER を null で生成する
       * @then IllegalArgumentExceptionを投げる
       */
      @Test
      void テナントIDがnullの場合_IllegalArgumentExceptionを投げる() {
        assertThatThrownBy(() -> new SES_AI_WEBAPP_M_USER(null))
            .isInstanceOf(IllegalArgumentException.class);
      }
    }
  }

  @Nested
  class コンストラクタ {

    @Nested
    class 正常系 {

      /**
       * テナントIDを指定せずに生成すると、ログイン時の検索用の仮のテナントIDを保持することを確かめる。 テナントIDはログイン後に設定するため、生成時は仮の値を入れる。
       *
       * @given テナントIDを指定しない
       * @when SES_AI_WEBAPP_M_USER を引数なしで生成する
       * @then 仮のテナントID "_temp_" を返す
       */
      @Test
      void テナントIDを指定しない場合_仮のテナントID_tempを返す() {
        assertThat(new SES_AI_WEBAPP_M_USER().getTenantId()).isEqualTo("_temp_");
      }
    }
  }

  @Nested
  class isRegistered {

    @Nested
    class 正常系 {

      /**
       * 単体の検索で行が見つかった場合に登録済みとして true を返すことを確かめる。 未登録と登録済みで画面の遷移を分ける。
       *
       * @given ユーザーIDが "U001"、SELECT の結果が 1 行
       * @when selectByPk を接続で実行してから isRegistered を実行する
       * @then trueを返す
       */
      @MockDbConnection
      @Test
      void 該当行がある場合_trueを返す(final Connection connection) throws SQLException {
        JdbcMocks.stubQuery(
            connection,
            MockResultSetBuilder.builder()
                .columns(COLUMNS)
                .row(USER_ID, "山田太郎", "30", "10", REGISTER_DATE_TEXT, "U000", TENANT_ID)
                .build());
        SES_AI_WEBAPP_M_USER user = userWithoutOptionalFields();

        user.selectByPk(connection);

        assertThat(user.isRegistered()).isTrue();
      }

      /**
       * 単体の検索で行が見つからない場合に未登録として false を返すことを確かめる。 未登録のユーザーを登録済みと誤認しない。
       *
       * @given ユーザーIDが "U001"、SELECT の結果が 0 行
       * @when selectByPk を接続で実行してから isRegistered を実行する
       * @then falseを返す
       */
      @MockDbConnection
      @Test
      void 該当行が無い場合_falseを返す(final Connection connection) throws SQLException {
        JdbcMocks.stubQuery(connection, MockResultSetBuilder.builder().columns(COLUMNS).build());
        SES_AI_WEBAPP_M_USER user = userWithoutOptionalFields();

        user.selectByPk(connection);

        assertThat(user.isRegistered()).isFalse();
      }
    }
  }

  @Nested
  class hasSystemUseAuth {

    @Nested
    class 認可 {

      /**
       * ロールが未設定の場合にシステムを利用できないと判定することを確かめる。 ロールが無いユーザーに業務機能を開放しない。
       *
       * @given ロールが null、プランが PREMIUM
       * @when hasSystemUseAuth を実行する
       * @then falseを返す
       */
      @Test
      void ロールが未設定の場合_falseを返す() {
        SES_AI_WEBAPP_M_USER user = user();
        user.setRole(null);

        assertThat(user.hasSystemUseAuth()).isFalse();
      }

      /**
       * ロールがシステム利用不可の場合にシステムを利用できないと判定することを確かめる。 システム利用不可のロールは業務機能を一切利用させない。
       *
       * @given ロールが システム利用不可、プランが PREMIUM
       * @when hasSystemUseAuth を実行する
       * @then falseを返す
       */
      @Test
      void ロールがシステム利用不可の場合_falseを返す() {
        SES_AI_WEBAPP_M_USER user = user();
        user.setRole(Role.システム利用不可);

        assertThat(user.hasSystemUseAuth()).isFalse();
      }

      /**
       * ロールがシステムユーザーの場合にシステムを利用できると判定することを確かめる。 利用可能な最も低いロールの境界を確かめる。
       *
       * @given ロールが システムユーザー、プランが PREMIUM
       * @when hasSystemUseAuth を実行する
       * @then trueを返す
       */
      @Test
      void ロールがシステムユーザーの場合_trueを返す() {
        SES_AI_WEBAPP_M_USER user = user();
        user.setRole(Role.システムユーザー);

        assertThat(user.hasSystemUseAuth()).isTrue();
      }
    }
  }

  @Nested
  class getPermissions {

    @Nested
    class 正常系 {

      /**
       * ロールとプランの権限を合わせた権限集合を返すことを確かめる。 運用ロールの管理権限と、PREMIUM プランの一覧インポート権限の両方を使える。
       *
       * @given ロールが 運用、プランが PREMIUM
       * @when getPermissions を実行する
       * @then 利用者管理と一覧インポートの権限を含む権限集合を返す
       */
      @Test
      void ロールが運用でプランがPREMIUMの場合_両方の権限を含む権限集合を返す() {
        assertThat(user().getPermissions())
            .contains(Permission.MANAGE_USERS, Permission.REGISTER_INFO_LIST_IMPORT);
      }
    }

    @Nested
    class 境界値 {

      /**
       * ロールがシステムユーザーで PREMIUM の場合に一覧インポート権限を含めることを確かめる。 利用可能な最も低いロールでも、プランの権限は付与される。
       *
       * @given ロールが システムユーザー、プランが PREMIUM
       * @when getPermissions を実行する
       * @then 一覧インポートの権限を含む権限集合を返す
       */
      @Test
      void ロールがシステムユーザーでプランがPREMIUMの場合_一覧インポートの権限を含む権限集合を返す() {
        SES_AI_WEBAPP_M_USER user = user();
        user.setRole(Role.システムユーザー);

        assertThat(user.getPermissions()).contains(Permission.REGISTER_INFO_LIST_IMPORT);
      }
    }

    @Nested
    class 認可 {

      /**
       * ロールがシステム利用不可の場合に、プランに関係なく空の権限集合を返すことを確かめる。 業務機能はプランの権限で開放してはならない。
       *
       * @given ロールが システム利用不可、プランが PREMIUM
       * @when getPermissions を実行する
       * @then 空の権限集合を返す
       */
      @Test
      void ロールがシステム利用不可の場合_空の権限集合を返す() {
        SES_AI_WEBAPP_M_USER user = user();
        user.setRole(Role.システム利用不可);

        assertThat(user.getPermissions()).isEmpty();
      }

      /**
       * ロールが未設定の場合に、空の権限集合を返すことを確かめる。 ロールの無いユーザーに権限を与えない。
       *
       * @given ロールが null、プランが PREMIUM
       * @when getPermissions を実行する
       * @then 空の権限集合を返す
       */
      @Test
      void ロールが未設定の場合_空の権限集合を返す() {
        SES_AI_WEBAPP_M_USER user = user();
        user.setRole(null);

        assertThat(user.getPermissions()).isEmpty();
      }

      /**
       * プランが FREE の場合に一覧インポート権限を含めないことを確かめる。 一覧インポートはプレミアムプランだけに提供する機能である。
       *
       * @given ロールが 運用、プランが FREE
       * @when getPermissions を実行する
       * @then 一覧インポートの権限を含まず、利用者管理の権限を含む権限集合を返す
       */
      @Test
      void プランがFREEの場合_一覧インポートの権限を含まない権限集合を返す() {
        SES_AI_WEBAPP_M_USER user = user();
        user.setPlan(Plan.FREE);

        assertThat(user.getPermissions())
            .doesNotContain(Permission.REGISTER_INFO_LIST_IMPORT)
            .contains(Permission.MANAGE_USERS);
      }

      /**
       * プランが未設定の場合に一覧インポート権限を含めないことを確かめる。 プランの無いユーザーに有料の権限を与えない。
       *
       * @given ロールが 運用、プランが null
       * @when getPermissions を実行する
       * @then 一覧インポートの権限を含まず、利用者管理の権限を含む権限集合を返す
       */
      @Test
      void プランが未設定の場合_一覧インポートの権限を含まない権限集合を返す() {
        SES_AI_WEBAPP_M_USER user = user();
        user.setPlan(null);

        assertThat(user.getPermissions())
            .doesNotContain(Permission.REGISTER_INFO_LIST_IMPORT)
            .contains(Permission.MANAGE_USERS);
      }
    }
  }

  @Nested
  class insert {

    @Nested
    class 正常系 {

      /**
       * 役割とプランと登録日時を揃えて登録することを確かめる。 役割とプランはコードに変換して保存する。
       *
       * @given ロールが 運用、プランが PREMIUM、登録日時が "2026-01-15 10:00:00"、登録ユーザーが "U000"、更新件数が 1 件
       * @when insert を接続で実行する
       * @then ロールのコード "30" とプランのコード "10" を設定し、1件を返す
       */
      @MockDbConnection
      @Test
      void 項目が揃っている場合_コードを設定して1件を返す(final Connection connection) throws SQLException {
        PreparedStatement statement = stubUpdate(connection, 1);

        assertThat(user().insert(connection)).isEqualTo(1);

        verify(connection)
            .prepareStatement(
                "INSERT INTO SES_AI_WEBAPP_M_USER (user_id, user_name, role_cd, plan_cd,"
                    + " register_date, register_user) VALUES (?, ?, ?, ?, ?, ?)");
        verify(statement).setString(1, USER_ID);
        verify(statement).setString(3, "30");
        verify(statement).setString(4, "10");
        verify(statement).setTimestamp(5, Timestamp.valueOf(REGISTER_DATE_TEXT));
        verify(statement).setString(6, "U000");
      }
    }

    @Nested
    class 境界値 {

      /**
       * 役割とプランと登録日時が未設定の場合に、各項目の位置に null を設定することを確かめる。 未設定の項目があっても登録を失敗させない。
       *
       * @given ロールが null、プランが null、登録日時が未設定、更新件数が 1 件
       * @when insert を接続で実行する
       * @then ロール、プラン、登録日時の位置に null を設定し、1件を返す
       */
      @MockDbConnection
      @Test
      void 役割とプランと登録日時が未設定の場合_nullを設定して1件を返す(final Connection connection) throws SQLException {
        PreparedStatement statement = stubUpdate(connection, 1);

        assertThat(userWithoutOptionalFields().insert(connection)).isEqualTo(1);

        verify(statement).setString(3, null);
        verify(statement).setString(4, null);
        verify(statement).setTimestamp(5, null);
      }
    }
  }

  @Nested
  class selectByPk {

    @Nested
    class 正常系 {

      /**
       * ユーザーIDとテナントIDが一致する行の項目を読み込み、登録済みとすることを確かめる。 ログイン後の画面に、保存済みの役割とプランを反映する。
       *
       * @given ユーザーIDが "U001"、テナントIDが "T001"、SELECT の結果が 1 行ロール "30"、プラン "10"
       * @when selectByPk を接続で実行する
       * @then trueを返し、ロールを 運用、プランを PREMIUM に更新し、登録済みとする
       */
      @MockDbConnection
      @Test
      void 該当行がある場合_trueを返す(final Connection connection) throws SQLException {
        JdbcMocks.stubQuery(
            connection,
            MockResultSetBuilder.builder()
                .columns(COLUMNS)
                .row(USER_ID, "山田太郎", "30", "10", REGISTER_DATE_TEXT, "U000", TENANT_ID)
                .build());
        SES_AI_WEBAPP_M_USER user = userWithoutOptionalFields();

        assertThat(user.selectByPk(connection)).isTrue();

        assertThat(user.getRole()).isEqualTo(Role.運用);
        assertThat(user.getPlan()).isEqualTo(Plan.PREMIUM);
        assertThat(user.isRegistered()).isTrue();
      }

      /**
       * 該当行が無い場合に項目を変えず、未登録とすることを確かめる。 存在しないユーザーの読み込みで入力中の値を壊さない。
       *
       * @given ユーザーIDが "U001"、SELECT の結果が 0 行
       * @when selectByPk を接続で実行する
       * @then falseを返し、ユーザー名を変えず、未登録とする
       */
      @MockDbConnection
      @Test
      void 該当行が無い場合_falseを返す(final Connection connection) throws SQLException {
        JdbcMocks.stubQuery(connection, MockResultSetBuilder.builder().columns(COLUMNS).build());
        SES_AI_WEBAPP_M_USER user = userWithoutOptionalFields();

        assertThat(user.selectByPk(connection)).isFalse();

        assertThat(user.getUserName()).isNull();
        assertThat(user.isRegistered()).isFalse();
      }
    }

    @Nested
    class 境界値 {

      /**
       * ユーザーIDが null の場合に DB を呼ばず、未登録とすることを確かめる。 キーの無い検索は別のユーザーを読むおそれがある。
       *
       * @given ユーザーIDが null
       * @when selectByPk を接続で実行する
       * @then falseを返し、SQL の準備を呼ばず、未登録とする
       */
      @MockDbConnection
      @Test
      void ユーザーIDがnullの場合_DBを呼ばずfalseを返す(final Connection connection) throws SQLException {
        SES_AI_WEBAPP_M_USER user = new SES_AI_WEBAPP_M_USER(TENANT_ID);

        assertThat(user.selectByPk(connection)).isFalse();

        assertThat(user.isRegistered()).isFalse();
        verify(connection, never()).prepareStatement(anyString());
      }
    }

    @Nested
    class テナント隔離 {

      /**
       * 検索の条件にテナントIDを渡して、他テナントのユーザーを読まないことを確かめる。 他テナントのユーザーの権限を読むと、権限の漏えいになる。
       *
       * @given ユーザーIDが "U001"、テナントIDが "T001"、SELECT の結果が 1 行ロール "30"、プラン "10"
       * @when selectByPk を接続で実行する
       * @then trueを返し、条件の2番目に "T001" を設定する
       */
      @MockDbConnection
      @Test
      void テナントIDがT001の場合_trueを返す(final Connection connection) throws SQLException {
        PreparedStatement statement =
            JdbcMocks.stubQuery(
                connection,
                MockResultSetBuilder.builder()
                    .columns(COLUMNS)
                    .row(USER_ID, "山田太郎", "30", "10", REGISTER_DATE_TEXT, "U000", TENANT_ID)
                    .build());

        assertThat(userWithoutOptionalFields().selectByPk(connection)).isTrue();

        verify(statement).setString(2, TENANT_ID);
      }
    }
  }

  @Nested
  class selectByPkWithoutTenantId {

    @Nested
    class 正常系 {

      /**
       * ユーザーIDが一致する行を、テナントの条件を付けずに読み込むことを確かめる。 システム管理者の画面は全テナントのユーザーを扱う。
       *
       * @given ユーザーIDが "U001"、SELECT の結果が 1 行テナントID "T002"
       * @when selectByPkWithoutTenantId を接続で実行する
       * @then テナントIDを "T002" に更新し、登録済みとする
       */
      @MockDbConnection
      @Test
      void 該当行がある場合_テナントIDを更新する(final Connection connection) throws SQLException {
        PreparedStatement statement =
            JdbcMocks.stubQuery(
                connection,
                MockResultSetBuilder.builder()
                    .columns(COLUMNS)
                    .row(USER_ID, "山田太郎", "10", "00", REGISTER_DATE_TEXT, "U000", "T002")
                    .build());
        SES_AI_WEBAPP_M_USER user = userWithoutOptionalFields();

        user.selectByPkWithoutTenantId(connection);

        assertThat(user.getTenantId()).isEqualTo("T002");
        assertThat(user.isRegistered()).isTrue();
        verify(statement, never()).setString(eq(2), anyString());
      }

      /**
       * ユーザーIDに一致する行が無い場合に未登録とすることを確かめる。 存在しないユーザーを登録済みと誤認しない。
       *
       * @given ユーザーIDが "U001"、SELECT の結果が 0 行
       * @when selectByPkWithoutTenantId を接続で実行する
       * @then 未登録とし、ユーザー名を変えずに何もしない
       */
      @MockDbConnection
      @Test
      void 該当行が無い場合_何もしない(final Connection connection) throws SQLException {
        JdbcMocks.stubQuery(connection, MockResultSetBuilder.builder().columns(COLUMNS).build());
        SES_AI_WEBAPP_M_USER user = userWithoutOptionalFields();

        user.selectByPkWithoutTenantId(connection);

        assertThat(user.isRegistered()).isFalse();
        assertThat(user.getUserName()).isNull();
      }
    }

    @Nested
    class 境界値 {

      /**
       * ユーザーIDが null の場合に DB を呼ばず、未登録とすることを確かめる。 キーの無い検索は全ユーザーを対象にする。
       *
       * @given ユーザーIDが null
       * @when selectByPkWithoutTenantId を接続で実行する
       * @then SQL の準備を呼ばず、未登録とする
       */
      @MockDbConnection
      @Test
      void ユーザーIDがnullの場合_DBを呼ばない(final Connection connection) throws SQLException {
        SES_AI_WEBAPP_M_USER user = new SES_AI_WEBAPP_M_USER(TENANT_ID);

        user.selectByPkWithoutTenantId(connection);

        assertThat(user.isRegistered()).isFalse();
        verify(connection, never()).prepareStatement(anyString());
      }
    }
  }

  @Nested
  class updateByPk {

    @Nested
    class 正常系 {

      /**
       * ユーザーIDとテナントIDが一致する行を、項目の値で更新することを確かめる。 ユーザー情報の変更を保存する。
       *
       * @given ユーザーIDが "U001"、ロールが 運用、プランが PREMIUM、更新件数が 1 件
       * @when updateByPk を接続で実行する
       * @then tenant_id を条件に含めた UPDATE 文を準備し、trueを返す
       */
      @MockDbConnection
      @Test
      void 更新件数が1件の場合_更新してtrueを返す(final Connection connection) throws SQLException {
        PreparedStatement statement = stubUpdate(connection, 1);

        assertThat(user().updateByPk(connection)).isTrue();

        verify(connection)
            .prepareStatement(
                "UPDATE SES_AI_WEBAPP_M_USER SET user_id = ?, user_name = ?, role_cd = ?,"
                    + " plan_cd = ?, register_date = ?, register_user = ? WHERE user_id = ? AND"
                    + " tenant_id = ?");
        verify(statement).setString(3, "30");
        verify(statement).setString(4, "10");
        verify(statement).setString(7, USER_ID);
      }

      /**
       * 役割とプランと登録日時が未設定の場合に、各項目の位置に null を設定して更新することを確かめる。 未設定の項目があっても更新を失敗させない。
       *
       * @given ロールが null、プランが null、登録日時が未設定、更新件数が 1 件
       * @when updateByPk を接続で実行する
       * @then ロール、プラン、登録日時の位置に null を設定し、trueを返す
       */
      @MockDbConnection
      @Test
      void 役割とプランと登録日時が未設定の場合_nullを設定してtrueを返す(final Connection connection) throws SQLException {
        PreparedStatement statement = stubUpdate(connection, 1);

        assertThat(userWithoutOptionalFields().updateByPk(connection)).isTrue();

        verify(statement).setString(3, null);
        verify(statement).setString(4, null);
        verify(statement).setTimestamp(5, null);
      }
    }

    @Nested
    class 異常系 {

      /**
       * 更新件数が 0 件の場合に false を返すことを確かめる。 対象のユーザーが無いときに更新成功と誤認しない。
       *
       * @given ユーザーIDが "U001"、更新件数が 0 件
       * @when updateByPk を接続で実行する
       * @then falseを返す
       */
      @MockDbConnection
      @Test
      void 更新件数が0件の場合_falseを返す(final Connection connection) throws SQLException {
        stubUpdate(connection, 0);

        assertThat(user().updateByPk(connection)).isFalse();
      }
    }

    @Nested
    class 境界値 {

      /**
       * ユーザーIDが null の場合に DB を呼ばずに false を返すことを確かめる。 キーの無い更新は全ユーザーを書き換えるおそれがある。
       *
       * @given ユーザーIDが null
       * @when updateByPk を接続で実行する
       * @then SQL の準備を呼ばず、falseを返す
       */
      @MockDbConnection
      @Test
      void ユーザーIDがnullの場合_DBを呼ばずにfalseを返す(final Connection connection) throws SQLException {
        assertThat(new SES_AI_WEBAPP_M_USER(TENANT_ID).updateByPk(connection)).isFalse();

        verify(connection, never()).prepareStatement(anyString());
      }
    }

    @Nested
    class テナント隔離 {

      /**
       * 更新の条件にテナントIDを含め、他テナントのユーザーを更新しないことを確かめる。 他テナントのユーザーの権限を書き換えると、権限の漏えいになる。
       *
       * @given テナントIDが "T001"、ユーザーIDが "U001"、更新件数が 1 件
       * @when updateByPk を接続で実行する
       * @then 条件の8番目に "T001" を設定し、trueを返す
       */
      @MockDbConnection
      @Test
      void テナントIDがT001の場合_条件にT001を設定してtrueを返す(final Connection connection) throws SQLException {
        PreparedStatement statement = stubUpdate(connection, 1);

        assertThat(user().updateByPk(connection)).isTrue();

        verify(statement).setString(8, TENANT_ID);
      }
    }
  }

  @Nested
  class updateByPkWithoutTenantId {

    @Nested
    class 正常系 {

      /**
       * ユーザーIDが一致する行を、テナントの条件を付けずに更新し、テナントIDも書き換えることを確かめる。 システム管理者がユーザーのテナントを付け替える。
       *
       * @given ユーザーIDが "U001"、テナントIDが "T001"、ロールが 運用、プランが PREMIUM、更新件数が 1 件
       * @when updateByPkWithoutTenantId を接続で実行する
       * @then tenant_id の位置に "T001" を設定し、trueを返す
       */
      @MockDbConnection
      @Test
      void 更新件数が1件の場合_テナントIDを設定して更新しtrueを返す(final Connection connection) throws SQLException {
        PreparedStatement statement = stubUpdate(connection, 1);

        assertThat(user().updateByPkWithoutTenantId(connection)).isTrue();

        verify(connection)
            .prepareStatement(
                "UPDATE SES_AI_WEBAPP_M_USER SET user_id = ?, user_name = ?, role_cd = ?,"
                    + " plan_cd = ?, register_date = ?, register_user = ?, tenant_id = ? WHERE"
                    + " user_id = ?");
        verify(statement).setString(7, TENANT_ID);
        verify(statement).setString(8, USER_ID);
      }

      /**
       * 役割とプランと登録日時が未設定の場合に、各項目の位置に null を設定して更新することを確かめる。 未設定の項目があっても更新を失敗させない。
       *
       * @given ロールが null、プランが null、登録日時が未設定、更新件数が 1 件
       * @when updateByPkWithoutTenantId を接続で実行する
       * @then ロール、プラン、登録日時の位置に null を設定し、trueを返す
       */
      @MockDbConnection
      @Test
      void 役割とプランと登録日時が未設定の場合_nullを設定してtrueを返す(final Connection connection) throws SQLException {
        PreparedStatement statement = stubUpdate(connection, 1);

        assertThat(userWithoutOptionalFields().updateByPkWithoutTenantId(connection)).isTrue();

        verify(statement).setString(3, null);
        verify(statement).setString(4, null);
        verify(statement).setTimestamp(5, null);
      }
    }

    @Nested
    class 異常系 {

      /**
       * 更新件数が 0 件の場合に false を返すことを確かめる。 対象のユーザーが無いときに更新成功と誤認しない。
       *
       * @given ユーザーIDが "U001"、更新件数が 0 件
       * @when updateByPkWithoutTenantId を接続で実行する
       * @then falseを返す
       */
      @MockDbConnection
      @Test
      void 更新件数が0件の場合_falseを返す(final Connection connection) throws SQLException {
        stubUpdate(connection, 0);

        assertThat(user().updateByPkWithoutTenantId(connection)).isFalse();
      }
    }

    @Nested
    class 境界値 {

      /**
       * ユーザーIDが null の場合に DB を呼ばずに false を返すことを確かめる。 キーの無い更新は全ユーザーを書き換えるおそれがある。
       *
       * @given ユーザーIDが null
       * @when updateByPkWithoutTenantId を接続で実行する
       * @then SQL の準備を呼ばず、falseを返す
       */
      @MockDbConnection
      @Test
      void ユーザーIDがnullの場合_DBを呼ばずにfalseを返す(final Connection connection) throws SQLException {
        assertThat(new SES_AI_WEBAPP_M_USER(TENANT_ID).updateByPkWithoutTenantId(connection))
            .isFalse();

        verify(connection, never()).prepareStatement(anyString());
      }
    }
  }

  @Nested
  class deleteByPk {

    @Nested
    class 正常系 {

      /**
       * ユーザーIDとテナントIDが一致する行を削除することを確かめる。 退職したユーザーの権限を残さない。
       *
       * @given ユーザーIDが "U001"、テナントIDが "T001"、削除件数が 1 件
       * @when deleteByPk を接続で実行する
       * @then tenant_id を条件に含めて削除し、trueを返す
       */
      @MockDbConnection
      @Test
      void 削除件数が1件の場合_削除してtrueを返す(final Connection connection) throws SQLException {
        PreparedStatement statement = stubUpdate(connection, 1);

        assertThat(user().deleteByPk(connection)).isTrue();

        verify(connection)
            .prepareStatement(
                "DELETE FROM SES_AI_WEBAPP_M_USER WHERE user_id = ? AND tenant_id = ?");
        verify(statement).setString(1, USER_ID);
      }

      /**
       * 削除件数が 0 件の場合に false を返すことを確かめる。 対象のユーザーが無いときに削除成功と誤認しない。
       *
       * @given ユーザーIDが "U001"、削除件数が 0 件
       * @when deleteByPk を接続で実行する
       * @then falseを返す
       */
      @MockDbConnection
      @Test
      void 削除件数が0件の場合_falseを返す(final Connection connection) throws SQLException {
        stubUpdate(connection, 0);

        assertThat(user().deleteByPk(connection)).isFalse();
      }
    }

    @Nested
    class 境界値 {

      /**
       * ユーザーIDが null の場合に DB を呼ばずに false を返すことを確かめる。 キーの無い削除は全ユーザーを消すおそれがある。
       *
       * @given ユーザーIDが null
       * @when deleteByPk を接続で実行する
       * @then SQL の準備を呼ばず、falseを返す
       */
      @MockDbConnection
      @Test
      void ユーザーIDがnullの場合_DBを呼ばずにfalseを返す(final Connection connection) throws SQLException {
        assertThat(new SES_AI_WEBAPP_M_USER(TENANT_ID).deleteByPk(connection)).isFalse();

        verify(connection, never()).prepareStatement(anyString());
      }
    }

    @Nested
    class テナント隔離 {

      /**
       * 削除の条件にテナントIDを含め、他テナントのユーザーを削除しないことを確かめる。 他テナントのユーザーを消すと、別の契約社の業務が止まる。
       *
       * @given テナントIDが "T001"、ユーザーIDが "U001"、削除件数が 1 件
       * @when deleteByPk を接続で実行する
       * @then 条件の2番目に "T001" を設定し、trueを返す
       */
      @MockDbConnection
      @Test
      void テナントIDがT001の場合_条件にT001を設定してtrueを返す(final Connection connection) throws SQLException {
        PreparedStatement statement = stubUpdate(connection, 1);

        assertThat(user().deleteByPk(connection)).isTrue();

        verify(statement).setString(2, TENANT_ID);
      }
    }
  }

  @Nested
  class deleteByPkWithoutTenantIdFilter {

    @Nested
    class 正常系 {

      /**
       * ユーザーIDが一致する行を、テナントの条件を付けずに削除することを確かめる。 システム管理者がテナントを問わずユーザーを削除する。
       *
       * @given ユーザーIDが "U001"、削除件数が 1 件
       * @when deleteByPkWithoutTenantIdFilter を接続で実行する
       * @then ユーザーIDだけを条件にして削除し、trueを返す
       */
      @MockDbConnection
      @Test
      void 削除件数が1件の場合_削除してtrueを返す(final Connection connection) throws SQLException {
        PreparedStatement statement = stubUpdate(connection, 1);

        assertThat(user().deleteByPkWithoutTenantIdFilter(connection)).isTrue();

        verify(connection).prepareStatement("DELETE FROM SES_AI_WEBAPP_M_USER WHERE user_id = ?");
        verify(statement).setString(1, USER_ID);
      }

      /**
       * 削除件数が 0 件の場合に false を返すことを確かめる。 対象のユーザーが無いときに削除成功と誤認しない。
       *
       * @given ユーザーIDが "U001"、削除件数が 0 件
       * @when deleteByPkWithoutTenantIdFilter を接続で実行する
       * @then falseを返す
       */
      @MockDbConnection
      @Test
      void 削除件数が0件の場合_falseを返す(final Connection connection) throws SQLException {
        stubUpdate(connection, 0);

        assertThat(user().deleteByPkWithoutTenantIdFilter(connection)).isFalse();
      }
    }

    @Nested
    class 境界値 {

      /**
       * ユーザーIDが null の場合に DB を呼ばずに false を返すことを確かめる。 キーの無い削除は全ユーザーを消すおそれがある。
       *
       * @given ユーザーIDが null
       * @when deleteByPkWithoutTenantIdFilter を接続で実行する
       * @then SQL の準備を呼ばず、falseを返す
       */
      @MockDbConnection
      @Test
      void ユーザーIDがnullの場合_DBを呼ばずにfalseを返す(final Connection connection) throws SQLException {
        assertThat(new SES_AI_WEBAPP_M_USER(TENANT_ID).deleteByPkWithoutTenantIdFilter(connection))
            .isFalse();

        verify(connection, never()).prepareStatement(anyString());
      }
    }
  }
}
