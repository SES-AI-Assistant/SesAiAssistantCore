package copel.sesproductpackage.tools.testsupport.jdbc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** {@link MockResultSetBuilder} の単体テスト。 */
class MockResultSetBuilderTest {

  @Nested
  class builder {

    @Nested
    class 正常系 {

      /**
       * 空のビルダーから作った結果セットは next が false を返すことを確かめる。 行を指定しない場合に読み取りが始まる不具合を防ぐ。
       *
       * @given 列を指定せず、行も追加しない
       * @when builder で作ったビルダーの build を呼び出し、next を呼び出す
       * @then false を返す
       */
      @Test
      void 呼び出した場合_行を持たない結果セットを返す() throws SQLException {
        ResultSet resultSet = MockResultSetBuilder.builder().build();

        assertThat(resultSet.next()).isFalse();
      }
    }
  }

  @Nested
  class columns {

    @Nested
    class 正常系 {

      /**
       * 列名を指定し直した場合は新しい列名で値を読めることを確かめる。 列名の指定を追加ではなく置き換えと定め、古い列名が残る不具合を防ぐ。
       *
       * @given 列名を "old" で指定してから "id" と "name" で指定し直し、行 1 と "山田" を追加する
       * @when 結果セットの next を呼び出し、getString を列名 "name" で呼び出す
       * @then "山田" を返す
       */
      @Test
      void 列名を指定し直した場合_新しい列名の値を返す() throws SQLException {
        ResultSet resultSet =
            MockResultSetBuilder.builder()
                .columns("old")
                .columns("id", "name")
                .row(1L, "山田")
                .build();

        resultSet.next();

        assertThat(resultSet.getString("name")).isEqualTo("山田");
      }

      /**
       * 列名を指定し直した後は古い列名での読み取りが例外を投げることを確かめる。 置き換えた列名が有効であることを、古い列名が残らないことで裏付ける。
       *
       * @given 列名を "old" で指定してから "id" で指定し直し、行 1 を追加する
       * @when 結果セットの next を呼び出し、getString を列名 "old" で呼び出す
       * @then SQLException を投げる
       */
      @Test
      void 列名を指定し直した後に古い列名を読んだ場合_SQLExceptionを投げる() throws SQLException {
        ResultSet resultSet =
            MockResultSetBuilder.builder().columns("old").columns("id").row(1L).build();

        resultSet.next();

        assertThatThrownBy(() -> resultSet.getString("old")).isInstanceOf(SQLException.class);
      }
    }
  }

  @Nested
  class row {

    @Nested
    class 異常系 {

      /**
       * 値の数が列数と異なる行の追加は IllegalArgumentException を投げることを確かめる。 値と列がずれた状態で読み取りが進む不具合を防ぐ。
       *
       * @given 列名が id と name の2列
       * @when row を値 1 の1つだけで呼び出す
       * @then IllegalArgumentException を投げる
       */
      @Test
      void 値の数が列数と異なる場合_IllegalArgumentExceptionを投げる() {
        MockResultSetBuilder builder = MockResultSetBuilder.builder().columns("id", "name");

        assertThatThrownBy(() -> builder.row(1L)).isInstanceOf(IllegalArgumentException.class);
      }
    }
  }

  @Nested
  class build {

    @Nested
    class 正常系 {

      /**
       * 行を指定した結果セットは行を順に進め、最後に false を返すことを確かめる。 行の境界で読み取りが止まらない不具合を防ぐ。
       *
       * @given 列名が id、行が 1 と 2 の2件
       * @when next を3回呼び出す
       * @then 1回目と2回目は true を返し、3回目は false を返す
       */
      @Test
      void 行を指定した場合_nextが行を順に進めて最後にfalseを返す() throws SQLException {
        ResultSet resultSet = MockResultSetBuilder.builder().columns("id").row(1L).row(2L).build();

        assertThat(resultSet.next()).isTrue();
        assertThat(resultSet.next()).isTrue();
        assertThat(resultSet.next()).isFalse();
      }

      /**
       * ビルド後に追加した行は、既に返した結果セットに含まれないことを確かめる。 読み取り中の結果セットが途中で増える不具合を防ぐ。
       *
       * @given 列名が id、行 1 を追加してビルドした結果セット
       * @when ビルド後に行 2 を追加し、結果セットの next を2回呼び出す
       * @then 1回目は true を返し、2回目は false を返す
       */
      @Test
      void ビルド後に行を追加した場合_返した結果セットのnextがfalseを返す() throws SQLException {
        MockResultSetBuilder builder = MockResultSetBuilder.builder().columns("id").row(1L);
        ResultSet resultSet = builder.build();
        builder.row(2L);

        assertThat(resultSet.next()).isTrue();
        assertThat(resultSet.next()).isFalse();
      }

      /**
       * 列名を指定した場合は getString が行の値を文字列で返すことを確かめる。 列名での読み取りが列番号の読み取りとずれる不具合を防ぐ。
       *
       * @given 列名が id と name、行が 1 と "山田"
       * @when next を呼び出し、getString を列名 "name" で呼び出す
       * @then "山田" を返す
       */
      @Test
      void 列名を指定した場合_行の値の文字列を返す() throws SQLException {
        ResultSet resultSet =
            MockResultSetBuilder.builder().columns("id", "name").row(1L, "山田").build();

        resultSet.next();

        assertThat(resultSet.getString("name")).isEqualTo("山田");
      }

      /**
       * 列番号を指定した場合は getString が行の値を文字列で返すことを確かめる。 列番号は1から始まるため、0番目の値を返す不具合を防ぐ。
       *
       * @given 列名が id と name、行が 1 と "山田"
       * @when next を呼び出し、getString を列番号 2 で呼び出す
       * @then "山田" を返す
       */
      @Test
      void 列番号を指定した場合_行の値の文字列を返す() throws SQLException {
        ResultSet resultSet =
            MockResultSetBuilder.builder().columns("id", "name").row(1L, "山田").build();

        resultSet.next();

        assertThat(resultSet.getString(2)).isEqualTo("山田");
      }

      /**
       * 列名の大文字と小文字が異なっても同じ列の値を返すことを確かめる。 JDBC の列名は大文字と小文字を区別せずに読める。
       *
       * @given 列名が id と name、行が 1 と "山田"
       * @when next を呼び出し、getString を列名 "NAME" で呼び出す
       * @then "山田" を返す
       */
      @Test
      void 列名の大文字と小文字が異なる場合_同じ列の値を返す() throws SQLException {
        ResultSet resultSet =
            MockResultSetBuilder.builder().columns("id", "name").row(1L, "山田").build();

        resultSet.next();

        assertThat(resultSet.getString("NAME")).isEqualTo("山田");
      }

      /**
       * 数値の列は getInt、getLong、getDouble が値を返すことを確かめる。 数値の列を型ごとに読む処理が、同じ値を返す前提を守る。
       *
       * @given 列名が n、行が Integer の 7
       * @when next を呼び出し、getInt、getLong、getDouble を列名 "n" で呼び出す
       * @then 7、7、7.0 を返す
       */
      @Test
      void 数値の列を指定した場合_getIntとgetLongとgetDoubleが値を返す() throws SQLException {
        ResultSet resultSet = MockResultSetBuilder.builder().columns("n").row(7).build();

        resultSet.next();

        assertThat(resultSet.getInt("n")).isEqualTo(7);
        assertThat(resultSet.getLong("n")).isEqualTo(7L);
        assertThat(resultSet.getDouble("n")).isEqualTo(7.0);
      }

      /**
       * 文字列の数値は getInt が数値に変換して返すことを確かめる。 数値を文字列で保持する列も、数値として読める前提を守る。
       *
       * @given 列名が n、行が文字列の "12"
       * @when next を呼び出し、getInt を列名 "n" で呼び出す
       * @then 12 を返す
       */
      @Test
      void 文字列の数値を指定した場合_数値に変換した値を返す() throws SQLException {
        ResultSet resultSet = MockResultSetBuilder.builder().columns("n").row("12").build();

        resultSet.next();

        assertThat(resultSet.getInt("n")).isEqualTo(12);
      }

      /**
       * 真偽値の列は getBoolean が値を返すことを確かめる。 真偽値の列を読み取る処理が、値を反転させない前提を守る。
       *
       * @given 列名が flag、行が Boolean の true
       * @when next を呼び出し、getBoolean を列名 "flag" で呼び出す
       * @then true を返す
       */
      @Test
      void 真偽値の列を指定した場合_getBooleanが値を返す() throws SQLException {
        ResultSet resultSet = MockResultSetBuilder.builder().columns("flag").row(true).build();

        resultSet.next();

        assertThat(resultSet.getBoolean("flag")).isTrue();
      }

      /**
       * BigDecimal の列は getBigDecimal が値を返すことを確かめる。 金額の列を精度を落とさずに読める前提を守る。
       *
       * @given 列名が amount、行が BigDecimal の "1234.50"
       * @when next を呼び出し、getBigDecimal を列名 "amount" で呼び出す
       * @then "1234.50" の BigDecimal を返す
       */
      @Test
      void BigDecimalの列を指定した場合_getBigDecimalが値を返す() throws SQLException {
        ResultSet resultSet =
            MockResultSetBuilder.builder().columns("amount").row(new BigDecimal("1234.50")).build();

        resultSet.next();

        assertThat(resultSet.getBigDecimal("amount")).isEqualTo(new BigDecimal("1234.50"));
      }

      /**
       * Timestamp の列は getTimestamp が値をそのまま返すことを確かめる。 日時の列を変換せずに読める前提を守る。
       *
       * @given 列名が created_at、行が Timestamp の 2026-01-15 12:04:05
       * @when next を呼び出し、getTimestamp を列名 "created_at" で呼び出す
       * @then 同じ Timestamp を返す
       */
      @Test
      void Timestampの列を指定した場合_getTimestampが値を返す() throws SQLException {
        Timestamp timestamp = Timestamp.valueOf(LocalDateTime.of(2026, 1, 15, 12, 4, 5));
        ResultSet resultSet =
            MockResultSetBuilder.builder().columns("created_at").row(timestamp).build();

        resultSet.next();

        assertThat(resultSet.getTimestamp("created_at")).isEqualTo(timestamp);
      }

      /**
       * LocalDateTime の列は getTimestamp が同じ日時の Timestamp に変換して返すことを確かめる。 java.time で作ったテストデータを JDBC
       * の型で読めるようにする。
       *
       * @given 列名が created_at、行が LocalDateTime の 2026-01-15T12:04:05
       * @when next を呼び出し、getTimestamp を列名 "created_at" で呼び出す
       * @then 2026-01-15 12:04:05 と同じ日時の Timestamp を返す
       */
      @Test
      void LocalDateTimeの列を指定した場合_getTimestampが変換した値を返す() throws SQLException {
        ResultSet resultSet =
            MockResultSetBuilder.builder()
                .columns("created_at")
                .row(LocalDateTime.of(2026, 1, 15, 12, 4, 5))
                .build();

        resultSet.next();

        assertThat(resultSet.getTimestamp("created_at"))
            .isEqualTo(Timestamp.valueOf(LocalDateTime.of(2026, 1, 15, 12, 4, 5)));
      }

      /**
       * java.sql.Date の列は getDate が値をそのまま返すことを確かめる。 日付の列を変換せずに読める前提を守る。
       *
       * @given 列名が birthday、行が java.sql.Date の 2026-01-15
       * @when next を呼び出し、getDate を列名 "birthday" で呼び出す
       * @then 同じ java.sql.Date を返す
       */
      @Test
      void Date型の列を指定した場合_getDateが値を返す() throws SQLException {
        Date date = Date.valueOf(LocalDate.of(2026, 1, 15));
        ResultSet resultSet = MockResultSetBuilder.builder().columns("birthday").row(date).build();

        resultSet.next();

        assertThat(resultSet.getDate("birthday")).isEqualTo(date);
      }

      /**
       * LocalDate の列は getDate が同じ日付の java.sql.Date に変換して返すことを確かめる。 java.time で作ったテストデータを JDBC
       * の型で読めるようにする。
       *
       * @given 列名が birthday、行が LocalDate の 2026-01-15
       * @when next を呼び出し、getDate を列名 "birthday" で呼び出す
       * @then 2026-01-15 と同じ java.sql.Date を返す
       */
      @Test
      void LocalDateの列を指定した場合_getDateが変換した値を返す() throws SQLException {
        ResultSet resultSet =
            MockResultSetBuilder.builder()
                .columns("birthday")
                .row(LocalDate.of(2026, 1, 15))
                .build();

        resultSet.next();

        assertThat(resultSet.getDate("birthday"))
            .isEqualTo(Date.valueOf(LocalDate.of(2026, 1, 15)));
      }

      /**
       * 任意の型の列は getObject が値をそのまま返すことを確かめる。 型を限定しない読み取りで、値を変換しない前提を守る。
       *
       * @given 列名が payload、行が文字列の "raw"
       * @when next を呼び出し、getObject を列名 "payload" で呼び出す
       * @then "raw" を返す
       */
      @Test
      void 任意の型の列を指定した場合_行の値を返す() throws SQLException {
        ResultSet resultSet = MockResultSetBuilder.builder().columns("payload").row("raw").build();

        resultSet.next();

        assertThat(resultSet.getObject("payload")).isEqualTo("raw");
      }

      /**
       * 値が null の列は wasNull が true を返し、getString が null を返すことを確かめる。 null を空文字として扱う不具合を防ぎ、呼び出し側が
       * null を判定できるようにする。
       *
       * @given 列名が name、行が null
       * @when next を呼び出し、getString を列名 "name" で呼び出してから wasNull を呼び出す
       * @then getString は null を返し、wasNull は true を返す
       */
      @Test
      void 値がnullの場合_getStringがnullを返しwasNullがtrueを返す() throws SQLException {
        ResultSet resultSet =
            MockResultSetBuilder.builder().columns("name").row((Object) null).build();

        resultSet.next();

        assertThat(resultSet.getString("name")).isNull();
        assertThat(resultSet.wasNull()).isTrue();
      }

      /**
       * 値が null でない列は wasNull が false を返すことを確かめる。 直前の null の判定を引き継いで、値のある列を null と誤る不具合を防ぐ。
       *
       * @given 列名が name、行が "山田"
       * @when next を呼び出し、getString を列名 "name" で呼び出してから wasNull を呼び出す
       * @then wasNull は false を返す
       */
      @Test
      void 値がnullでない場合_wasNullがfalseを返す() throws SQLException {
        ResultSet resultSet = MockResultSetBuilder.builder().columns("name").row("山田").build();

        resultSet.next();
        resultSet.getString("name");

        assertThat(resultSet.wasNull()).isFalse();
      }

      /**
       * 値が null の数値の列は getInt、getLong、getDouble が 0 を返すことを確かめる。 null を数値として読んだときの既定値を JDBC と同じ 0
       * に揃える。
       *
       * @given 列名が n、行が null
       * @when next を呼び出し、getInt、getLong、getDouble を列名 "n" で呼び出す
       * @then 0、0、0.0 を返す
       */
      @Test
      void 値がnullの場合_数値の列のgetIntとgetLongとgetDoubleが0を返す() throws SQLException {
        ResultSet resultSet =
            MockResultSetBuilder.builder().columns("n").row((Object) null).build();

        resultSet.next();

        assertThat(resultSet.getInt("n")).isZero();
        assertThat(resultSet.getLong("n")).isZero();
        assertThat(resultSet.getDouble("n")).isZero();
      }

      /**
       * 値が null の真偽値の列は getBoolean が false を返すことを確かめる。 null の真偽値を true と誤って読む不具合を防ぐ。
       *
       * @given 列名が flag、行が null
       * @when next を呼び出し、getBoolean を列名 "flag" で呼び出す
       * @then false を返す
       */
      @Test
      void 値がnullの場合_getBooleanがfalseを返す() throws SQLException {
        ResultSet resultSet =
            MockResultSetBuilder.builder().columns("flag").row((Object) null).build();

        resultSet.next();

        assertThat(resultSet.getBoolean("flag")).isFalse();
      }

      /**
       * 値が null の日時、日付、金額の列は、それぞれ null を返すことを確かめる。 null を変換して別の値にする不具合を防ぐ。
       *
       * @given 列名が amount、created_at、birthday の3列、行の値がすべて null
       * @when next を呼び出し、getBigDecimal、getTimestamp、getDate を各列名で呼び出す
       * @then 3つとも null を返す
       */
      @Test
      void 値がnullの場合_getBigDecimalとgetTimestampとgetDateがnullを返す() throws SQLException {
        ResultSet resultSet =
            MockResultSetBuilder.builder()
                .columns("amount", "created_at", "birthday")
                .row((Object) null, (Object) null, (Object) null)
                .build();

        resultSet.next();

        assertThat(resultSet.getBigDecimal("amount")).isNull();
        assertThat(resultSet.getTimestamp("created_at")).isNull();
        assertThat(resultSet.getDate("birthday")).isNull();
      }

      /**
       * close を呼び出しても例外を投げず、値の読み取りを続けられることを確かめる。 try-with-resources で閉じた後も、テストの検証で値を読める前提を守る。
       *
       * @given 列名が id、行が 1
       * @when next を呼び出し、close を呼び出す
       * @then 例外を投げず、何もしない
       */
      @Test
      void closeを呼んだ場合_何もしない() throws SQLException {
        ResultSet resultSet = MockResultSetBuilder.builder().columns("id").row(1L).build();

        resultSet.next();
        resultSet.close();

        assertThat(resultSet.getLong("id")).isEqualTo(1L);
      }

      /**
       * toString を呼び出すと、モックの名前を返すことを確かめる。 テストの失敗メッセージで結果セットを識別できるようにする。
       *
       * @given 列名が id、行が 1
       * @when 結果セットの toString を呼び出す
       * @then "MockResultSet" を返す
       */
      @Test
      void toStringを呼んだ場合_モックの名前を返す() {
        ResultSet resultSet = MockResultSetBuilder.builder().columns("id").row(1L).build();

        assertThat(resultSet.toString()).isEqualTo("MockResultSet");
      }

      /**
       * 未対応のメソッドを呼び出すと UnsupportedOperationException を投げることを確かめる。 対応していない読み取りを黙って null
       * で返し、テストが誤って通る不具合を防ぐ。
       *
       * @given 列名が id、行が 1
       * @when getFetchSize を呼び出す
       * @then UnsupportedOperationException を投げる
       */
      @Test
      void 未対応のメソッドを呼んだ場合_UnsupportedOperationExceptionを投げる() {
        ResultSet resultSet = MockResultSetBuilder.builder().columns("id").row(1L).build();

        assertThatThrownBy(resultSet::getFetchSize)
            .isInstanceOf(UnsupportedOperationException.class);
      }

      /**
       * メタデータから列数を取得できることを確かめる。 列数の取得で実際の列の数と合わない不具合を防ぐ。
       *
       * @given 列名が id と name の2列、行が 1 と "山田"
       * @when getMetaData を呼び出し、getColumnCount を呼び出す
       * @then 2 を返す
       */
      @Test
      void メタデータを取得した場合_列数を返す() throws SQLException {
        ResultSet resultSet =
            MockResultSetBuilder.builder().columns("id", "name").row(1L, "山田").build();

        assertThat(resultSet.getMetaData().getColumnCount()).isEqualTo(2);
      }

      /**
       * メタデータの列番号から列名を取得できることを確かめる。 列番号と列名の対応を取り違える不具合を防ぐ。
       *
       * @given 列名が id と name の2列、行が 1 と "山田"
       * @when getMetaData を呼び出し、getColumnLabel を列番号 2、getColumnName を列番号 1 で呼び出す
       * @then getColumnLabel は "name" を返し、getColumnName は "id" を返す
       */
      @Test
      void メタデータの列番号を指定した場合_列名を返す() throws SQLException {
        ResultSet resultSet =
            MockResultSetBuilder.builder().columns("id", "name").row(1L, "山田").build();

        assertThat(resultSet.getMetaData().getColumnLabel(2)).isEqualTo("name");
        assertThat(resultSet.getMetaData().getColumnName(1)).isEqualTo("id");
      }

      /**
       * メタデータの未対応のメソッドを呼び出すと UnsupportedOperationException を投げることを確かめる。 対応していない情報を黙って既定値で返す不具合を防ぐ。
       *
       * @given 列名が id、行が 1
       * @when getMetaData で得たメタデータに対して getPrecision を列番号 1 で呼び出す
       * @then UnsupportedOperationException を投げる
       */
      @Test
      void メタデータの未対応のメソッドを呼んだ場合_UnsupportedOperationExceptionを投げる() {
        ResultSet resultSet = MockResultSetBuilder.builder().columns("id").row(1L).build();

        assertThatThrownBy(() -> resultSet.getMetaData().getPrecision(1))
            .isInstanceOf(UnsupportedOperationException.class);
      }
    }

    @Nested
    class 異常系 {

      /**
       * next を呼ぶ前に値を読むと SQLException を投げることを確かめる。 行の位置が無い状態で値を返す不具合を防ぐ。
       *
       * @given 列名が id、行が 1
       * @when next を呼ばずに getString を列名 "id" で呼び出す
       * @then SQLException を投げる
       */
      @Test
      void 次の行へ進める前に読んだ場合_SQLExceptionを投げる() {
        ResultSet resultSet = MockResultSetBuilder.builder().columns("id").row(1L).build();

        assertThatThrownBy(() -> resultSet.getString("id")).isInstanceOf(SQLException.class);
      }

      /**
       * 全行を読み終えた後に値を読むと SQLException を投げることを確かめる。 最後の行より後の値を返す不具合を防ぐ。
       *
       * @given 列名が id、行が 1 の1件
       * @when next を2回呼び出し、2回目の後に getString を列名 "id" で呼び出す
       * @then SQLException を投げる
       */
      @Test
      void 全行を読み終えた後に読んだ場合_SQLExceptionを投げる() throws SQLException {
        ResultSet resultSet = MockResultSetBuilder.builder().columns("id").row(1L).build();

        resultSet.next();
        resultSet.next();

        assertThatThrownBy(() -> resultSet.getString("id")).isInstanceOf(SQLException.class);
      }

      /**
       * 存在しない列名で読むと SQLException を投げることを確かめる。 列名の誤りを黙って別の列の値で埋める不具合を防ぐ。
       *
       * @given 列名が id、行が 1
       * @when next を呼び出し、getString を列名 "missing" で呼び出す
       * @then SQLException を投げる
       */
      @Test
      void 列名が存在しない場合_SQLExceptionを投げる() throws SQLException {
        ResultSet resultSet = MockResultSetBuilder.builder().columns("id").row(1L).build();

        resultSet.next();

        assertThatThrownBy(() -> resultSet.getString("missing")).isInstanceOf(SQLException.class);
      }

      /**
       * 列番号が0の場合は SQLException を投げることを確かめる。 列番号は1から始まるため、範囲外の番号を黙って受け付ける不具合を防ぐ。
       *
       * @given 列名が id、行が 1
       * @when next を呼び出し、getString を列番号 0 で呼び出す
       * @then SQLException を投げる
       */
      @Test
      void 列番号が0の場合_SQLExceptionを投げる() throws SQLException {
        ResultSet resultSet = MockResultSetBuilder.builder().columns("id").row(1L).build();

        resultSet.next();

        assertThatThrownBy(() -> resultSet.getString(0)).isInstanceOf(SQLException.class);
      }

      /**
       * 列番号が列数を超える場合は SQLException を投げることを確かめる。 存在しない列の値を読む不具合を防ぐ。
       *
       * @given 列名が id の1列、行が 1
       * @when next を呼び出し、getString を列番号 2 で呼び出す
       * @then SQLException を投げる
       */
      @Test
      void 列番号が列数を超える場合_SQLExceptionを投げる() throws SQLException {
        ResultSet resultSet = MockResultSetBuilder.builder().columns("id").row(1L).build();

        resultSet.next();

        assertThatThrownBy(() -> resultSet.getString(2)).isInstanceOf(SQLException.class);
      }

      /**
       * 数値の列に真偽値の値を読むと SQLException を投げることを確かめる。 数値に変換できない値を0に置き換える不具合を防ぐ。
       *
       * @given 列名が n、行が Boolean の true
       * @when next を呼び出し、getInt を列名 "n" で呼び出す
       * @then SQLException を投げる
       */
      @Test
      void 数値の列に真偽値の値の場合_SQLExceptionを投げる() throws SQLException {
        ResultSet resultSet = MockResultSetBuilder.builder().columns("n").row(true).build();

        resultSet.next();

        assertThatThrownBy(() -> resultSet.getInt("n")).isInstanceOf(SQLException.class);
      }

      /**
       * 真偽値の列に数値の値を読むと SQLException を投げることを確かめる。 真偽値に変換できない値を黙って読む不具合を防ぐ。
       *
       * @given 列名が flag、行が Integer の 1
       * @when next を呼び出し、getBoolean を列名 "flag" で呼び出す
       * @then SQLException を投げる
       */
      @Test
      void 真偽値の列に数値の値の場合_SQLExceptionを投げる() throws SQLException {
        ResultSet resultSet = MockResultSetBuilder.builder().columns("flag").row(1).build();

        resultSet.next();

        assertThatThrownBy(() -> resultSet.getBoolean("flag")).isInstanceOf(SQLException.class);
      }

      /**
       * 日時の列に文字列の値を読むと SQLException を投げることを確かめる。 日時に変換できない文字列を読んで不正な日時を返す不具合を防ぐ。
       *
       * @given 列名が created_at、行が文字列の "2026-01-15"
       * @when next を呼び出し、getTimestamp を列名 "created_at" で呼び出す
       * @then SQLException を投げる
       */
      @Test
      void 日時の列に文字列の値の場合_SQLExceptionを投げる() throws SQLException {
        ResultSet resultSet =
            MockResultSetBuilder.builder().columns("created_at").row("2026-01-15").build();

        resultSet.next();

        assertThatThrownBy(() -> resultSet.getTimestamp("created_at"))
            .isInstanceOf(SQLException.class);
      }

      /**
       * 日付の列に文字列の値を読むと SQLException を投げることを確かめる。 日付に変換できない文字列を読んで不正な日付を返す不具合を防ぐ。
       *
       * @given 列名が birthday、行が文字列の "2026-01-15"
       * @when next を呼び出し、getDate を列名 "birthday" で呼び出す
       * @then SQLException を投げる
       */
      @Test
      void 日付の列に文字列の値の場合_SQLExceptionを投げる() throws SQLException {
        ResultSet resultSet =
            MockResultSetBuilder.builder().columns("birthday").row("2026-01-15").build();

        resultSet.next();

        assertThatThrownBy(() -> resultSet.getDate("birthday")).isInstanceOf(SQLException.class);
      }

      /**
       * 整数の範囲外の値を getInt で読むと SQLException を投げることを確かめる。 int に収まらない値を切り捨てて別の値で通る不具合を防ぐ。
       *
       * @given 列名が n、行が長整数の 3000000000 と -3000000000 の2行
       * @when next を呼び出して getInt を列名 "n" で呼び出し、2行目でも同じ操作を繰り返す
       * @then いずれの場合も SQLException を投げる
       */
      @Test
      void 整数の範囲外の値の場合_SQLExceptionを投げる() throws SQLException {
        ResultSet resultSet =
            MockResultSetBuilder.builder().columns("n").row(3000000000L).row(-3000000000L).build();

        resultSet.next();
        assertThatThrownBy(() -> resultSet.getInt("n")).isInstanceOf(SQLException.class);
        resultSet.next();
        assertThatThrownBy(() -> resultSet.getInt("n")).isInstanceOf(SQLException.class);
      }

      /**
       * long の範囲外の値を getLong で読むと SQLException を投げることを確かめる。 long に収まらない値を切り捨てて別の値で通る不具合を防ぐ。
       *
       * @given 列名が n、行が BigDecimal の 1E+30
       * @when next を呼び出し、getLong を列名 "n" で呼び出す
       * @then SQLException を投げる
       */
      @Test
      void 長整数の範囲外の値の場合_SQLExceptionを投げる() throws SQLException {
        ResultSet resultSet =
            MockResultSetBuilder.builder().columns("n").row(new BigDecimal("1E+30")).build();

        resultSet.next();

        assertThatThrownBy(() -> resultSet.getLong("n")).isInstanceOf(SQLException.class);
      }

      /**
       * 数値として読めない文字列を getInt で読むと SQLException を投げることを確かめる。 NumberFormatException が呼び出し側に漏れ、JDBC
       * の例外の扱いと食い違う不具合を防ぐ。
       *
       * @given 列名が n、行が文字列の "abc"
       * @when next を呼び出し、getInt を列名 "n" で呼び出す
       * @then SQLException を投げる
       */
      @Test
      void 数値として読めない文字列の場合_SQLExceptionを投げる() throws SQLException {
        ResultSet resultSet = MockResultSetBuilder.builder().columns("n").row("abc").build();

        resultSet.next();

        assertThatThrownBy(() -> resultSet.getInt("n")).isInstanceOf(SQLException.class);
      }

      /**
       * 倍精度の範囲外の値を getDouble で読むと SQLException を投げることを確かめる。 無限大の値を正常な数値として返す不具合を防ぐ。
       *
       * @given 列名が n、行が BigDecimal の 1E+400
       * @when next を呼び出し、getDouble を列名 "n" で呼び出す
       * @then SQLException を投げる
       */
      @Test
      void 倍精度の範囲外の値の場合_SQLExceptionを投げる() throws SQLException {
        ResultSet resultSet =
            MockResultSetBuilder.builder().columns("n").row(new BigDecimal("1E+400")).build();

        resultSet.next();

        assertThatThrownBy(() -> resultSet.getDouble("n")).isInstanceOf(SQLException.class);
      }

      /**
       * 値を読む前に wasNull を呼ぶと SQLException を投げることを確かめる。 判定の対象がない状態で false を返し、null の判定を誤る不具合を防ぐ。
       *
       * @given 列名が id、行が 1
       * @when next を呼び出してから、値を読まずに wasNull を呼び出す
       * @then SQLException を投げる
       */
      @Test
      void 値を読む前にwasNullを呼んだ場合_SQLExceptionを投げる() throws SQLException {
        ResultSet resultSet = MockResultSetBuilder.builder().columns("id").row(1L).build();

        resultSet.next();

        assertThatThrownBy(resultSet::wasNull).isInstanceOf(SQLException.class);
      }

      /**
       * メタデータの列番号が範囲外の場合は SQLException を投げることを確かめる。 存在しない列名を黙って返す不具合を防ぐ。
       *
       * @given 列名が id の1列、行が 1
       * @when getMetaData で得たメタデータに対して getColumnLabel を列番号 0 と 2 で呼び出す
       * @then いずれの場合も SQLException を投げる
       */
      @Test
      void メタデータの列番号が範囲外の場合_SQLExceptionを投げる() {
        ResultSet resultSet = MockResultSetBuilder.builder().columns("id").row(1L).build();

        assertThatThrownBy(() -> resultSet.getMetaData().getColumnLabel(0))
            .isInstanceOf(SQLException.class);
        assertThatThrownBy(() -> resultSet.getMetaData().getColumnLabel(2))
            .isInstanceOf(SQLException.class);
      }
    }
  }
}
