package copel.sesproductpackage.tools.itsupport;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * {@link StatementLog} の UT.
 *
 * @author Copel Co., Ltd.
 */
class StatementLogTest {

  private static final String APP = "PGSQL|PostgreSQL JDBC Driver| ";

  /** バインド値つきの SQL は、プレースホルダーを値で置き換える. */
  @Test
  void バインド値つきのSQLの場合_プレースホルダーを値で置き換える() {
    String log =
        APP
            + "LOG:  execute <unnamed>: SELECT * FROM t WHERE tenant_id = $1 AND name = $2\n"
            + APP
            + "DETAIL:  parameters: $1 = 'T001', $2 = 'it''s'\n";

    List<StatementLog.Statement> statements = StatementLog.parse(log);

    assertThat(statements).hasSize(1);
    assertThat(statements.get(0).sql())
        .isEqualTo("SELECT * FROM t WHERE tenant_id = $1 AND name = $2");
    assertThat(statements.get(0).boundSql())
        .isEqualTo("SELECT * FROM t WHERE tenant_id = 'T001' AND name = 'it''s'");
  }

  /** $1 と $10 を取り違えず、NULL もそのまま埋める. */
  @Test
  void 十個以上のバインド値の場合_番号どおりに置き換える() {
    StringBuilder placeholders = new StringBuilder();
    StringBuilder values = new StringBuilder();
    for (int i = 1; i <= 10; i++) {
      placeholders.append(i == 1 ? "" : ", ").append('$').append(i);
      values
          .append(i == 1 ? "" : ", ")
          .append('$')
          .append(i)
          .append(" = ")
          .append(i == 10 ? "NULL" : "'v" + i + "'");
    }
    String log =
        APP
            + "LOG:  execute S_1: VALUES ("
            + placeholders
            + ")\n"
            + APP
            + "DETAIL:  parameters: "
            + values
            + "\n";

    List<StatementLog.Statement> statements = StatementLog.parse(log);

    assertThat(statements.get(0).boundSql())
        .isEqualTo("VALUES ('v1', 'v2', 'v3', 'v4', 'v5', 'v6', 'v7', 'v8', 'v9', NULL)");
  }

  /** ベクトルのような数千文字のバインド値でも、正規表現の再帰が深くならず、値を最後まで読み取る. */
  @Test
  void 数千文字の長いバインド値の場合_スタックを溢れさせず置き換える() {
    String longValue = "[" + "0,".repeat(5000) + "1]";
    String log =
        APP
            + "LOG:  execute <unnamed>: SELECT * FROM t WHERE v = $1\n"
            + APP
            + "DETAIL:  parameters: $1 = '"
            + longValue
            + "'\n";

    List<StatementLog.Statement> statements = StatementLog.parse(log);

    assertThat(statements.get(0).boundSql())
        .isEqualTo("SELECT * FROM t WHERE v = '" + longValue + "'");
  }

  /** 複数行の SQL は、タブで始まる続きの行を連結する. */
  @Test
  void 複数行のSQLの場合_続きの行を連結する() {
    String log =
        APP
            + "LOG:  execute <unnamed>: SELECT *\n\tFROM t\n\tWHERE a = $1\n"
            + APP
            + "DETAIL:  parameters: $1 = '1'\n";

    assertThat(StatementLog.parse(log).get(0).boundSql())
        .isEqualTo("SELECT *\nFROM t\nWHERE a = '1'");
  }

  /** テストの準備用の接続、JDBC ドライバーの設定の文、PostgreSQL 以外の行は含めない. */
  @Test
  void アプリ以外の行が混ざっている場合_アプリのSQLだけを返す() {
    String log =
        "127.0.0.1 - - [21/Oct/2026] \"POST / HTTP/1.1\" 200 -\n"
            + "PGSQL|it-setup| LOG:  execute <unnamed>: TRUNCATE t\n"
            + APP
            + "LOG:  statement: SET extra_float_digits = 3\n"
            + APP
            + "LOG:  statement: BEGIN\n"
            + APP
            + "LOG:  connection authorized: user=nectar\n"
            + APP
            + "LOG:  execute <unnamed>: SELECT 1\n";

    assertThat(StatementLog.parse(log))
        .extracting(StatementLog.Statement::boundSql)
        .containsExactly("BEGIN", "SELECT 1");
  }
}
