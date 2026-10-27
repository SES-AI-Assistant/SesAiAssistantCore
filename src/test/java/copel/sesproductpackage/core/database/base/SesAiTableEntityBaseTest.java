package copel.sesproductpackage.core.database.base;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import copel.sesproductpackage.core.api.gpt.Transformer;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** {@link SesAiTableEntityBase} の単体テスト。 */
class SesAiTableEntityBaseTest {

  /** 重複チェックのSQL。テナントIDと原文と類似度を順に受け取る形を模した文字列。 */
  private static final String CHECK_SQL =
      "SELECT COUNT(*) FROM SES_AI_T_JOB WHERE tenant_id = ? AND raw_content = ? "
          + "AND similarity(raw_content, ?) >= ?";

  /** テスト用の {@link SesAiTableEntityBase} の具象クラス。 原文・要約・重複チェックのSQLを持ち、DB 操作の抽象メソッドは何もしない。 */
  private static class TestVectorEntity extends SesAiTableEntityBase {

    /** 原文。 */
    private final String rawContent;

    /** 要約。 */
    private final String contentSummary;

    /**
     * 原文と要約を指定して生成する。
     *
     * @param tenantId テナントID
     * @param rawContent 原文
     * @param contentSummary 要約
     */
    TestVectorEntity(final String tenantId, final String rawContent, final String contentSummary) {
      super(tenantId);
      this.rawContent = rawContent;
      this.contentSummary = contentSummary;
    }

    @Override
    public int insert(final Connection connection) {
      return 0;
    }

    @Override
    public boolean selectByPk(final Connection connection) {
      // テストでは使わない
      return false;
    }

    @Override
    public boolean updateByPk(final Connection connection) {
      return false;
    }

    @Override
    public boolean deleteByPk(final Connection connection) {
      return false;
    }

    @Override
    protected String getRawContent() {
      return rawContent;
    }

    @Override
    protected String getContentSummary() {
      return contentSummary;
    }

    @Override
    protected String getCheckSql() {
      return CHECK_SQL;
    }
  }

  /**
   * 重複チェックの問い合わせが、指定した件数を返す接続のモックを作る。
   *
   * @param count 問い合わせが返す件数。null の場合は結果の行が無い
   * @return 接続のモック
   * @throws SQLException 設定中に例外が起きた場合
   */
  private static Connection mockConnectionWithCount(final Integer count) throws SQLException {
    Connection connection = mock(Connection.class);
    PreparedStatement statement = mock(PreparedStatement.class);
    ResultSet resultSet = mock(ResultSet.class);
    when(connection.prepareStatement(CHECK_SQL)).thenReturn(statement);
    when(statement.executeQuery()).thenReturn(resultSet);
    if (count == null) {
      when(resultSet.next()).thenReturn(false);
    } else {
      when(resultSet.next()).thenReturn(true);
      when(resultSet.getInt(1)).thenReturn(count);
    }
    return connection;
  }

  @Nested
  class コンストラクタ {

    @Nested
    class 正常系 {

      /**
       * テナントIDを指定すると、そのテナントIDを保持することを確かめる。 ベクトルデータの行は必ず所属テナントを持つためである。
       *
       * @given テナントIDが T001
       * @when TestVectorEntity を T001 のテナントIDで生成する
       * @then T001 のテナントIDを返す
       */
      @Test
      void テナントIDがT001の場合_T001のテナントIDを返す() {
        assertThat(new TestVectorEntity("T001", "原文", "要約").getTenantId()).isEqualTo("T001");
      }
    }
  }

  @Nested
  class embedding {

    @Nested
    class 正常系 {

      /**
       * 要約をエンベディングして得たベクトルを保持することを確かめる。 原文ではなく要約を検索の対象にするため、変換の入力は要約になる。
       *
       * @given 原文が 元の本文、要約が 要約文、変換APIが 要約文 に対して 0.1 と 0.2 を返す
       * @when embedding を変換APIのモックで実行する
       * @then 要約文を入力にして、0.1 と 0.2 のベクトルを返す
       */
      @Test
      void 要約がある場合_要約のベクトルを返す() throws Exception {
        Transformer transformer = mock(Transformer.class);
        when(transformer.embedding("要約文")).thenReturn(new float[] {0.1f, 0.2f});
        TestVectorEntity entity = new TestVectorEntity("T001", "元の本文", "要約文");

        entity.embedding(transformer);

        verify(transformer).embedding("要約文");
        assertThat(entity.getVectorData().getValue()).containsExactly(0.1f, 0.2f);
      }
    }

    @Nested
    class 異常系 {

      /**
       * 変換APIが null を返した場合に例外を投げることを確かめる。 無効な結果をベクトルとして保存すると、後の検索結果が不正になるためである。
       *
       * @given 変換APIが要約文に対して null を返す
       * @when embedding を実行する
       * @then RuntimeException を投げる
       */
      @Test
      void 変換APIの結果がnullの場合_RuntimeExceptionを投げる() throws Exception {
        Transformer transformer = mock(Transformer.class);
        when(transformer.embedding("要約文")).thenReturn(null);
        TestVectorEntity entity = new TestVectorEntity("T001", "元の本文", "要約文");

        assertThatThrownBy(() -> entity.embedding(transformer))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("エンベディング処理に失敗しました。エンベディングAPIが無効な結果を返却しました。");
      }

      /**
       * 変換APIが空の配列を返した場合に例外を投げることを確かめる。 次元の無いベクトルは検索に使えないためである。
       *
       * @given 変換APIが要約文に対して空の配列を返す
       * @when embedding を実行する
       * @then RuntimeException を投げる
       */
      @Test
      void 変換APIの結果が空配列の場合_RuntimeExceptionを投げる() throws Exception {
        Transformer transformer = mock(Transformer.class);
        when(transformer.embedding("要約文")).thenReturn(new float[0]);
        TestVectorEntity entity = new TestVectorEntity("T001", "元の本文", "要約文");

        assertThatThrownBy(() -> entity.embedding(transformer))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("エンベディング処理に失敗しました。エンベディングAPIが無効な結果を返却しました。");
      }

      /**
       * 変換APIが入出力の例外を投げた場合に、その例外をそのまま投げることを確かめる。 呼び出し元で再試行や通知を判断できるように、例外を握りつぶさないためである。
       *
       * @given 変換APIが IOException を投げる
       * @when embedding を実行する
       * @then IOException を投げる
       */
      @Test
      void 変換APIが失敗する場合_IOExceptionを投げる() throws Exception {
        Transformer transformer = mock(Transformer.class);
        when(transformer.embedding("要約文")).thenThrow(new IOException("通信失敗"));
        TestVectorEntity entity = new TestVectorEntity("T001", "元の本文", "要約文");

        assertThatThrownBy(() -> entity.embedding(transformer)).isInstanceOf(IOException.class);
      }
    }

    @Nested
    class 境界値 {

      /**
       * 要約が null の場合に例外を投げることを確かめる。 要約が無いとベクトルを作れず、検索の対象が無くなるためである。
       *
       * @given 要約が null
       * @when embedding を実行する
       * @then RuntimeException を投げる
       */
      @Test
      void 要約がnullの場合_RuntimeExceptionを投げる() {
        TestVectorEntity entity = new TestVectorEntity("T001", "元の本文", null);

        assertThatThrownBy(() -> entity.embedding(mock(Transformer.class)))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("エンベディング対象の要約内容が空です。");
      }

      /**
       * 要約が空文字の場合に例外を投げることを確かめる。 空の要約から作ったベクトルは検索の意味を持たないためである。
       *
       * @given 要約が空文字
       * @when embedding を実行する
       * @then RuntimeException を投げる
       */
      @Test
      void 要約が空文字の場合_RuntimeExceptionを投げる() {
        TestVectorEntity entity = new TestVectorEntity("T001", "元の本文", "");

        assertThatThrownBy(() -> entity.embedding(mock(Transformer.class)))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("エンベディング対象の要約内容が空です。");
      }
    }
  }

  @Nested
  class uniqueCheck {

    @Nested
    class 正常系 {

      /**
       * 類似するレコードが無い場合に true を返すことを確かめる。 類似が無いレコードだけを新規に登録するためである。
       *
       * @given 重複チェックの件数が 0 件
       * @when uniqueCheck を類似度 0.8 で実行する
       * @then true を返す
       */
      @Test
      void 類似するレコードが無い場合_trueを返す() throws SQLException {
        TestVectorEntity entity = new TestVectorEntity("T001", "原文", "要約");

        assertThat(entity.uniqueCheck(mockConnectionWithCount(0), 0.8)).isTrue();
      }
    }

    @Nested
    class 異常系 {

      /**
       * 問い合わせに失敗した場合に SQLException を投げることを確かめる。 失敗を重複なしとして扱うと、同じ内容のレコードが登録されるためである。
       *
       * @given 準備文の作成が SQLException を投げる
       * @when uniqueCheck を実行する
       * @then SQLException を投げる
       */
      @Test
      void 問い合わせに失敗した場合_SQLExceptionを投げる() throws SQLException {
        Connection connection = mock(Connection.class);
        when(connection.prepareStatement(anyString())).thenThrow(new SQLException("接続失敗"));
        TestVectorEntity entity = new TestVectorEntity("T001", "原文", "要約");

        assertThatThrownBy(() -> entity.uniqueCheck(connection, 0.8))
            .isInstanceOf(SQLException.class);
      }
    }

    @Nested
    class 境界値 {

      /**
       * 類似するレコードが1件ある場合に false を返すことを確かめる。 1件でも類似があれば重複とみなして登録しないためである。
       *
       * @given 重複チェックの件数が 1 件
       * @when uniqueCheck を類似度 0.8 で実行する
       * @then false を返す
       */
      @Test
      void 類似するレコードが1件の場合_falseを返す() throws SQLException {
        TestVectorEntity entity = new TestVectorEntity("T001", "原文", "要約");

        assertThat(entity.uniqueCheck(mockConnectionWithCount(1), 0.8)).isFalse();
      }

      /**
       * 結果の行が無い場合に true を返すことを確かめる。 件数を読めない状態は類似が無いものとして扱うためである。
       *
       * @given 重複チェックの結果に行が無い
       * @when uniqueCheck を類似度 0.8 で実行する
       * @then true を返す
       */
      @Test
      void 結果の行が無い場合_trueを返す() throws SQLException {
        TestVectorEntity entity = new TestVectorEntity("T001", "原文", "要約");

        assertThat(entity.uniqueCheck(mockConnectionWithCount(null), 0.8)).isTrue();
      }
    }

    @Nested
    class テナント隔離 {

      /**
       * 重複チェックの問い合わせの1番目の値に、自分のテナントIDを設定することを確かめる。 他テナントの類似レコードで判定すると、別の顧客の情報を見ることになるためである。
       *
       * @given テナントIDが T001、重複チェックの件数が 0 件
       * @when uniqueCheck を類似度 0.8 で実行する
       * @then 1番目の値に T001 を設定し、true を返す
       */
      @Test
      void テナントIDがT001の場合_trueを返す() throws SQLException {
        Connection connection = mockConnectionWithCount(0);
        TestVectorEntity entity = new TestVectorEntity("T001", "原文", "要約");

        boolean unique = entity.uniqueCheck(connection, 0.8);

        verify(connection.prepareStatement(CHECK_SQL)).setString(1, "T001");
        assertThat(unique).isTrue();
      }
    }
  }
}
