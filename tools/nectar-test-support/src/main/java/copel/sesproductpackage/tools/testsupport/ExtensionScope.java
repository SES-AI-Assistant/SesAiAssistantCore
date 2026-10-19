package copel.sesproductpackage.tools.testsupport;

import java.lang.reflect.AnnotatedElement;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * テストの実行階層をたどり、各階層の注釈対象を内側から順に集める、単一インスタンスの列挙型。
 *
 * <p>JUnit の拡張は、テストメソッド、そのテストを囲む {@code @Nested} クラス、外側のテストクラスの順に階層を持つ。注釈は
 * メソッドに付いたものを優先するため、階層を内側から順にたどれるようにする。
 *
 * @author Copel Co., Ltd.
 */
public enum ExtensionScope {
  /** アプリケーション全体で共有する唯一のインスタンスです。利用は {@code ExtensionScope.INSTANCE} で行います。 */
  INSTANCE;

  /**
   * テストの実行階層を内側から順に、注釈を付けられる要素の一覧で返します。
   *
   * <p>階層の要素を持たない階層（ルートの階層）は一覧に含めない。戻り値は新しい一覧であり、呼び出し側が変更しても階層の情報には影響しない。
   *
   * @param context テストの実行コンテキスト
   * @return 内側から順に並べた要素の一覧。階層が無い場合は空の一覧
   */
  public List<AnnotatedElement> innermostFirst(final ExtensionContext context) {
    List<AnnotatedElement> elements = new ArrayList<>();
    ExtensionContext current = context;
    while (current != null) {
      current.getElement().ifPresent(elements::add);
      current = current.getParent().orElse(null);
    }
    return elements;
  }
}
