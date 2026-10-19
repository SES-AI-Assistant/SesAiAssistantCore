package copel.sesproductpackage.tools.testsupport.config;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * テストの実行中だけ、設定値を1件登録することを示す注釈です。
 *
 * <p>Core の {@code Properties} に、テスト用の値をメモリ上で登録する。読み込み元が memory のときだけ動く。テストの後には、登録したすべての設定値を初期化する。
 * クラスとメソッドの両方に同じキーがある場合は、メソッドの値を使う。複数指定する場合は、この注釈を繰り返し付ける。 使い方は利用側の README を参照する。
 *
 * @author Copel Co., Ltd.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@Repeatable(TestProperties.class)
@ExtendWith(TestPropertyExtension.class)
public @interface TestProperty {

  /**
   * 登録する設定値のキーを指定します。
   *
   * @return 設定値のキー
   */
  String key();

  /**
   * 登録する設定値を指定します。
   *
   * @return 設定値
   */
  String value();
}
