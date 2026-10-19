package copel.sesproductpackage.tools.testsupport.config;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * {@link TestProperty} を複数回付けるための入れ物の注釈です。利用側が直接書く必要はありません。
 *
 * <p>Java の繰り返し注釈は入れ物の注釈を経由して付くため、拡張の登録を入れ物の側にも付けておく。登録は JUnit で1回にまとめられる。
 *
 * @author Copel Co., Ltd.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@ExtendWith(TestPropertyExtension.class)
public @interface TestProperties {

  /**
   * 繰り返し付けた {@link TestProperty} の一覧を返します。
   *
   * @return {@link TestProperty} の配列
   */
  TestProperty[] value();
}
