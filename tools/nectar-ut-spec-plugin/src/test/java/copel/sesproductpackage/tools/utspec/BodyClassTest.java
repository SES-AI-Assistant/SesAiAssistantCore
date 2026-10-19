package copel.sesproductpackage.tools.utspec;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** {@link BodyClass} の単体テスト。 */
class BodyClassTest {

  @Nested
  class fqn {

    @Nested
    class 正常系 {

      /**
       * パッケージ名とクラス名をつないだ完全修飾名を返すことを確かめる。 テストクラスとの1対1の対応付けに使う。
       *
       * @given パッケージが "pkg.sub"、クラス名が "Foo" の本体クラス
       * @when fqn を実行する
       * @then "pkg.sub.Foo" を返す
       */
      @Test
      void パッケージがある場合_完全修飾名を返す() {
        BodyClass body =
            new BodyClass(
                "pkg.sub", "Foo", "src/main/java/pkg/sub/Foo.java", 3, List.of("f"), false);

        assertThat(body.fqn()).isEqualTo("pkg.sub.Foo");
      }
    }

    @Nested
    class 境界値 {

      /**
       * デフォルトパッケージの場合はクラス名だけを返すことを確かめる。 先頭に区切りの文字を付けない。
       *
       * @given パッケージが空、クラス名が "Main" の本体クラス
       * @when fqn を実行する
       * @then "Main" を返す
       */
      @Test
      void パッケージが空の場合_クラス名だけを返す() {
        BodyClass body = new BodyClass("", "Main", "src/main/java/Main.java", 1, List.of(), false);

        assertThat(body.fqn()).isEqualTo("Main");
      }
    }
  }
}
