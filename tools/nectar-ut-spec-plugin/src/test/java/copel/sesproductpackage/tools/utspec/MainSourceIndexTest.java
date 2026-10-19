package copel.sesproductpackage.tools.utspec;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** {@link MainSourceIndex} の単体テスト。 */
class MainSourceIndexTest {

  @Nested
  class コンストラクタ {

    @Nested
    class 正常系 {

      /**
       * 本体クラスの対応と除外の一覧を、そのまま保持することを確かめる。 後続の検査が同じ索引を参照できるようにする。
       *
       * @given 本体クラス1件と、除外1件を渡して生成する
       * @when bodies と exclusions を取り出す
       * @then 渡した本体クラスと除外の一覧を返す
       */
      @Test
      void 本体と除外が設定されている場合_両方を返す() {
        BodyClass body =
            new BodyClass("pkg", "A", "src/main/java/pkg/A.java", 1, List.of("f"), true);
        MainSourceIndex index =
            new MainSourceIndex(
                Map.of("pkg.A", body),
                List.of(new MainSourceIndex.ExclusionEntry("pkg.A", "生成")),
                List.of());

        assertThat(index.bodies()).containsEntry("pkg.A", body);
        assertThat(index.exclusions()).hasSize(1);
        assertThat(index.exclusions().get(0).reason()).isEqualTo("生成");
        assertThat(index.violations()).isEmpty();
      }
    }
  }
}
