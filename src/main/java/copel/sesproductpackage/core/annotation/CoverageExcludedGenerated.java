package copel.sesproductpackage.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * カバレッジの集計から除外する要素であることを示すアノテーションです。
 *
 * <p>JaCoCo は、単純名に {@code Generated} を含み保持ポリシーが CLASS または RUNTIME
 * のアノテーションが付いた要素を、カバレッジの集計から自動で除外する。 このアノテーションは、その仕様を利用して除外を示すために使う。
 *
 * <p><b>除外は原則禁止である。</b> 使われていないコードは除外せず削除し、到達できない分岐は削除かリファクタリングで解消すること。やむを得ず除外する場合だけ付け、{@link
 * #reason()} に除外理由を必ず書くこと。除外した要素は UT 仕様書の除外クラス一覧に出力される。
 *
 * <p>現時点では、どの要素にも付けていない。
 *
 * @author Copel Co., Ltd.
 */
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.CONSTRUCTOR})
public @interface CoverageExcludedGenerated {

  /**
   * カバレッジから除外する理由を返します。
   *
   * <p>必ず空でない理由を書く。理由が空の場合は、UT 仕様書の検査でビルドを失敗させる。
   *
   * @return 除外理由
   */
  String reason();
}
