package copel.sesproductpackage.core.database.base;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** {@link TenantFilterSql} の単体テスト。 */
class TenantFilterSqlTest {

  /** テスト用のテナントID。 */
  private static final String TENANT_ID = "T001";

  @Nested
  class withTenantFilter {
    @Nested
    class 正常系 {

      /**
       * WHERE 句の無い SELECT 文に、テナントIDの条件を WHERE 句として付けることを確かめる。 テナント条件は必ず付けて、他テナントの行を返さないためである。
       *
       * @given 基本SQLが SELECT id FROM T の形
       * @when withTenantFilter をテナントID T001 で実行する
       * @then tenant_id の WHERE 句を末尾に付けたSQLを返す
       */
      @Test
      void WHERE句が無い場合_WHERE句でテナントIDの条件を付けたSQLを返す() {
        assertThat(new TenantFilterSql("SELECT id FROM T").withTenantFilter(TENANT_ID))
            .isEqualTo("SELECT id FROM T WHERE tenant_id = ?");
      }

      /**
       * WHERE 句のある SQL に、既存の条件を残してテナント条件を AND で加えることを確かめる。 既存の絞り込みを消さずに、テナント条件を加えるためである。
       *
       * @given 基本SQLが WHERE 句に条件を1つ持つ
       * @when withTenantFilter をテナントID T001 で実行する
       * @then AND でテナント条件を付けたSQLを返す
       */
      @Test
      void WHERE句がある場合_AND条件でテナントIDの条件を付けたSQLを返す() {
        assertThat(new TenantFilterSql("SELECT id FROM T WHERE x = ?").withTenantFilter(TENANT_ID))
            .isEqualTo("SELECT id FROM T WHERE x = ? AND tenant_id = ?");
      }

      /**
       * ORDER BY 句を含む SQL で、テナント条件を ORDER BY 句の前に入れることを確かめる。 句の後ろに条件を置くと SQL の構文が壊れるためである。
       *
       * @given 基本SQLが WHERE 句なしで ORDER BY 句を持つ
       * @when withTenantFilter をテナントID T001 で実行する
       * @then WHERE 句を補い ORDER BY 句の前に置いたSQLを返す
       */
      @Test
      void ORDER句を含む場合_ORDER句の前にテナントIDの条件を付けたSQLを返す() {
        assertThat(new TenantFilterSql("SELECT id FROM T ORDER BY id").withTenantFilter(TENANT_ID))
            .isEqualTo("SELECT id FROM T WHERE tenant_id = ? ORDER BY id");
      }

      /**
       * LIMIT と OFFSET を含む SQL で、テナント条件を句の前に入れることを確かめる。 テナント条件を LIMIT の後ろに置くと構文が壊れるためである。
       *
       * @given 基本SQLが WHERE 句と LIMIT 句と OFFSET 句を持つ
       * @when withTenantFilter をテナントID T001 で実行する
       * @then LIMIT の前に AND でテナント条件を付けたSQLを返す
       */
      @Test
      void LIMIT句とOFFSET句を含む場合_LIMITの前にテナントIDの条件を付けたSQLを返す() {
        assertThat(
                new TenantFilterSql("SELECT id FROM T WHERE x = ? LIMIT ? OFFSET ?")
                    .withTenantFilter(TENANT_ID))
            .isEqualTo("SELECT id FROM T WHERE x = ? AND tenant_id = ? LIMIT ? OFFSET ?");
      }

      /**
       * WHERE 句に OR があると、OR 全体を括弧で包んでからテナント条件を AND で付けることを確かめる。 括弧で包まないと、テナント条件が最後の OR
       * の項だけに掛かるためである。
       *
       * @given 基本SQLの WHERE 句が a = ? OR b = ?
       * @when withTenantFilter をテナントID T001 で実行する
       * @then OR 全体を括弧で包み、AND でテナント条件を付けたSQLを返す
       */
      @Test
      void OR条件の場合_OR全体を括弧で包んだSQLを返す() {
        assertThat(
                new TenantFilterSql("SELECT id FROM T WHERE a = ? OR b = ?")
                    .withTenantFilter(TENANT_ID))
            .isEqualTo("SELECT id FROM T WHERE (a = ? OR b = ?) AND tenant_id = ?");
      }

      /**
       * SELECT EXISTS の括弧内の WHERE 句にテナント条件を挿入することを確かめる。 存在確認も他テナントの行を見つけないように括弧の中で絞るためである。
       *
       * @given 基本SQLが SELECT EXISTS の括弧内に WHERE 句を1つ持つ
       * @when withTenantFilter をテナントID T001 で実行する
       * @then 括弧の中にテナント条件を挿入したSQLを返す
       */
      @Test
      void EXISTS文の場合_括弧内にテナントIDの条件を挿入したSQLを返す() {
        assertThat(
                new TenantFilterSql("SELECT EXISTS(SELECT 1 FROM T WHERE id = ?)")
                    .withTenantFilter(TENANT_ID))
            .isEqualTo("SELECT EXISTS(SELECT 1 FROM T WHERE id = ? AND tenant_id = ?)");
      }

      /**
       * 括弧の無い EXISTS 文は通常の SQL と同じ方法で条件を付けることを確かめる。 括弧の位置が無いと、括弧の中に条件を挿入できないためである。
       *
       * @given 基本SQLが SELECT EXISTS で括弧が無く WHERE 句を持つ
       * @when withTenantFilter をテナントID T001 で実行する
       * @then WHERE 句の末尾に AND でテナント条件を付けたSQLを返す
       */
      @Test
      void EXISTS文で括弧が無い場合_通常の条件を付けたSQLを返す() {
        assertThat(
                new TenantFilterSql("SELECT EXISTS 1 FROM T WHERE id = ?")
                    .withTenantFilter(TENANT_ID))
            .isEqualTo("SELECT EXISTS 1 FROM T WHERE id = ? AND tenant_id = ?");
      }

      /**
       * UPDATE 文の WHERE 句にテナント条件を付けることを確かめる。 更新も他テナントの行を対象にしないように絞るためである。
       *
       * @given 基本SQLが UPDATE 文で WHERE 句を1つ持つ
       * @when withTenantFilter をテナントID T001 で実行する
       * @then 末尾に AND でテナント条件を付けたSQLを返す
       */
      @Test
      void UPDATE文の場合_末尾にAND条件を付けたSQLを返す() {
        assertThat(
                new TenantFilterSql("UPDATE T SET a = ? WHERE id = ?").withTenantFilter(TENANT_ID))
            .isEqualTo("UPDATE T SET a = ? WHERE id = ? AND tenant_id = ?");
      }

      /**
       * DELETE 文の FROM 句の表を主テーブルとして、テナント条件を付けることを確かめる。 削除も他テナントの行を対象にしないように絞るためである。
       *
       * @given 基本SQLが DELETE FROM T の後ろに WHERE 句を持つ
       * @when withTenantFilter をテナントID T001 で実行する
       * @then WHERE 句にテナント条件を付けたSQLを返す
       */
      @Test
      void DELETE文の場合_WHERE句にテナントIDの条件を付けたSQLを返す() {
        assertThat(new TenantFilterSql("DELETE FROM T WHERE id = ?").withTenantFilter(TENANT_ID))
            .isEqualTo("DELETE FROM T WHERE id = ? AND tenant_id = ?");
      }

      /**
       * 別名を持つ主テーブルでは、別名で修飾したテナント条件を付けることを確かめる。 別名の無い列名は結合時に曖昧になるためである。
       *
       * @given 基本SQLが FROM T t と WHERE 句を持つ
       * @when withTenantFilter をテナントID T001 で実行する
       * @then 別名 t のテナント条件を付けたSQLを返す
       */
      @Test
      void テーブルの別名がある場合_別名付きの条件を付けたSQLを返す() {
        assertThat(
                new TenantFilterSql("SELECT t.id FROM T t WHERE t.x = ?")
                    .withTenantFilter(TENANT_ID))
            .isEqualTo("SELECT t.id FROM T t WHERE t.x = ? AND t.tenant_id = ?");
      }

      /**
       * INNER JOIN では、結合先の ON 句に主テーブルと同じテナントの条件を付けることを確かめる。 結合先の行が他テナントの場合に、その行を結合させないためである。
       *
       * @given 基本SQLが INNER JOIN と ON 句と WHERE 句を持つ
       * @when withTenantFilter をテナントID T001 で実行する
       * @then ON 句を括弧で包んで結合先の条件を加え、主テーブルの条件を WHERE 句に付けたSQLを返す
       */
      @Test
      void INNER結合の場合_結合先と主テーブルの条件を付けたSQLを返す() {
        assertThat(
                new TenantFilterSql(
                        "SELECT a.id FROM T1 a INNER JOIN T2 b ON b.x = a.id WHERE a.y = ?")
                    .withTenantFilter(TENANT_ID))
            .isEqualTo(
                "SELECT a.id FROM T1 a INNER JOIN T2 b ON (b.x = a.id) AND b.tenant_id = a.tenant_id WHERE a.y = ? AND a.tenant_id = ?");
      }

      /**
       * LEFT JOIN の右側の条件は、WHERE 句ではなく ON 句に付けることを確かめる。 WHERE 句で絞ると、LEFT JOIN が内部結合に変わるためである。
       *
       * @given 基本SQLが LEFT JOIN と ON 句を持ち WHERE 句が無い
       * @when withTenantFilter をテナントID T001 で実行する
       * @then ON 句に結合先の条件を加え、主テーブルの条件を WHERE 句に付けたSQLを返す
       */
      @Test
      void LEFT結合の場合_右側の条件をON句に付けたSQLを返す() {
        assertThat(
                new TenantFilterSql("SELECT a.id FROM T1 a LEFT JOIN T2 b ON b.x = a.id")
                    .withTenantFilter(TENANT_ID))
            .isEqualTo(
                "SELECT a.id FROM T1 a LEFT JOIN T2 b ON (b.x = a.id) AND b.tenant_id = a.tenant_id WHERE a.tenant_id = ?");
      }

      /**
       * CROSS JOIN は ON 句が無いため、結合先の条件を WHERE 句の末尾に付けることを確かめる。 結合先の列どうしを比べる条件はプレースホルダを使わないためである。
       *
       * @given 基本SQLが FROM T1 a と CROSS JOIN T2 b を持ち WHERE 句が1つ
       * @when withTenantFilter をテナントID T001 で実行する
       * @then 主テーブルと結合先の条件を WHERE 句の末尾に付けたSQLを返す
       */
      @Test
      void CROSS結合の場合_結合先の条件をWHERE句の末尾に付けたSQLを返す() {
        assertThat(
                new TenantFilterSql("SELECT * FROM T1 a CROSS JOIN T2 b WHERE a.x = ?")
                    .withTenantFilter(TENANT_ID))
            .isEqualTo(
                "SELECT * FROM T1 a CROSS JOIN T2 b WHERE a.x = ? AND a.tenant_id = ? AND b.tenant_id = a.tenant_id");
      }

      /**
       * ON 句の無い INNER JOIN では、結合先の条件を WHERE 句の末尾に付けることを確かめる。 ON 句が無い結合の条件は WHERE 句に置くしかないためである。
       *
       * @given 基本SQLが INNER JOIN と WHERE 句を持ち ON 句が無い
       * @when withTenantFilter をテナントID T001 で実行する
       * @then 主テーブルと結合先の条件を WHERE 句の末尾に付けたSQLを返す
       */
      @Test
      void ON句の無い内部結合の場合_結合先の条件をWHERE句の末尾に付けたSQLを返す() {
        assertThat(
                new TenantFilterSql("SELECT * FROM T1 a INNER JOIN T2 b WHERE a.x = ?")
                    .withTenantFilter(TENANT_ID))
            .isEqualTo(
                "SELECT * FROM T1 a INNER JOIN T2 b WHERE a.x = ? AND a.tenant_id = ? AND b.tenant_id = a.tenant_id");
      }

      /**
       * 別名の無い結合では、主テーブル名で修飾したテナント条件を付けることを確かめる。 別名が無いと列の修飾先が無くなるためである。
       *
       * @given 基本SQLが別名の無い FROM T1 と INNER JOIN T2 を持つ
       * @when withTenantFilter をテナントID T001 で実行する
       * @then ON 句を括弧で包み、主テーブル名で修飾した条件を WHERE 句に付けたSQLを返す
       */
      @Test
      void 別名の無い結合の場合_主テーブル名で修飾した条件を付けたSQLを返す() {
        assertThat(
                new TenantFilterSql("SELECT * FROM T1 INNER JOIN T2 ON T2.x = T1.id")
                    .withTenantFilter(TENANT_ID))
            .isEqualTo(
                "SELECT * FROM T1 INNER JOIN T2 ON (T2.x = T1.id) AND T2.tenant_id = T1.tenant_id WHERE T1.tenant_id = ?");
      }

      /**
       * カンマ区切りの FROM 句では、2 番目以降の表の条件を WHERE 句の末尾に付けることを確かめる。 カンマ区切りの表には ON 句が無いためである。
       *
       * @given 基本SQLが FROM T1 a, T2 b と WHERE 句を持つ
       * @when withTenantFilter をテナントID T001 で実行する
       * @then 主テーブルと結合先の条件を WHERE 句の末尾に付けたSQLを返す
       */
      @Test
      void カンマ区切りの場合_結合先の条件をWHERE句の末尾に付けたSQLを返す() {
        assertThat(
                new TenantFilterSql("SELECT * FROM T1 a, T2 b WHERE a.x = ?")
                    .withTenantFilter(TENANT_ID))
            .isEqualTo(
                "SELECT * FROM T1 a, T2 b WHERE a.x = ? AND a.tenant_id = ? AND b.tenant_id = a.tenant_id");
      }

      /**
       * 2 文字以上の別名でも、別名で修飾した条件を付けることを確かめる。 1 文字だけを別名と認識すると、条件が別名を取り違えるためである。
       *
       * @given 基本SQLが FROM T tt と INNER JOIN U uu を持つ
       * @when withTenantFilter をテナントID T001 で実行する
       * @then 結合先と主テーブルの別名付きの条件を付けたSQLを返す
       */
      @Test
      void 二文字以上の別名の結合の場合_別名付きの条件を付けたSQLを返す() {
        assertThat(
                new TenantFilterSql("SELECT tt.id FROM T tt INNER JOIN U uu ON uu.x = tt.id")
                    .withTenantFilter(TENANT_ID))
            .isEqualTo(
                "SELECT tt.id FROM T tt INNER JOIN U uu ON (uu.x = tt.id) AND uu.tenant_id = tt.tenant_id WHERE tt.tenant_id = ?");
      }

      /**
       * 副問い合わせの中の FROM と WHERE を句の区切りと誤認せず、外側の句だけを書き換えることを確かめる。 内側の句を書き換えると、副問い合わせの結果が変わるためである。
       *
       * @given 基本SQLの WHERE 句に IN の副問い合わせがあり、その中に FROM と WHERE がある
       * @when withTenantFilter をテナントID T001 で実行する
       * @then 副問い合わせは変えず、主テーブルの条件を末尾に付けたSQLを返す
       */
      @Test
      void R1副問い合わせのFROMとWHEREを区切りと誤認しない場合_主テーブルの条件だけを付けたSQLを返す() {
        assertThat(
                new TenantFilterSql("SELECT id FROM T WHERE id IN (SELECT x FROM U WHERE y = ?)")
                    .withTenantFilter(TENANT_ID))
            .isEqualTo(
                "SELECT id FROM T WHERE id IN (SELECT x FROM U WHERE y = ?) AND tenant_id = ?");
      }

      /**
       * EXTRACT の中の FROM を句の区切りと誤認せず、テナント条件を WHERE 句に付けることを確かめる。 区切りを誤認すると、表名の範囲がずれて書き換えが壊れるためである。
       *
       * @given 基本SQLの SELECT 句に EXTRACT の関数呼び出しがあり、その中に FROM がある
       * @when withTenantFilter をテナントID T001 で実行する
       * @then WHERE 句の末尾に AND でテナント条件を付けたSQLを返す
       */
      @Test
      void R2EXTRACTのFROMを区切りと誤認しない場合_条件を末尾に付けたSQLを返す() {
        assertThat(
                new TenantFilterSql("SELECT EXTRACT(YEAR FROM d) AS y FROM T WHERE d > ?")
                    .withTenantFilter(TENANT_ID))
            .isEqualTo("SELECT EXTRACT(YEAR FROM d) AS y FROM T WHERE d > ? AND tenant_id = ?");
      }

      /**
       * ON 句の関数の引数のカンマを表の区切りと誤認せず、ON 句全体を括弧で包むことを確かめる。 カンマで区切ると ON 句の途中で切れ、結合先の条件が壊れるためである。
       *
       * @given 基本SQLの ON 句に関数 f の呼び出しがあり、引数に a.x と b.y を取る
       * @when withTenantFilter をテナントID T001 で実行する
       * @then ON 句を括弧で包み、結合先の条件を加えたSQLを返す
       */
      @Test
      void R3ON句の関数のカンマを区切りと誤認しない場合_ON句全体を括弧で包んだSQLを返す() {
        assertThat(
                new TenantFilterSql(
                        "SELECT a.id FROM T1 a INNER JOIN T2 b ON f(a.x, b.y) = a.id WHERE a.z = ?")
                    .withTenantFilter(TENANT_ID))
            .isEqualTo(
                "SELECT a.id FROM T1 a INNER JOIN T2 b ON (f(a.x, b.y) = a.id) AND b.tenant_id = a.tenant_id WHERE a.z = ? AND a.tenant_id = ?");
      }

      /**
       * 括弧の中の ORDER BY と LIMIT を外側の句の区切りと誤認せず、テナント条件を外側の WHERE 句に付けることを確かめる。
       * 括弧の中で区切ると、副問い合わせの途中に条件が入って構文が壊れるためである。
       *
       * @given 基本SQLの WHERE 句の副問い合わせに ORDER BY と LIMIT がある
       * @when withTenantFilter をテナントID T001 で実行する
       * @then 副問い合わせは変えず、主テーブルの条件を WHERE 句の末尾に付けたSQLを返す
       */
      @Test
      void R4括弧内のORDER句とLIMIT句を区切りと誤認しない場合_WHERE句の末尾に付けたSQLを返す() {
        assertThat(
                new TenantFilterSql(
                        "SELECT id FROM T WHERE id IN (SELECT id FROM U ORDER BY id LIMIT 1)")
                    .withTenantFilter(TENANT_ID))
            .isEqualTo(
                "SELECT id FROM T WHERE id IN (SELECT id FROM U ORDER BY id LIMIT 1) AND tenant_id = ?");
      }

      /**
       * 文字列リテラルの中の WHERE を句の区切りと誤認せず、テナント条件を付けることを確かめる。 リテラルの中で区切ると、値の途中に条件が入るためである。
       *
       * @given 基本SQLの WHERE 句の文字列リテラルに WHERE という語がある
       * @when withTenantFilter をテナントID T001 で実行する
       * @then WHERE 句の末尾に AND でテナント条件を付けたSQLを返す
       */
      @Test
      void 文字列リテラル内のキーワードを区切りと誤認しない場合_条件を付けたSQLを返す() {
        assertThat(
                new TenantFilterSql("SELECT id FROM T WHERE memo = 'a WHERE b' AND x = ?")
                    .withTenantFilter(TENANT_ID))
            .isEqualTo("SELECT id FROM T WHERE memo = 'a WHERE b' AND x = ? AND tenant_id = ?");
      }

      /**
       * FROM 句の無い SELECT 文に、テナント条件の WHERE 句を付けることを確かめる。 テナント条件は必ず付けるためである。
       *
       * @given 基本SQLが SELECT 1 の1行
       * @when withTenantFilter をテナントID T001 で実行する
       * @then WHERE 句を付けたSQLを返す
       */
      @Test
      void SELECT句のみの場合_WHERE句を付けたSQLを返す() {
        assertThat(new TenantFilterSql("SELECT 1").withTenantFilter(TENANT_ID))
            .isEqualTo("SELECT 1 WHERE tenant_id = ?");
      }

      /**
       * LEFT OUTER JOIN でも、右側の条件を ON 句に付けることを確かめる。 WHERE 句で絞ると外部結合が内部結合に変わるためである。
       *
       * @given 基本SQLが LEFT OUTER JOIN と ON 句を持つ
       * @when withTenantFilter をテナントID T001 で実行する
       * @then ON 句に結合先の条件を加え、主テーブルの条件を WHERE 句に付けたSQLを返す
       */
      @Test
      void LEFT外部結合の場合_右側の条件をON句に付けたSQLを返す() {
        assertThat(
                new TenantFilterSql("SELECT a.id FROM T1 a LEFT OUTER JOIN T2 b ON b.x = a.id")
                    .withTenantFilter(TENANT_ID))
            .isEqualTo(
                "SELECT a.id FROM T1 a LEFT OUTER JOIN T2 b ON (b.x = a.id) AND b.tenant_id = a.tenant_id WHERE a.tenant_id = ?");
      }

      /**
       * 修飾の無い JOIN は内部結合として扱い、JOIN の語を残すことを確かめる。 修飾の無い JOIN の語を書き換えると、元の結合の意味が変わるためである。
       *
       * @given 基本SQLが JOIN と ON 句を持つ
       * @when withTenantFilter をテナントID T001 で実行する
       * @then ON 句に結合先の条件を加えたSQLを返す
       */
      @Test
      void JOIN単独の場合_内部結合として扱いJOINの語を残したSQLを返す() {
        assertThat(
                new TenantFilterSql("SELECT a.id FROM T1 a JOIN T2 b ON b.x = a.id")
                    .withTenantFilter(TENANT_ID))
            .isEqualTo(
                "SELECT a.id FROM T1 a JOIN T2 b ON (b.x = a.id) AND b.tenant_id = a.tenant_id WHERE a.tenant_id = ?");
      }

      /**
       * ON 句に OR があっても、ON 句全体を括弧で包んでから結合先の条件を加えることを確かめる。 括弧で包まないと、結合先の条件が OR の一方にだけ掛かるためである。
       *
       * @given 基本SQLの ON 句に OR がある
       * @when withTenantFilter をテナントID T001 で実行する
       * @then ON 句全体を括弧で包み、結合先の条件を加えたSQLを返す
       */
      @Test
      void ON句のOR条件の場合_ON句全体を括弧で包んだSQLを返す() {
        assertThat(
                new TenantFilterSql(
                        "SELECT a.id FROM T1 a INNER JOIN T2 b ON a.x = b.x OR a.y = b.y WHERE a.z = ?")
                    .withTenantFilter(TENANT_ID))
            .isEqualTo(
                "SELECT a.id FROM T1 a INNER JOIN T2 b ON (a.x = b.x OR a.y = b.y) AND b.tenant_id = a.tenant_id WHERE a.z = ? AND a.tenant_id = ?");
      }

      /**
       * HAVING 句の OR は WHERE 句の範囲に含めず、HAVING 句をそのまま残すことを確かめる。 HAVING 句の OR
       * を括弧で包むと、集計の条件の意味が変わるためである。
       *
       * @given 基本SQLの HAVING 句に OR がある
       * @when withTenantFilter をテナントID T001 で実行する
       * @then WHERE 句の条件だけを書き換え、HAVING 句をそのまま残したSQLを返す
       */
      @Test
      void HAVING句のOR条件の場合_HAVING句は変えずに条件を付けたSQLを返す() {
        assertThat(
                new TenantFilterSql("SELECT id FROM T WHERE x = ? GROUP BY a HAVING b = ? OR c = ?")
                    .withTenantFilter(TENANT_ID))
            .isEqualTo(
                "SELECT id FROM T WHERE x = ? AND tenant_id = ? GROUP BY a HAVING b = ? OR c = ?");
      }

      /**
       * WITHIN GROUP の括弧の中の ORDER BY を句の区切りと誤認せず、WHERE 句に条件を付けることを確かめる。 WITHIN GROUP
       * の括弧を区切りとみなすと、集計の式が壊れるためである。
       *
       * @given 基本SQLの SELECT 句に WITHIN GROUP がある
       * @when withTenantFilter をテナントID T001 で実行する
       * @then WHERE 句の末尾に AND でテナント条件を付けたSQLを返す
       */
      @Test
      void WITHINGROUP句の場合_BY句でない括弧を句の区切りにしないSQLを返す() {
        assertThat(
                new TenantFilterSql(
                        "SELECT percentile_cont(0.5) WITHIN GROUP (ORDER BY x) AS m FROM T WHERE y = ?")
                    .withTenantFilter(TENANT_ID))
            .isEqualTo(
                "SELECT percentile_cont(0.5) WITHIN GROUP (ORDER BY x) AS m FROM T WHERE y = ? AND tenant_id = ?");
      }

      /**
       * リテラル内の二つの一重引用符を一つの引用符として扱い、条件を付けることを確かめる。 リテラルの終わりを誤認すると、後ろの条件が文字列の中に入るためである。
       *
       * @given 基本SQLの WHERE 句に '' を含む文字列リテラルがある
       * @when withTenantFilter をテナントID T001 で実行する
       * @then WHERE 句の末尾に AND でテナント条件を付けたSQLを返す
       */
      @Test
      void エスケープされた一重引用符の場合_リテラルを壊さず条件を付けたSQLを返す() {
        assertThat(
                new TenantFilterSql("SELECT id FROM T WHERE memo = 'it''s' AND x = ?")
                    .withTenantFilter(TENANT_ID))
            .isEqualTo("SELECT id FROM T WHERE memo = 'it''s' AND x = ? AND tenant_id = ?");
      }

      /**
       * 二重引用符で囲んだ列名の中のキーワードを句の区切りと誤認せず、条件を付けることを確かめる。 引用符の中で区切ると、列名の途中に条件が入るためである。
       *
       * @given 基本SQLの WHERE 句に二重引用符の列名がある
       * @when withTenantFilter をテナントID T001 で実行する
       * @then WHERE 句の末尾に AND でテナント条件を付けたSQLを返す
       */
      @Test
      void 二重引用符の列名の場合_引用符の中を区切りと誤認せず条件を付けたSQLを返す() {
        assertThat(
                new TenantFilterSql("SELECT \"id\" FROM T WHERE \"name\" = ?")
                    .withTenantFilter(TENANT_ID))
            .isEqualTo("SELECT \"id\" FROM T WHERE \"name\" = ? AND tenant_id = ?");
      }

      /**
       * アンダースコアで始まる列名を一つの語として扱い、条件を付けることを確かめる。 語の先頭を誤認すると、列名の一部が別の語として扱われるためである。
       *
       * @given 基本SQLの WHERE 句に _x の列名がある
       * @when withTenantFilter をテナントID T001 で実行する
       * @then WHERE 句の末尾に AND でテナント条件を付けたSQLを返す
       */
      @Test
      void アンダースコアで始まる列名の場合_列名を一つの語として条件を付けたSQLを返す() {
        assertThat(
                new TenantFilterSql("SELECT _id FROM T WHERE _x = ?").withTenantFilter(TENANT_ID))
            .isEqualTo("SELECT _id FROM T WHERE _x = ? AND tenant_id = ?");
      }

      /**
       * EXISTS の中の OR 条件は、括弧の中で OR 全体を包んでからテナント条件を付けることを確かめる。 OR
       * の一方だけにテナント条件が掛かると、他テナントの行が存在と判定されるためである。
       *
       * @given 基本SQLが SELECT EXISTS の括弧の中に OR を持つ
       * @when withTenantFilter をテナントID T001 で実行する
       * @then 括弧の中の OR 全体を括弧で包み、テナント条件を AND で付けたSQLを返す
       */
      @Test
      void EXISTS文の括弧内にOR条件がある場合_括弧内のOR全体を括弧で包んだSQLを返す() {
        assertThat(
                new TenantFilterSql("SELECT EXISTS(SELECT 1 FROM T WHERE a = ? OR b = ?)")
                    .withTenantFilter(TENANT_ID))
            .isEqualTo("SELECT EXISTS(SELECT 1 FROM T WHERE (a = ? OR b = ?) AND tenant_id = ?)");
      }

      /**
       * FOR UPDATE は句の終わりとして扱い、テナント条件を WHERE 句の中で FOR UPDATE の前に入れることを確かめる。 FOR UPDATE の後ろに条件を置くと
       * SQL の構文が壊れるためである。
       *
       * @given 基本SQLが WHERE 句と FOR UPDATE 句を持つ
       * @when withTenantFilter をテナントID T001 で実行する
       * @then FOR UPDATE の前に AND でテナント条件を付けたSQLを返す
       */
      @Test
      void FORUPDATE句がある場合_句の前に条件を付けたSQLを返す() {
        assertThat(
                new TenantFilterSql("SELECT id FROM T WHERE x = ? FOR UPDATE")
                    .withTenantFilter(TENANT_ID))
            .isEqualTo("SELECT id FROM T WHERE x = ? AND tenant_id = ? FOR UPDATE");
      }

      /**
       * WHERE 句の無い FROM 句の後ろに FOR UPDATE がある場合、FOR UPDATE の前に WHERE 句を付けることを確かめる。 テナント条件は必ず付けるが、FOR
       * UPDATE は SQL の最後に置く必要があるためである。
       *
       * @given 基本SQLが FROM 句と FOR UPDATE 句を持ち WHERE 句が無い
       * @when withTenantFilter をテナントID T001 で実行する
       * @then FOR UPDATE の前に WHERE 句でテナント条件を付けたSQLを返す
       */
      @Test
      void WHERE句が無くFORUPDATE句がある場合_句の前にWHERE句を付けたSQLを返す() {
        assertThat(new TenantFilterSql("SELECT id FROM T FOR UPDATE").withTenantFilter(TENANT_ID))
            .isEqualTo("SELECT id FROM T WHERE tenant_id = ? FOR UPDATE");
      }

      /**
       * RETURNING は句の終わりとして扱い、テナント条件を WHERE 句の中で RETURNING の前に入れることを確かめる。 RETURNING の後ろに条件を置くと SQL
       * の構文が壊れるためである。
       *
       * @given 基本SQLの UPDATE 文が WHERE 句と RETURNING 句を持つ
       * @when withTenantFilter をテナントID T001 で実行する
       * @then RETURNING の前に AND でテナント条件を付けたSQLを返す
       */
      @Test
      void RETURNING句がある場合_句の前に条件を付けたSQLを返す() {
        assertThat(
                new TenantFilterSql("UPDATE T SET a = ? WHERE id = ? RETURNING id")
                    .withTenantFilter(TENANT_ID))
            .isEqualTo("UPDATE T SET a = ? WHERE id = ? AND tenant_id = ? RETURNING id");
      }

      /**
       * SELECT 句の tenant_id は列の取得であり、テナント条件として扱わず条件を付けることを確かめる。 列を取得する指定まで拒否すると、テナントIDを一覧に含める正当な
       * SQL が実行できなくなるためである。
       *
       * @given 基本SQLの SELECT 句に p.tenant_id がある
       * @when withTenantFilter をテナントID T001 で実行する
       * @then SELECT 句の列は変えず、WHERE 句の末尾に別名付きの条件を付けたSQLを返す
       */
      @Test
      void SELECT句にtenant_idの列がある場合_列は変えず条件を付けたSQLを返す() {
        assertThat(
                new TenantFilterSql("SELECT p.tenant_id FROM T p WHERE p.x = ?")
                    .withTenantFilter(TENANT_ID))
            .isEqualTo("SELECT p.tenant_id FROM T p WHERE p.x = ? AND p.tenant_id = ?");
      }

      /**
       * GROUP BY と ORDER BY にある tenant_id は手書きとみなさず、テナント条件を付けたSQLを返すことを確かめる。 集計や並び順で tenant_id
       * を使う正当な SQL を拒否しないためである。
       *
       * @given 基本SQLの WHERE 句が x = ? で、GROUP BY と ORDER BY に tenant_id がある
       * @when withTenantFilter をテナントID T001 で実行する
       * @then WHERE 句にテナント条件を加え、GROUP BY と ORDER BY の tenant_id はそのまま残したSQLを返す
       */
      @Test
      void GROUP句とORDER句にtenant_idがある場合_句をそのまま残したSQLを返す() {
        assertThat(
                new TenantFilterSql(
                        "SELECT id FROM T WHERE x = ? GROUP BY tenant_id ORDER BY tenant_id")
                    .withTenantFilter(TENANT_ID))
            .isEqualTo(
                "SELECT id FROM T WHERE x = ? AND tenant_id = ? GROUP BY tenant_id ORDER BY tenant_id");
      }
    }

    @Nested
    class 異常系 {

      /**
       * テナントIDが null の場合に例外を投げることを確かめる。 テナントIDの無い条件は全テナントの行を対象にするためである。
       *
       * @given テナントIDが null
       * @when withTenantFilter をテナントID null で実行する
       * @then IllegalArgumentException を投げる
       */
      @Test
      void テナントIDがnullの場合_IllegalArgumentExceptionを投げる() {
        assertThatThrownBy(() -> new TenantFilterSql("SELECT id FROM T").withTenantFilter(null))
            .isInstanceOf(IllegalArgumentException.class);
      }

      /**
       * テナントIDが空文字の場合に例外を投げることを確かめる。 空のテナントIDの条件は全テナントの行を対象にするためである。
       *
       * @given テナントIDが空文字
       * @when withTenantFilter をテナントID 空文字で実行する
       * @then IllegalArgumentException を投げる
       */
      @Test
      void テナントIDが空文字の場合_IllegalArgumentExceptionを投げる() {
        assertThatThrownBy(() -> new TenantFilterSql("SELECT id FROM T").withTenantFilter(""))
            .isInstanceOf(IllegalArgumentException.class);
      }

      /**
       * 基本SQL が null の場合に例外を投げることを確かめる。 条件を付ける対象が無いため、先に止めるためである。
       *
       * @given 基本SQLが null
       * @when withTenantFilter を基本SQL null で実行する
       * @then IllegalArgumentException を投げる
       */
      @Test
      void SQLがnullの場合_IllegalArgumentExceptionを投げる() {
        assertThatThrownBy(() -> new TenantFilterSql(null).withTenantFilter(TENANT_ID))
            .isInstanceOf(IllegalArgumentException.class);
      }

      /**
       * USING による結合は書き換えず、例外で拒否することを確かめる。 USING の列は結合条件を表に出さないため、安全に書き換えられないためである。
       *
       * @given 基本SQLが INNER JOIN と USING を持つ
       * @when withTenantFilter をテナントID T001 で実行する
       * @then IllegalArgumentException を投げる
       */
      @Test
      void USINGによる結合の場合_IllegalArgumentExceptionを投げる() {
        assertThatThrownBy(
                () ->
                    new TenantFilterSql("SELECT * FROM T1 a INNER JOIN T2 b USING (id)")
                        .withTenantFilter(TENANT_ID))
            .isInstanceOf(IllegalArgumentException.class);
      }

      /**
       * NATURAL 結合は書き換えず、例外で拒否することを確かめる。 結合の列が表から決まらないため、安全に書き換えられないためである。
       *
       * @given 基本SQLが NATURAL JOIN を持つ
       * @when withTenantFilter をテナントID T001 で実行する
       * @then IllegalArgumentException を投げる
       */
      @Test
      void NATURAL結合の場合_IllegalArgumentExceptionを投げる() {
        assertThatThrownBy(
                () ->
                    new TenantFilterSql("SELECT * FROM T1 NATURAL JOIN T2")
                        .withTenantFilter(TENANT_ID))
            .isInstanceOf(IllegalArgumentException.class);
      }

      /**
       * UNION を含む SQL は書き換えず、例外で拒否することを確かめる。 どの側に条件を付けるかを決められないためである。
       *
       * @given 基本SQLが UNION を持つ
       * @when withTenantFilter をテナントID T001 で実行する
       * @then IllegalArgumentException を投げる
       */
      @Test
      void UNIONの場合_IllegalArgumentExceptionを投げる() {
        assertThatThrownBy(
                () ->
                    new TenantFilterSql("SELECT id FROM T UNION SELECT id FROM U")
                        .withTenantFilter(TENANT_ID))
            .isInstanceOf(IllegalArgumentException.class);
      }

      /**
       * EXCEPT を含む SQL は書き換えず、例外で拒否することを確かめる。 どの側に条件を付けるかを決められないためである。
       *
       * @given 基本SQLが EXCEPT を持つ
       * @when withTenantFilter をテナントID T001 で実行する
       * @then IllegalArgumentException を投げる
       */
      @Test
      void EXCEPTの場合_IllegalArgumentExceptionを投げる() {
        assertThatThrownBy(
                () ->
                    new TenantFilterSql("SELECT id FROM T EXCEPT SELECT id FROM U")
                        .withTenantFilter(TENANT_ID))
            .isInstanceOf(IllegalArgumentException.class);
      }

      /**
       * FROM 句の副問い合わせは書き換えず、例外で拒否することを確かめる。 副問い合わせの中の表に条件を付けられないためである。
       *
       * @given 基本SQLの FROM 句が副問い合わせ
       * @when withTenantFilter をテナントID T001 で実行する
       * @then IllegalArgumentException を投げる
       */
      @Test
      void FROM句の副問い合わせの場合_IllegalArgumentExceptionを投げる() {
        assertThatThrownBy(
                () ->
                    new TenantFilterSql("SELECT * FROM (SELECT id FROM U) x WHERE x.a = ?")
                        .withTenantFilter(TENANT_ID))
            .isInstanceOf(IllegalArgumentException.class);
      }

      /**
       * ON 句の無い LEFT JOIN は書き換えず、例外で拒否することを確かめる。 結合の条件を決められず、テナント条件を安全に付けられないためである。
       *
       * @given 基本SQLが ON 句の無い LEFT JOIN を持つ
       * @when withTenantFilter をテナントID T001 で実行する
       * @then IllegalArgumentException を投げる
       */
      @Test
      void ON句の無いLEFT結合の場合_IllegalArgumentExceptionを投げる() {
        assertThatThrownBy(
                () ->
                    new TenantFilterSql("SELECT * FROM T1 a LEFT JOIN T2 b WHERE a.x = ?")
                        .withTenantFilter(TENANT_ID))
            .isInstanceOf(IllegalArgumentException.class);
      }

      /**
       * カンマ区切りの表に ON 句がある SQL は書き換えず、例外で拒否することを確かめる。 どの表の結合条件か決められないためである。
       *
       * @given 基本SQLが FROM T1 a, T2 b の後ろに ON 句を持つ
       * @when withTenantFilter をテナントID T001 で実行する
       * @then IllegalArgumentException を投げる
       */
      @Test
      void カンマ区切りの表にON句がある場合_IllegalArgumentExceptionを投げる() {
        assertThatThrownBy(
                () ->
                    new TenantFilterSql("SELECT * FROM T1 a, T2 b ON b.x = a.id")
                        .withTenantFilter(TENANT_ID))
            .isInstanceOf(IllegalArgumentException.class);
      }

      /**
       * JOIN の無い LEFT の句は書き換えず、例外で拒否することを確かめる。 結合の対象の表を決められないためである。
       *
       * @given 基本SQLが LEFT の後に JOIN が無い
       * @when withTenantFilter をテナントID T001 で実行する
       * @then IllegalArgumentException を投げる
       */
      @Test
      void 結合の句が不完全な場合_IllegalArgumentExceptionを投げる() {
        assertThatThrownBy(
                () ->
                    new TenantFilterSql("SELECT * FROM T1 a LEFT T2 b ON b.x = a.id")
                        .withTenantFilter(TENANT_ID))
            .isInstanceOf(IllegalArgumentException.class);
      }

      /**
       * 表の指定が副問い合わせの場合は書き換えず、例外で拒否することを確かめる。 副問い合わせを結合先とみなすと、条件の付け先が決められないためである。
       *
       * @given 基本SQLの結合先が副問い合わせ
       * @when withTenantFilter をテナントID T001 で実行する
       * @then IllegalArgumentException を投げる
       */
      @Test
      void 表の指定が解析できない場合_IllegalArgumentExceptionを投げる() {
        assertThatThrownBy(
                () ->
                    new TenantFilterSql(
                            "SELECT * FROM T1 a INNER JOIN (SELECT id FROM U) b ON b.id = a.id")
                        .withTenantFilter(TENANT_ID))
            .isInstanceOf(IllegalArgumentException.class);
      }

      /**
       * 開き括弧が閉じていないSQLは書き換えず、例外で拒否することを確かめる。 括弧の範囲を決められず、句の切り出しができないためである。
       *
       * @given 基本SQLの括弧が閉じていない
       * @when withTenantFilter をテナントID T001 で実行する
       * @then IllegalArgumentException を投げる
       */
      @Test
      void 括弧の対応が取れない場合_IllegalArgumentExceptionを投げる() {
        assertThatThrownBy(
                () ->
                    new TenantFilterSql("SELECT id FROM T WHERE (x = ?")
                        .withTenantFilter(TENANT_ID))
            .isInstanceOf(IllegalArgumentException.class);
      }

      /**
       * 閉じ括弧が余分なSQLは書き換えず、例外で拒否することを確かめる。 括弧の範囲を決められず、句の切り出しができないためである。
       *
       * @given 基本SQLの括弧が余分に閉じている
       * @when withTenantFilter をテナントID T001 で実行する
       * @then IllegalArgumentException を投げる
       */
      @Test
      void 括弧が余分に閉じている場合_IllegalArgumentExceptionを投げる() {
        assertThatThrownBy(
                () ->
                    new TenantFilterSql("SELECT id FROM T WHERE x = ?)")
                        .withTenantFilter(TENANT_ID))
            .isInstanceOf(IllegalArgumentException.class);
      }

      /**
       * 文字列リテラルが閉じていないSQLは書き換えず、例外で拒否することを確かめる。 リテラルの範囲を決められず、句の切り出しができないためである。
       *
       * @given 基本SQLの文字列リテラルが閉じていない
       * @when withTenantFilter をテナントID T001 で実行する
       * @then IllegalArgumentException を投げる
       */
      @Test
      void 文字列が閉じていない場合_IllegalArgumentExceptionを投げる() {
        assertThatThrownBy(
                () ->
                    new TenantFilterSql("SELECT id FROM T WHERE x = 'abc")
                        .withTenantFilter(TENANT_ID))
            .isInstanceOf(IllegalArgumentException.class);
      }

      /**
       * 括弧の中に WHERE 句が無い EXISTS 文に、テナント条件の WHERE 句を括弧の中へ付けることを確かめる。 括弧の外に付けると条件が EXISTS
       * の外側に出て、他テナントの行を含むためである。
       *
       * @given 基本SQLが SELECT EXISTS の括弧の中に FROM 句だけを持つ
       * @when withTenantFilter をテナントID T001 で実行する
       * @then 括弧の中に WHERE 句を付けたSQLを返す
       */
      @Test
      void EXISTS文の括弧内にWHERE句が無い場合_WHERE句を付けたSQLを返す() {
        assertThat(
                new TenantFilterSql("SELECT EXISTS(SELECT 1 FROM T)").withTenantFilter(TENANT_ID))
            .isEqualTo("SELECT EXISTS(SELECT 1 FROM T WHERE tenant_id = ?)");
      }

      /**
       * 結合の無い ON 句は書き換えず、例外で拒否することを確かめる。 結合先の表を決められず、テナント条件を安全に付けられないためである。
       *
       * @given 基本SQLが結合の無い ON 句を持つ
       * @when withTenantFilter をテナントID T001 で実行する
       * @then IllegalArgumentException を投げる
       */
      @Test
      void 結合の対象が無いON句の場合_IllegalArgumentExceptionを投げる() {
        assertThatThrownBy(
                () -> new TenantFilterSql("SELECT id FROM T ON x = ?").withTenantFilter(TENANT_ID))
            .isInstanceOf(IllegalArgumentException.class);
      }

      /**
       * 結合の修飾語が二つ続く場合に例外を投げることを確かめる。 結合の対象の表を決められず、テナント条件を安全に付けられないためである。
       *
       * @given 基本SQLが LEFT と INNER の修飾語を続けて持つ
       * @when withTenantFilter をテナントID T001 で実行する
       * @then IllegalArgumentException を投げる
       */
      @Test
      void 結合の修飾語が続く場合_IllegalArgumentExceptionを投げる() {
        assertThatThrownBy(
                () ->
                    new TenantFilterSql("SELECT a.id FROM T1 a LEFT INNER JOIN T2 b ON b.x = a.id")
                        .withTenantFilter(TENANT_ID))
            .isInstanceOf(IllegalArgumentException.class);
      }

      /**
       * EXISTS 文の外側に FROM 句が続く形は書き換えず、例外で拒否することを確かめる。 括弧の外の表に条件を付けると、EXISTS の判定とテナント条件の範囲がずれるためである。
       *
       * @given 基本SQLの EXISTS の括弧の外に FROM 句がある
       * @when withTenantFilter をテナントID T001 で実行する
       * @then IllegalArgumentException を投げる
       */
      @Test
      void EXISTS文の外側にFROM句が続く場合_IllegalArgumentExceptionを投げる() {
        assertThatThrownBy(
                () ->
                    new TenantFilterSql("SELECT EXISTS(SELECT 1 FROM T WHERE x = ?) FROM U")
                        .withTenantFilter(TENANT_ID))
            .isInstanceOf(IllegalArgumentException.class);
      }

      /**
       * EXISTS 文の外側に AND の条件が続く形は書き換えず、例外で拒否することを確かめる。 括弧の外の条件は EXISTS
       * の判定の一部でなく、テナント条件を安全に付けられないためである。
       *
       * @given 基本SQLの EXISTS の括弧の外に AND の条件がある
       * @when withTenantFilter をテナントID T001 で実行する
       * @then IllegalArgumentException を投げる
       */
      @Test
      void EXISTS文の外側に条件が続く場合_IllegalArgumentExceptionを投げる() {
        assertThatThrownBy(
                () ->
                    new TenantFilterSql("SELECT EXISTS(SELECT 1 FROM T WHERE x = ?) AND y = ?")
                        .withTenantFilter(TENANT_ID))
            .isInstanceOf(IllegalArgumentException.class);
      }

      /**
       * UPDATE の FROM 句は書き換えず、例外で拒否することを確かめる。 FROM の表が更新対象になるため、更新対象の表にテナント条件が付かないためである。
       *
       * @given 基本SQLが UPDATE 文で FROM 句を持つ
       * @when withTenantFilter をテナントID T001 で実行する
       * @then IllegalArgumentException を投げる
       */
      @Test
      void UPDATEのFROM句の場合_IllegalArgumentExceptionを投げる() {
        assertThatThrownBy(
                () ->
                    new TenantFilterSql("UPDATE T SET a = ? FROM U WHERE T.id = U.id")
                        .withTenantFilter(TENANT_ID))
            .isInstanceOf(IllegalArgumentException.class);
      }

      /**
       * INSERT 文にはテナント条件を付けず、例外で拒否することを確かめる。 INSERT は executeInsert 側でテナントIDを列に入れて扱うため、WHERE
       * 句の条件は付けられないためである。
       *
       * @given 基本SQLが INSERT 文である
       * @when withTenantFilter をテナントID T001 で実行する
       * @then IllegalArgumentException を投げる
       */
      @Test
      void INSERT文の場合_IllegalArgumentExceptionを投げる() {
        assertThatThrownBy(
                () ->
                    new TenantFilterSql("INSERT INTO T (a) VALUES (?)").withTenantFilter(TENANT_ID))
            .isInstanceOf(IllegalArgumentException.class);
      }

      /**
       * 別名の主テーブルに tenant_id を手書きした SQL を拒否することを確かめる。 テナント条件は必ず付与されるため、手書きすると二重に付くためである。
       *
       * @given 基本SQLが FROM T t と WHERE t.tenant_id = ? を持つ
       * @when withTenantFilter をテナントID T001 で実行する
       * @then IllegalArgumentException を投げる
       */
      @Test
      void 別名の主テーブルに手書きのテナント条件がある場合_IllegalArgumentExceptionを投げる() {
        assertThatThrownBy(
                () ->
                    new TenantFilterSql("SELECT t.id FROM T t WHERE t.tenant_id = ?")
                        .withTenantFilter(TENANT_ID))
            .isInstanceOf(IllegalArgumentException.class);
      }

      /**
       * 結合先の ON 句に tenant_id を手書きした SQL を拒否することを確かめる。
       * 結合先の条件は書き換えで付与するため、手書きすると二重に付き、結合の意味も変わるためである。
       *
       * @given 基本SQLの ON 句に b.tenant_id = a.tenant_id がある
       * @when withTenantFilter をテナントID T001 で実行する
       * @then IllegalArgumentException を投げる
       */
      @Test
      void 結合先のテナント条件が手書きされている場合_IllegalArgumentExceptionを投げる() {
        assertThatThrownBy(
                () ->
                    new TenantFilterSql(
                            "SELECT a.id FROM T1 a INNER JOIN T2 b ON b.tenant_id = a.tenant_id WHERE a.x = ?")
                        .withTenantFilter(TENANT_ID))
            .isInstanceOf(IllegalArgumentException.class);
      }

      /**
       * SQL が空文字の場合に例外を投げることを確かめる。 書き換える対象が無いため、テナント条件を付けられないためである。
       *
       * @given 基本SQLが空文字
       * @when withTenantFilter をテナントID T001 で実行する
       * @then IllegalArgumentException を投げる
       */
      @Test
      void SQLが空文字の場合_IllegalArgumentExceptionを投げる() {
        assertThatThrownBy(() -> new TenantFilterSql("   ").withTenantFilter(TENANT_ID))
            .isInstanceOf(IllegalArgumentException.class);
      }

      /**
       * UPDATE の SET 句で tenant_id に代入する SQL を拒否することを確かめる。 更新後の行が別テナントに移り、テナント隔離が崩れるためである。
       *
       * @given 基本SQLが UPDATE 文で、SET 句の tenant_id に値を代入している
       * @when withTenantFilter をテナントID T001 で実行する
       * @then IllegalArgumentException を投げる
       */
      @Test
      void UPDATEのSET句でtenant_idに代入する場合_IllegalArgumentExceptionを投げる() {
        assertThatThrownBy(
                () ->
                    new TenantFilterSql("UPDATE T SET a = ?, tenant_id = ? WHERE id = ?")
                        .withTenantFilter(TENANT_ID))
            .isInstanceOf(IllegalArgumentException.class);
      }

      /**
       * UPDATE 文に SET 句が無い場合に例外を投げることを確かめる。 更新する列が分からないと、テナント条件を安全に付けられないためである。
       *
       * @given 基本SQLが UPDATE 文で、SET 句が無い
       * @when withTenantFilter をテナントID T001 で実行する
       * @then IllegalArgumentException を投げる
       */
      @Test
      void UPDATEのSET句がない場合_IllegalArgumentExceptionを投げる() {
        assertThatThrownBy(
                () -> new TenantFilterSql("UPDATE T WHERE id = ?").withTenantFilter(TENANT_ID))
            .isInstanceOf(IllegalArgumentException.class);
      }

      /**
       * WHERE 句の無い UPDATE 文の SET 句で tenant_id に代入する場合に例外を投げることを確かめる。 WHERE
       * 句が無くても、更新後の行のテナントは変えてはならないためである。
       *
       * @given 基本SQLが WHERE 句の無い UPDATE 文で、SET 句の tenant_id に値を代入している
       * @when withTenantFilter をテナントID T001 で実行する
       * @then IllegalArgumentException を投げる
       */
      @Test
      void WHERE句がないUPDATEのSET句でtenant_idに代入する場合_IllegalArgumentExceptionを投げる() {
        assertThatThrownBy(
                () -> new TenantFilterSql("UPDATE T SET tenant_id = ?").withTenantFilter(TENANT_ID))
            .isInstanceOf(IllegalArgumentException.class);
      }
    }

    @Nested
    class テナント隔離 {

      /**
       * 副問い合わせの中に tenant_id の条件があっても、主テーブルの条件を必ず付けることを確かめる。 副問い合わせの条件で主テーブルの条件を省くと、他テナントの行が返るためである。
       *
       * @given 基本SQLの WHERE 句に副問い合わせがあり、その中に tenant_id = ? がある
       * @when withTenantFilter をテナントID T001 で実行する
       * @then 副問い合わせは変えず、主テーブルの条件を末尾に付けたSQLを返す
       */
      @Test
      void WHERE句の副問い合わせにテナント条件がある場合_主テーブルの条件を付けたSQLを返す() {
        assertThat(
                new TenantFilterSql(
                        "SELECT id FROM T WHERE x = ? AND id IN (SELECT id FROM U WHERE tenant_id = ?)")
                    .withTenantFilter(TENANT_ID))
            .isEqualTo(
                "SELECT id FROM T WHERE x = ? AND id IN (SELECT id FROM U WHERE tenant_id = ?) AND tenant_id = ?");
      }

      /**
       * 別名付きの tenant_id を OR で書いた SQL を拒否することを確かめる。 OR の一方だけがテナント条件を満たす行を返すと、他テナントの行が漏れるためである。
       *
       * @given 基本SQLの WHERE 句が t.x = ? OR t.tenant_id = ? で別名 t を持つ
       * @when withTenantFilter をテナントID T001 で実行する
       * @then IllegalArgumentException を投げる
       */
      @Test
      void WHERE句のORに別名付きのテナント条件がある場合_IllegalArgumentExceptionを投げる() {
        assertThatThrownBy(
                () ->
                    new TenantFilterSql("SELECT t.id FROM T t WHERE t.x = ? OR t.tenant_id = ?")
                        .withTenantFilter(TENANT_ID))
            .isInstanceOf(IllegalArgumentException.class);
      }

      /**
       * WHERE 句に主テーブルの tenant_id を手書きした SQL を拒否することを確かめる。
       * テナント条件は必ず付与されるため、手書きすると二重に付き呼び出し側のバインド規約と合わなくなるためである。
       *
       * @given 基本SQLの WHERE 句に tenant_id = ? と x = ? がある
       * @when withTenantFilter をテナントID T001 で実行する
       * @then IllegalArgumentException を投げる
       */
      @Test
      void 主テーブルの条件が手書きされている場合_IllegalArgumentExceptionを投げる() {
        assertThatThrownBy(
                () ->
                    new TenantFilterSql("SELECT id FROM T WHERE tenant_id = ? AND x = ?")
                        .withTenantFilter(TENANT_ID))
            .isInstanceOf(IllegalArgumentException.class);
      }
    }
  }
}
