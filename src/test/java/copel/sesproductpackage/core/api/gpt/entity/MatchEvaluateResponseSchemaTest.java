package copel.sesproductpackage.core.api.gpt.entity;

import static org.assertj.core.api.Assertions.assertThat;

import copel.sesproductpackage.core.api.gpt.entity.MatchEvaluateResponseSchema.EvaluateType;
import copel.sesproductpackage.core.api.gpt.entity.MatchEvaluateResponseSchema.OtherEvaluateResult;
import copel.sesproductpackage.core.api.gpt.entity.MatchEvaluateResponseSchema.SkillEvaluateResult;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** {@link MatchEvaluateResponseSchema} の単体テスト。 */
class MatchEvaluateResponseSchemaTest {

  /** 評価文の切り詰めを確かめるための項目名の文字数。 制限の2000文字を超えて切り詰めが起きるように2100文字にする。 */
  private static final int LONG_PERSPECTIVE_LENGTH = 2100;

  /** 評価文の文字数の上限。 DB の評価文カラムの上限に合わせる。 */
  private static final int TEXT_LIMIT = 2000;

  @Nested
  class isMatch {

    @Nested
    class 正常系 {

      /**
       * 必須が全て FullyMet の場合に、既定のルールでマッチすると判定することを確かめる。 既定のルールは必須を FullyMet とする仕様である。
       *
       * @given 必須の評価が FullyMet の2件、尚可と その他の一覧が未設定
       * @when isMatch を引数なしで実行する
       * @then trueを返す
       */
      @Test
      void 必須が全てFullyMetの場合_trueを返す() {
        MatchEvaluateResponseSchema schema =
            schema(List.of(skill(EvaluateType.FullyMet), skill(EvaluateType.FullyMet)), null, null);

        assertThat(schema.isMatch()).isTrue();
      }
    }

    @Nested
    class 境界値 {

      /**
       * 必須に PartiallyMet が1件でもある場合に、既定のルールではマッチしないと判定することを確かめる。 既定の必須のしきい値は FullyMet
       * であり、類似経験だけの項目は対象外とする仕様を守る。
       *
       * @given 必須の評価が FullyMet と PartiallyMet の2件
       * @when isMatch を引数なしで実行する
       * @then falseを返す
       */
      @Test
      void 必須にPartiallyMetがある場合_falseを返す() {
        MatchEvaluateResponseSchema schema =
            schema(
                List.of(skill(EvaluateType.FullyMet), skill(EvaluateType.PartiallyMet)),
                null,
                null);

        assertThat(schema.isMatch()).isFalse();
      }
    }
  }

  @Nested
  class isMatch_FilteringRules {

    @Nested
    class 正常系 {

      /**
       * 必須のしきい値が FullyMet で必須が全て FullyMet の場合に、マッチすると判定することを確かめる。 しきい値を満たす必須の評価を通す仕様である。
       *
       * @given 必須のしきい値が FullyMet、必須の評価が FullyMet の1件、尚可のしきい値が null
       * @when isMatch をルールで実行する
       * @then trueを返す
       */
      @Test
      void 必須のしきい値がFullyMetで必須が全てFullyMetの場合_trueを返す() {
        MatchEvaluateResponseSchema schema =
            schema(List.of(skill(EvaluateType.FullyMet)), null, null);

        assertThat(schema.isMatch(rules(EvaluateType.FullyMet, null))).isTrue();
      }

      /**
       * 必須のしきい値が PartiallyMet で必須に PartiallyMet を含む場合に、マッチすると判定することを確かめる。 テナント設定で必須に類似経験を許容する仕様を守る。
       *
       * @given 必須のしきい値が PartiallyMet、必須の評価が FullyMet と PartiallyMet の2件、尚可のしきい値が null
       * @when isMatch をルールで実行する
       * @then trueを返す
       */
      @Test
      void 必須のしきい値がPartiallyMetで必須にPartiallyMetを含む場合_trueを返す() {
        MatchEvaluateResponseSchema schema =
            schema(
                List.of(skill(EvaluateType.FullyMet), skill(EvaluateType.PartiallyMet)),
                null,
                null);

        assertThat(schema.isMatch(rules(EvaluateType.PartiallyMet, null))).isTrue();
      }

      /**
       * 尚可のしきい値が FullyMet で尚可が全て FullyMet の場合に、マッチすると判定することを確かめる。 尚可もしきい値を満たす場合だけ通す仕様である。
       *
       * @given 必須のしきい値が FullyMet、必須の評価が FullyMet の1件、尚可のしきい値が FullyMet、尚可の評価が FullyMet の1件
       * @when isMatch をルールで実行する
       * @then trueを返す
       */
      @Test
      void 尚可のしきい値がFullyMetで尚可が全てFullyMetの場合_trueを返す() {
        MatchEvaluateResponseSchema schema =
            schema(
                List.of(skill(EvaluateType.FullyMet)), List.of(skill(EvaluateType.FullyMet)), null);

        assertThat(schema.isMatch(rules(EvaluateType.FullyMet, EvaluateType.FullyMet))).isTrue();
      }

      /**
       * 尚可のしきい値が PartiallyMet で尚可が PartiallyMet の場合に、マッチすると判定することを確かめる。 しきい値と同じ水準の尚可の評価を通す仕様である。
       *
       * @given 必須のしきい値が FullyMet、必須の評価が FullyMet の1件、尚可のしきい値が PartiallyMet、尚可の評価が PartiallyMet の1件
       * @when isMatch をルールで実行する
       * @then trueを返す
       */
      @Test
      void 尚可のしきい値がPartiallyMetで尚可がPartiallyMetの場合_trueを返す() {
        MatchEvaluateResponseSchema schema =
            schema(
                List.of(skill(EvaluateType.FullyMet)),
                List.of(skill(EvaluateType.PartiallyMet)),
                null);

        assertThat(schema.isMatch(rules(EvaluateType.FullyMet, EvaluateType.PartiallyMet)))
            .isTrue();
      }

      /**
       * 必須のリストが空または未設定の場合に、必須の判定を通すことを確かめる。 案件に必須の記載が無い場合は必須の不足にしない仕様を守る。
       *
       * @given 必須の一覧が空リスト、尚可のしきい値が null
       * @when isMatch をルールで実行する
       * @then trueを返す
       */
      @Test
      void 必須のリストが空の場合_trueを返す() {
        MatchEvaluateResponseSchema schema = schema(List.of(), null, null);

        assertThat(schema.isMatch(rules(EvaluateType.FullyMet, null))).isTrue();
      }

      /**
       * 必須のリストが未設定の場合に、必須の判定を通すことを確かめる。 必須の記載が無い案件を未充足として除外しない仕様を守る。
       *
       * @given 必須の一覧が null、尚可のしきい値が null
       * @when isMatch をルールで実行する
       * @then trueを返す
       */
      @Test
      void 必須のリストが未設定の場合_trueを返す() {
        assertThat(schema(null, null, null).isMatch(rules(EvaluateType.FullyMet, null))).isTrue();
      }

      /**
       * 必須に満たさない項目を含むが必須のしきい値が null の場合に、必須を判定せずマッチすると判定することを確かめる。 テナント設定で必須を制限なしとした場合の仕様を守る。
       *
       * @given 必須のしきい値が null、必須の評価が NotMet の1件、尚可のしきい値が null
       * @when isMatch をルールで実行する
       * @then trueを返す
       */
      @Test
      void 必須のしきい値がnullの場合_必須を判定せずtrueを返す() {
        MatchEvaluateResponseSchema schema =
            schema(List.of(skill(EvaluateType.NotMet)), null, null);

        assertThat(schema.isMatch(rules(null, null))).isTrue();
      }

      /**
       * ルールが null の場合に、既定のルールで必須を FullyMet として判定することを確かめる。 引数なしの判定と同じ結果になる仕様を守る。
       *
       * @given 必須の評価が FullyMet の1件、尚可の一覧が未設定
       * @when isMatch を null のルールで実行する
       * @then trueを返す
       */
      @Test
      void ルールがnullで必須が全てFullyMetの場合_trueを返す() {
        MatchEvaluateResponseSchema schema =
            schema(List.of(skill(EvaluateType.FullyMet)), null, null);

        assertThat(schema.isMatch(null)).isTrue();
      }

      /**
       * その他の評価が満たさない項目を含んでも、必須と尚可が満たせばマッチすると判定することを確かめる。 その他は判定に使わずスコアと評価文にのみ反映する仕様である。
       *
       * @given 必須の評価が FullyMet の1件、尚可の評価が FullyMet の1件、その他の評価が NotMet の1件、必須と尚可のしきい値が FullyMet
       * @when isMatch をルールで実行する
       * @then trueを返す
       */
      @Test
      void その他に満たさない項目がある場合_trueを返す() {
        MatchEvaluateResponseSchema schema =
            schema(
                List.of(skill(EvaluateType.FullyMet)),
                List.of(skill(EvaluateType.FullyMet)),
                List.of(other(EvaluateType.NotMet)));

        assertThat(schema.isMatch(rules(EvaluateType.FullyMet, EvaluateType.FullyMet))).isTrue();
      }
    }

    @Nested
    class 境界値 {

      /**
       * 必須のしきい値が FullyMet で必須に PartiallyMet を含む場合に、マッチしないと判定することを確かめる。 類似経験だけの必須を既定では通さない仕様を守る。
       *
       * @given 必須のしきい値が FullyMet、必須の評価が FullyMet と PartiallyMet の2件、尚可のしきい値が null
       * @when isMatch をルールで実行する
       * @then falseを返す
       */
      @Test
      void 必須のしきい値がFullyMetで必須にPartiallyMetを含む場合_falseを返す() {
        MatchEvaluateResponseSchema schema =
            schema(
                List.of(skill(EvaluateType.FullyMet), skill(EvaluateType.PartiallyMet)),
                null,
                null);

        assertThat(schema.isMatch(rules(EvaluateType.FullyMet, null))).isFalse();
      }

      /**
       * 必須のしきい値が PartiallyMet で必須に NotMet を含む場合に、マッチしないと判定することを確かめる。 同じ案件で未充足の必須を通さない仕様を守る。
       *
       * @given 必須のしきい値が PartiallyMet、必須の評価が FullyMet と NotMet の2件、尚可のしきい値が null
       * @when isMatch をルールで実行する
       * @then falseを返す
       */
      @Test
      void 必須のしきい値がPartiallyMetで必須にNotMetを含む場合_falseを返す() {
        MatchEvaluateResponseSchema schema =
            schema(List.of(skill(EvaluateType.FullyMet), skill(EvaluateType.NotMet)), null, null);

        assertThat(schema.isMatch(rules(EvaluateType.PartiallyMet, null))).isFalse();
      }

      /**
       * 必須に評価の値が未設定の項目がある場合に、マッチしないと判定することを確かめる。 判定できない必須を充足として扱わない仕様を守る。
       *
       * @given 必須のしきい値が FullyMet、必須の評価の値が null の1件、尚可のしきい値が null
       * @when isMatch をルールで実行する
       * @then falseを返す
       */
      @Test
      void 必須の評価の値が未設定の場合_falseを返す() {
        MatchEvaluateResponseSchema schema =
            schema(List.of(new SkillEvaluateResult("時系列データ分析", null, null)), null, null);

        assertThat(schema.isMatch(rules(EvaluateType.FullyMet, null))).isFalse();
      }

      /**
       * 必須に Unknown の項目を含む場合に、マッチしないと判定することを確かめる。 不明な必須を充足として扱わない仕様を守る。
       *
       * @given 必須のしきい値が PartiallyMet、必須の評価が Unknown の1件、尚可のしきい値が null
       * @when isMatch をルールで実行する
       * @then falseを返す
       */
      @Test
      void 必須にUnknownを含む場合_falseを返す() {
        MatchEvaluateResponseSchema schema =
            schema(List.of(skill(EvaluateType.Unknown)), null, null);

        assertThat(schema.isMatch(rules(EvaluateType.PartiallyMet, null))).isFalse();
      }

      /**
       * ルールが null で必須に PartiallyMet を含む場合に、既定の FullyMet のしきい値でマッチしないと判定することを確かめる。
       * 引数なしの判定と同じ結果になる仕様を守る。
       *
       * @given 必須の評価が FullyMet と PartiallyMet の2件、尚可の一覧が未設定
       * @when isMatch を null のルールで実行する
       * @then falseを返す
       */
      @Test
      void ルールがnullで必須にPartiallyMetを含む場合_falseを返す() {
        MatchEvaluateResponseSchema schema =
            schema(
                List.of(skill(EvaluateType.FullyMet), skill(EvaluateType.PartiallyMet)),
                null,
                null);

        assertThat(schema.isMatch(null)).isFalse();
      }

      /**
       * 尚可のしきい値が FullyMet で尚可に PartiallyMet を含む場合に、マッチしないと判定することを確かめる。
       * 尚可を制限する設定のテナントで、一部の類似経験だけの候補を通さない仕様を守る。
       *
       * @given 必須のしきい値が FullyMet、必須の評価が FullyMet の1件、尚可のしきい値が FullyMet、尚可の評価が FullyMet と
       *     PartiallyMet の2件
       * @when isMatch をルールで実行する
       * @then falseを返す
       */
      @Test
      void 尚可のしきい値がFullyMetで尚可にPartiallyMetを含む場合_falseを返す() {
        MatchEvaluateResponseSchema schema =
            schema(
                List.of(skill(EvaluateType.FullyMet)),
                List.of(skill(EvaluateType.FullyMet), skill(EvaluateType.PartiallyMet)),
                null);

        assertThat(schema.isMatch(rules(EvaluateType.FullyMet, EvaluateType.FullyMet))).isFalse();
      }

      /**
       * 尚可のしきい値が PartiallyMet で尚可に NotMet を含む場合に、マッチしないと判定することを確かめる。 しきい値を満たさない尚可の候補を通さない仕様を守る。
       *
       * @given 必須のしきい値が FullyMet、必須の評価が FullyMet の1件、尚可のしきい値が PartiallyMet、尚可の評価が PartiallyMet と
       *     NotMet の2件
       * @when isMatch をルールで実行する
       * @then falseを返す
       */
      @Test
      void 尚可のしきい値がPartiallyMetで尚可にNotMetを含む場合_falseを返す() {
        MatchEvaluateResponseSchema schema =
            schema(
                List.of(skill(EvaluateType.FullyMet)),
                List.of(skill(EvaluateType.PartiallyMet), skill(EvaluateType.NotMet)),
                null);

        assertThat(schema.isMatch(rules(EvaluateType.FullyMet, EvaluateType.PartiallyMet)))
            .isFalse();
      }

      /**
       * 尚可のしきい値が null の場合に、尚可に満たさない項目があっても判定しないことを確かめる。 尚可を制限しないテナントの候補を除外しない仕様を守る。
       *
       * @given 必須のしきい値が FullyMet、必須の評価が FullyMet の1件、尚可のしきい値が null、尚可の評価が NotMet の1件
       * @when isMatch をルールで実行する
       * @then trueを返す
       */
      @Test
      void 尚可のしきい値がnullの場合_尚可を判定せずtrueを返す() {
        MatchEvaluateResponseSchema schema =
            schema(
                List.of(skill(EvaluateType.FullyMet)), List.of(skill(EvaluateType.NotMet)), null);

        assertThat(schema.isMatch(rules(EvaluateType.FullyMet, null))).isTrue();
      }

      /**
       * 尚可のしきい値が設定され尚可の一覧が未設定の場合に、尚可の判定を通すことを確かめる。 案件に尚可の記載が無い場合は尚可の不足にしない仕様を守る。
       *
       * @given 必須のしきい値が FullyMet、必須の評価が FullyMet の1件、尚可のしきい値が FullyMet、尚可の一覧が null
       * @when isMatch をルールで実行する
       * @then trueを返す
       */
      @Test
      void 尚可のリストが未設定の場合_trueを返す() {
        MatchEvaluateResponseSchema schema =
            schema(List.of(skill(EvaluateType.FullyMet)), null, null);

        assertThat(schema.isMatch(rules(EvaluateType.FullyMet, EvaluateType.FullyMet))).isTrue();
      }
    }
  }

  @Nested
  class getMatchScore {

    @Nested
    class 正常系 {

      /**
       * 全ての評価が FullyMet の場合に、配点の合計の100点を返すことを確かめる。 配点の合計が100点になる仕様を守る。
       *
       * @given 必須、尚可、その他の評価が全て FullyMet の1件ずつ
       * @when getMatchScore を実行する
       * @then 100を返す
       */
      @Test
      void 全ての評価がFullyMetの場合_100を返す() {
        MatchEvaluateResponseSchema schema =
            schema(
                List.of(skill(EvaluateType.FullyMet)),
                List.of(skill(EvaluateType.FullyMet)),
                List.of(other(EvaluateType.FullyMet)));

        assertThat(schema.getMatchScore()).isEqualTo(100);
      }

      /**
       * 必須が一部のみ満たす場合に、必須の配点に達率を掛けた点数を合計することを確かめる。 必須の達率は配点の重みで点数に換算する仕様である。
       *
       * @given 必須が FullyMet と PartiallyMet の2件、尚可が NotMet の1件、その他が FullyMet の1件
       * @when getMatchScore を実行する
       * @then 57 に 0.7 を掛けた値と 14 を合計した 53.9 を四捨五入して 54 を返す
       */
      @Test
      void 必須が一部のみ満たし尚可を満たさない場合_54を返す() {
        MatchEvaluateResponseSchema schema =
            schema(
                List.of(skill(EvaluateType.FullyMet), skill(EvaluateType.PartiallyMet)),
                List.of(skill(EvaluateType.NotMet)),
                List.of(other(EvaluateType.FullyMet)));

        assertThat(schema.getMatchScore()).isEqualTo(54);
      }

      /**
       * 合計が小数になる場合に、四捨五入した点数を返すことを確かめる。 点数は整数で表示する仕様である。
       *
       * @given 必須が PartiallyMet の1件、尚可と その他の一覧が未設定
       * @when getMatchScore を実行する
       * @then 57 に 0.4 を掛けた値に尚可の29点とその他の14点を加えた 65.8 を四捨五入して 66 を返す
       */
      @Test
      void 合計が小数の場合_四捨五入して66を返す() {
        MatchEvaluateResponseSchema schema =
            schema(List.of(skill(EvaluateType.PartiallyMet)), null, null);

        assertThat(schema.getMatchScore()).isEqualTo(66);
      }

      /**
       * 評価の値が未設定の項目を、達率0として点数に換算することを確かめる。 判定できない項目を加点しない仕様を守る。
       *
       * @given 必須の評価の値が null の1件、尚可と その他の一覧が未設定
       * @when getMatchScore を実行する
       * @then 必須を 0 点とし、尚可の29点とその他の14点を合計して 43 を返す
       */
      @Test
      void 評価の値が未設定の項目がある場合_その項目を0点として43を返す() {
        MatchEvaluateResponseSchema schema =
            schema(List.of(new SkillEvaluateResult("時系列データ分析", null, null)), null, null);

        assertThat(schema.getMatchScore()).isEqualTo(43);
      }
    }

    @Nested
    class 境界値 {

      /**
       * 全ての評価が最低の場合に、点数が0になることを確かめる。 全ての評価が満たされない案件の点数を0に保つ仕様を守る。
       *
       * @given 必須が NotMet、尚可が Unknown、その他が NotMet の1件ずつ
       * @when getMatchScore を実行する
       * @then 0 を返す
       */
      @Test
      void 全ての評価が最低の場合_0を返す() {
        MatchEvaluateResponseSchema schema =
            schema(
                List.of(skill(EvaluateType.NotMet)),
                List.of(skill(EvaluateType.Unknown)),
                List.of(other(EvaluateType.NotMet)));

        assertThat(schema.getMatchScore()).isEqualTo(0);
      }

      /**
       * 全ての一覧が未設定の場合に、各配点の満点の合計を返すことを確かめる。 記載が無い項目は減点しない仕様を守る。
       *
       * @given 必須、尚可、その他の一覧がいずれも null
       * @when getMatchScore を実行する
       * @then 100を返す
       */
      @Test
      void 一覧が全て未設定の場合_満点の100を返す() {
        assertThat(schema(null, null, null).getMatchScore()).isEqualTo(100);
      }

      /**
       * 全ての一覧が空の場合に、各配点の満点の合計を返すことを確かめる。 空の一覧は未設定と同じく記載が無い扱いにする仕様を守る。
       *
       * @given 必須、尚可、その他の一覧がいずれも空リスト
       * @when getMatchScore を実行する
       * @then 100を返す
       */
      @Test
      void 一覧が全て空の場合_満点の100を返す() {
        MatchEvaluateResponseSchema schema = schema(List.of(), List.of(), List.of());

        assertThat(schema.getMatchScore()).isEqualTo(100);
      }

      /**
       * 必須の一覧だけが未設定の場合に、必須の満点57点だけが残ることを確かめる。 尚可とその他が満たさないときの点数の内訳を守る。
       *
       * @given 必須の一覧が null、尚可が NotMet の1件、その他が NotMet の1件
       * @when getMatchScore を実行する
       * @then 必須の57を返す
       */
      @Test
      void 必須の一覧が未設定で尚可とその他を満たさない場合_必須の57を返す() {
        MatchEvaluateResponseSchema schema =
            schema(null, List.of(skill(EvaluateType.NotMet)), List.of(other(EvaluateType.NotMet)));

        assertThat(schema.getMatchScore()).isEqualTo(57);
      }
    }
  }

  @Nested
  class getMustEvaluationText {

    @Nested
    class 正常系 {

      /**
       * 必須の評価がある場合に、見出しと評価の行を含む文字列を返すことを確かめる。 評価の根拠を画面と通知の文章に残す仕様である。
       *
       * @given 必須の評価が "時系列データ分析"、FullyMet、根拠 "根拠A" の1件
       * @when getMustEvaluationText を実行する
       * @then 必須の見出しと項目の行を含む文字列を返す
       */
      @Test
      void 必須の評価がある場合_見出しと評価の行を含む文字列を返す() {
        MatchEvaluateResponseSchema schema =
            schema(
                List.of(new SkillEvaluateResult("時系列データ分析", EvaluateType.FullyMet, "根拠A")),
                null,
                null);

        assertThat(schema.getMustEvaluationText()).isEqualTo("■必須\n・時系列データ分析：◎(根拠A)\n");
      }
    }

    @Nested
    class 境界値 {

      /**
       * 必須の一覧が未設定の場合に、空文字を返すことを確かめる。 必須の記載が無い案件は見出しも出さない。
       *
       * @given 必須の一覧が null
       * @when getMustEvaluationText を実行する
       * @then 空文字を返す
       */
      @Test
      void 必須の一覧が未設定の場合_空文字を返す() {
        assertThat(schema(null, null, null).getMustEvaluationText()).isEmpty();
      }

      /**
       * 必須の一覧が空の場合に、空文字を返すことを確かめる。 未設定と同じく見出しを出さない仕様を守る。
       *
       * @given 必須の一覧が空リスト
       * @when getMustEvaluationText を実行する
       * @then 空文字を返す
       */
      @Test
      void 必須の一覧が空の場合_空文字を返す() {
        assertThat(schema(List.of(), null, null).getMustEvaluationText()).isEmpty();
      }

      /**
       * 必須の評価の値が未設定の場合に、記号の代わりにハイフンを表示することを確かめる。 判定の無い項目を満たすと誤認させない仕様を守る。
       *
       * @given 必須の評価の値が null、項目 "時系列データ分析"、根拠が null の1件
       * @when getMustEvaluationText を実行する
       * @then 項目名とハイフンの行を含む文字列を返す
       */
      @Test
      void 必須の評価の値が未設定の場合_ハイフンを表示した文字列を返す() {
        MatchEvaluateResponseSchema schema =
            schema(List.of(new SkillEvaluateResult("時系列データ分析", null, null)), null, null);

        assertThat(schema.getMustEvaluationText()).isEqualTo("■必須\n・時系列データ分析：-\n");
      }

      /**
       * 必須の評価が NotMet の場合に、不一致の記号を表示することを確かめる。 充足の項目と未充足の項目を記号で区別する仕様を守る。
       *
       * @given 必須の評価が "時系列データ分析"、NotMet、根拠 "根拠B" の1件
       * @when getMustEvaluationText を実行する
       * @then 必須の見出しと項目の行を含む文字列を返す
       */
      @Test
      void 必須の評価がNotMetの場合_不一致の記号を表示した文字列を返す() {
        MatchEvaluateResponseSchema schema =
            schema(
                List.of(new SkillEvaluateResult("時系列データ分析", EvaluateType.NotMet, "根拠B")),
                null,
                null);

        assertThat(schema.getMustEvaluationText()).isEqualTo("■必須\n・時系列データ分析：×(根拠B)\n");
      }
    }
  }

  @Nested
  class getWantEvaluationText {

    @Nested
    class 正常系 {

      /**
       * 尚可の評価がある場合に、見出しと評価の行を含む文字列を返すことを確かめる。 尚可の根拠を画面と通知の文章に残す仕様である。
       *
       * @given 尚可の評価が "Java"、PartiallyMet、根拠 "類似" の1件
       * @when getWantEvaluationText を実行する
       * @then 尚可の見出しと項目の行を含む文字列を返す
       */
      @Test
      void 尚可の評価がある場合_見出しと評価の行を含む文字列を返す() {
        MatchEvaluateResponseSchema schema =
            schema(
                null,
                List.of(new SkillEvaluateResult("Java", EvaluateType.PartiallyMet, "類似")),
                null);

        assertThat(schema.getWantEvaluationText()).isEqualTo("■尚可\n・Java：〇(類似)\n");
      }
    }

    @Nested
    class 境界値 {

      /**
       * 尚可の一覧が未設定の場合に、空文字を返すことを確かめる。 尚可の記載が無い案件は見出しも出さない。
       *
       * @given 尚可の一覧が null
       * @when getWantEvaluationText を実行する
       * @then 空文字を返す
       */
      @Test
      void 尚可の一覧が未設定の場合_空文字を返す() {
        assertThat(schema(null, null, null).getWantEvaluationText()).isEmpty();
      }

      /**
       * 尚可の一覧が空の場合に、空文字を返すことを確かめる。 未設定と同じく見出しを出さない仕様を守る。
       *
       * @given 尚可の一覧が空リスト
       * @when getWantEvaluationText を実行する
       * @then 空文字を返す
       */
      @Test
      void 尚可の一覧が空の場合_空文字を返す() {
        assertThat(schema(null, List.of(), null).getWantEvaluationText()).isEmpty();
      }

      /**
       * 尚可の評価の値が未設定の場合に、記号の代わりにハイフンを表示することを確かめる。 判定の無い項目を満たすと誤認させない仕様を守る。
       *
       * @given 尚可の評価の値が null、項目 "Java"、根拠が null の1件
       * @when getWantEvaluationText を実行する
       * @then 項目名とハイフンの行を含む文字列を返す
       */
      @Test
      void 尚可の評価の値が未設定の場合_ハイフンを表示した文字列を返す() {
        MatchEvaluateResponseSchema schema =
            schema(null, List.of(new SkillEvaluateResult("Java", null, null)), null);

        assertThat(schema.getWantEvaluationText()).isEqualTo("■尚可\n・Java：-\n");
      }

      /**
       * 尚可の評価が Unknown の場合に、判定不明の記号を表示することを確かめる。 判定できない項目を満たすと誤認させない仕様を守る。
       *
       * @given 尚可の評価が "Java"、Unknown、根拠 "類似" の1件
       * @when getWantEvaluationText を実行する
       * @then 尚可の見出しと項目の行を含む文字列を返す
       */
      @Test
      void 尚可の評価がUnknownの場合_判定不明の記号を表示した文字列を返す() {
        MatchEvaluateResponseSchema schema =
            schema(
                null, List.of(new SkillEvaluateResult("Java", EvaluateType.Unknown, "類似")), null);

        assertThat(schema.getWantEvaluationText()).isEqualTo("■尚可\n・Java：-(類似)\n");
      }
    }
  }

  @Nested
  class getOtherEvaluationText {

    @Nested
    class 正常系 {

      /**
       * その他の評価がある場合に、見出しと評価の行をコメント付きで返すことを確かめる。 年齢や商流の条件の根拠を画面と通知の文章に残す仕様である。
       *
       * @given その他の評価が "成長環境"、FullyMet、コメント "裁量あり" の1件
       * @when getOtherEvaluationText を実行する
       * @then その他の見出しと項目の行を含む文字列を返す
       */
      @Test
      void その他の評価がある場合_見出しと評価の行を含む文字列を返す() {
        MatchEvaluateResponseSchema schema =
            schema(
                null,
                null,
                List.of(new OtherEvaluateResult("成長環境", EvaluateType.FullyMet, "裁量あり")));

        assertThat(schema.getOtherEvaluationText()).isEqualTo("■その他：\n・成長環境：◎（裁量あり）\n");
      }
    }

    @Nested
    class 境界値 {

      /**
       * コメントが無いその他の評価の場合に、記号だけを表示することを確かめる。 コメントが無くても判定の結果は表示する仕様を守る。
       *
       * @given その他の評価が "成長環境"、FullyMet、コメントが null の1件
       * @when getOtherEvaluationText を実行する
       * @then 項目名と記号の行だけを含む文字列を返す
       */
      @Test
      void コメントが無い場合_記号だけを表示した文字列を返す() {
        MatchEvaluateResponseSchema schema =
            schema(
                null, null, List.of(new OtherEvaluateResult("成長環境", EvaluateType.FullyMet, null)));

        assertThat(schema.getOtherEvaluationText()).isEqualTo("■その他：\n・成長環境：◎\n");
      }

      /**
       * その他の一覧が未設定の場合に、空文字を返すことを確かめる。 その他の記載が無い案件は見出しも出さない。
       *
       * @given その他の一覧が null
       * @when getOtherEvaluationText を実行する
       * @then 空文字を返す
       */
      @Test
      void その他の一覧が未設定の場合_空文字を返す() {
        assertThat(schema(null, null, null).getOtherEvaluationText()).isEmpty();
      }

      /**
       * その他の評価の値が未設定の場合に、記号の代わりにハイフンを表示することを確かめる。 判定の無い項目を満たすと誤認させない仕様を守る。
       *
       * @given その他の評価の値が null、項目 "成長環境"、コメント "裁量あり" の1件
       * @when getOtherEvaluationText を実行する
       * @then 項目名とハイフンとコメントの行を含む文字列を返す
       */
      @Test
      void その他の評価の値が未設定の場合_ハイフンを表示した文字列を返す() {
        MatchEvaluateResponseSchema schema =
            schema(null, null, List.of(new OtherEvaluateResult("成長環境", null, "裁量あり")));

        assertThat(schema.getOtherEvaluationText()).isEqualTo("■その他：\n・成長環境：-（裁量あり）\n");
      }

      /**
       * その他の一覧が空の場合に、空文字を返すことを確かめる。 未設定と同じくその他の見出しを出さない仕様を守る。
       *
       * @given その他の一覧が空リスト
       * @when getOtherEvaluationText を実行する
       * @then 空文字を返す
       */
      @Test
      void その他の一覧が空の場合_空文字を返す() {
        assertThat(schema(null, null, List.of()).getOtherEvaluationText()).isEmpty();
      }
    }
  }

  @Nested
  class toNotificationSummary {

    @Nested
    class 正常系 {

      /**
       * 必須の全てが FullyMet の場合に、全一致を示す1行を返すことを確かめる。 通知で必須が全て一致したことを伝える仕様である。
       *
       * @given 必須の評価が FullyMet の2件
       * @when toNotificationSummary を実行する
       * @then 必須の項目と全一致の記号の1行を返す
       */
      @Test
      void 必須の全てがFullyMetの場合_全一致を示す1行を返す() {
        MatchEvaluateResponseSchema schema =
            schema(List.of(skill(EvaluateType.FullyMet), skill(EvaluateType.FullyMet)), null, null);

        assertThat(schema.toNotificationSummary()).isEqualTo("必須◎");
      }
    }

    @Nested
    class 境界値 {

      /**
       * 必須に一部のみ満たす項目がある場合に、全一致ではないことを示す1行を返すことを確かめる。 全てが一致しない場合は詳細を画面で確認させる仕様である。
       *
       * @given 必須の評価が FullyMet と PartiallyMet の2件
       * @when toNotificationSummary を実行する
       * @then 必須の項目と全一致ではない記号の1行を返す
       */
      @Test
      void 必須に一部のみ満たす項目がある場合_全一致ではないことを示す1行を返す() {
        MatchEvaluateResponseSchema schema =
            schema(
                List.of(skill(EvaluateType.FullyMet), skill(EvaluateType.PartiallyMet)),
                null,
                null);

        assertThat(schema.toNotificationSummary()).isEqualTo("必須〇");
      }

      /**
       * 必須の評価の値が未設定の項目がある場合に、全一致ではないことを示す1行を返すことを確かめる。 判定の無い項目を満たすと誤認させない仕様を守る。
       *
       * @given 必須の評価の値が null の1件
       * @when toNotificationSummary を実行する
       * @then 必須の項目と全一致ではない記号の1行を返す
       */
      @Test
      void 必須の評価の値が未設定の場合_全一致ではないことを示す1行を返す() {
        MatchEvaluateResponseSchema schema =
            schema(List.of(new SkillEvaluateResult("時系列データ分析", null, null)), null, null);

        assertThat(schema.toNotificationSummary()).isEqualTo("必須〇");
      }

      /**
       * 必須の一覧が未設定の場合に、空文字を返すことを確かめる。 必須が評価されていない場合は通知に項目を出さない仕様である。
       *
       * @given 必須の一覧が null
       * @when toNotificationSummary を実行する
       * @then 空文字を返す
       */
      @Test
      void 必須の一覧が未設定の場合_空文字を返す() {
        assertThat(schema(null, null, null).toNotificationSummary()).isEmpty();
      }

      /**
       * 必須の一覧が空の場合に、空文字を返すことを確かめる。 未設定と同じく通知に項目を出さない仕様を守る。
       *
       * @given 必須の一覧が空リスト
       * @when toNotificationSummary を実行する
       * @then 空文字を返す
       */
      @Test
      void 必須の一覧が空の場合_空文字を返す() {
        assertThat(schema(List.of(), null, null).toNotificationSummary()).isEmpty();
      }
    }
  }

  @Nested
  class toEvaluiationText {

    @Nested
    class 正常系 {

      /**
       * 全ての評価がある場合に、マッチ度と各評価を改行で並べた評価文を返すことを確かめる。 マッチング詳細テーブルの評価文に判定の根拠を残す仕様である。
       *
       * @given 必須 "時系列データ分析" が FullyMet、尚可 "Java" が PartiallyMet、その他 "成長環境" が FullyMet の各1件
       * @when toEvaluiationText を実行する
       * @then 必須の57に1.0を掛けた値と尚可の29に0.4を掛けた値と、その他の14を合計した82.6を四捨五入した83点のマッチ度と各評価の行を、末尾の改行を除いて返す
       */
      @Test
      void 全ての評価がある場合_マッチ度と各評価を改行で並べた評価文を返す() {
        MatchEvaluateResponseSchema schema =
            schema(
                List.of(new SkillEvaluateResult("時系列データ分析", EvaluateType.FullyMet, "根拠A")),
                List.of(new SkillEvaluateResult("Java", EvaluateType.PartiallyMet, "類似")),
                List.of(new OtherEvaluateResult("成長環境", EvaluateType.FullyMet, "裁量あり")));

        assertThat(schema.toEvaluiationText())
            .isEqualTo(
                "マッチ度：83点\n■必須\n・時系列データ分析：◎(根拠A)\n■尚可\n・Java：〇(類似)\n" + "■その他：\n・成長環境：◎（裁量あり）");
      }
    }

    @Nested
    class 境界値 {

      /**
       * 全ての評価の一覧が未設定の場合に、マッチ度の満点だけの評価文を返すことを確かめる。 記載の無い項目を減点せず、マッチ度を満点で示す仕様を守る。
       *
       * @given 必須、尚可、その他の一覧がいずれも null
       * @when toEvaluiationText を実行する
       * @then マッチ度の100点だけを返す
       */
      @Test
      void 評価の一覧が全て未設定の場合_マッチ度の100点だけを返す() {
        assertThat(schema(null, null, null).toEvaluiationText()).isEqualTo("マッチ度：100点");
      }

      /**
       * 評価文が2000文字を超える場合に、先頭の2000文字に切り詰めることを確かめる。 評価文のカラムの文字数上限を超えないように切り詰める仕様である。
       *
       * @given その他の評価の項目が 2100 文字、評価が FullyMet、コメントが null の1件
       * @when toEvaluiationText を実行する
       * @then 先頭がマッチ度の100点の行で、2000文字の文字列を返す
       */
      @Test
      void 評価文が2000文字を超える場合_2000文字に切り詰めた文字列を返す() {
        String longPerspective = "あ".repeat(LONG_PERSPECTIVE_LENGTH);
        MatchEvaluateResponseSchema schema =
            schema(
                null,
                null,
                List.of(new OtherEvaluateResult(longPerspective, EvaluateType.FullyMet, null)));

        String result = schema.toEvaluiationText();

        assertThat(result).hasSize(TEXT_LIMIT).startsWith("マッチ度：100点");
      }
    }
  }

  /**
   * 必須と尚可のしきい値を指定したフィルタリングルールを生成する。その他制約は判定に使わないため false にする。
   *
   * @param mustLevel 必須のしきい値。null も可
   * @param wantLevel 尚可のしきい値。null も可
   * @return 生成したフィルタリングルール
   */
  private static FilteringRules rules(final EvaluateType mustLevel, final EvaluateType wantLevel) {
    return new FilteringRules(mustLevel, wantLevel, false);
  }

  /**
   * 指定の一覧を持つマッチ評価結果を生成する。
   *
   * @param mustList 必須スキルの評価一覧。null も可
   * @param wantList 尚可スキルの評価一覧。null も可
   * @param otherList その他の評価一覧。null も可
   * @return 生成したマッチ評価結果
   */
  private static MatchEvaluateResponseSchema schema(
      final List<SkillEvaluateResult> mustList,
      final List<SkillEvaluateResult> wantList,
      final List<OtherEvaluateResult> otherList) {
    return new MatchEvaluateResponseSchema(mustList, wantList, otherList);
  }

  /**
   * 評価区分だけを指定したスキル評価を生成する。項目名と根拠は固定の文字列にする。
   *
   * @param result 評価区分。null も可
   * @return 生成したスキル評価
   */
  private static SkillEvaluateResult skill(final EvaluateType result) {
    return new SkillEvaluateResult("評価項目", result, "根拠");
  }

  /**
   * 評価区分だけを指定したその他の評価を生成する。項目名と根拠は固定の文字列にする。
   *
   * @param result 評価区分。null も可
   * @return 生成したその他の評価
   */
  private static OtherEvaluateResult other(final EvaluateType result) {
    return new OtherEvaluateResult("成長環境", result, "裁量あり");
  }
}
