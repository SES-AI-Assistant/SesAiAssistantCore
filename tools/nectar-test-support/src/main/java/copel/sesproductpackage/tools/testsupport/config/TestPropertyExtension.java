package copel.sesproductpackage.tools.testsupport.config;

import copel.sesproductpackage.core.util.ConfigSource;
import copel.sesproductpackage.core.util.Properties;
import copel.sesproductpackage.tools.testsupport.ExtensionScope;
import copel.sesproductpackage.tools.testsupport.MemoryModeGuard;
import java.lang.reflect.AnnotatedElement;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.support.AnnotationSupport;

/**
 * {@link TestProperty} の注釈に従い、Core の {@link Properties} へ設定値を登録し、後始末で削除する JUnit 拡張です。
 *
 * <p>クラスに付けた注釈は、クラスの開始時に登録し、クラスの終了時に削除する。メソッドに付けた注釈は、テストの前に登録し、テストの後に削除する。
 * 削除は注釈で登録したキーだけを対象にする。{@code @BeforeAll} などで直接登録した設定値は残す。
 *
 * <p>同じキーがクラスとメソッドの両方にある場合は、メソッドの値を使う。メソッドの値を削除した後は、クラスの値を登録し直す。 外側と内側に同じキーがある場合も、同じ考え方で内側の値を使う。
 *
 * <p>登録と削除は Core の {@link Properties} の差し替え口を使う。読み込み元が memory で、かつ直列実行の場合だけ許可されるため、 登録の前に {@link
 * MemoryModeGuard} で判定する。
 *
 * @author Copel Co., Ltd.
 */
public class TestPropertyExtension
    implements BeforeAllCallback, AfterAllCallback, BeforeEachCallback, AfterEachCallback {

  /**
   * クラスの開始時に、クラスに付いた注釈の設定値を登録します。
   *
   * @param context テストの実行コンテキスト
   */
  @Override
  public void beforeAll(final ExtensionContext context) {
    register(context, ownAnnotations(context));
  }

  /**
   * クラスの終了時に、クラスに付いた注釈で登録した設定値を削除します。
   *
   * @param context テストの実行コンテキスト
   */
  @Override
  public void afterAll(final ExtensionContext context) {
    release(context, ownAnnotations(context));
  }

  /**
   * テストの前に、メソッドに付いた注釈の設定値を登録します。
   *
   * <p>注釈が無い場合は何もしない。注釈がある場合は、読み込み元が memory で、かつ直列実行であることを確かめてから登録する。
   *
   * @param context テストの実行コンテキスト
   */
  @Override
  public void beforeEach(final ExtensionContext context) {
    register(context, ownAnnotations(context));
  }

  /**
   * テストの後に、メソッドに付いた注釈で登録した設定値を削除します。
   *
   * <p>注釈が無い場合や、読み込み元が memory でない場合は何もしない。読み込み元が memory でない場合は登録もされていないためである。
   *
   * @param context テストの実行コンテキスト
   */
  @Override
  public void afterEach(final ExtensionContext context) {
    release(context, ownAnnotations(context));
  }

  /**
   * 注釈の設定値を、判定のうえで登録します。
   *
   * <p>注釈が無い場合は何もしない。注釈の順番に登録するため、同じキーは後の注釈が優先される。
   *
   * @param context テストの実行コンテキスト
   * @param properties 登録する注釈の一覧
   */
  private static void register(
      final ExtensionContext context, final List<TestProperty> properties) {
    if (properties.isEmpty()) {
      return;
    }
    MemoryModeGuard.INSTANCE.require("@TestProperty");
    MemoryModeGuard.INSTANCE.requireNotParallel(context, "@TestProperty");
    for (TestProperty property : properties) {
      Properties.putForTest(property.key(), property.value());
    }
  }

  /**
   * 注釈で登録した設定値を削除し、同じキーがクラスの注釈にもあれば、そのクラスの値を登録し直します。
   *
   * <p>削除するのは注釈で登録したキーだけである。外側のクラスの値を残すため、登録し直しを行う。
   *
   * @param context テストの実行コンテキスト
   * @param own 削除対象の注釈の一覧
   */
  private static void release(final ExtensionContext context, final List<TestProperty> own) {
    if (own.isEmpty() || !ConfigSource.INSTANCE.isMemory()) {
      return;
    }
    for (TestProperty property : own) {
      Properties.removeForTest(property.key());
    }
    for (TestProperty enclosing : enclosingAnnotations(context)) {
      if (own.stream().anyMatch(property -> property.key().equals(enclosing.key()))) {
        Properties.putForTest(enclosing.key(), enclosing.value());
      }
    }
  }

  /**
   * 要素に直接付いた {@link TestProperty} の注釈を、付いている順番に返します。
   *
   * @param context テストの実行コンテキスト
   * @return 要素に付いた注釈の一覧。付いていない場合は空の一覧
   */
  private static List<TestProperty> ownAnnotations(final ExtensionContext context) {
    return context
        .getElement()
        .map(element -> AnnotationSupport.findRepeatableAnnotations(element, TestProperty.class))
        .orElseGet(List::of);
  }

  /**
   * 現在の階層より外側のクラスに付いた {@link TestProperty} の注釈を、外側から順に返します。
   *
   * <p>外側から並べることで、後に登録する内側の値が外側の値を上書きする。現在の要素自身の注釈は含めない。
   *
   * @param context テストの実行コンテキスト
   * @return 外側から順に並べた注釈の一覧
   */
  private static List<TestProperty> enclosingAnnotations(final ExtensionContext context) {
    List<AnnotatedElement> innermostFirst = ExtensionScope.INSTANCE.innermostFirst(context);
    AnnotatedElement self = context.getElement().orElse(null);
    List<TestProperty> result = new ArrayList<>();
    for (int i = innermostFirst.size() - 1; i >= 0; i--) {
      AnnotatedElement element = innermostFirst.get(i);
      if (element instanceof Class<?> && !element.equals(self)) {
        result.addAll(AnnotationSupport.findRepeatableAnnotations(element, TestProperty.class));
      }
    }
    return result;
  }
}
