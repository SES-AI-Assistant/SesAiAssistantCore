package copel.sesproductpackage.tools.testsupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.AnnotatedElement;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;

/** {@link ExtensionScope} の単体テスト。 */
class ExtensionScopeTest {

  @Nested
  class innermostFirst {

    @Nested
    class 正常系 {

      /**
       * 階層の要素が内側から順に返ることを確かめる。 注釈の優先順位は内側から決まるため、順番が入れ替わる不具合を防ぐ。
       *
       * @given テストメソッドに Integer、外側のクラスに String の要素があり、ルートは要素を持たない
       * @when テストメソッドの階層を指定して innermostFirst を呼び出す
       * @then Integer、String の順番の一覧を返す
       */
      @Test
      void 階層に要素がある場合_内側から順に要素を返す() {
        ExtensionContext root = contextOf(null, null);
        ExtensionContext outer = contextOf(String.class, root);
        ExtensionContext method = contextOf(Integer.class, outer);

        List<AnnotatedElement> elements = ExtensionScope.INSTANCE.innermostFirst(method);

        assertThat(elements).containsExactly(Integer.class, String.class);
      }
    }

    @Nested
    class 境界値 {

      /**
       * 階層が1つだけの場合は、その要素だけを返すことを確かめる。 ルートだけの階層で要素を読み飛ばす不具合を防ぐ。
       *
       * @given 要素が Integer の階層が1つだけ
       * @when その階層を指定して innermostFirst を呼び出す
       * @then Integer だけの一覧を返す
       */
      @Test
      void 階層が1つだけの場合_その要素だけを返す() {
        ExtensionContext only = contextOf(Integer.class, null);

        List<AnnotatedElement> elements = ExtensionScope.INSTANCE.innermostFirst(only);

        assertThat(elements).containsExactly(Integer.class);
      }

      /**
       * 要素を持つ階層が無い場合は、空の一覧を返すことを確かめる。 要素が無いテストで注釈の探索が例外になる不具合を防ぐ。
       *
       * @given 要素を持たない階層が1つだけ
       * @when その階層を指定して innermostFirst を呼び出す
       * @then 空の一覧を返す
       */
      @Test
      void 要素を持つ階層が無い場合_空の一覧を返す() {
        ExtensionContext empty = contextOf(null, null);

        List<AnnotatedElement> elements = ExtensionScope.INSTANCE.innermostFirst(empty);

        assertThat(elements).isEmpty();
      }

      /**
       * 途中の階層が要素を持たない場合は、その階層を除いて返すことを確かめる。 空の階層で一覧に欠けた要素が入る不具合を防ぐ。
       *
       * @given テストメソッドに Integer、途中の階層は要素なし、ルートに String の要素がある
       * @when テストメソッドの階層を指定して innermostFirst を呼び出す
       * @then Integer、String の順番の一覧を返す
       */
      @Test
      void 途中の階層に要素が無い場合_その階層を除いた一覧を返す() {
        ExtensionContext root = contextOf(String.class, null);
        ExtensionContext middle = contextOf(null, root);
        ExtensionContext method = contextOf(Integer.class, middle);

        List<AnnotatedElement> elements = ExtensionScope.INSTANCE.innermostFirst(method);

        assertThat(elements).containsExactly(Integer.class, String.class);
      }
    }
  }

  /**
   * 要素と親の階層を持つ、テスト実行コンテキストのモックを作ります。
   *
   * @param element 階層の要素。null の場合は要素を持たない
   * @param parent 親の階層。null の場合は親を持たない
   * @return テスト実行コンテキストのモック
   */
  private static ExtensionContext contextOf(
      final AnnotatedElement element, final ExtensionContext parent) {
    ExtensionContext context = mock(ExtensionContext.class);
    when(context.getElement()).thenReturn(Optional.ofNullable(element));
    when(context.getParent()).thenReturn(Optional.ofNullable(parent));
    return context;
  }
}
