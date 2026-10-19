package copel.sesproductpackage.tools.testsupport.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import copel.sesproductpackage.core.util.ConfigSource;
import copel.sesproductpackage.core.util.Properties;
import java.lang.reflect.AnnotatedElement;
import java.util.Optional;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;

/** {@link TestPropertyExtension} の単体テスト。 */
class TestPropertyExtensionTest {

  /** 外側の階層に設定値を持つ、テスト用の要素。キー NECTAR_TEST_A の値は outer。 */
  @TestProperty(key = "NECTAR_TEST_A", value = "outer")
  static final class 外側の設定値を持つクラス {}

  /** 最も外側の階層に、別のキーの設定値を持つ、テスト用の要素。キー NECTAR_TEST_OTHER の値は other。 */
  @TestProperty(key = "NECTAR_TEST_OTHER", value = "other")
  static final class 別のキーを持つクラス {}

  /** 内側の階層に設定値を2件持つ、テスト用の要素。キー NECTAR_TEST_A の値は method。 */
  @TestProperty(key = "NECTAR_TEST_A", value = "method")
  @TestProperty(key = "NECTAR_TEST_B", value = "2")
  static final class 内側の設定値を持つクラス {}

  @Nested
  class beforeAll {

    @Nested
    class 正常系 {

      /**
       * クラスに設定値の注釈が付いている場合は、クラスの開始時に設定値を登録することを確かめる。 クラス単位の注釈が、そのクラスのすべてのテストで使える仕組みを守る。
       *
       * @given 要素に キー NECTAR_TEST_A の値 outer の注釈が付いている
       * @when beforeAll を要素を持つ階層のコンテキストで呼び出す
       * @then キー NECTAR_TEST_A の値が outer である
       */
      @Test
      void クラスに設定値の注釈が付いている場合_設定値を登録する() {
        Properties.resetForTest();
        TestPropertyExtension extension = new TestPropertyExtension();
        ExtensionContext context = contextOf(外側の設定値を持つクラス.class, null);
        try {
          extension.beforeAll(context);

          assertThat(Properties.get("NECTAR_TEST_A")).isEqualTo("outer");
        } finally {
          extension.afterAll(context);
        }
      }

      /**
       * 注釈が付いていない場合は設定値を登録しないことを確かめる。 注釈の無いクラスに別のクラスの設定値が残る不具合を防ぐ。
       *
       * @given Properties を初期化済みで、要素は注釈を持たない
       * @when beforeAll を要素を持つ階層のコンテキストで呼び出す
       * @then キー NECTAR_TEST_A の値が空のままで、何もしない
       */
      @Test
      void 注釈が付いていない場合_何もしない() {
        Properties.resetForTest();

        new TestPropertyExtension().beforeAll(contextOf(Object.class, null));

        assertThat(Properties.get("NECTAR_TEST_A")).isEmpty();
      }
    }

    @Nested
    class 異常系 {

      /**
       * 読み込み元が memory 以外の場合は IllegalStateException を投げることを確かめる。 本番の設定を書き換える不具合を防ぐ。
       *
       * @given システムプロパティ nectar.config.source が prod、要素に設定値の注釈がある
       * @when beforeAll を要素を持つ階層のコンテキストで呼び出す
       * @then IllegalStateException を投げ、メッセージに nectar.config.source=memory を含む
       */
      @Test
      void 読み込み元がmemory以外の場合_IllegalStateExceptionを投げる() {
        読み込み元を変えて実行する(
            "prod",
            () ->
                assertThatThrownBy(
                        () ->
                            new TestPropertyExtension()
                                .beforeAll(contextOf(外側の設定値を持つクラス.class, null)))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("nectar.config.source=memory"));
      }
    }
  }

  @Nested
  class beforeEach {

    @Nested
    class 正常系 {

      /**
       * メソッドに設定値の注釈が1つ付いたテストで、設定値が登録されることを確かめる。 注釈が JUnit の拡張として登録され、テストの前に Properties へ値が入る仕組みを守る。
       *
       * @given テストメソッドに {@code @TestProperty(key = "NECTAR_TEST_SINGLE", value = "single")} が付いている
       * @when テストの本体で Properties の値を読む
       * @then "single" を返す
       */
      @TestProperty(key = "NECTAR_TEST_SINGLE", value = "single")
      @Test
      void メソッドに設定値の注釈が1つ付いている場合_設定値を登録する() {
        assertThat(Properties.get("NECTAR_TEST_SINGLE")).isEqualTo("single");
      }

      /**
       * メソッドに設定値の注釈が2つ付いたテストで、両方の設定値が登録されることを確かめる。 繰り返しの注釈が入れ物の注釈経由で拡張に届く仕組みを守る。
       *
       * @given テストメソッドに {@code @TestProperty} を2つ付けている
       * @when テストの本体で Properties の値を読む
       * @then キー NECTAR_TEST_C の値が "3"、キー NECTAR_TEST_D の値が "4" である
       */
      @TestProperty(key = "NECTAR_TEST_C", value = "3")
      @TestProperty(key = "NECTAR_TEST_D", value = "4")
      @Test
      void メソッドに設定値の注釈が2つ付いている場合_両方の設定値を登録する() {
        assertThat(Properties.get("NECTAR_TEST_C")).isEqualTo("3");
        assertThat(Properties.get("NECTAR_TEST_D")).isEqualTo("4");
      }

      /**
       * 外側のクラスと内側のメソッドに同じキーがある場合は、内側の値を登録することを確かめる。 メソッドの指定がクラスの指定より優先される仕様を守る。
       *
       * @given クラスの注釈でキー NECTAR_TEST_A が outer、メソッドの注釈でキー NECTAR_TEST_A が method と NECTAR_TEST_B が
       *     2
       * @when beforeAll をクラスの階層で、beforeEach をメソッドの階層で呼び出す
       * @then キー NECTAR_TEST_A の値が method、キー NECTAR_TEST_B の値が 2 である
       */
      @Test
      void 外側と内側に同じキーがある場合_内側の値を登録する() throws Exception {
        Properties.resetForTest();
        TestPropertyExtension extension = new TestPropertyExtension();
        ExtensionContext outer = contextOf(外側の設定値を持つクラス.class, null);
        ExtensionContext inner = contextOf(内側の設定値を持つクラス.class, null);
        extension.beforeAll(outer);
        try {
          extension.beforeEach(inner);

          assertThat(Properties.get("NECTAR_TEST_A")).isEqualTo("method");
          assertThat(Properties.get("NECTAR_TEST_B")).isEqualTo("2");
        } finally {
          extension.afterEach(inner);
          extension.afterAll(outer);
        }
      }

      /**
       * 注釈が付いていない場合は設定値を登録しないことを確かめる。 注釈の無いテストに別のテストの設定値が残る不具合を防ぐ。
       *
       * @given Properties を初期化済みで、要素は注釈を持たない
       * @when beforeEach を要素を持つ階層のコンテキストで呼び出す
       * @then キー NECTAR_TEST_A の値が空のままで、何もしない
       */
      @Test
      void 注釈が付いていない場合_何もしない() {
        Properties.resetForTest();

        new TestPropertyExtension().beforeEach(contextOf(Object.class, null));

        assertThat(Properties.get("NECTAR_TEST_A")).isEmpty();
      }
    }

    @Nested
    class 異常系 {

      /**
       * 読み込み元が memory 以外の場合は IllegalStateException を投げることを確かめる。 本番の設定を書き換える不具合を防ぐ。
       *
       * @given システムプロパティ nectar.config.source が prod、要素に設定値の注釈がある
       * @when beforeEach を内側の階層のコンテキストで呼び出す
       * @then IllegalStateException を投げ、メッセージに nectar.config.source=memory を含む
       */
      @Test
      void 読み込み元がmemory以外の場合_IllegalStateExceptionを投げる() {
        読み込み元を変えて実行する(
            "prod",
            () ->
                assertThatThrownBy(
                        () ->
                            new TestPropertyExtension()
                                .beforeEach(contextOf(内側の設定値を持つクラス.class, null)))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("nectar.config.source=memory"));
      }

      /**
       * 並列実行が有効な場合は IllegalStateException を投げることを確かめる。 設定値の登録と削除が並列のテストと競合する不具合を防ぐ。
       *
       * @given システムプロパティ nectar.config.source が memory、並列実行が有効、要素に設定値の注釈がある
       * @when beforeEach を内側の階層のコンテキストで呼び出す
       * @then IllegalStateException を投げ、メッセージに並列実行を含む
       */
      @Test
      void 並列実行が有効の場合_IllegalStateExceptionを投げる() {
        ExtensionContext context = contextOf(内側の設定値を持つクラス.class, null);
        when(context.getConfigurationParameter("junit.jupiter.execution.parallel.enabled"))
            .thenReturn(Optional.of("true"));

        assertThatThrownBy(() -> new TestPropertyExtension().beforeEach(context))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("並列実行");
      }
    }
  }

  @Nested
  class afterEach {

    @Nested
    class 正常系 {

      /**
       * 注釈で登録した設定値を、テストの後に削除することを確かめる。 設定値がテストの後まで残り、別のテストの結果に影響する不具合を防ぐ。
       *
       * @given 要素にキー NECTAR_TEST_A と NECTAR_TEST_B の注釈があり、beforeEach で登録済み
       * @when afterEach を内側の階層のコンテキストで呼び出す
       * @then キー NECTAR_TEST_A と NECTAR_TEST_B の値が空である
       */
      @Test
      void 注釈が付いている場合_設定値を削除する() {
        Properties.resetForTest();
        TestPropertyExtension extension = new TestPropertyExtension();
        ExtensionContext inner = contextOf(内側の設定値を持つクラス.class, null);
        extension.beforeEach(inner);

        extension.afterEach(inner);

        assertThat(Properties.get("NECTAR_TEST_A")).isEmpty();
        assertThat(Properties.get("NECTAR_TEST_B")).isEmpty();
      }

      /**
       * 同じキーがクラスの注釈にもある場合は、テストの後にクラスの値を登録することを確かめる。 注釈で登録していないキーの値を変えない不具合を防ぐ。
       *
       * @given 外側から順に NECTAR_TEST_OTHER が other、NECTAR_TEST_A が outer の注釈があり、メソッドに NECTAR_TEST_A が
       *     method の注釈がある
       * @when afterEach をメソッドの階層で呼び出す
       * @then キー NECTAR_TEST_A の値が outer、キー NECTAR_TEST_B の値が空、キー NECTAR_TEST_OTHER の値が other である
       */
      @Test
      void 同じキーがクラスにもある場合_クラスの値を登録する() throws Exception {
        Properties.resetForTest();
        TestPropertyExtension extension = new TestPropertyExtension();
        ExtensionContext other = contextOf(別のキーを持つクラス.class, null);
        ExtensionContext outer = contextOf(外側の設定値を持つクラス.class, other);
        ExtensionContext inner = contextOf(内側の設定値を持つクラス.class, outer);
        extension.beforeAll(other);
        extension.beforeAll(outer);
        extension.beforeEach(inner);
        try {
          extension.afterEach(inner);

          assertThat(Properties.get("NECTAR_TEST_A")).isEqualTo("outer");
          assertThat(Properties.get("NECTAR_TEST_B")).isEmpty();
          assertThat(Properties.get("NECTAR_TEST_OTHER")).isEqualTo("other");
        } finally {
          extension.afterAll(outer);
          extension.afterAll(other);
        }
      }

      /**
       * 注釈が付いていない場合は設定値に触れないことを確かめる。 注釈の無いテストが、別のテストで登録した設定値を消す不具合を防ぐ。
       *
       * @given キー NECTAR_TEST_A に "x" を登録済みで、要素は注釈を持たない
       * @when afterEach を要素を持つ階層のコンテキストで呼び出す
       * @then キー NECTAR_TEST_A の値が "x" のままで、何もしない
       */
      @Test
      void 注釈が付いていない場合_何もしない() {
        Properties.putForTest("NECTAR_TEST_A", "x");
        try {
          new TestPropertyExtension().afterEach(contextOf(Object.class, null));

          assertThat(Properties.get("NECTAR_TEST_A")).isEqualTo("x");
        } finally {
          Properties.resetForTest();
        }
      }

      /**
       * 注釈で登録していないキーは、テストの後も削除しないことを確かめる。 直接登録した設定値を消して、後続のテストの前提を壊す不具合を防ぐ。
       *
       * @given キー NECTAR_DIRECT に "kept" を直接登録済みで、要素に設定値の注釈がある
       * @when afterEach を内側の階層のコンテキストで呼び出す
       * @then キー NECTAR_DIRECT の値が "kept" のままで、何もしない
       */
      @Test
      void 注釈で登録していないキーの場合_何もしない() {
        Properties.resetForTest();
        Properties.putForTest("NECTAR_DIRECT", "kept");
        try {
          new TestPropertyExtension().afterEach(contextOf(内側の設定値を持つクラス.class, null));

          assertThat(Properties.get("NECTAR_DIRECT")).isEqualTo("kept");
        } finally {
          Properties.resetForTest();
        }
      }
    }

    @Nested
    class 境界値 {

      /**
       * 読み込み元が memory 以外の場合は、設定値を削除しないことを確かめる。 memory 以外では設定値を登録していないため、削除の操作で例外を投げる不具合を防ぐ。
       *
       * @given キー NECTAR_TEST_A に "x" を登録済みで、システムプロパティ nectar.config.source が prod、要素に設定値の注釈がある
       * @when afterEach を内側の階層のコンテキストで呼び出す
       * @then 例外を投げず、何もしない
       */
      @Test
      void 読み込み元がmemory以外の場合_何もしない() {
        Properties.putForTest("NECTAR_TEST_A", "x");
        try {
          読み込み元を変えて実行する(
              "prod",
              () ->
                  assertThatCode(
                          () ->
                              new TestPropertyExtension()
                                  .afterEach(contextOf(内側の設定値を持つクラス.class, null)))
                      .doesNotThrowAnyException());

          assertThat(Properties.get("NECTAR_TEST_A")).isEqualTo("x");
        } finally {
          Properties.resetForTest();
        }
      }
    }
  }

  @Nested
  class afterAll {

    @Nested
    class 正常系 {

      /**
       * クラスの注釈で登録した設定値を、クラスの終了時に削除することを確かめる。 クラス単位の設定値が後続のクラスに残る不具合を防ぐ。
       *
       * @given beforeAll でキー NECTAR_TEST_A の値 outer を登録済みで、要素にクラスの注釈がある
       * @when afterAll を要素を持つ階層のコンテキストで呼び出す
       * @then キー NECTAR_TEST_A の値が空である
       */
      @Test
      void クラスに設定値の注釈が付いている場合_設定値を削除する() throws Exception {
        Properties.resetForTest();
        TestPropertyExtension extension = new TestPropertyExtension();
        ExtensionContext outer = contextOf(外側の設定値を持つクラス.class, null);
        extension.beforeAll(outer);

        extension.afterAll(outer);

        assertThat(Properties.get("NECTAR_TEST_A")).isEmpty();
      }

      /**
       * BeforeAll 相当で直接登録した設定値は、クラスの終了時にも残ることを確かめる。 注釈で登録していない設定値を拡張が消す不具合を防ぐ。
       *
       * @given キー NECTAR_DIRECT に "kept" を直接登録済みで、要素にクラスの注釈がある
       * @when afterAll を要素を持つ階層のコンテキストで呼び出す
       * @then キー NECTAR_DIRECT の値が "kept" のままで、何もしない
       */
      @Test
      void 注釈で登録していないキーの場合_何もしない() throws Exception {
        Properties.resetForTest();
        Properties.putForTest("NECTAR_DIRECT", "kept");
        try {
          new TestPropertyExtension().afterAll(contextOf(外側の設定値を持つクラス.class, null));

          assertThat(Properties.get("NECTAR_DIRECT")).isEqualTo("kept");
        } finally {
          Properties.resetForTest();
        }
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

  /**
   * システムプロパティ nectar.config.source を指定した値に変えて処理を実行し、最後に memory へ戻します。
   *
   * @param value 一時的に設定する値
   * @param action 実行する処理
   */
  private static void 読み込み元を変えて実行する(final String value, final Runnable action) {
    System.setProperty(ConfigSource.CONFIG_SOURCE_PROPERTY, value);
    try {
      action.run();
    } finally {
      System.setProperty(ConfigSource.CONFIG_SOURCE_PROPERTY, ConfigSource.CONFIG_SOURCE_MEMORY);
    }
  }
}
