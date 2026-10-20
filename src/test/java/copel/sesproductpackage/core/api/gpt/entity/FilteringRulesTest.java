package copel.sesproductpackage.core.api.gpt.entity;

import static org.assertj.core.api.Assertions.assertThat;

import copel.sesproductpackage.core.api.gpt.entity.MatchEvaluateResponseSchema.EvaluateType;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** {@link FilteringRules} の単体テスト。 */
class FilteringRulesTest {

  @Nested
  class createDefault {

    @Nested
    class 正常系 {

      /**
       * 既定のルールが必須と尚好の両方に完全一致を要求することを確かめる。 AI の検索結果を絞り込む既定の条件が変わると候補の件数が意図せず増減するので、既定値を守る。
       *
       * @given 既定のルールを作成する操作が存在する
       * @when createDefault を呼び出す
       * @then 必須と尚好のスキル要求レベルが FullyMet で、その他の制約の検証が必須である
       */
      @Test
      void 既定のルールを作成した場合_全ての条件を必須にしたルールを返す() {
        FilteringRules rules = FilteringRules.createDefault();

        assertThat(rules.getMustSkillLevel()).isEqualTo(EvaluateType.FullyMet);
        assertThat(rules.getWantSkillLevel()).isEqualTo(EvaluateType.FullyMet);
        assertThat(rules.isOtherConstraintsRequired()).isTrue();
      }
    }
  }
}
