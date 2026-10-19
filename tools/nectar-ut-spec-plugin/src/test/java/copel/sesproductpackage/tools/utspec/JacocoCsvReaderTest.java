package copel.sesproductpackage.tools.utspec;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** {@link JacocoCsvReader} の単体テスト。 */
class JacocoCsvReaderTest {

  /** 一時ディレクトリ。CSV を書き出すために使う。 */
  @TempDir Path tempDir;

  /** ヘッダーの行。JaCoCo の CSV の列の並びと同じにする。 */
  private static final String HEADER =
      "GROUP,PACKAGE,CLASS,INSTRUCTION_MISSED,INSTRUCTION_COVERED,BRANCH_MISSED,BRANCH_COVERED";

  /**
   * CSV を一時ディレクトリに書き出し、そのパスを返す。
   *
   * @param lines CSV の各行
   * @return 書き出した CSV のパス
   * @throws IOException 書き出しに失敗した場合
   */
  private Path writeCsv(String... lines) throws IOException {
    Path csv = tempDir.resolve("jacoco.csv");
    Files.write(csv, String.join("\n", lines).getBytes(java.nio.charset.StandardCharsets.UTF_8));
    return csv;
  }

  @Nested
  class read {

    @Nested
    class 正常系 {

      /**
       * 内部クラスを外側のクラスに寄せて、数えるコードを持つクラスを集計することを確かめる。 1対1 の判定を外側のクラス単位で行う。
       *
       * @given 外側のクラスと内部クラスの行、命令が1つもないインターフェースの行を含む CSV
       * @when read を実行する
       * @then 外側のクラスだけを数えるコードを持つクラスとして返し、命令と分岐の合計を返す
       */
      @Test
      void 内部クラスを含む場合_外側のクラスに寄せて集計した結果を返す() throws IOException {
        Path csv =
            writeCsv(
                HEADER, "g,pkg,Outer,1,3,0,2", "g,pkg,Outer.Inner,0,2,1,1", "g,pkg,Iface,0,0,0,0");

        JacocoSummary summary = new JacocoCsvReader().read(csv);

        assertThat(summary.countedClasses()).containsExactly("pkg.Outer");
        assertThat(summary.instructionCovered()).isEqualTo(5);
        assertThat(summary.instructionMissed()).isEqualTo(1);
        assertThat(summary.branchCovered()).isEqualTo(3);
        assertThat(summary.branchMissed()).isEqualTo(1);
      }

      /**
       * 空行を読み飛ばして集計することを確かめる。 CSV の末尾の空行で異常終了しないようにする。
       *
       * @given 1行のクラスの後ろに空行が続く CSV
       * @when read を実行する
       * @then 1つのクラスだけを数えるコードを持つクラスとして返す
       */
      @Test
      void 末尾に空行がある場合_空行を読み飛ばした結果を返す() throws IOException {
        Path csv = writeCsv(HEADER, "", "g,pkg,Only,0,4,0,0");

        JacocoSummary summary = new JacocoCsvReader().read(csv);

        assertThat(summary.countedClasses()).containsExactly("pkg.Only");
      }
    }

    @Nested
    class 異常系 {

      /**
       * jacoco.csv が無い場合は再実行を促す例外を投げることを確かめる。 古いレポートで検査を進めないようにする。
       *
       * @given 存在しないパスの jacoco.csv
       * @when read を実行する
       * @then IllegalStateException を投げる
       */
      @Test
      void CSVファイルが存在しない場合_IllegalStateExceptionを投げる() {
        Path missing = tempDir.resolve("none.csv");

        assertThatThrownBy(() -> new JacocoCsvReader().read(missing))
            .isInstanceOf(IllegalStateException.class);
      }
    }

    @Nested
    class 境界値 {

      /**
       * 中身が空のファイルは空の集計結果を返すことを確かめる。 テストの無いリポジトリでも検査を落とさずに進める。
       *
       * @given 中身が空の jacoco.csv
       * @when read を実行する
       * @then 数えるコードを持つクラスが空の集計結果を返す
       */
      @Test
      void 中身が空のCSVの場合_空の集計結果を返す() throws IOException {
        Path csv = writeCsv();

        JacocoSummary summary = new JacocoCsvReader().read(csv);

        assertThat(summary.countedClasses()).isEmpty();
        assertThat(summary.instructionCovered()).isZero();
      }
    }
  }

  @Nested
  class outerName {

    @Nested
    class 正常系 {

      /**
       * ドットで区切られた内部クラス名から外側のクラスの完全修飾名を求めることを確かめる。 内部クラスの行を外側のクラスに寄せる。
       *
       * @given パッケージが "pkg.sub"、クラス名が "Outer.Inner"
       * @when outerName を実行する
       * @then "pkg.sub.Outer" を返す
       */
      @Test
      void クラス名がドットの内部クラスの場合_外側のクラスの完全修飾名を返す() {
        assertThat(new JacocoCsvReader().outerName("pkg.sub", "Outer.Inner"))
            .isEqualTo("pkg.sub.Outer");
      }

      /**
       * ドル記号で区切られた内部クラス名からも外側のクラス名を求めることを確かめる。 ドル記号の区切りの内部クラスも外側へ寄せる。
       *
       * @given パッケージが "pkg"、クラス名が "Outer$1"
       * @when outerName を実行する
       * @then "pkg.Outer" を返す
       */
      @Test
      void クラス名がドル記号の内部クラスの場合_外側のクラスの完全修飾名を返す() {
        assertThat(new JacocoCsvReader().outerName("pkg", "Outer$1")).isEqualTo("pkg.Outer");
      }
    }

    @Nested
    class 境界値 {

      /**
       * デフォルトパッケージのクラスはクラス名だけを返すことを確かめる。 パッケージが空のときに先頭へ余分な区切りを付けない。
       *
       * @given パッケージが空、クラス名が "Main"
       * @when outerName を実行する
       * @then "Main" を返す
       */
      @Test
      void パッケージが空の場合_クラス名だけを返す() {
        assertThat(new JacocoCsvReader().outerName("", "Main")).isEqualTo("Main");
      }
    }
  }
}
