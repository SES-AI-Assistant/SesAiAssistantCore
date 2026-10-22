package copel.sesproductpackage.core.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** {@link FulltextConditionsWhereClause} の単体テスト。 */
class FulltextConditionsWhereClauseTest {

  /** 単一列の検索で使う列名式。 */
  private static final String COLUMN = "raw_content";

  /**
   * 検索条件を1件作る。
   *
   * @param operator 演算子
   * @param keyword 検索キーワード
   * @param negated 否定フラグ
   * @return 検索条件
   */
  private static FulltextCondition cond(
      final String operator, final String keyword, final boolean negated) {
    return new FulltextCondition(operator, keyword, negated);
  }

  @Nested
  class hasSearchableKeyword {

    @Nested
    class 正常系 {

      /**
       * 検索可能なキーワードが1件ある場合に true を返すことを確かめる。 検索条件があるときだけ全文検索の WHERE 句を組み立てるためである。
       *
       * @given 検索条件が AND の "Java" を1件持つリスト
       * @when hasSearchableKeyword を実行する
       * @then true を返す
       */
      @Test
      void 検索可能な条件が1件ある場合_trueを返す() {
        assertThat(
                FulltextConditionsWhereClause.hasSearchableKeyword(
                    List.of(cond("AND", "Java", false))))
            .isTrue();
      }

      /**
       * 先頭が null でも後ろに検索可能な条件があれば true を返すことを確かめる。 null の要素で検索全体が無効にならないようにするためである。
       *
       * @given 検索条件が null と AND の "Java" を持つリスト
       * @when hasSearchableKeyword を実行する
       * @then true を返す
       */
      @Test
      void null要素の後に検索可能な条件がある場合_trueを返す() {
        assertThat(
                FulltextConditionsWhereClause.hasSearchableKeyword(
                    Arrays.asList(null, cond("AND", "Java", false))))
            .isTrue();
      }
    }

    @Nested
    class 境界値 {

      /**
       * 検索条件のリストが null の場合に false を返すことを確かめる。 条件が無い検索では WHERE 句を組み立てないためである。
       *
       * @given 検索条件のリストが null
       * @when hasSearchableKeyword を実行する
       * @then false を返す
       */
      @Test
      void 検索条件のリストがnullの場合_falseを返す() {
        assertThat(FulltextConditionsWhereClause.hasSearchableKeyword(null)).isFalse();
      }

      /**
       * 検索条件のリストが空の場合に false を返すことを確かめる。 条件が無い検索では WHERE 句を組み立てないためである。
       *
       * @given 検索条件のリストが空
       * @when hasSearchableKeyword を実行する
       * @then false を返す
       */
      @Test
      void 検索条件のリストが空の場合_falseを返す() {
        assertThat(FulltextConditionsWhereClause.hasSearchableKeyword(Collections.emptyList()))
            .isFalse();
      }

      /**
       * 全ての要素が null の場合に false を返すことを確かめる。 null だけの検索条件で WHERE 句を組み立てないためである。
       *
       * @given 検索条件のリストが null の要素だけを持つ
       * @when hasSearchableKeyword を実行する
       * @then false を返す
       */
      @Test
      void 全ての条件がnullの場合_falseを返す() {
        assertThat(
                FulltextConditionsWhereClause.hasSearchableKeyword(Collections.singletonList(null)))
            .isFalse();
      }

      /**
       * キーワードが空白だけの条件しか無い場合に false を返すことを確かめる。 空白の検索語で全行に一致させないためである。
       *
       * @given 検索条件が AND の " " を1件持つリスト
       * @when hasSearchableKeyword を実行する
       * @then false を返す
       */
      @Test
      void 空白だけのキーワードの場合_falseを返す() {
        assertThat(
                FulltextConditionsWhereClause.hasSearchableKeyword(
                    List.of(cond("AND", "   ", false))))
            .isFalse();
      }
    }
  }

  @Nested
  class build {

    @Nested
    class 正常系 {

      /**
       * 含む条件を1件指定した場合に LIKE の条件と前後に % を付けた検索語を返すことを確かめる。 部分一致で検索するため、検索語の前後に % を付ける。
       *
       * @given 列名式が "raw_content"、検索条件が AND の "Java" を1件
       * @when build を実行する
       * @then {@code (((raw_content LIKE ? ESCAPE '\\')))} を返し、"%Java%" を値として返す
       */
      @Test
      void 含む条件が1件の場合_LIKE条件と検索語を返す() {
        FulltextConditionsWhereClause.Built built =
            FulltextConditionsWhereClause.build(COLUMN, List.of(cond("AND", "Java", false)));

        assertThat(built.getWhereClauseWithoutWhereKeyword())
            .isEqualTo("(((raw_content LIKE ? ESCAPE '\\')))");
        assertThat(built.getLikeParams()).containsExactly("%Java%");
      }

      /**
       * AND の条件を2件指定した場合に、両方の LIKE 条件を AND で連結することを確かめる。 複数のキーワードを両方含む行だけを対象にするためである。
       *
       * @given 列名式が "raw_content"、検索条件が AND の "Java" と AND の "AWS"
       * @when build を実行する
       * @then {@code (((raw_content LIKE ? ESCAPE '\\') AND (raw_content LIKE ? ESCAPE '\\')))}
       *     を返し、2件の検索語を返す
       */
      @Test
      void AND条件を2件指定する場合_両方のLIKE条件をANDで連結した条件式を返す() {
        FulltextConditionsWhereClause.Built built =
            FulltextConditionsWhereClause.build(
                COLUMN, List.of(cond("AND", "Java", false), cond("AND", "AWS", false)));

        assertThat(built.getWhereClauseWithoutWhereKeyword())
            .isEqualTo("(((raw_content LIKE ? ESCAPE '\\') AND (raw_content LIKE ? ESCAPE '\\')))");
        assertThat(built.getLikeParams()).containsExactly("%Java%", "%AWS%");
      }

      /**
       * OR の条件で区切った2つの区間を OR で連結することを確かめる。 区間ごとに満たせばよい検索条件を、画面と同じ意味で組み立てるためである。
       *
       * @given 列名式が "raw_content"、検索条件が AND の "Java" と OR の "AWS"
       * @when build を実行する
       * @then 2つの区間を OR で連結した条件式を返し、2件の検索語を返す
       */
      @Test
      void OR条件で区切る場合_区間同士をORで連結した条件式を返す() {
        FulltextConditionsWhereClause.Built built =
            FulltextConditionsWhereClause.build(
                COLUMN, List.of(cond("AND", "Java", false), cond("OR", "AWS", false)));

        assertThat(built.getWhereClauseWithoutWhereKeyword())
            .isEqualTo(
                "(((raw_content LIKE ? ESCAPE '\\')) OR ((raw_content LIKE ? ESCAPE '\\')))");
        assertThat(built.getLikeParams()).containsExactly("%Java%", "%AWS%");
      }

      /**
       * 含まない条件を指定した場合に、列が NULL の行も含むように IS NULL を加えることを確かめる。 LEFT JOIN で列が NULL
       * の行を含まない条件で消さないためである。
       *
       * @given 列名式が "raw_content"、検索条件が AND の否定 "Python" を1件
       * @when build を実行する
       * @then {@code (((raw_content IS NULL OR raw_content NOT LIKE ? ESCAPE '\\')))}
       *     を返し、"%Python%" を値として返す
       */
      @Test
      void 含まない条件の場合_列がNULLの行も含むNOT条件を返す() {
        FulltextConditionsWhereClause.Built built =
            FulltextConditionsWhereClause.build(COLUMN, List.of(cond("AND", "Python", true)));

        assertThat(built.getWhereClauseWithoutWhereKeyword())
            .isEqualTo("(((raw_content IS NULL OR raw_content NOT LIKE ? ESCAPE '\\')))");
        assertThat(built.getLikeParams()).containsExactly("%Python%");
      }

      /**
       * 演算子が AND と OR のどちらでもない条件を AND として連結することを確かめる。 演算子の値が想定外でも区間を分けず、従来どおり AND で扱うためである。
       *
       * @given 列名式が "raw_content"、検索条件が AND の "Java" と演算子 "XOR" の "AWS"
       * @when build を実行する
       * @then 演算子が XOR の条件をANDで連結した条件式を返す
       */
      @Test
      void 演算子がXORの場合_ANDで連結した条件式を返す() {
        FulltextConditionsWhereClause.Built built =
            FulltextConditionsWhereClause.build(
                COLUMN, List.of(cond("AND", "Java", false), cond("XOR", "AWS", false)));

        assertThat(built.getWhereClauseWithoutWhereKeyword())
            .isEqualTo("(((raw_content LIKE ? ESCAPE '\\') AND (raw_content LIKE ? ESCAPE '\\')))");
      }

      /**
       * 演算子が null の条件を AND として連結することを確かめる。 演算子が未設定の条件で区間を分けないためである。
       *
       * @given 列名式が "raw_content"、検索条件が AND の "Java" と演算子が null の "AWS"
       * @when build を実行する
       * @then 2件の条件をANDで連結した条件式を返す
       */
      @Test
      void 演算子がnullの場合_ANDで連結した条件式を返す() {
        FulltextConditionsWhereClause.Built built =
            FulltextConditionsWhereClause.build(
                COLUMN, List.of(cond("AND", "Java", false), cond(null, "AWS", false)));

        assertThat(built.getWhereClauseWithoutWhereKeyword())
            .isEqualTo("(((raw_content LIKE ? ESCAPE '\\') AND (raw_content LIKE ? ESCAPE '\\')))");
      }

      /**
       * 先頭の条件の演算子が OR でも解釈しないことを確かめる。 先頭の演算子は区間の区切りにならないため、ANDとして扱う。
       *
       * @given 列名式が "raw_content"、先頭が演算子 "OR" の "Java"、2件目が AND の "AWS"
       * @when build を実行する
       * @then 2件の条件を1つの区間でANDで連結した条件式を返す
       */
      @Test
      void 先頭の演算子がORの場合_先頭の演算子を無視してANDで連結した条件式を返す() {
        FulltextConditionsWhereClause.Built built =
            FulltextConditionsWhereClause.build(
                COLUMN, List.of(cond("OR", "Java", false), cond("AND", "AWS", false)));

        assertThat(built.getWhereClauseWithoutWhereKeyword())
            .isEqualTo("(((raw_content LIKE ? ESCAPE '\\') AND (raw_content LIKE ? ESCAPE '\\')))");
      }

      /**
       * 検索語の LIKE の特殊文字をエスケープして前後に % を付けることを確かめる。 キーワードの % や _ を部分一致の記号として解釈させないためである。
       *
       * @given 列名式が "raw_content"、検索条件が AND の "a%_\\b"
       * @when build を実行する
       * @then 特殊文字をエスケープした "%a\\%\\_\\\\b%" を値として返す
       */
      @Test
      void 検索語にLIKEの特殊文字を含む場合_エスケープした検索語を返す() {
        FulltextConditionsWhereClause.Built built =
            FulltextConditionsWhereClause.build(COLUMN, List.of(cond("AND", "a%_\\b", false)));

        assertThat(built.getLikeParams()).containsExactly("%a\\%\\_\\\\b%");
      }

      /**
       * 検索語の前後の空白を取り除いて検索することを確かめる。 入力の空白で一致しなくなる検索を防ぐためである。
       *
       * @given 列名式が "raw_content"、検索条件が AND の " Java "
       * @when build を実行する
       * @then "%Java%" を値として返す
       */
      @Test
      void 検索語の前後に空白がある場合_前後の空白を除いた検索語を返す() {
        FulltextConditionsWhereClause.Built built =
            FulltextConditionsWhereClause.build(COLUMN, List.of(cond("AND", " Java ", false)));

        assertThat(built.getLikeParams()).containsExactly("%Java%");
      }

      /**
       * 先頭が null の条件を読み飛ばし、後ろの条件だけで組み立てることを確かめる。 null の要素で WHERE 句が壊れないようにするためである。
       *
       * @given 列名式が "raw_content"、検索条件の先頭が null で2件目が AND の "Java"
       * @when build を実行する
       * @then 1件の含む条件の条件式を返す
       */
      @Test
      void 先頭がnullの場合_後ろの条件だけの条件式を返す() {
        FulltextConditionsWhereClause.Built built =
            FulltextConditionsWhereClause.build(
                COLUMN, Arrays.asList(null, cond("AND", "Java", false)));

        assertThat(built.getWhereClauseWithoutWhereKeyword())
            .isEqualTo("(((raw_content LIKE ? ESCAPE '\\')))");
      }

      /**
       * 先頭が null で次が OR の条件でも、OR の区間を始めることを確かめる。 区間が空のまま区切られて、空の区間が出力されないようにするためである。
       *
       * @given 列名式が "raw_content"、検索条件の先頭が null で2件目が OR の "Java"
       * @when build を実行する
       * @then 1件の含む条件の条件式を返す
       */
      @Test
      void 先頭がnullで次がORの場合_1区間の条件式を返す() {
        FulltextConditionsWhereClause.Built built =
            FulltextConditionsWhereClause.build(
                COLUMN, Arrays.asList(null, cond("OR", "Java", false)));

        assertThat(built.getWhereClauseWithoutWhereKeyword())
            .isEqualTo("(((raw_content LIKE ? ESCAPE '\\')))");
      }

      /**
       * 途中に null の条件があっても読み飛ばして組み立てることを確かめる。 null の要素が1件あっても全体の検索が止まらないようにするためである。
       *
       * @given 列名式が "raw_content"、検索条件が AND の "Java"、null、AND の "AWS"
       * @when build を実行する
       * @then 2件の条件をANDで連結した条件式を返す
       */
      @Test
      void 途中にnullの条件がある場合_2件をANDで連結した条件式を返す() {
        FulltextConditionsWhereClause.Built built =
            FulltextConditionsWhereClause.build(
                COLUMN, Arrays.asList(cond("AND", "Java", false), null, cond("AND", "AWS", false)));

        assertThat(built.getWhereClauseWithoutWhereKeyword())
            .isEqualTo("(((raw_content LIKE ? ESCAPE '\\') AND (raw_content LIKE ? ESCAPE '\\')))");
      }
    }

    @Nested
    class 異常系 {

      /**
       * 列名式が null の場合に例外を投げることを確かめる。 列名が無い条件式は組み立てられないためである。
       *
       * @given 列名式が null、検索条件が AND の "Java" を1件
       * @when build を実行する
       * @then IllegalArgumentException を投げる
       */
      @Test
      void 列名式がnullの場合_IllegalArgumentExceptionを投げる() {
        assertThatThrownBy(
                () ->
                    FulltextConditionsWhereClause.build(null, List.of(cond("AND", "Java", false))))
            .isInstanceOf(IllegalArgumentException.class);
      }

      /**
       * 列名式が空白だけの場合に例外を投げることを確かめる。 空の列名では SQL が壊れるためである。
       *
       * @given 列名式が " "、検索条件が AND の "Java" を1件
       * @when build を実行する
       * @then IllegalArgumentException を投げる
       */
      @Test
      void 列名式が空白だけの場合_IllegalArgumentExceptionを投げる() {
        assertThatThrownBy(
                () ->
                    FulltextConditionsWhereClause.build("  ", List.of(cond("AND", "Java", false))))
            .isInstanceOf(IllegalArgumentException.class);
      }

      /**
       * 列名式に記号を含む場合に例外を投げることを確かめる。 列名は SQL に直接連結されるため、識別子以外を拒否する。
       *
       * @given 列名式が "raw_content;"、検索条件が AND の "Java" を1件
       * @when build を実行する
       * @then IllegalArgumentException を投げる
       */
      @Test
      void 列名式に記号を含む場合_IllegalArgumentExceptionを投げる() {
        assertThatThrownBy(
                () ->
                    FulltextConditionsWhereClause.build(
                        "raw_content;", List.of(cond("AND", "Java", false))))
            .isInstanceOf(IllegalArgumentException.class);
      }

      /**
       * 列名式にドットを2つ含む場合に例外を投げることを確かめる。 テーブル名は1段階までの書き方に限るためである。
       *
       * @given 列名式が "a.b.c"、検索条件が AND の "Java" を1件
       * @when build を実行する
       * @then IllegalArgumentException を投げる
       */
      @Test
      void 列名式にドットを2つ含む場合_IllegalArgumentExceptionを投げる() {
        assertThatThrownBy(
                () ->
                    FulltextConditionsWhereClause.build(
                        "a.b.c", List.of(cond("AND", "Java", false))))
            .isInstanceOf(IllegalArgumentException.class);
      }

      /**
       * 検索条件が null の場合に例外を投げることを確かめる。 検索条件が無い検索は WHERE 句を組み立てないためである。
       *
       * @given 列名式が "raw_content"、検索条件が null
       * @when build を実行する
       * @then IllegalArgumentException を投げる
       */
      @Test
      void 検索条件がnullの場合_IllegalArgumentExceptionを投げる() {
        assertThatThrownBy(() -> FulltextConditionsWhereClause.build(COLUMN, null))
            .isInstanceOf(IllegalArgumentException.class);
      }

      /**
       * 検索可能なキーワードが無い場合に例外を投げることを確かめる。 空白だけの検索語で全行に一致させないためである。
       *
       * @given 列名式が "raw_content"、検索条件が AND の " " を1件
       * @when build を実行する
       * @then IllegalArgumentException を投げる
       */
      @Test
      void 検索可能なキーワードが無い場合_IllegalArgumentExceptionを投げる() {
        assertThatThrownBy(
                () ->
                    FulltextConditionsWhereClause.build(COLUMN, List.of(cond("AND", "  ", false))))
            .isInstanceOf(IllegalArgumentException.class);
      }
    }

    @Nested
    class 境界値 {

      /**
       * 区間の中のキーワードが空白だけの場合に、その区間を読み飛ばすことを確かめる。 空白だけの区間を OR で加えると、全行に一致してしまうためである。
       *
       * @given 列名式が "raw_content"、検索条件が AND の "Java" と、OR の区間に空白の " "
       * @when build を実行する
       * @then 検索可能な区間だけの条件式を返す
       */
      @Test
      void 空白だけの区間がある場合_検索可能な区間だけの条件式を返す() {
        FulltextConditionsWhereClause.Built built =
            FulltextConditionsWhereClause.build(
                COLUMN, List.of(cond("AND", "Java", false), cond("OR", "  ", false)));

        assertThat(built.getWhereClauseWithoutWhereKeyword())
            .isEqualTo("(((raw_content LIKE ? ESCAPE '\\')))");
        assertThat(built.getLikeParams()).containsExactly("%Java%");
      }

      /**
       * 列名式がアンダースコアだけの場合に、検証用の条件式を組み立てることを確かめる。 Backend が列名式を検証の目的で渡すため、この値は許可する必要がある。
       *
       * @given 列名式が "_"、検索条件が AND の "Java" を1件
       * @when build を実行する
       * @then {@code (((_ LIKE ? ESCAPE '\\')))} を返す
       */
      @Test
      void 列名式がアンダースコアだけの場合_検索語を1件含む条件式を返す() {
        FulltextConditionsWhereClause.Built built =
            FulltextConditionsWhereClause.build("_", List.of(cond("AND", "Java", false)));

        assertThat(built.getWhereClauseWithoutWhereKeyword())
            .isEqualTo("(((_ LIKE ? ESCAPE '\\')))");
      }

      /**
       * テーブル名付きの列名式を使う場合に、そのまま条件式に使うことを確かめる。 結合したテーブルの列を指定して検索するためである。
       *
       * @given 列名式が "p.raw_content"、検索条件が AND の "Java" を1件
       * @when build を実行する
       * @then {@code (((p.raw_content LIKE ? ESCAPE '\\')))} を返す
       */
      @Test
      void 列名式にテーブル名を付ける場合_テーブル名付きの条件式を返す() {
        FulltextConditionsWhereClause.Built built =
            FulltextConditionsWhereClause.build(
                "p.raw_content", List.of(cond("AND", "Java", false)));

        assertThat(built.getWhereClauseWithoutWhereKeyword())
            .isEqualTo("(((p.raw_content LIKE ? ESCAPE '\\')))");
      }
    }
  }

  @Nested
  class buildForMultipleColumns {

    @Nested
    class 正常系 {

      /**
       * 含む条件を複数の列に指定した場合に、各列の LIKE を OR で連結することを確かめる。 どの列かに検索語があれば対象にするためである。
       *
       * @given 主カラムが "p.raw_content"、副カラムが "s.file_content_summary"、検索条件が AND の "Java"
       * @when buildForMultipleColumns を実行する
       * @then 2つの列の LIKE を OR で連結した条件式を返し、検索語を列の数だけ返す
       */
      @Test
      void 含む条件を複数の列に指定する場合_各列のLIKEをORで連結した条件式を返す() {
        FulltextConditionsWhereClause.Built built =
            FulltextConditionsWhereClause.buildForMultipleColumns(
                "p.raw_content",
                List.of("s.file_content_summary"),
                List.of(cond("AND", "Java", false)));

        assertThat(built.getWhereClauseWithoutWhereKeyword())
            .isEqualTo(
                "(((p.raw_content LIKE ? ESCAPE '\\' OR s.file_content_summary LIKE ? ESCAPE '\\')))");
        assertThat(built.getLikeParams()).containsExactly("%Java%", "%Java%");
      }

      /**
       * 含まない条件を複数の列に指定した場合に、各列で NULL を含めた否定を AND で連結することを確かめる。 どの列にも検索語が無い行だけを対象にするためである。
       *
       * @given 主カラムが "p.raw_content"、副カラムが "s.file_content_summary"、検索条件が AND の否定 "Python"
       * @when buildForMultipleColumns を実行する
       * @then 2つの列の否定条件を AND で連結した条件式を返し、検索語を列の数だけ返す
       */
      @Test
      void 含まない条件を複数の列に指定する場合_各列のNULLを含む否定をANDで連結した条件式を返す() {
        FulltextConditionsWhereClause.Built built =
            FulltextConditionsWhereClause.buildForMultipleColumns(
                "p.raw_content",
                List.of("s.file_content_summary"),
                List.of(cond("AND", "Python", true)));

        assertThat(built.getWhereClauseWithoutWhereKeyword())
            .isEqualTo(
                "((("
                    + "(p.raw_content IS NULL OR p.raw_content NOT LIKE ? ESCAPE '\\')"
                    + " AND "
                    + "(s.file_content_summary IS NULL OR s.file_content_summary NOT LIKE ? ESCAPE '\\')"
                    + ")))");
        assertThat(built.getLikeParams()).containsExactly("%Python%", "%Python%");
      }

      /**
       * 副カラムが無い場合に、主カラムだけの条件式を組み立てることを確かめる。 副カラムを使わない検索でも同じ組み立てを使うためである。
       *
       * @given 主カラムが "p.raw_content"、副カラムが空のリスト、検索条件が AND の "Java"
       * @when buildForMultipleColumns を実行する
       * @then {@code (((p.raw_content LIKE ? ESCAPE '\\')))} を返す
       */
      @Test
      void 副カラムが無い場合_主カラムだけの条件式を返す() {
        FulltextConditionsWhereClause.Built built =
            FulltextConditionsWhereClause.buildForMultipleColumns(
                "p.raw_content", Collections.emptyList(), List.of(cond("AND", "Java", false)));

        assertThat(built.getWhereClauseWithoutWhereKeyword())
            .isEqualTo("(((p.raw_content LIKE ? ESCAPE '\\')))");
        assertThat(built.getLikeParams()).containsExactly("%Java%");
      }

      /**
       * 含む条件と含まない条件を OR の区間で区切った場合に、区間ごとの条件を OR で連結することを確かめる。 区間ごとに満たせばよい条件を組み立てるためである。
       *
       * @given 主カラムが "p.raw_content"、副カラムが "s.file_content_summary"、検索条件が AND の "Java" と OR の否定
       *     "Python"
       * @when buildForMultipleColumns を実行する
       * @then 2つの区間を OR で連結した条件式を返す
       */
      @Test
      void OR区間で含む条件と含まない条件を組み合わせる場合_区間同士をORで連結した条件式を返す() {
        FulltextConditionsWhereClause.Built built =
            FulltextConditionsWhereClause.buildForMultipleColumns(
                "p.raw_content",
                List.of("s.file_content_summary"),
                List.of(cond("AND", "Java", false), cond("OR", "Python", true)));

        assertThat(built.getWhereClauseWithoutWhereKeyword())
            .isEqualTo(
                "("
                    + "((p.raw_content LIKE ? ESCAPE '\\' OR s.file_content_summary LIKE ? ESCAPE '\\'))"
                    + " OR "
                    + "(((p.raw_content IS NULL OR p.raw_content NOT LIKE ? ESCAPE '\\')"
                    + " AND (s.file_content_summary IS NULL OR s.file_content_summary NOT LIKE ? ESCAPE '\\')))"
                    + ")");
        assertThat(built.getLikeParams())
            .containsExactly("%Java%", "%Java%", "%Python%", "%Python%");
      }
    }

    @Nested
    class 異常系 {

      /**
       * 主カラムが null の場合に例外を投げることを確かめる。 主カラムが無い条件式は組み立てられないためである。
       *
       * @given 主カラムが null、副カラムが空のリスト、検索条件が AND の "Java"
       * @when buildForMultipleColumns を実行する
       * @then IllegalArgumentException を投げる
       */
      @Test
      void 主カラムがnullの場合_IllegalArgumentExceptionを投げる() {
        assertThatThrownBy(
                () ->
                    FulltextConditionsWhereClause.buildForMultipleColumns(
                        null, Collections.emptyList(), List.of(cond("AND", "Java", false))))
            .isInstanceOf(IllegalArgumentException.class);
      }

      /**
       * 副カラムに記号を含む場合に例外を投げることを確かめる。 列名は SQL に直接連結されるため、識別子以外を拒否する。
       *
       * @given 主カラムが "p.raw_content"、副カラムが "s;x"、検索条件が AND の "Java"
       * @when buildForMultipleColumns を実行する
       * @then IllegalArgumentException を投げる
       */
      @Test
      void 副カラムに記号を含む場合_IllegalArgumentExceptionを投げる() {
        assertThatThrownBy(
                () ->
                    FulltextConditionsWhereClause.buildForMultipleColumns(
                        "p.raw_content", List.of("s;x"), List.of(cond("AND", "Java", false))))
            .isInstanceOf(IllegalArgumentException.class);
      }

      /**
       * 検索可能なキーワードが無い場合に例外を投げることを確かめる。 空白だけの検索語で全行に一致させないためである。
       *
       * @given 主カラムが "p.raw_content"、副カラムが空のリスト、検索条件が AND の " "
       * @when buildForMultipleColumns を実行する
       * @then IllegalArgumentException を投げる
       */
      @Test
      void 検索可能なキーワードが無い場合_IllegalArgumentExceptionを投げる() {
        assertThatThrownBy(
                () ->
                    FulltextConditionsWhereClause.buildForMultipleColumns(
                        "p.raw_content",
                        Collections.emptyList(),
                        List.of(cond("AND", "  ", false))))
            .isInstanceOf(IllegalArgumentException.class);
      }
    }
  }
}
