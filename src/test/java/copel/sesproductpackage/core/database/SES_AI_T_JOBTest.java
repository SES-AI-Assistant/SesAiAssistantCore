package copel.sesproductpackage.core.database;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import copel.sesproductpackage.core.api.gpt.Transformer;
import copel.sesproductpackage.core.unit.Area;
import copel.sesproductpackage.core.unit.Money;
import copel.sesproductpackage.core.unit.OriginalDateTime;
import copel.sesproductpackage.tools.testsupport.clock.FixedClock;
import copel.sesproductpackage.tools.testsupport.db.JdbcMocks;
import copel.sesproductpackage.tools.testsupport.db.MockDbConnection;
import copel.sesproductpackage.tools.testsupport.jdbc.MockResultSetBuilder;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.UUID;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/** {@link SES_AI_T_JOB} の単体テスト。 */
class SES_AI_T_JOBTest {

  /** テナントID。 */
  private static final String TENANT_ID = "T001";

  /** 案件IDの値。 */
  private static final String JOB_ID = "0123456789";

  /** 登録日時と有効期限の文字列。 */
  private static final String DATE_TEXT = "2026-01-15 10:00:00";

  /** 取得結果の列。 */
  private static final String[] COLUMNS = {
    "job_id",
    "from_group",
    "from_id",
    "from_name",
    "mail_subject",
    "raw_content",
    "content_summary",
    "unit_price",
    "vector_data",
    "title",
    "overview",
    "must_skills",
    "want_skills",
    "start_date",
    "place",
    "area",
    "office_requirements",
    "other_requirements",
    "register_date",
    "register_user",
    "ttl"
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
   * 項目が揃った案件を生成する。
   *
   * @return 生成した案件
   */
  /**
   * 要約をエンベディングしたベクトルを持つ案件を生成する。
   *
   * @return ベクトルを持つ案件
   * @throws Exception エンベディングに失敗した場合
   */
  private static SES_AI_T_JOB jobWithVector() throws Exception {
    SES_AI_T_JOB job = job();
    Transformer transformer = mock(Transformer.class);
    when(transformer.embedding("要約")).thenReturn(new float[] {0.1f, 0.2f});
    job.embedding(transformer);
    return job;
  }

  private static SES_AI_T_JOB job() {
    SES_AI_T_JOB job = new SES_AI_T_JOB(TENANT_ID);
    job.setJobId(JOB_ID);
    job.setFromGroup("G001");
    job.setFromId("S001");
    job.setFromName("送信者");
    job.setMailSubject("件名");
    job.setRawContent("原文");
    job.setContentSummary("要約");
    job.setUnitPrice(new Money(1_500_000L));
    job.setTitle("Javaエンジニア");
    job.setOverview("概要の文章");
    job.setMustSkills("Java\nSpring");
    job.setWantSkills("AWS");
    job.setStartDate(new OriginalDateTime("2026-02-01 00:00:00"));
    job.setPlace("東京");
    job.setArea(Area.関東_首都圏);
    job.setOfficeRequirements(3);
    job.setOtherRequirements("その他1\nその他2");
    job.setRegisterDate(new OriginalDateTime(DATE_TEXT));
    job.setRegisterUser("U001");
    job.setTtl(new OriginalDateTime(DATE_TEXT));
    return job;
  }

  /**
   * 1行分の案件の結果セットを作る。
   *
   * @return 1行の結果セット
   * @throws SQLException 結果セットの作成中に例外が起きた場合
   */
  private static java.sql.ResultSet row() throws SQLException {
    return MockResultSetBuilder.builder()
        .columns(COLUMNS)
        .row(
            JOB_ID,
            "G001",
            "S001",
            "送信者",
            "件名",
            "原文",
            "要約",
            new BigDecimal(1_500_000),
            null,
            "Javaエンジニア",
            "概要",
            "Java",
            "AWS",
            DATE_TEXT,
            "東京",
            "関東_首都圏",
            3,
            "その他",
            DATE_TEXT,
            "U001",
            DATE_TEXT)
        .build();
  }

  @Nested
  class コンストラクタ {

    @Nested
    class 正常系 {

      /**
       * テナントIDを指定して生成すると、そのテナントIDを保持することを確かめる。 案件をテナントの条件で管理する。
       *
       * @given テナントIDが "T001"
       * @when SES_AI_T_JOB を "T001" で生成する
       * @then 指定したテナントIDの "T001" を返す
       */
      @Test
      void テナントIDを指定した場合_指定したテナントIDを返す() {
        assertThat(new SES_AI_T_JOB(TENANT_ID).getTenantId()).isEqualTo(TENANT_ID);
      }
    }

    @Nested
    class 異常系 {

      /**
       * テナントIDが null の場合に生成を拒否することを確かめる。 テナントの無い案件は他テナントの要員と結び付く。
       *
       * @given テナントIDが null
       * @when SES_AI_T_JOB を null で生成する
       * @then IllegalArgumentExceptionを投げる
       */
      @Test
      void テナントIDがnullの場合_IllegalArgumentExceptionを投げる() {
        assertThatThrownBy(() -> new SES_AI_T_JOB(null))
            .isInstanceOf(IllegalArgumentException.class);
      }
    }
  }

  @Nested
  class to案件選出用文章 {

    @Nested
    class 正常系 {

      /**
       * 案件IDと原文を連結した選出用の文章を返すことを確かめる。 LLM に案件を選ばせる入力に使う。
       *
       * @given 案件ID "0123456789"、原文 "原文"
       * @when to案件選出用文章 を実行する
       * @then 案件IDと原文を並べた文章を返す
       */
      @Test
      void 案件IDと原文がある場合_連結した文章を返す() {
        assertThat(job().to案件選出用文章()).isEqualTo("案件ID：0123456789内容：原文");
      }
    }
  }

  @Nested
  class setPlace {

    @Nested
    class 正常系 {

      /**
       * 勤務地を設定すると、そのまま保持することを確かめる。 画面の入力した勤務地を保存する。
       *
       * @given 勤務地が "東京"
       * @when setPlace を "東京" で実行する
       * @then 勤務地に "東京" を保持する
       */
      @Test
      void 勤務地を指定した場合_指定した勤務地を返す() {
        SES_AI_T_JOB job = new SES_AI_T_JOB(TENANT_ID);

        job.setPlace("東京");

        assertThat(job.getPlace()).isEqualTo("東京");
      }
    }

    @Nested
    class 境界値 {

      /**
       * 勤務地が未指定の場合に null を保持することを確かめる。 未指定の文字列を勤務地として表示させない。
       *
       * @given 勤務地が "unknown"
       * @when setPlace を "unknown" で実行する
       * @then 勤務地をnullにする
       */
      @Test
      void 勤務地が未指定の場合_nullを返す() {
        SES_AI_T_JOB job = new SES_AI_T_JOB(TENANT_ID);

        job.setPlace("unknown");

        assertThat(job.getPlace()).isNull();
      }

      /**
       * 勤務地が30文字を超える場合に、30文字で切り詰めることを確かめる。 保存先の列の長さを超えないようにする。
       *
       * @given 勤務地が 31 文字の文字列
       * @when setPlace を 31 文字の文字列で実行する
       * @then 勤務地を先頭の 30 文字に切り詰めて保持する
       */
      @Test
      void 勤務地が31文字の場合_先頭の30文字を返す() {
        SES_AI_T_JOB job = new SES_AI_T_JOB(TENANT_ID);

        job.setPlace("あ".repeat(31));

        assertThat(job.getPlace()).isEqualTo("あ".repeat(30));
      }
    }
  }

  @Nested
  class setOverview {

    @Nested
    class 境界値 {

      /**
       * 案件概要が200文字ちょうどの場合に、そのまま保持することを確かめる。 保存先の列に収まる値を加工すると内容が欠ける。
       *
       * @given 案件概要が 200 文字の文字列
       * @when setOverview を 200 文字の文字列で実行する
       * @then 案件概要に 200 文字の文字列をそのまま保持する
       */
      @Test
      void 案件概要が200文字の場合_指定した案件概要を返す() {
        SES_AI_T_JOB job = new SES_AI_T_JOB(TENANT_ID);

        job.setOverview("あ".repeat(200));

        assertThat(job.getOverview()).isEqualTo("あ".repeat(200));
      }

      /**
       * 案件概要が200文字を超える場合に、省略記号を含めて200文字に切り詰めることを確かめる。 保存先の列の長さを超えると案件の登録が失われる。
       *
       * @given 案件概要が 201 文字の文字列
       * @when setOverview を 201 文字の文字列で実行する
       * @then 案件概要を先頭の 199 文字に省略記号を付けた 200 文字で保持する
       */
      @Test
      void 案件概要が201文字の場合_先頭の199文字に省略記号を付けた文字列を返す() {
        SES_AI_T_JOB job = new SES_AI_T_JOB(TENANT_ID);

        job.setOverview("あ".repeat(201));

        assertThat(job.getOverview()).isEqualTo("あ".repeat(199) + "…");
      }
    }
  }

  @Nested
  class getOfficeRequirementsForNotification {

    @Nested
    class 正常系 {

      /**
       * 出社要件が0の場合にフルリモートと返すことを確かめる。 通知の文面で出社の条件を示す。
       *
       * @given 出社要件が 0
       * @when getOfficeRequirementsForNotification を実行する
       * @then "フルリモート" を返す
       */
      @Test
      void 出社要件が0の場合_フルリモートを返す() {
        SES_AI_T_JOB job = new SES_AI_T_JOB(TENANT_ID);
        job.setOfficeRequirements(0);

        assertThat(job.getOfficeRequirementsForNotification()).isEqualTo("フルリモート");
      }

      /**
       * 出社要件が5の場合に常駐と返すことを確かめる。 常駐の案件を通知で区別する。
       *
       * @given 出社要件が 5
       * @when getOfficeRequirementsForNotification を実行する
       * @then "常駐" を返す
       */
      @Test
      void 出社要件が5の場合_常駐を返す() {
        SES_AI_T_JOB job = new SES_AI_T_JOB(TENANT_ID);
        job.setOfficeRequirements(5);

        assertThat(job.getOfficeRequirementsForNotification()).isEqualTo("常駐");
      }

      /**
       * 出社要件が0と5以外の場合に週の出社日数と返すことを確かめる。 週何日の出社かを通知で示す。
       *
       * @given 出社要件が 3
       * @when getOfficeRequirementsForNotification を実行する
       * @then "週3出社" を返す
       */
      @Test
      void 出社要件が0と5以外の場合_週の出社日数を返す() {
        SES_AI_T_JOB job = new SES_AI_T_JOB(TENANT_ID);
        job.setOfficeRequirements(3);

        assertThat(job.getOfficeRequirementsForNotification()).isEqualTo("週3出社");
      }
    }

    @Nested
    class 境界値 {

      /**
       * 出社要件が未設定の場合に null を返すことを確かめる。 出社の条件が無い案件に文言を付けない。
       *
       * @given 出社要件が null
       * @when getOfficeRequirementsForNotification を実行する
       * @then nullを返す
       */
      @Test
      void 出社要件が未設定の場合_nullを返す() {
        assertThat(new SES_AI_T_JOB(TENANT_ID).getOfficeRequirementsForNotification()).isNull();
      }
    }
  }

  @Nested
  class toNotificationInfo {

    @Nested
    class 正常系 {

      /**
       * 案件名と単価の表示を組み合わせた通知文を返すことを確かめる。 通知の1行に案件名と単価を並べる。
       *
       * @given 案件名が "Javaエンジニア"、単価が 1500000 円
       * @when toNotificationInfo を実行する
       * @then 案件名と単価の表示を区切り文字で並べた文字列を返す
       */
      @Test
      void 案件名と単価がある場合_区切り文字で並べた通知文を返す() {
        SES_AI_T_JOB job = job();

        assertThat(job.toNotificationInfo())
            .isEqualTo("Javaエンジニア｜" + new Money(1_500_000L).toPriceLabel());
      }
    }

    @Nested
    class 境界値 {

      /**
       * 単価が未設定の場合に案件名だけの通知文を返すことを確かめる。 単価の無い案件でも通知を作れるようにする。
       *
       * @given 案件名が "Javaエンジニア"、単価が null
       * @when toNotificationInfo を実行する
       * @then 案件名だけの文字列を返す
       */
      @Test
      void 単価が未設定の場合_案件名だけの通知文を返す() {
        SES_AI_T_JOB job = job();
        job.setUnitPrice(null);

        assertThat(job.toNotificationInfo()).isEqualTo("Javaエンジニア");
      }

      /**
       * 案件名が未指定の場合に既定の案件名で通知文を作ることを確かめる。 案件名の無い案件を空欄で通知しない。
       *
       * @given 案件名が null、単価が null
       * @when toNotificationInfo を実行する
       * @then 既定の案件名 "案件名不明" を返す
       */
      @Test
      void 案件名が未指定の場合_既定の案件名を返す() {
        SES_AI_T_JOB job = job();
        job.setTitle(null);
        job.setUnitPrice(null);

        assertThat(job.toNotificationInfo()).isEqualTo("案件名不明");
      }
    }
  }

  @Nested
  class findSimilarRecord {

    @Nested
    class 正常系 {

      /**
       * 類似する案件が見つかった場合に、案件IDと単価を持つ案件を返すことを確かめる。 重複の候補を画面に示す。
       *
       * @given 検索の結果が 1 行案件ID "0123456789"、単価 1500000
       * @when findSimilarRecord を接続、テナントID "T001"、本文 "原文"、類似度 0.8 で実行する
       * @then 案件ID "0123456789" の案件を返す
       */
      @MockDbConnection
      @Test
      void 類似する案件がある場合_案件IDを持つ案件を返す(final Connection connection) throws SQLException {
        JdbcMocks.stubQuery(
            connection,
            MockResultSetBuilder.builder()
                .columns("job_id", "unit_price")
                .row(JOB_ID, new BigDecimal(1_500_000))
                .build());

        SES_AI_T_JOB result = SES_AI_T_JOB.findSimilarRecord(connection, TENANT_ID, "原文", 0.8);

        assertThat(result.getJobId()).isEqualTo(JOB_ID);
        assertThat(result.getUnitPrice().getValue())
            .isEqualByComparingTo(new BigDecimal(1_500_000));
      }

      /**
       * 本文のURLを除いて類似検索の条件に使うことを確かめる。 URLの違いで同じ案件を別物と判定しない。
       *
       * @given 本文が "原文 https://example.com/a"
       * @when findSimilarRecord を接続、テナントID "T001"、その本文、類似度 0.8 で実行する
       * @then URLを除いた "原文 " を検索条件に2回設定する
       */
      @MockDbConnection
      @Test
      void 本文にURLがある場合_URLを除いた本文を条件に設定してnullを返す(final Connection connection) throws SQLException {
        PreparedStatement statement =
            JdbcMocks.stubQuery(
                connection, MockResultSetBuilder.builder().columns("job_id", "unit_price").build());

        SES_AI_T_JOB.findSimilarRecord(connection, TENANT_ID, "原文 https://example.com/a", 0.8);

        verify(statement).setString(2, "原文 ");
        verify(statement).setString(3, "原文 ");
      }

      /**
       * 単価が未設定の類似案件を、単価を空の値として返すことを確かめる。 単価の無い案件でも候補として扱えるようにする。
       *
       * @given 検索の結果が 1 行案件ID "0123456789"、単価が null
       * @when findSimilarRecord を接続、テナントID "T001"、本文 "原文"、類似度 0.8 で実行する
       * @then 単価が空の案件を返す
       */
      @MockDbConnection
      @Test
      void 単価が未設定の類似案件の場合_単価が空の案件を返す(final Connection connection) throws SQLException {
        JdbcMocks.stubQuery(
            connection,
            MockResultSetBuilder.builder()
                .columns("job_id", "unit_price")
                .row(JOB_ID, null)
                .build());

        SES_AI_T_JOB result = SES_AI_T_JOB.findSimilarRecord(connection, TENANT_ID, "原文", 0.8);

        assertThat(result.getUnitPrice().isEmpty()).isTrue();
      }
    }

    @Nested
    class 境界値 {

      /**
       * 類似する案件が無い場合に null を返すことを確かめる。 類似が無いのに候補を返すと、重複でない案件を登録から除外する。
       *
       * @given 検索の結果が 0 行
       * @when findSimilarRecord を接続、テナントID "T001"、本文 "原文"、類似度 0.8 で実行する
       * @then nullを返す
       */
      @MockDbConnection
      @Test
      void 類似する案件が無い場合_nullを返す(final Connection connection) throws SQLException {
        JdbcMocks.stubQuery(
            connection, MockResultSetBuilder.builder().columns("job_id", "unit_price").build());

        assertThat(SES_AI_T_JOB.findSimilarRecord(connection, TENANT_ID, "原文", 0.8)).isNull();
      }

      /**
       * 接続、テナントID、本文のいずれかが null の場合に DB を呼ばずに null を返すことを確かめる。 条件が欠けた検索で全件を比べない。
       *
       * @given 本文が null
       * @when findSimilarRecord を接続、テナントID "T001"、本文 null で実行する
       * @then nullを返し、SQL の準備を呼ばない
       */
      @MockDbConnection
      @Test
      void 本文がnullの場合_nullを返しDBを呼ばない(final Connection connection) throws SQLException {
        assertThat(SES_AI_T_JOB.findSimilarRecord(connection, TENANT_ID, null, 0.8)).isNull();

        verify(connection, never()).prepareStatement(anyString());
      }

      /**
       * テナントIDが null の場合に DB を呼ばずに null を返すことを確かめる。 テナントの無い検索は他テナントの案件を返す。
       *
       * @given テナントIDが null
       * @when findSimilarRecord を接続、テナントID null、本文 "原文" で実行する
       * @then nullを返し、SQL の準備を呼ばない
       */
      @MockDbConnection
      @Test
      void テナントIDがnullの場合_nullを返しDBを呼ばない(final Connection connection) throws SQLException {
        assertThat(SES_AI_T_JOB.findSimilarRecord(connection, null, "原文", 0.8)).isNull();

        verify(connection, never()).prepareStatement(anyString());
      }

      /**
       * DB 接続が null の場合に null を返すことを確かめる。 接続が無い状態で検索を行わない。
       *
       * @given 接続が null
       * @when findSimilarRecord を接続 null、テナントID "T001"、本文 "原文" で実行する
       * @then nullを返す
       */
      @Test
      void 接続がnullの場合_nullを返す() throws SQLException {
        assertThat(SES_AI_T_JOB.findSimilarRecord(null, TENANT_ID, "原文", 0.8)).isNull();
      }
    }
  }

  @Nested
  class getRawContent {

    @Nested
    class 正常系 {

      /**
       * 保持している原文を返すことを確かめる。 重複チェックや埋め込みの入力に原文を使う。
       *
       * @given 原文が "原文"
       * @when getRawContent を実行する
       * @then "原文" を返す
       */
      @Test
      void 原文を保持している場合_原文を返す() {
        assertThat(job().getRawContent()).isEqualTo("原文");
      }
    }
  }

  @Nested
  class getContentSummary {

    @Nested
    class 正常系 {

      /**
       * 保持している要約を返すことを確かめる。 埋め込みの入力に要約を使う。
       *
       * @given 要約が "要約"
       * @when getContentSummary を実行する
       * @then "要約" を返す
       */
      @Test
      void 要約を保持している場合_要約を返す() {
        assertThat(job().getContentSummary()).isEqualTo("要約");
      }
    }
  }

  @Nested
  class getCheckSql {

    @Nested
    class 正常系 {

      /**
       * 重複チェックの SQL を返すことを確かめる。 同じ原文の案件を二重に登録しないための判定に使う。
       *
       * @given 案件の原文が "原文"
       * @when getCheckSql を実行する
       * @then 原文の類似度で件数を数える SQL を返す
       */
      @Test
      void 重複チェックの場合_類似度で件数を数える文を返す() {
        assertThat(job().getCheckSql()).startsWith("SELECT COUNT(*) FROM SES_AI_T_JOB");
      }
    }
  }

  @Nested
  class insert {

    @Nested
    class 正常系 {

      /**
       * 案件の項目を列の順に設定し、発行した案件IDで登録することを確かめる。 案件IDはテナントの中で一意にする。
       *
       * @given 項目が揃った案件、発行されるUUIDが固定値、現在時刻が 2026-01-15 12:00 の日本時間、更新件数が 1 件
       * @when insert を接続で実行する
       * @then 案件IDに "0123456789" を発行し、テナントIDを含めて1件を返す
       */
      @FixedClock("2026-01-15T12:00:00+09:00")
      @MockDbConnection
      @Test
      void 項目が揃っている場合_発行した案件IDで1件を返す(final Connection connection) throws Exception {
        PreparedStatement statement = stubUpdate(connection, 1);
        SES_AI_T_JOB job = jobWithVector();
        UUID fixed = UUID.fromString("01234567-89ab-cdef-0123-456789abcdef");

        try (MockedStatic<UUID> uuid = mockStatic(UUID.class)) {
          uuid.when(UUID::randomUUID).thenReturn(fixed);
          assertThat(job.insert(connection)).isEqualTo(1);
        }

        assertThat(job.getJobId()).isEqualTo(JOB_ID);
        verify(statement).setString(1, JOB_ID);
        verify(statement).setObject(8, new BigDecimal(1_500_000));
        verify(statement).setString(eq(9), anyString());
        verify(statement).setTimestamp(14, Timestamp.valueOf("2026-02-01 00:00:00"));
        verify(statement).setObject(17, 3);
        verify(statement).setTimestamp(21, Timestamp.valueOf(DATE_TEXT));
        verify(statement).setString(22, TENANT_ID);
      }
    }

    @Nested
    class 境界値 {

      /**
       * 単価、開始日、エリア、出社要件、登録日時、有効期限が未設定の場合に、各項目の位置に null を設定することを確かめる。 未設定の項目があっても登録を失敗させない。
       *
       * @given 単価、開始日、エリア、出社要件、登録日時、有効期限が未設定、更新件数が 1 件
       * @when insert を接続で実行する
       * @then 未設定の位置に null を設定して1件を返す
       */
      @FixedClock("2026-01-15T12:00:00+09:00")
      @MockDbConnection
      @Test
      void 任意の項目が未設定の場合_nullを設定して1件を返す(final Connection connection) throws Exception {
        PreparedStatement statement = stubUpdate(connection, 1);
        SES_AI_T_JOB job = new SES_AI_T_JOB(TENANT_ID);
        job.setJobId(JOB_ID);

        assertThat(job.insert(connection)).isEqualTo(1);

        verify(statement).setObject(8, null);
        verify(statement).setTimestamp(14, null);
        verify(statement).setString(16, null);
        verify(statement).setObject(17, null);
        verify(statement).setTimestamp(19, null);
        verify(statement).setTimestamp(21, null);
      }
    }
  }

  @Nested
  class selectByPk {

    @Nested
    class 正常系 {

      /**
       * 案件IDとテナントIDが一致する行の項目を読み込むことを確かめる。 保存済みの案件を編集画面に表示する。
       *
       * @given 案件ID "0123456789"、SELECT の結果が 1 行エリア "関東_首都圏"、出社要件 3
       * @when selectByPk を接続で実行する
       * @then trueを返し、エリアを 関東_首都圏、出社要件を 3、場所を "東京" に更新する
       */
      @MockDbConnection
      @Test
      void 該当行がある場合_trueを返す(final Connection connection) throws SQLException {
        JdbcMocks.stubQuery(connection, row());
        SES_AI_T_JOB job = new SES_AI_T_JOB(TENANT_ID);
        job.setJobId(JOB_ID);

        assertThat(job.selectByPk(connection)).isTrue();

        assertThat(job.getArea()).isEqualTo(Area.関東_首都圏);
        assertThat(job.getOfficeRequirements()).isEqualTo(3);
        assertThat(job.getPlace()).isEqualTo("東京");
      }

      /**
       * 任意の項目が NULL の行を読み込み、単価・エリア・出社要件を未設定にすることを確かめる。 未入力の案件を編集画面で壊さずに表示する。
       *
       * @given 案件ID "0123456789"、SELECT の結果が 1 行、単価・エリア・出社要件が NULL
       * @when selectByPk を接続で実行する
       * @then trueを返し、単価・エリア・出社要件を未設定にする
       */
      @MockDbConnection
      @Test
      void 任意の項目がNULLの行の場合_trueを返す(final Connection connection) throws SQLException {
        JdbcMocks.stubQuery(
            connection,
            MockResultSetBuilder.builder()
                .columns(COLUMNS)
                .row(
                    JOB_ID,
                    "G001",
                    "S001",
                    "送信者",
                    "件名",
                    "原文",
                    "要約",
                    null,
                    null,
                    "Javaエンジニア",
                    "概要",
                    "Java",
                    "AWS",
                    DATE_TEXT,
                    "東京",
                    null,
                    null,
                    "その他",
                    DATE_TEXT,
                    "U001",
                    DATE_TEXT)
                .build());
        SES_AI_T_JOB job = new SES_AI_T_JOB(TENANT_ID);
        job.setJobId(JOB_ID);

        assertThat(job.selectByPk(connection)).isTrue();

        assertThat(job.getUnitPrice().getValue()).isNull();
        assertThat(job.getArea()).isNull();
        assertThat(job.getOfficeRequirements()).isNull();
      }

      /**
       * 該当行が無い場合に false を返し、項目を変えないことを確かめる。 存在しない案件を読み込んでも、入力中の値を壊さない。
       *
       * @given 案件ID "0123456789"、SELECT の結果が 0 行
       * @when selectByPk を接続で実行する
       * @then falseを返し、案件名を変えない
       */
      @MockDbConnection
      @Test
      void 該当行が無い場合_falseを返す(final Connection connection) throws SQLException {
        JdbcMocks.stubQuery(connection, MockResultSetBuilder.builder().columns(COLUMNS).build());
        SES_AI_T_JOB job = job();

        assertThat(job.selectByPk(connection)).isFalse();

        assertThat(job.getTitle()).isEqualTo("Javaエンジニア");
      }
    }

    @Nested
    class 境界値 {

      /**
       * 案件IDが null の場合に DB を呼ばないことを確かめる。 キーの無い問い合わせは全件を返すおそれがある。
       *
       * @given 案件IDが null
       * @when selectByPk を接続で実行する
       * @then falseを返し、SQL の準備を呼ばない
       */
      @MockDbConnection
      @Test
      void 案件IDがnullの場合_DBを呼ばずfalseを返す(final Connection connection) throws SQLException {
        assertThat(new SES_AI_T_JOB(TENANT_ID).selectByPk(connection)).isFalse();

        verify(connection, never()).prepareStatement(anyString());
      }
    }
  }

  @Nested
  class updateByPk {

    @Nested
    class 正常系 {

      /**
       * 案件IDとテナントIDが一致する行を、項目の値で更新することを確かめる。 案件の内容の変更を保存する。
       *
       * @given 案件ID "0123456789"、項目が揃った案件、更新件数が 1 件
       * @when updateByPk を接続で実行する
       * @then 案件IDを条件にして更新し、trueを返す
       */
      @MockDbConnection
      @Test
      void 更新件数が1件の場合_更新してtrueを返す(final Connection connection) throws SQLException {
        PreparedStatement statement = stubUpdate(connection, 1);

        assertThat(job().updateByPk(connection)).isTrue();

        verify(statement).setString(19, JOB_ID);
      }

      /**
       * ベクトルを持つ案件の場合に、ベクトルの文字列を更新の値として設定することを確かめる。 検索に使うベクトルを更新後も保つ。
       *
       * @given 案件ID "0123456789"、要約をエンベディングしたベクトルを持つ案件、更新件数が 1 件
       * @when updateByPk を接続で実行する
       * @then ベクトルの位置に文字列を設定し、trueを返す
       */
      @MockDbConnection
      @Test
      void ベクトルがある場合_ベクトルの文字列を設定してtrueを返す(final Connection connection) throws Exception {
        PreparedStatement statement = stubUpdate(connection, 1);

        assertThat(jobWithVector().updateByPk(connection)).isTrue();

        verify(statement).setString(eq(8), anyString());
      }
    }

    @Nested
    class 異常系 {

      /**
       * 更新件数が 0 件の場合に false を返すことを確かめる。 対象の案件が無いときに更新成功と誤認しない。
       *
       * @given 案件ID "0123456789"、更新件数が 0 件
       * @when updateByPk を接続で実行する
       * @then falseを返す
       */
      @MockDbConnection
      @Test
      void 更新件数が0件の場合_falseを返す(final Connection connection) throws SQLException {
        stubUpdate(connection, 0);

        assertThat(job().updateByPk(connection)).isFalse();
      }
    }

    @Nested
    class 境界値 {

      /**
       * 案件IDが null の場合に DB を呼ばずに false を返すことを確かめる。 キーの無い更新は全件を書き換えるおそれがある。
       *
       * @given 案件IDが null
       * @when updateByPk を接続で実行する
       * @then SQL の準備を呼ばず、falseを返す
       */
      @MockDbConnection
      @Test
      void 案件IDがnullの場合_DBを呼ばずにfalseを返す(final Connection connection) throws SQLException {
        assertThat(new SES_AI_T_JOB(TENANT_ID).updateByPk(connection)).isFalse();

        verify(connection, never()).prepareStatement(anyString());
      }

      /**
       * 任意の項目が未設定の場合に、各項目の位置に null を設定して更新することを確かめる。 未設定の項目があっても更新を失敗させない。
       *
       * @given 単価、ベクトル、開始日、エリア、出社要件、有効期限が未設定、案件ID "0123456789"、更新件数が 1 件
       * @when updateByPk を接続で実行する
       * @then 未設定の位置に null を設定し、trueを返す
       */
      @MockDbConnection
      @Test
      void 任意の項目が未設定の場合_nullを設定してtrueを返す(final Connection connection) throws SQLException {
        PreparedStatement statement = stubUpdate(connection, 1);
        SES_AI_T_JOB job = new SES_AI_T_JOB(TENANT_ID);
        job.setJobId(JOB_ID);

        assertThat(job.updateByPk(connection)).isTrue();

        verify(statement).setObject(7, null);
        verify(statement).setString(8, null);
        verify(statement).setTimestamp(13, null);
        verify(statement).setString(15, null);
        verify(statement).setObject(16, null);
        verify(statement).setTimestamp(18, null);
      }
    }
  }

  @Nested
  class deleteByPk {

    @Nested
    class 正常系 {

      /**
       * 案件IDとテナントIDが一致する行を削除することを確かめる。 不要になった案件を残さない。
       *
       * @given 案件ID "0123456789"、削除件数が 1 件
       * @when deleteByPk を接続で実行する
       * @then 案件IDを条件にして削除し、trueを返す
       */
      @MockDbConnection
      @Test
      void 削除件数が1件の場合_削除してtrueを返す(final Connection connection) throws SQLException {
        PreparedStatement statement = stubUpdate(connection, 1);

        assertThat(job().deleteByPk(connection)).isTrue();

        verify(statement).setString(1, JOB_ID);
      }

      /**
       * 削除件数が 0 件の場合に false を返すことを確かめる。 対象の案件が無いときに削除成功と誤認しない。
       *
       * @given 案件ID "0123456789"、削除件数が 0 件
       * @when deleteByPk を接続で実行する
       * @then falseを返す
       */
      @MockDbConnection
      @Test
      void 削除件数が0件の場合_falseを返す(final Connection connection) throws SQLException {
        stubUpdate(connection, 0);

        assertThat(job().deleteByPk(connection)).isFalse();
      }
    }

    @Nested
    class 境界値 {

      /**
       * 案件IDが null の場合に DB を呼ばずに false を返すことを確かめる。 キーの無い削除は全件を消すおそれがある。
       *
       * @given 案件IDが null
       * @when deleteByPk を接続で実行する
       * @then SQL の準備を呼ばず、falseを返す
       */
      @MockDbConnection
      @Test
      void 案件IDがnullの場合_DBを呼ばずにfalseを返す(final Connection connection) throws SQLException {
        assertThat(new SES_AI_T_JOB(TENANT_ID).deleteByPk(connection)).isFalse();

        verify(connection, never()).prepareStatement(anyString());
      }
    }
  }

  @Nested
  class getTruncatedOverview {

    @Nested
    class 正常系 {

      /**
       * 概要の空白を整えて、上限以内なら全文を1行で返すことを確かめる。 一覧で概要を1行に収める。
       *
       * @given 概要が "概要 の\n文章"、上限が 100 文字
       * @when getTruncatedOverview を上限 100 で実行する
       * @then 空白を整えた "概要 の 文章" を返す
       */
      @Test
      void 上限以内の場合_空白を整えた全文を返す() {
        SES_AI_T_JOB job = new SES_AI_T_JOB(TENANT_ID);
        job.setOverview("概要 の\n文章");

        assertThat(job.getTruncatedOverview(100)).isEqualTo("概要 の 文章");
      }

      /**
       * 概要が上限を超える場合に、上限で切り詰めて省略記号を付けることを確かめる。 一覧の表示幅に収める。
       *
       * @given 概要が 10 文字の文字列、上限が 5 文字
       * @when getTruncatedOverview を上限 5 で実行する
       * @then 先頭の5文字に省略記号を付けて返す
       */
      @Test
      void 上限を超える場合_切り詰めて省略記号を付けた文字列を返す() {
        SES_AI_T_JOB job = new SES_AI_T_JOB(TENANT_ID);
        job.setOverview("あいうえおかきくけこ");

        assertThat(job.getTruncatedOverview(5)).isEqualTo("あいうえお…");
      }
    }

    @Nested
    class 境界値 {

      /**
       * 概要が未指定の場合に空の文字列を返すことを確かめる。 未指定の概要を表示させない。
       *
       * @given 概要が null
       * @when getTruncatedOverview を上限 5 で実行する
       * @then 空の文字列を返す
       */
      @Test
      void 概要が未指定の場合_空の文字列を返す() {
        assertThat(new SES_AI_T_JOB(TENANT_ID).getTruncatedOverview(5)).isEmpty();
      }
    }
  }

  @Nested
  class getMustSkillsAsList {

    @Nested
    class 正常系 {

      /**
       * 必須スキルを改行で分けた一覧を返すことを確かめる。 スキルを1件ずつ表示する。
       *
       * @given 必須スキルが "Java" と "Spring" を改行で連結した文字列
       * @when getMustSkillsAsList を実行する
       * @then "Java" と "Spring" の2件の一覧を返す
       */
      @Test
      void 必須スキルがある場合_改行で分けた2件の一覧を返す() {
        assertThat(job().getMustSkillsAsList()).containsExactly("Java", "Spring");
      }
    }

    @Nested
    class 境界値 {

      /**
       * 必須スキルが未設定の場合に空の一覧を返すことを確かめる。 スキルの無い案件を表示で落とさない。
       *
       * @given 必須スキルが null
       * @when getMustSkillsAsList を実行する
       * @then 空の一覧を返す
       */
      @Test
      void 必須スキルが未設定の場合_空の一覧を返す() {
        assertThat(new SES_AI_T_JOB(TENANT_ID).getMustSkillsAsList()).isEmpty();
      }
    }
  }

  @Nested
  class getWantSkillsAsList {

    @Nested
    class 正常系 {

      /**
       * 尚可スキルを改行で分けた一覧を返すことを確かめる。 スキルを1件ずつ表示する。
       *
       * @given 尚可スキルが "AWS" の1件
       * @when getWantSkillsAsList を実行する
       * @then "AWS" の1件の一覧を返す
       */
      @Test
      void 尚可スキルがある場合_1件の一覧を返す() {
        assertThat(job().getWantSkillsAsList()).containsExactly("AWS");
      }
    }

    @Nested
    class 境界値 {

      /**
       * 尚可スキルが未設定の場合に空の一覧を返すことを確かめる。 スキルの無い案件を表示で落とさない。
       *
       * @given 尚可スキルが null
       * @when getWantSkillsAsList を実行する
       * @then 空の一覧を返す
       */
      @Test
      void 尚可スキルが未設定の場合_空の一覧を返す() {
        assertThat(new SES_AI_T_JOB(TENANT_ID).getWantSkillsAsList()).isEmpty();
      }
    }
  }

  @Nested
  class getOtherRequirementsAsList {

    @Nested
    class 正常系 {

      /**
       * その他条件を改行で分けた一覧を返すことを確かめる。 条件を1件ずつ表示する。
       *
       * @given その他条件が "その他1" と "その他2" を改行で連結した文字列
       * @when getOtherRequirementsAsList を実行する
       * @then "その他1" と "その他2" の2件の一覧を返す
       */
      @Test
      void その他条件がある場合_改行で分けた2件の一覧を返す() {
        assertThat(job().getOtherRequirementsAsList()).containsExactly("その他1", "その他2");
      }
    }

    @Nested
    class 境界値 {

      /**
       * その他条件が未設定の場合に空の一覧を返すことを確かめる。 条件の無い案件を表示で落とさない。
       *
       * @given その他条件が null
       * @when getOtherRequirementsAsList を実行する
       * @then 空の一覧を返す
       */
      @Test
      void その他条件が未設定の場合_空の一覧を返す() {
        assertThat(new SES_AI_T_JOB(TENANT_ID).getOtherRequirementsAsList()).isEmpty();
      }
    }
  }

  @Nested
  class getSummaryWithTargetPrice_Money {

    @Nested
    class 正常系 {

      /**
       * 対象単価を指定すると、既定の係数で単価を含む要約を返すことを確かめる。 要員への提案文の要約に使う。
       *
       * @given 項目が揃った案件、対象単価が 1000000 円単価不問ではない
       * @when getSummaryWithTargetPrice を対象単価で実行する
       * @then 案件名と概要と必須と尚可と開始と単価と出社要件と場所とその他を含む要約を返す
       */
      @Test
      void 対象単価がある場合_各項目を含む要約を返す() {
        String summary = job().getSummaryWithTargetPrice(new Money(1_000_000L));

        assertThat(summary)
            .startsWith("Javaエンジニア\n■概要\n概要の文章\n")
            .contains("■必須\nJava\nSpring\n")
            .contains("■尚可\nAWS\n")
            .contains("■開始: ")
            .contains("■単価: " + new Money(1_000_000L).toJapaneseFormat() + "\n")
            .contains("■出社要件: 週3")
            .contains("■場所: 東京\n")
            .contains("■その他\nその他1\nその他2\n");
      }

      /**
       * 対象単価が単価不問の場合に、案件の単価に係数を掛けた単価を要約に含めることを確かめる。 応相談の案件でも単価の目安を示す。
       *
       * @given 案件単価が 1500000 円、対象単価が単価不問
       * @when getSummaryWithTargetPrice を単価不問の対象単価で実行する
       * @then 案件単価に係数 0.9 を掛けた単価を要約に含める
       */
      @Test
      void 対象単価が単価不問の場合_係数を掛けた単価を含む要約を返す() {
        String summary = job().getSummaryWithTargetPrice(new Money(9_990_000L));

        assertThat(summary)
            .contains("■単価: " + new Money(1_500_000L).multiply(0.9).toJapaneseFormat() + "\n");
      }

      /**
       * 出社要件が常駐の場合に、場所を付けて要約を返すことを確かめる。 常駐の案件では勤務地が重要な。
       *
       * @given 出社要件が 5、場所が "大阪"
       * @when getSummaryWithTargetPrice を対象単価 null で実行する
       * @then 常駐と場所を含む要約を返す
       */
      @Test
      void 出社要件が常駐の場合_常駐と場所を含む要約を返す() {
        SES_AI_T_JOB job = job();
        job.setOfficeRequirements(5);
        job.setPlace("大阪");

        assertThat(job.getSummaryWithTargetPrice(null))
            .contains("■出社要件: 常駐\n■場所: 大阪\n")
            .doesNotContain("■単価: ");
      }

      /**
       * 出社要件が0の場合に、フルリモートとして場所を含めないことを確かめる。 リモートの案件では勤務地を表示しない。
       *
       * @given 出社要件が 0
       * @when getSummaryWithTargetPrice を対象単価 null で実行する
       * @then フルリモートを含み、場所を含まない要約を返す
       */
      @Test
      void 出社要件がフルリモートの場合_場所を含めない要約を返す() {
        SES_AI_T_JOB job = job();
        job.setOfficeRequirements(0);

        assertThat(job.getSummaryWithTargetPrice(null))
            .contains("■出社要件: フルリモート\n")
            .doesNotContain("■場所: ");
      }

      /**
       * 対象単価が空の場合に単価の行を含めないことを確かめる。 単価の無い比較を要約に出さない。
       *
       * @given 対象単価が空の単価
       * @when getSummaryWithTargetPrice を空の対象単価で実行する
       * @then 単価の行を含まない要約を返す
       */
      @Test
      void 対象単価が空の場合_単価の行を含めない要約を返す() {
        assertThat(job().getSummaryWithTargetPrice(Money.empty())).doesNotContain("■単価: ");
      }
    }

    @Nested
    class 境界値 {

      /**
       * 必須、尚可、開始日、その他が未設定または空の場合に、その行を含めないことを確かめる。 未設定の項目を空の見出しとして出さない。
       *
       * @given 必須と尚可が空文字、開始日が null、その他が null
       * @when getSummaryWithTargetPrice を対象単価 null で実行する
       * @then 必須、尚可、開始、その他の行を含まない要約を返す
       */
      @Test
      void 任意の項目が未設定の場合_その行を含めない要約を返す() {
        SES_AI_T_JOB job = job();
        job.setMustSkills("");
        job.setWantSkills("");
        job.setStartDate(null);
        job.setOtherRequirements(null);

        assertThat(job.getSummaryWithTargetPrice(null))
            .doesNotContain("■必須")
            .doesNotContain("■尚可")
            .doesNotContain("■開始")
            .doesNotContain("■その他");
      }

      /**
       * 必須と尚可が未設定で、その他が空文字の場合に、その行を含めないことを確かめる。 未設定と空文字のどちらでも空の見出しを出さない。
       *
       * @given 必須と尚可が null、その他が空文字
       * @when getSummaryWithTargetPrice を対象単価 null で実行する
       * @then 必須、尚可、その他の行を含まない要約を返す
       */
      @Test
      void 必須と尚可が未設定でその他が空文字の場合_その行を含めない要約を返す() {
        SES_AI_T_JOB job = job();
        job.setMustSkills(null);
        job.setWantSkills(null);
        job.setOtherRequirements("");

        assertThat(job.getSummaryWithTargetPrice(null))
            .doesNotContain("■必須")
            .doesNotContain("■尚可")
            .doesNotContain("■その他");
      }

      /**
       * 出社要件が常駐で場所が未設定の場合に、場所を不明として出すことを確かめる。 場所の無い案件を空欄で出さない。
       *
       * @given 出社要件が 5、場所が null
       * @when getSummaryWithTargetPrice を対象単価 null で実行する
       * @then 場所に "不明" を含む要約を返す
       */
      @Test
      void 出社要件が常駐で場所が未設定の場合_場所に不明を含む要約を返す() {
        SES_AI_T_JOB job = job();
        job.setOfficeRequirements(5);
        job.setPlace(null);

        assertThat(job.getSummaryWithTargetPrice(null)).contains("■場所: 不明");
      }

      /**
       * 出社要件が未設定の場合に、出社要件と場所の行を含めないことを確かめる。 未設定の要件を誤った表示にしない。
       *
       * @given 出社要件が null
       * @when getSummaryWithTargetPrice を対象単価 null で実行する
       * @then 出社要件と場所の行を含まない要約を返す
       */
      @Test
      void 出社要件が未設定の場合_出社要件と場所の行を含まない要約を返す() {
        SES_AI_T_JOB j = job();
        j.setOfficeRequirements(null);
        assertThat(j.getSummaryWithTargetPrice(null)).doesNotContain("■出社要件").doesNotContain("■場所");
      }

      /**
       * 開始日が空の場合に、開始を未定と表示することを確かめる。 DB の開始日が null の案件は日付の無いオブジェクトになり、従来どおり未定と出す。
       *
       * @given 開始日が日付の無いオブジェクト
       * @when getSummaryWithTargetPrice を対象単価 null で実行する
       * @then 開始が未定の行を含む要約を返す
       */
      @Test
      void 開始日が空の場合_開始が未定の要約を返す() {
        SES_AI_T_JOB j = job();
        j.setStartDate(new OriginalDateTime((String) null));
        assertThat(j.getSummaryWithTargetPrice(null)).contains("■開始: 未定\n");
      }

      /**
       * 単価が未設定で単価不問の対象単価の場合に、単価の行を含めないことを確かめる。 掛ける単価が無いため誤った金額を出さない。
       *
       * @given 案件の単価が null、対象単価が単価不問
       * @when getSummaryWithTargetPrice を単価不問の対象単価で実行する
       * @then 単価の行を含まない要約を返す
       */
      @Test
      void 単価が未設定で単価不問の場合_単価の行を含まない要約を返す() {
        SES_AI_T_JOB j = job();
        j.setUnitPrice(null);
        assertThat(j.getSummaryWithTargetPrice(new Money(9_990_000L))).doesNotContain("■単価");
      }
    }
  }

  @Nested
  class getSummaryWithTargetPrice_Money_double {

    @Nested
    class 正常系 {

      /**
       * 係数を指定すると、単価不問の案件の単価にその係数を掛けることを確かめる。 係数を画面から変えて単価の目安を調整する。
       *
       * @given 案件単価が 1000000 円、対象単価が単価不問、係数が 0.5
       * @when getSummaryWithTargetPrice を対象単価と係数 0.5 で実行する
       * @then 案件単価に 0.5 を掛けた単価を要約に含める
       */
      @Test
      void 係数を指定した場合_係数を掛けた単価を含む要約を返す() {
        SES_AI_T_JOB job = job();
        job.setUnitPrice(new Money(1_000_000L));

        assertThat(job.getSummaryWithTargetPrice(new Money(9_990_000L), 0.5))
            .contains("■単価: " + new Money(1_000_000L).multiply(0.5).toJapaneseFormat() + "\n");
      }
    }
  }
}
