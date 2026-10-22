package copel.sesproductpackage.core.database;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import copel.sesproductpackage.core.database.SES_AI_T_WATCH.TargetType;
import copel.sesproductpackage.core.unit.Gender;
import copel.sesproductpackage.tools.testsupport.db.MockDbConnection;
import copel.sesproductpackage.tools.testsupport.jdbc.MockResultSetBuilder;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** {@link SES_AI_T_WATCH_JOB_PERSONLot} の単体テスト。 */
class SES_AI_T_WATCH_JOB_PERSONLotTest {

  /** テナントID。 */
  private static final String TENANT_ID = "T001";

  /** 取得結果の列SELECT 文の列の順。 */
  private static final String[] COLUMNS = {
    "user_id",
    "target_id",
    "target_type",
    "memo",
    "register_date",
    "register_user",
    "ttl",
    "tenant_id",
    "job_title",
    "job_from_name",
    "job_mail_subject",
    "job_content_summary",
    "job_raw_content",
    "job_start_date",
    "job_office_requirements",
    "job_unit_price",
    "job_place",
    "job_area",
    "person_name",
    "person_from_name",
    "person_mail_subject",
    "person_content_summary",
    "person_raw_content",
    "person_age",
    "person_gender",
    "person_start_date",
    "person_office_availability",
    "person_unit_price",
    "person_place",
    "person_area"
  };

  /**
   * 案件と要員が結合できた行と、結合できなかった行削除済みの案件と要員、日時と対象種別が未設定の2行を持つ結果セットを作る。
   *
   * @return 2行の結果セット
   * @throws SQLException 結果セットの作成中に例外が起きた場合
   */
  private static ResultSet twoRows() throws SQLException {
    return MockResultSetBuilder.builder()
        .columns(COLUMNS)
        .row(
            "U001",
            "J001",
            "JOB",
            "メモ",
            "2026-01-15 10:00:00",
            "U000",
            "2026-02-15 10:00:00",
            TENANT_ID,
            "案件名",
            "送信者",
            "件名",
            "要約",
            "原文",
            "2026-03-01 00:00:00",
            2,
            new BigDecimal(1_500_000),
            "東京",
            "関東",
            "山田太郎",
            "要員送信者",
            "要員件名",
            "要員要約",
            "要員原文",
            35,
            "Man",
            "2026-04-01 00:00:00",
            5,
            new BigDecimal(500_000),
            "大阪",
            "関西")
        .row(
            "U002", "J002", null, null, null, "U000", null, TENANT_ID, null, null, null, null, null,
            null, null, null, null, null, null, null, null, null, null, null, null, null, null,
            null, null, null)
        .build();
  }

  @Nested
  class selectAll {

    @Nested
    class 異常系 {

      /**
       * 全件の取得を呼び出すと、ユーザーIDによる取得を案内する例外を投げることを確かめる。 結合の一覧は必ずページ指定で取得する。
       *
       * @given 読み取り専用の結合Lot
       * @when selectAll を接続とテナントID "T001" で呼び出す
       * @then UnsupportedOperationExceptionを投げる
       */
      @Test
      void 呼び出された場合_UnsupportedOperationExceptionを投げる() {
        assertThatThrownBy(
                () -> new SES_AI_T_WATCH_JOB_PERSONLot().selectAll((Connection) null, TENANT_ID))
            .isInstanceOf(UnsupportedOperationException.class);
      }
    }
  }

  @Nested
  class getSelectAllSql {

    @Nested
    class 異常系 {

      /**
       * 全件用の SQL を求めると例外を投げることを確かめる。 結合の一覧は全件ではなくページ指定で取得する。
       *
       * @given 読み取り専用の結合Lot
       * @when selectAllPaged を接続、テナントID "T001"、ページ 1、サイズ 10 で呼び出す
       * @then UnsupportedOperationExceptionを投げる
       */
      @Test
      void selectAllPagedから呼ばれた場合_UnsupportedOperationExceptionを投げる() {
        assertThatThrownBy(
                () ->
                    new SES_AI_T_WATCH_JOB_PERSONLot()
                        .selectAllPaged((Connection) null, TENANT_ID, 1, 10))
            .isInstanceOf(UnsupportedOperationException.class);
      }
    }
  }

  @Nested
  class selectByUserIdPaged {

    @Nested
    class 正常系 {

      /**
       * 結合できた行と結合できなかった行の両方を読み込み、項目を変換することを確かめる。 削除済みの案件や要員があっても、ウォッチの一覧を表示する。
       *
       * @given 件数が 2 件、SELECT の結果が結合できた行と結合できなかった行の 2 行
       * @when selectByUserIdPaged を接続、テナントID "T001"、ユーザーID "U001"、ページ 1、サイズ 10 で実行する
       * @then 件数に 2 を保持し、1件目の対象種別を JOB、2件目の対象種別を null とした2件の一覧を返す
       */
      @MockDbConnection
      @Test
      void 結合できた行と結合できなかった行の場合_2件の一覧を返す(final Connection connection) throws SQLException {
        stubCountAndRows(connection, 2L, twoRows());
        SES_AI_T_WATCH_JOB_PERSONLot lot = new SES_AI_T_WATCH_JOB_PERSONLot();

        lot.selectByUserIdPaged(connection, TENANT_ID, "U001", 1, 10);

        assertThat(lot.getTotalCount()).isEqualTo(2L);
        assertThat(lot.size()).isEqualTo(2);
        assertThat(lot.get(0).getTargetType()).isEqualTo(TargetType.JOB);
        assertThat(lot.get(0).getPersonGender()).isEqualTo(Gender.Man);
        assertThat(lot.get(0).getTtl().toString()).isEqualTo("2026-02-15 10:00:00");
        assertThat(lot.get(1).getTargetType()).isNull();
        assertThat(lot.get(1).getJobUnitPrice().isEmpty()).isTrue();
        assertThat(lot.get(1).getPersonGender()).isNull();
      }
    }

    @Nested
    class 異常系 {

      /**
       * DB 接続が null の場合に検索を拒否することを確かめる。 接続が無いまま件数を数えられない。
       *
       * @given 接続が null、ユーザーIDが "U001"
       * @when selectByUserIdPaged を null の接続で実行する
       * @then IllegalArgumentExceptionを投げる
       */
      @Test
      void 接続がnullの場合_IllegalArgumentExceptionを投げる() {
        assertThatThrownBy(
                () ->
                    new SES_AI_T_WATCH_JOB_PERSONLot()
                        .selectByUserIdPaged(null, TENANT_ID, "U001", 1, 10))
            .isInstanceOf(IllegalArgumentException.class);
      }
    }

    @Nested
    class 境界値 {

      /**
       * ユーザーIDが null の場合に検索を行わないことを確かめる。 キーの無い検索は全ユーザーのウォッチを返す。
       *
       * @given ユーザーIDが null、テナントIDが "T001"
       * @when selectByUserIdPaged を接続、テナントID "T001"、ユーザーID null で実行する
       * @then SQL の準備を呼ばない
       */
      @MockDbConnection
      @Test
      void ユーザーIDがnullの場合_DBを呼ばない(final Connection connection) throws SQLException {
        new SES_AI_T_WATCH_JOB_PERSONLot().selectByUserIdPaged(connection, TENANT_ID, null, 1, 10);

        verify(connection, never()).prepareStatement(anyString());
      }
    }

    @Nested
    class テナント隔離 {

      /**
       * 件数と行の検索の条件にテナントIDを渡して、他テナントのウォッチを読まないことを確かめる。 他テナントの案件と要員の情報が漏えいする。
       *
       * @given 件数が 0 件
       * @when selectByUserIdPaged を接続、テナントID "T001"、ユーザーID "U001" で実行する
       * @then 件数の検索の条件に "T001" を設定し、空の一覧を返す
       */
      @MockDbConnection
      @Test
      void テナントIDがT001の場合_件数の条件にT001を設定して空の一覧を返す(final Connection connection) throws SQLException {
        PreparedStatement countStatement = mock(PreparedStatement.class);
        when(countStatement.executeQuery())
            .thenReturn(MockResultSetBuilder.builder().columns("cnt").row(0L).build());
        when(connection.prepareStatement(contains("COUNT(*)"))).thenReturn(countStatement);
        SES_AI_T_WATCH_JOB_PERSONLot lot = new SES_AI_T_WATCH_JOB_PERSONLot();

        lot.selectByUserIdPaged(connection, TENANT_ID, "U001", 1, 10);

        assertThat(lot.isEmpty()).isTrue();
        verify(countStatement).setString(2, TENANT_ID);
      }
    }
  }

  /**
   * 件数の問い合わせと結果の問い合わせに、それぞれ別の準備文を返すように設定する。
   *
   * @param connection 接続のモック
   * @param count 件数
   * @param rows 行の結果セット順に返す
   * @throws SQLException 設定中に例外が起きた場合
   */
  private static void stubCountAndRows(
      final Connection connection, final long count, final ResultSet rows) throws SQLException {
    PreparedStatement countStatement = mock(PreparedStatement.class);
    when(countStatement.executeQuery())
        .thenReturn(MockResultSetBuilder.builder().columns("cnt").row(count).build());
    PreparedStatement rowStatement = mock(PreparedStatement.class);
    when(rowStatement.executeQuery()).thenReturn(rows);
    when(connection.prepareStatement(anyString())).thenReturn(rowStatement);
    when(connection.prepareStatement(contains("COUNT(*)"))).thenReturn(countStatement);
  }
}
