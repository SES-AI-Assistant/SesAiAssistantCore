package copel.sesproductpackage.tools.testsupport.clock;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * テストの実行中、アプリケーションの現在時刻を指定した時刻に固定することを示す注釈です。
 *
 * <p>クラスに付けるとそのクラスのすべてのテストに、メソッドに付けるとそのメソッドだけに適用する。両方に付いている場合はメソッドの指定を優先する。 テストの後には、現在時刻の取得元を既定に戻す。
 * 使い方は利用側の README を参照する。
 *
 * @author Copel Co., Ltd.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@ExtendWith(FixedClockExtension.class)
public @interface FixedClock {

  /**
   * 固定する時刻を、オフセット付きの ISO-8601 形式で指定します。例: "2026-01-15T12:04:05+09:00"
   *
   * @return 固定する時刻
   */
  String value();
}
