# nectar-test-support

Nectar の Java リポジトリで単体テスト（UT）を書くための共通テスト部品です。
時刻の固定、設定値の差し替え、DB 接続のモック、ResultSet のモックを、宣言的に使える形で提供します。

- 対象: Java 17 のリポジトリ（JDK 17 以上 23 未満で実行）
- 依存: Core（`sesaiassistantcore`）は利用側が持つ前提（provided）。UT の仕様は `.claude/.log/Java_UT仕様.md` の第3章に従う
- 配布: GitHub Packages（`com.github.SES-AI-Assistant:nectar-test-support`）
- 差し替えの前提: 読み込み元が memory のときだけ使える（後述）

## 1. 利用側の pom.xml への追加

親 pom（`nectar-parent`）を継承すれば、本ライブラリは `test` スコープで依存に加わり、surefire の設定（memory への切り替え）も済みます。
利用側の pom に追加するものはありません。導入手順とコマンドは [`tools/README.md`](../README.md) を参照してください。

親 pom が行っている設定は次のとおりです（親 pom を使わない場合は、これらを自分で書きます）。

- `nectar-bom` の import と、本ライブラリの `test` スコープでの依存（JUnit 5・Mockito・AssertJ も推移的に揃う）
- surefire の `systemPropertyVariables` に `nectar.config.source=memory`。この設定が無い場合、拡張は
  「surefire の systemPropertyVariables に nectar.config.source=memory を設定すること」というメッセージで即座に失敗します。拡張側では自動で設定しません
- surefire の `systemPropertyVariables` に `logback.configurationFile=logback-ut.xml`（4章）

## 2. 拡張（宣言的に使う注釈）

### 2.1 時刻の固定: `@FixedClock`

現在時刻（Core の `ApplicationClock`）を指定した時刻に固定します。
クラスに付けるとそのクラスのすべてのテストに、メソッドに付けるとそのメソッドだけに適用します。
両方に付いている場合はメソッドの指定を優先します。テストの後は既定の時計に戻します。
時刻は、オフセット付きの ISO-8601 形式で書きます。タイムゾーンは Core と同じ Asia/Tokyo です。

```java
import copel.sesproductpackage.tools.testsupport.clock.FixedClock;

@FixedClock("2026-01-15T12:04:05+09:00")
class ExampleTest {

  @Test
  void 時刻を読む() {
    // ApplicationClock.INSTANCE.clock() は 2026-01-15T12:04:05+09:00 を返す
  }
}
```

### 2.2 設定値の差し替え: `@TestProperty`

Core の `Properties` にテスト用の設定値を登録します。複数指定する場合は、注釈を繰り返し付けます。

- クラスに付けた注釈は、クラスの開始時（`@BeforeAll` 相当）に登録し、クラスの終了時に削除します。
- メソッドに付けた注釈は、テストの前に登録し、テストの後に削除します。
- 削除するのは、注釈で登録したキーだけです。`@BeforeAll` などで直接登録した設定値は残ります。
- 同じキーがクラスとメソッドの両方にある場合は、メソッドの値を使います。テストの後にはクラスの値に戻します。

```java
import copel.sesproductpackage.tools.testsupport.config.TestProperty;

class ExampleTest {

  @TestProperty(key = "CACHE_TTL_MS", value = "0")
  @TestProperty(key = "NECTAR_TEST_NAME", value = "テスト")
  @Test
  void 設定値を読む() {
    // Properties.get("NECTAR_TEST_NAME") は "テスト" を返す
  }
}
```

### 2.3 DB 接続のモック: `@MockDbConnection`

Core の `DBConnection` が Mockito のモック接続を返すように設定します。
テストメソッドの引数に `java.sql.Connection` を宣言すると、同じモック接続を受け取れます。
テストの後は接続生成関数を削除します。

```java
import copel.sesproductpackage.tools.testsupport.db.MockDbConnection;
import java.sql.Connection;

class ExampleTest {

  @MockDbConnection
  @Test
  void 接続を使う(final Connection connection) throws Exception {
    // DBConnection.getConnection() は connection と同じ接続を返す
  }
}
```

### 2.4 SQL 結果のモック: `JdbcMocks` と `MockResultSetBuilder`

`MockResultSetBuilder` は、列名と行を与えると行に沿って値を返す `ResultSet` のモックを作ります。
`JdbcMocks.stubQuery` は、接続のモックに「準備文の作成、問い合わせの実行、結果の返却」を1行で設定します。

```java
import copel.sesproductpackage.tools.testsupport.db.JdbcMocks;
import copel.sesproductpackage.tools.testsupport.db.MockDbConnection;
import copel.sesproductpackage.tools.testsupport.jdbc.MockResultSetBuilder;
import java.sql.Connection;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class ExampleRepositoryTest {

  @MockDbConnection
  @Test
  void 一覧を取得する(final Connection connection) throws Exception {
    ResultSet resultSet =
        MockResultSetBuilder.builder()
            .columns("id", "name", "created_at")
            .row(1L, "山田", LocalDateTime.of(2026, 1, 15, 12, 4, 5))
            .row(2L, null, LocalDateTime.of(2026, 1, 16, 9, 0, 0))
            .build();
    JdbcMocks.stubQuery(connection, resultSet);

    // connection.prepareStatement(...).executeQuery() は resultSet を返す
  }
}
```

- `columns` で列名を順番どおりに指定する。`row` の値の数が列数と一致しない場合は `IllegalArgumentException` を投げる。
- `next` は行を順に進め、最後の行の後は `false` を返す。最初の `next` の前と最後の後で値を読むと `SQLException` を投げる。
- 列は列名（大文字と小文字は区別しない）と列番号（1から始まる）の両方で読める。存在しない列は `SQLException` を投げる。
- 対応するメソッドは `next`、`wasNull`、`close`、`getString`、`getInt`、`getLong`、`getDouble`、`getBoolean`、`getBigDecimal`、`getTimestamp`、`getDate`、`getObject`、`getMetaData`。
  それ以外のメソッドは `UnsupportedOperationException` を投げる。
- `getMetaData` は `getColumnCount`、`getColumnLabel`、`getColumnName` に対応する。列番号が範囲外の場合は `SQLException` を投げる。
- `wasNull` は、値を1つも読んでいない状態で呼ぶと `SQLException` を投げる。
- 値の変換は次のとおり。
  - 数値は `getInt`、`getLong`、`getDouble`、`getBigDecimal` で読める。文字列の数値も変換する。
  - `getInt` と `getLong` の範囲外の値、`getDouble` で無限大になる値、数値として読めない文字列は `SQLException` を投げる。
  - 日時は `Timestamp` と `LocalDateTime`、日付は `java.sql.Date` と `LocalDate` を受け付ける。
  - 値が null の場合は `wasNull` が `true` を返し、数値は 0、真偽値は `false`、その他は null を返す。

`JdbcMocks` の補助は次のとおりです。

| 補助 | 設定する呼び出し | 戻り値 |
| :--- | :--- | :--- |
| `JdbcMocks.stubQuery(connection, resultSet)` | `prepareStatement(String)` → `executeQuery()` が `resultSet` を返す | 準備文のモック |
| `JdbcMocks.stubStatement(connection, resultSet)` | `createStatement()` → `executeQuery(String)` が `resultSet` を返す | 文のモック |

## 3. 読み込み元が memory 以外の場合、または並列実行の場合

`@FixedClock`、`@TestProperty`、`@MockDbConnection` は、読み込み元が memory のときだけ動きます。
また、差し替えは静的な状態を書き換えるため、JUnit の並列実行（`junit.jupiter.execution.parallel.enabled=true`）では使えません。
並列実行が有効な場合は、テストの前に次の趣旨のメッセージで失敗します。並列実行を無効にしてください。

memory 以外（`nectar.config.source` が未設定、または memory 以外の値）では、テストの前に次のメッセージで失敗します。

```
@FixedClock は読み込み元が memory のときだけ使えます。surefire の systemPropertyVariables に nectar.config.source=memory を設定してください。
```

本番の設定や接続先を差し替えないための制限です。対処は、親 pom を継承するか、1章の surefire 設定を追加することです。

## 4. UT 用のログ設定（`logback-ut.xml`）

本ライブラリは、UT でログを出力しないための `logback-ut.xml` を同梱しています。
親 pom の surefire 設定（`logback.configurationFile=logback-ut.xml`）からだけ参照されます。

- Core の jar に同梱の `logback.xml`（Logstash のエンコーダを参照する）も、この設定で置き換わります。
- `logback-test.xml` の名前にしていないのは、テストのクラスパスで動く IT1 の起動クラスでもログが消えてしまうためです。
- ロガーのレベルは INFO のままです。テストで追加したアペンダーはログを受け取れます。
- 利用側の `src/test/resources` に同名のファイルを置くと、そちらが優先されます。

本プロジェクト（`nectar-test-support`）自身のテストは、`src/test/resources/logback-test.xml` でログを抑止しています。

## 5. 開発者向け

### 5.1 ビルドと検査

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 18)   # Maven が JDK 17 以上 23 未満で動くこと
mvn -B clean install
```

- `mvn verify` で、単体テストの実行、JaCoCo の 100% 検査（命令と分岐）、UT 仕様の検査が行われる。
- `nectar-bom` と `nectar-ut-spec-plugin` は、先に `tools/` 配下で `mvn install` しておく必要がある。
- Core の取得は GitHub Packages から行う。オフラインでの再ビルドには `-o` を使う。

### 5.2 配布

`.github/workflows/deploy-tools.yml` が、`main` への push のうち `tools/**` に変更があった場合と、手動実行で配布します。
配布の順番は、`nectar-bom`、`nectar-ut-spec-plugin`、`nectar-test-support`、`nectar-parent` です。
`nectar-test-support` は build 時に `nectar-ut-spec-plugin` を使うため、その後に配布します。
