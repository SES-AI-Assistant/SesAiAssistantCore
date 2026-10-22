package copel.sesproductpackage.core.unit;

import static copel.sesproductpackage.core.unit.Area.その他;
import static copel.sesproductpackage.core.unit.Area.中国;
import static copel.sesproductpackage.core.unit.Area.九州;
import static copel.sesproductpackage.core.unit.Area.北海道;
import static copel.sesproductpackage.core.unit.Area.北陸;
import static copel.sesproductpackage.core.unit.Area.四国;
import static copel.sesproductpackage.core.unit.Area.東北;
import static copel.sesproductpackage.core.unit.Area.東海;
import static copel.sesproductpackage.core.unit.Area.沖縄;
import static copel.sesproductpackage.core.unit.Area.近畿;
import static copel.sesproductpackage.core.unit.Area.関東_首都圏;
import static copel.sesproductpackage.core.unit.Area.関東_首都圏以外;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** {@link Area} の単体テスト。 */
class AreaTest {

  @Nested
  class コンストラクタ {

    @Nested
    class 正常系 {

      /**
       * 地域の区分の定義順を取得することを確かめる。 地域の定義順が変わると、保存済みの区分と表示順の対応がずれるので、定義順を固定する。
       *
       * @given 地域の区分の列挙値が定義されている
       * @when values を実行する
       * @then 北海道から始まる12件を定義順に返す
       */
      @Test
      void 全ての地域を取得した場合_定義順の12件を返す() {
        assertThat(Area.values())
            .containsExactly(北海道, 東北, 北陸, 関東_首都圏以外, 関東_首都圏, 東海, 近畿, 中国, 四国, 九州, 沖縄, その他);
      }
    }
  }
}
