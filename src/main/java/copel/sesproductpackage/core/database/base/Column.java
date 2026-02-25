package copel.sesproductpackage.core.database.base;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 【FW部品】カラムアノテーション
 *
 * <p>このアノテーションは、データベースのカラムに関連する情報をマークアップするために使用されます。 通常、エンティティクラスのフィールドにこのアノテーションを適用し、
 * カラムの物理名、論理名、必須フラグ、主キーフラグなどの情報を提供します。
 *
 * <p>使用例:
 *
 * <p>{@code @Column(physicalName = "user_name", logicalName = "User Name", required = true, primary
 * = true) private String userName; }
 *
 * <p>この例では、"userName" フィールドがデータベースのカラムに関連付けられており、物理名は "user_name"、論理名は "User
 * Name"、必須フラグと主キーフラグが設定されています。
 *
 * @author 鈴木一矢
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Column {
  /**
   * カラムの物理名を指定します。 デフォルト値は空文字列です。
   *
   * @return カラムの物理名
   */
  String physicalName() default "";

  /**
   * カラムの論理名を指定します。 デフォルト値は空文字列です。
   *
   * @return カラムの論理名
   */
  String logicalName() default "";

  /**
   * カラムが必須であるかどうかを指定します。 デフォルト値は false です。
   *
   * @return カラムが必須である場合は true、それ以外の場合は false
   */
  boolean required() default false;

  /**
   * カラムが主キーであるかどうかを指定します。 デフォルト値は false です。
   *
   * @return カラムが主キーである場合は true、それ以外の場合は false
   */
  boolean primary() default false;
}
