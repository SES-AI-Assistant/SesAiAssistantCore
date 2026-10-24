package copel.sesproductpackage.core.database;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import copel.sesproductpackage.core.unit.OriginalDateTime;
import copel.sesproductpackage.tools.testsupport.db.JdbcMocks;
import copel.sesproductpackage.tools.testsupport.db.MockDbConnection;
import copel.sesproductpackage.tools.testsupport.jdbc.MockResultSetBuilder;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** {@link SES_AI_T_PERSONLot} の単体テスト。 */
class SES_AI_T_PERSONLotTest {

  /** テナントID。 */
  private static final String TENANT_ID = "T001";

  /** 取得結果の列距離を含まない。 */
  private static final String[] COLUMNS = {
    "person_id",
    "from_group",
    "from_id",
    "from_name",
    "mail_subject",
    "raw_content",
    "content_summary",
    "file_id",
    "unit_price",
    "name",
    "age",
    "gender",
    "nationality",
    "start_date",
    "place",
    "area",
    "office_availability",
    "organization",
    "experiences",
    "ng_requirements",
    "other_requirements",
    "url",
    "register_date",
    "register_user",
    "ttl",
    "tenant_id"
  };

  /**
   * 要員を1行分与える結果セットを作る。距離を含むかどうかを指定する。
   *
   * @param withDistance 距離の列を含める場合は true
   * @return 1行の結果セット
   * @throws SQLException 結果セットの作成中に例外が起きた場合
   */
  private static ResultSet row(final boolean withDistance) throws SQLException {
    List<Object> values =
        new ArrayList<>(
            List.of(
                "0123456789",
                "G001",
                "S001",
                "送信者",
                "件名",
                "原文",
                "要約",
                "F001",
                new BigDecimal(600_000),
                "山田太郎",
                35,
                "Man",
                "日本",
                "2026-02-01 00:00:00",
                "東京",
                "関東_首都圏",
                3,
                "組織",
                "経験",
                "NG",
                "その他",
                "https://example.com",
                "2026-01-15 10:00:00",
                "U001",
                "2026-02-15 10:00:00"));
    List<String> columns = new ArrayList<>(List.of(COLUMNS));
    if (withDistance) {
      values.add(0.1);
      columns.add(columns.size() - 1, "distance");
    }
    values.add(TENANT_ID);
    return MockResultSetBuilder.builder()
        .columns(columns.toArray(new String[0]))
        .row(values.toArray())
        .build();
  }

  /**
   * 件数の問い合わせと結果の問い合わせに、それぞれ別の準備文を返すように設定する。
   *
   * @param connection 接続のモック
   * @param count 件数
   * @param rows 行の結果セット
   * @return 件数の準備文のモック
   * @throws SQLException 設定中に例外が起きた場合
   */
  /**
   * 任意の項目が NULL の1行分の結果セットを作る。
   *
   * @return 1行の結果セット
   * @throws SQLException 結果セットの作成中に例外が起きた場合
   */
  private static ResultSet rowWithNullOptionals() throws SQLException {
    return MockResultSetBuilder.builder()
        .columns(COLUMNS)
        .row(
            "0123456789",
            "G001",
            "S001",
            "送信者",
            "件名",
            "原文",
            "要約",
            "F001",
            null,
            "山田太郎",
            null,
            null,
            "日本",
            "2026-02-01 00:00:00",
            "東京",
            null,
            null,
            "組織",
            "経験",
            "NG",
            "その他",
            "https://example.com",
            "2026-01-15 10:00:00",
            "U001",
            "2026-02-15 10:00:00",
            TENANT_ID)
        .build();
  }

  private static PreparedStatement stubCountAndRows(
      final Connection connection, final long count, final ResultSet rows) throws SQLException {
    PreparedStatement countStatement = mock(PreparedStatement.class);
    when(countStatement.executeQuery())
        .thenReturn(MockResultSetBuilder.builder().columns("cnt").row(count).build());
    PreparedStatement rowStatement = mock(PreparedStatement.class);
    when(rowStatement.executeQuery()).thenReturn(rows);
    when(connection.prepareStatement(anyString())).thenReturn(rowStatement);
    when(connection.prepareStatement(contains("COUNT(*)"))).thenReturn(countStatement);
    return countStatement;
  }

  /**
   * 要員IDを持つ要員を生成する。
   *
   * @param personId 要員ID
   * @return 生成した要員
   */
  private static SES_AI_T_PERSON personOf(final String personId) {
    SES_AI_T_PERSON person = new SES_AI_T_PERSON(TENANT_ID);
    person.setPersonId(personId);
    person.setRawContent("原文");
    return person;
  }

  @Nested
  class getSelectAllSql {

    @Nested
    class 正常系 {

      /**
       * 全件の SQL が要員の表から全件を読む SELECT 文であることを確かめる。 全件の一覧を取得する基になる。
       *
       * @given 要員Lot
       * @when getSelectAllSql を実行する
       * @then SES_AI_T_PERSON から全件を読む SELECT 文を返す
       */
      @Test
      void 全件の取得の場合_要員の表からのSELECT文を返す() {
        assertThat(new SES_AI_T_PERSONLot().getSelectAllSql())
            .startsWith("SELECT")
            .contains("FROM SES_AI_T_PERSON");
      }
    }
  }

  @Nested
  class getSelectLikeSql {

    @Nested
    class 正常系 {

      /**
       * 全文検索の SQL が原文の LIKE 条件を持つことを確かめる。 原文の全文検索の基になる。
       *
       * @given 要員Lot
       * @when getSelectLikeSql を実行する
       * @then 原文の LIKE 条件を持つ SELECT 文を返す
       */
      @Test
      void 全文検索の場合_原文のLIKE条件を含むSELECT文を返す() {
        assertThat(new SES_AI_T_PERSONLot().getSelectLikeSql()).contains("raw_content LIKE ?");
      }
    }
  }

  @Nested
  class selectAll {

    @Nested
    class 正常系 {

      /**
       * テナントの要員を全件読み込み、項目を変換することを確かめる。 画面の一覧は自テナントの要員だけを表示する。
       *
       * @given SELECT の結果が 1 行テナントID "T001"、性別 "Man"
       * @when selectAll を接続とテナントID "T001" で実行する
       * @then 要員IDが "0123456789" の1件の一覧を返す
       */
      @MockDbConnection
      @Test
      void 結果が1行の場合_変換した1件の一覧を返す(final Connection connection) throws SQLException {
        JdbcMocks.stubQuery(connection, row(false));
        SES_AI_T_PERSONLot lot = new SES_AI_T_PERSONLot();

        lot.selectAll(connection, TENANT_ID);

        assertThat(lot.size()).isEqualTo(1);
        assertThat(lot.get(0).getPersonId()).isEqualTo("0123456789");
        assertThat(lot.get(0).getGender().name()).isEqualTo("Man");
      }

      /**
       * 単価・年齢・性別・エリア・出社許容が NULL の行を読み込み、対応する項目を未設定にすることを確かめる。 未入力の要員を一覧で壊さない。
       *
       * @given SELECT の結果が 1 行、単価・年齢・性別・エリア・出社許容が NULL
       * @when selectAll を接続とテナントID "T001" で実行する
       * @then 単価・年齢・性別・エリア・出社許容を未設定にした1件の一覧を返す
       */
      @MockDbConnection
      @Test
      void 任意の項目がNULLの行の場合_未設定の項目を持つ1件の一覧を返す(final Connection connection) throws SQLException {
        JdbcMocks.stubQuery(connection, rowWithNullOptionals());
        SES_AI_T_PERSONLot lot = new SES_AI_T_PERSONLot();

        lot.selectAll(connection, TENANT_ID);

        assertThat(lot.size()).isEqualTo(1);
        assertThat(lot.get(0).getUnitPrice().getValue()).isNull();
        assertThat(lot.get(0).getAge()).isNull();
        assertThat(lot.get(0).getGender()).isNull();
        assertThat(lot.get(0).getArea()).isNull();
        assertThat(lot.get(0).getOfficeAvailability()).isNull();
      }
    }

    @Nested
    class 異常系 {

      /**
       * テナントIDが null の場合に検索を拒否することを確かめる。 テナントの無い検索は全テナントの要員を返す。
       *
       * @given テナントIDが null
       * @when selectAll を接続と null のテナントIDで実行する
       * @then IllegalArgumentExceptionを投げる
       */
      @MockDbConnection
      @Test
      void テナントIDがnullの場合_IllegalArgumentExceptionを投げる(final Connection connection) {
        assertThatThrownBy(() -> new SES_AI_T_PERSONLot().selectAll(connection, null))
            .isInstanceOf(IllegalArgumentException.class);
      }
    }

    @Nested
    class テナント隔離 {

      /**
       * 検索の条件にテナントIDを渡して、他テナントの要員を読まないことを確かめる。 他テナントの要員の情報が漏えいする。
       *
       * @given SELECT の結果が 0 行
       * @when selectAll を接続とテナントID "T001" で実行する
       * @then tenant_id の位置に "T001" を設定し、空の一覧を返す
       */
      @MockDbConnection
      @Test
      void テナントIDがT001の場合_T001で絞り込んだ空の一覧を返す(final Connection connection) throws SQLException {
        PreparedStatement statement =
            JdbcMocks.stubQuery(
                connection, MockResultSetBuilder.builder().columns(COLUMNS).build());

        new SES_AI_T_PERSONLot().selectAll(connection, TENANT_ID);

        verify(statement).setString(1, TENANT_ID);
      }
    }
  }

  @Nested
  class selectAllWithoutTenantId {

    @Nested
    class 正常系 {

      /**
       * テナントの条件を付けずに全テナントの要員を読み込むことを確かめる。 バッチ処理は全テナントの要員を対象にする。
       *
       * @given SELECT の結果が 1 行
       * @when selectAllWithoutTenantId を接続で実行する
       * @then 1件の一覧を返す
       */
      @MockDbConnection
      @Test
      void 結果が1行の場合_テナントを問わず1件の一覧を返す(final Connection connection) throws SQLException {
        JdbcMocks.stubQuery(connection, row(false));
        SES_AI_T_PERSONLot lot = new SES_AI_T_PERSONLot();

        lot.selectAllWithoutTenantId(connection);

        assertThat(lot.size()).isEqualTo(1);
      }
    }

    @Nested
    class 異常系 {

      /**
       * DB 接続が null の場合に検索を拒否することを確かめる。 接続が無いまま SQL を実行できない。
       *
       * @given 接続が null
       * @when selectAllWithoutTenantId を null の接続で実行する
       * @then IllegalArgumentExceptionを投げる
       */
      @Test
      void 接続がnullの場合_IllegalArgumentExceptionを投げる() {
        assertThatThrownBy(() -> new SES_AI_T_PERSONLot().selectAllWithoutTenantId(null))
            .isInstanceOf(IllegalArgumentException.class);
      }
    }
  }

  @Nested
  class selectByRegisterDateAfter {

    @Nested
    class 正常系 {

      /**
       * 指定の日時より後に登録された要員を読み込むことを確かめる。 直近の登録分だけを表示する。
       *
       * @given 基準日時が "2026-01-01 00:00:00"、SELECT の結果が 1 行
       * @when selectByRegisterDateAfter を接続、テナントID "T001"、基準日時で実行する
       * @then 基準日時を1番目の条件に設定し、テナントIDを2番目に設定して1件の一覧を返す
       */
      @MockDbConnection
      @Test
      void 基準日時がある場合_基準日時の後の1件の一覧を返す(final Connection connection) throws SQLException {
        PreparedStatement statement = JdbcMocks.stubQuery(connection, row(false));
        SES_AI_T_PERSONLot lot = new SES_AI_T_PERSONLot();

        lot.selectByRegisterDateAfter(
            connection, TENANT_ID, new OriginalDateTime("2026-01-01 00:00:00"));

        assertThat(lot.size()).isEqualTo(1);
        verify(statement).setTimestamp(1, Timestamp.valueOf("2026-01-01 00:00:00"));
        verify(statement).setString(2, TENANT_ID);
      }

      /**
       * 基準日時が未設定の場合に現在時刻を基準にして、テナントIDで絞り込むことを確かめる。 基準が無い検索でも既定の時刻で絞り込めるようにする。
       *
       * @given 基準日時が null、SELECT の結果が 1 行
       * @when selectByRegisterDateAfter を接続、テナントID "T001"、基準日時 null で実行する
       * @then 基準日時の位置に時刻を設定し、テナントIDを2番目に設定して1件の一覧を返す
       */
      @MockDbConnection
      @Test
      void 基準日時が未設定の場合_現在時刻を基準にして1件の一覧を返す(final Connection connection) throws SQLException {
        PreparedStatement statement = JdbcMocks.stubQuery(connection, row(false));
        SES_AI_T_PERSONLot lot = new SES_AI_T_PERSONLot();

        lot.selectByRegisterDateAfter(connection, TENANT_ID, null);

        assertThat(lot.size()).isEqualTo(1);
        verify(statement).setTimestamp(eq(1), any(Timestamp.class));
        verify(statement).setString(2, TENANT_ID);
      }
    }

    @Nested
    class 異常系 {

      /**
       * DB 接続が null の場合に検索を拒否することを確かめる。 接続が無いまま SQL を実行できない。
       *
       * @given 接続が null
       * @when selectByRegisterDateAfter を null の接続で実行する
       * @then IllegalArgumentExceptionを投げる
       */
      @Test
      void 接続がnullの場合_IllegalArgumentExceptionを投げる() {
        assertThatThrownBy(
                () ->
                    new SES_AI_T_PERSONLot()
                        .selectByRegisterDateAfter(
                            null, TENANT_ID, new OriginalDateTime("2026-01-01 00:00:00")))
            .isInstanceOf(IllegalArgumentException.class);
      }
    }
  }

  @Nested
  class selectExpiredPersonsNotInMatchWithoutTenantId {

    @Nested
    class 正常系 {

      /**
       * 期限切れの要員を全テナントから読み込み、一覧に保持することを確かめる。 期限切れで一覧に残っていない要員を削除対象にする。
       *
       * @given SELECT の結果が 1 行、経過日数 30、オフセット 0、件数 100
       * @when selectExpiredPersonsNotInMatchWithoutTenantId を接続で実行する
       * @then 1件の一覧を返す
       */
      @MockDbConnection
      @Test
      void 期限切れの行が1件の場合_1件の一覧を返す(final Connection connection) throws SQLException {
        JdbcMocks.stubQuery(connection, row(false));
        SES_AI_T_PERSONLot lot = new SES_AI_T_PERSONLot();

        lot.selectExpiredPersonsNotInMatchWithoutTenantId(connection, 30, 0, 100);

        assertThat(lot.size()).isEqualTo(1);
      }

      /**
       * 期限切れの要員を探す SQL が、マッチングの存在を相関の NOT EXISTS で確かめることを確かめる。 NOT IN は NULL が1件でもあると全件不一致になる。
       *
       * @given SELECT の結果が 0 行、経過日数 30、オフセット 0、件数 100
       * @when selectExpiredPersonsNotInMatchWithoutTenantId を接続で実行する
       * @then マッチングの存在確認に NOT EXISTS を含む SQL を準備する
       */
      @MockDbConnection
      @Test
      void 期限切れの検索の場合_NOT_EXISTSを含むSQLで空の一覧を返す(final Connection connection) throws SQLException {
        JdbcMocks.stubQuery(connection, MockResultSetBuilder.builder().columns(COLUMNS).build());
        SES_AI_T_PERSONLot lot = new SES_AI_T_PERSONLot();

        lot.selectExpiredPersonsNotInMatchWithoutTenantId(connection, 30, 0, 100);

        verify(connection)
            .prepareStatement(contains("AND NOT EXISTS (SELECT 1 FROM SES_AI_T_MATCH m"));
        assertThat(lot.isEmpty()).isTrue();
      }

      /**
       * 期限切れの判定が ttl を優先し、ttl も登録日も無い行を対象にしないことを確かめる。 ttl が未来の要員を登録日の欠落だけで削除すると有効なデータを失う。
       *
       * @given SELECT の結果が 0 行、経過日数 30、オフセット 0、件数 100
       * @when selectExpiredPersonsNotInMatchWithoutTenantId を接続で実行する
       * @then register_date IS NULL を条件にせず、登録日と要員IDの順に並べる SQL を準備する
       */
      @MockDbConnection
      @Test
      void 期限切れの検索の場合_登録日が無い行を条件にしないSQLで空の一覧を返す(final Connection connection) throws SQLException {
        JdbcMocks.stubQuery(connection, MockResultSetBuilder.builder().columns(COLUMNS).build());
        SES_AI_T_PERSONLot lot = new SES_AI_T_PERSONLot();

        lot.selectExpiredPersonsNotInMatchWithoutTenantId(connection, 30, 0, 100);

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(connection).prepareStatement(sql.capture());
        assertThat(sql.getValue())
            .doesNotContain("OR (register_date IS NULL)")
            .contains("ORDER BY register_date ASC, person_id ASC");
        assertThat(lot.isEmpty()).isTrue();
      }
    }
  }

  @Nested
  class getEntityByPk {

    @Nested
    class 正常系 {

      /**
       * 要員IDが一致する要員を一覧から返すことを確かめる。 選出された候補の詳細を取り出す。
       *
       * @given 一覧に要員ID "P001" と "P002" の要員がある
       * @when getEntityByPk を "P002" で実行する
       * @then 要員ID "P002" の要員を返す
       */
      @Test
      void 要員IDが一致する要素がある場合_該当の要員を返す() {
        SES_AI_T_PERSONLot lot = new SES_AI_T_PERSONLot();
        lot.add(personOf("P001"));
        lot.add(personOf("P002"));

        assertThat(lot.getEntityByPk("P002").getPersonId()).isEqualTo("P002");
      }

      /**
       * 要員IDが一致する要素が無い場合に null を返すことを確かめる。 存在しない候補を取り出すと誤った詳細を表示する。
       *
       * @given 一覧に要員ID "P001" の要員がある
       * @when getEntityByPk を "P999" で実行する
       * @then nullを返す
       */
      @Test
      void 要員IDが一致する要素が無い場合_nullを返す() {
        SES_AI_T_PERSONLot lot = new SES_AI_T_PERSONLot();
        lot.add(personOf("P001"));

        assertThat(lot.getEntityByPk("P999")).isNull();
      }
    }

    @Nested
    class 境界値 {

      /**
       * 要員IDが null の場合に null を返すことを確かめる。 キーの無い検索で先頭の要員を誤って返さない。
       *
       * @given 一覧に要員ID "P001" の要員がある
       * @when getEntityByPk を null で実行する
       * @then nullを返す
       */
      @Test
      void 要員IDがnullの場合_nullを返す() {
        SES_AI_T_PERSONLot lot = new SES_AI_T_PERSONLot();
        lot.add(personOf("P001"));

        assertThat(lot.getEntityByPk(null)).isNull();
      }
    }
  }

  @Nested
  class to要員選出用文章 {

    @Nested
    class 正常系 {

      /**
       * 一覧の各要員を人目の番号を付けて連結した文章を返すことを確かめる。 LLM に候補を番号で示す。
       *
       * @given 一覧に要員ID "P001" と "P002" の要員が2件ある
       * @when to要員選出用文章 を実行する
       * @then 1人目と2人目の番号を付けた文章を返す
       */
      @Test
      void 一覧に2件ある場合_番号を付けて連結した文章を返す() {
        SES_AI_T_PERSONLot lot = new SES_AI_T_PERSONLot();
        lot.add(personOf("P001"));
        lot.add(personOf("P002"));

        assertThat(lot.to要員選出用文章()).startsWith("1人目：").contains("2人目：");
      }
    }
  }

  @Nested
  class toString {

    @Nested
    class 正常系 {

      /**
       * 一覧の各要員を1行ずつ文字列にして返すことを確かめる。 一覧の内容をログや確認に出す。
       *
       * @given 一覧に要員が2件ある
       * @when toString を実行する
       * @then 要員ごとに改行で区切った文字列を返す
       */
      @Test
      void 一覧に2件ある場合_要員ごとに改行で区切った文字列を返す() {
        SES_AI_T_PERSONLot lot = new SES_AI_T_PERSONLot();
        lot.add(personOf("P001"));
        lot.add(personOf("P002"));

        assertThat(lot.toString()).contains(personOf("P001").toString()).endsWith("\n");
      }
    }
  }

  @Nested
  class mapResultSetWithDistance {

    @Nested
    class 正常系 {

      /**
       * 距離の列を持つ行を要員に変換し、距離を設定することを確かめる。 類似検索の結果を並べ替えと表示に使うためである。
       *
       * @given 距離が 0.1 の1行の結果セット
       * @when mapResultSetWithDistance を結果セットで実行する
       * @then 要員IDが "0123456789" で距離が 0.1 の要員を返す
       */
      @Test
      void 距離の列がある行の場合_距離を設定した要員を返す() throws SQLException {
        ResultSet resultSet = row(true);
        resultSet.next();

        SES_AI_T_PERSON person = new SES_AI_T_PERSONLot().mapResultSetWithDistance(resultSet);

        assertThat(person.getPersonId()).isEqualTo("0123456789");
        assertThat(person.getDistance()).isEqualTo(0.1);
      }
    }
  }

  @Nested
  class getThresholdSql {

    @Nested
    class 正常系 {

      /**
       * 類似度検索の基底SQLが要員の表を対象にすることを確かめる。 閾値検索はこのSQLに条件を足して実行する。
       *
       * @given 要員の閾値検索の基底SQL
       * @when getThresholdSql を実行する
       * @then 要員の表からのSELECT文を返す
       */
      @Test
      void 閾値検索の場合_要員の表からのSELECT文を返す() {
        assertThat(new SES_AI_T_PERSONLot().getThresholdSql()).contains("FROM SES_AI_T_PERSON");
      }
    }
  }

  @Nested
  class getRawContentFulltextSql {

    @Nested
    class 正常系 {

      /**
       * 全文検索の複合条件で使う基底SQLが要員の表を対象にすることを確かめる。 WHERE 句を後ろから連ねるためである。
       *
       * @given 要員の全文検索の基底SQL
       * @when getRawContentFulltextSql を実行する
       * @then 要員の表からのSELECT文を返す
       */
      @Test
      void 全文検索の場合_要員の表からのSELECT文を返す() {
        assertThat(new SES_AI_T_PERSONLot().getRawContentFulltextSql())
            .contains("FROM SES_AI_T_PERSON WHERE");
      }
    }
  }
}
