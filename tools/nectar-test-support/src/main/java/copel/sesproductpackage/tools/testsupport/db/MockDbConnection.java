package copel.sesproductpackage.tools.testsupport.db;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * テストの実行中、Core の DB 接続が Mockito のモック接続を返すことを示す注釈です。
 *
 * <p>テストの前に、モック接続を返す接続生成関数を Core の {@code DBConnection} に設定する。テストの後には接続生成関数を解除する。 テストメソッドの引数に
 * {@link java.sql.Connection} を宣言すると、同じモック接続を受け取れる。 使い方は利用側の README を参照する。
 *
 * @author Copel Co., Ltd.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@ExtendWith(MockDbConnectionExtension.class)
public @interface MockDbConnection {}
