package copel.sesproductpackage.core.api.gpt.entity;

import static org.assertj.core.api.Assertions.assertThat;

import copel.sesproductpackage.core.api.gpt.entity.MatchEvaluateResponseSchema.EvaluateType;
import copel.sesproductpackage.core.api.gpt.entity.MatchEvaluateResponseSchema.OfficeEvaluateResult;
import copel.sesproductpackage.core.api.gpt.entity.MatchEvaluateResponseSchema.OtherEvaluateResult;
import copel.sesproductpackage.core.api.gpt.entity.MatchEvaluateResponseSchema.PersonMonthsEvaluateResult;
import copel.sesproductpackage.core.api.gpt.entity.MatchEvaluateResponseSchema.PlaceEvaluateResult;
import copel.sesproductpackage.core.api.gpt.entity.MatchEvaluateResponseSchema.PriceEvaluateResult;
import copel.sesproductpackage.core.api.gpt.entity.MatchEvaluateResponseSchema.SkillEvaluateResult;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** {@link MatchEvaluateResponseSchema} の単体テスト。 */
class MatchEvaluateResponseSchemaTest {

  @Nested
  class isMatch {

    @Nested
    class 正常系 {

      /**
       * 全ての必須項目を満たす場合に、マッチと判定することを確かめる。 マッチの判定は通知の可否に直結するため、満たした項目だけで true になることを守る。
       *
       * @given 必須が全て FullyMet、単価と出社要件を満たし、場所と人月工数は未評価である
       * @when isMatch を呼び出す
       * @then true を返す
       */
      @Test
      void 全ての必須項目を満たす場合_trueを返す() {
        assertThat(matchingSchema().isMatch()).isTrue();
      }

      /**
       * 人月工数を満たす場合に、マッチと判定することを確かめる。 人月工数の条件を満たす候補を除外しないことを守る。
       *
       * @given 必須、単価、出社要件が満たされ、人月工数の評価が true である
       * @when isMatch を呼び出す
       * @then true を返す
       */
      @Test
      void 人月工数を満たす場合_trueを返す() throws Exception {
        MatchEvaluateResponseSchema schema = matchingSchema();
        schema.setPersonMonthsResult(new PersonMonthsEvaluateResult(true, ""));

        assertThat(schema.isMatch()).isTrue();
      }
    }

    @Nested
    class 異常系 {

      /**
       * 必須スキルの一覧が未設定の場合に、マッチではないと判定することを確かめる。 評価が無い候補を採用すると、根拠の無い提案になることを守る。
       *
       * @given 必須スキルの一覧が未設定で、他の項目は全て満たす
       * @when isMatch を呼び出す
       * @then false を返す
       */
      @Test
      void 必須スキルの一覧が未設定の場合_falseを返す() {
        MatchEvaluateResponseSchema schema = matchingSchema();
        schema.setMustList(null);

        assertThat(schema.isMatch()).isFalse();
      }

      /**
       * 単価の評価が未設定の場合に、マッチではないと判定することを確かめる。 単価を評価していない候補を採用すると、単価の条件を確かめないまま提案することを守る。
       *
       * @given 単価の評価が未設定で、他の項目は全て満たす
       * @when isMatch を呼び出す
       * @then false を返す
       */
      @Test
      void 単価の評価が未設定の場合_falseを返す() {
        MatchEvaluateResponseSchema schema = matchingSchema();
        schema.setPriceResult(null);

        assertThat(schema.isMatch()).isFalse();
      }

      /**
       * 出社要件の評価が未設定の場合に、マッチではないと判定することを確かめる。 出社要件を評価していない候補を採用すると、出社の条件を確かめないまま提案することを守る。
       *
       * @given 出社要件の評価が未設定で、他の項目は全て満たす
       * @when isMatch を呼び出す
       * @then false を返す
       */
      @Test
      void 出社要件の評価が未設定の場合_falseを返す() {
        MatchEvaluateResponseSchema schema = matchingSchema();
        schema.setOfficeResult(null);

        assertThat(schema.isMatch()).isFalse();
      }
    }

    @Nested
    class 境界値 {

      /**
       * 必須スキルが1件でも完全に満たさない場合に、マッチではないと判定することを確かめる。 必須スキルは1件でも欠けると採用できないため、部分一致で通さないことを守る。
       *
       * @given 必須の1件が PartiallyMet で、他の項目は全て満たす
       * @when isMatch を呼び出す
       * @then false を返す
       */
      @Test
      void 必須スキルに1件でも満たさない項目がある場合_falseを返す() {
        MatchEvaluateResponseSchema schema = matchingSchema();
        schema.setMustList(List.of(skill(EvaluateType.PartiallyMet)));

        assertThat(schema.isMatch()).isFalse();
      }

      /**
       * 必須スキルの一覧が空の場合に、必須の判定を満たしたものとして扱うことを確かめる。 案件に必須スキルの記載が無い場合は、全て FullyMet とする仕様を守る。
       *
       * @given 必須スキルの一覧が空で、他の項目は全て満たす
       * @when isMatch を呼び出す
       * @then true を返す
       */
      @Test
      void 必須スキルの一覧が空の場合_trueを返す() {
        MatchEvaluateResponseSchema schema = matchingSchema();
        schema.setMustList(List.of());

        assertThat(schema.isMatch()).isTrue();
      }

      /**
       * 単価を満たさない場合に、マッチではないと判定することを確かめる。 単価の条件は要員が希望する条件のため、満たさない候補を通さないことを守る。
       *
       * @given 単価の評価が false で、他の項目は全て満たす
       * @when isMatch を呼び出す
       * @then false を返す
       */
      @Test
      void 単価を満たさない場合_falseを返す() {
        MatchEvaluateResponseSchema schema = matchingSchema();
        schema.setPriceResult(new PriceEvaluateResult(false, ""));

        assertThat(schema.isMatch()).isFalse();
      }

      /**
       * 出社要件が完全に満たされない場合に、マッチではないと判定することを確かめる。 出社要件は案件の必須条件のため、部分的な一致では通さないことを守る。
       *
       * @given 出社要件の評価が PartiallyMet で、他の項目は全て満たす
       * @when isMatch を呼び出す
       * @then false を返す
       */
      @Test
      void 出社要件が完全に満たされない場合_falseを返す() {
        MatchEvaluateResponseSchema schema = matchingSchema();
        schema.setOfficeResult(new OfficeEvaluateResult(EvaluateType.PartiallyMet, ""));

        assertThat(schema.isMatch()).isFalse();
      }

      /**
       * 場所が NotMet の場合に、マッチではないと判定することを確かめる。 場所は NotMet で除外する仕様のため、その判定を守る。
       *
       * @given 場所の評価が NotMet で、他の項目は全て満たす
       * @when isMatch を呼び出す
       * @then false を返す
       */
      @Test
      void 場所がNotMetの場合_falseを返す() {
        MatchEvaluateResponseSchema schema = matchingSchema();
        schema.setPlaceResult(new PlaceEvaluateResult(EvaluateType.NotMet, ""));

        assertThat(schema.isMatch()).isFalse();
      }

      /**
       * 場所が PartiallyMet の場合に、場所を理由として除外しないことを確かめる。 場所は NotMet の時だけ除外するため、部分的な一致を除外しないことを守る。
       *
       * @given 場所の評価が PartiallyMet で、他の項目は全て満たす
       * @when isMatch を呼び出す
       * @then true を返す
       */
      @Test
      void 場所がPartiallyMetの場合_trueを返す() {
        MatchEvaluateResponseSchema schema = matchingSchema();
        schema.setPlaceResult(new PlaceEvaluateResult(EvaluateType.PartiallyMet, ""));

        assertThat(schema.isMatch()).isTrue();
      }

      /**
       * 場所の評価が未設定の場合に、場所を判定から外すことを確かめる。 場所を評価しなかった候補を、場所の不一致として除外しないことを守る。
       *
       * @given 場所の評価が未設定で、他の項目は全て満たす
       * @when isMatch を呼び出す
       * @then true を返す
       */
      @Test
      void 場所の評価が未設定の場合_trueを返す() {
        assertThat(matchingSchema().isMatch()).isTrue();
      }

      /**
       * 人月工数を満たさない場合に、マッチではないと判定することを確かめる。 稼働可能な工数に届かない要員を提案しないことを守る。
       *
       * @given 人月工数の評価が false で、他の項目は全て満たす
       * @when isMatch を呼び出す
       * @then false を返す
       */
      @Test
      void 人月工数を満たさない場合_falseを返す() {
        MatchEvaluateResponseSchema schema = matchingSchema();
        schema.setPersonMonthsResult(new PersonMonthsEvaluateResult(false, ""));

        assertThat(schema.isMatch()).isFalse();
      }
    }
  }

  @Nested
  class getMatchScore {

    @Nested
    class 正常系 {

      /**
       * 全ての評価が最高の場合に、100点を返すことを確かめる。 点数の上限が100点で、全てを満たした候補が最も高く評価されることを守る。
       *
       * @given 必須、尚可、その他が FullyMet、単価と出社要件と人月工数を満たし、場所は未評価である
       * @when getMatchScore を呼び出す
       * @then 100点を返す
       */
      @Test
      void 全ての評価が最高の場合_100点を返す() {
        MatchEvaluateResponseSchema schema = matchingSchema();
        schema.setWantList(List.of(skill(EvaluateType.FullyMet)));
        schema.setOtherList(List.of(other(EvaluateType.FullyMet, "")));

        assertThat(schema.getMatchScore()).isEqualTo(100);
      }

      /**
       * 部分的に満たす評価を重み付けして、39点を返すことを確かめる。 部分一致の重みと未評価の重みを点数に反映させ、評価の差が点数に出るようにすることを守る。
       *
       * @given 必須が PartiallyMet、出社要件が PartiallyMet、場所が Unknown、尚可が Unknown、その他が PartiallyMet
       *     で、単価と人月工数を満たす
       * @when getMatchScore を呼び出す
       * @then 39点を返す
       */
      @Test
      void 部分的に満たす評価が含まれる場合_重み付けした39点を返す() {
        MatchEvaluateResponseSchema schema = new MatchEvaluateResponseSchema();
        schema.setMustList(List.of(skill(EvaluateType.PartiallyMet)));
        schema.setPriceResult(new PriceEvaluateResult(true, ""));
        schema.setOfficeResult(new OfficeEvaluateResult(EvaluateType.PartiallyMet, ""));
        schema.setPlaceResult(new PlaceEvaluateResult(EvaluateType.Unknown, ""));
        schema.setPersonMonthsResult(new PersonMonthsEvaluateResult(true, ""));
        schema.setWantList(List.of(skill(EvaluateType.Unknown)));
        schema.setOtherList(List.of(other(EvaluateType.PartiallyMet, "")));

        assertThat(schema.getMatchScore()).isEqualTo(39);
      }
    }

    @Nested
    class 境界値 {

      /**
       * 全ての評価が最低の場合に、0点を返すことを確かめる。 満たさない評価だけの候補の点数が0になり、負の値を返さないことを守る。
       *
       * @given 必須、尚可、その他が NotMet、単価と出社要件と人月工数を満たさず、場所が NotMet である
       * @when getMatchScore を呼び出す
       * @then 0点を返す
       */
      @Test
      void 全ての評価が最低の場合_0点を返す() {
        MatchEvaluateResponseSchema schema = new MatchEvaluateResponseSchema();
        schema.setMustList(List.of(skill(EvaluateType.NotMet)));
        schema.setPriceResult(new PriceEvaluateResult(false, ""));
        schema.setOfficeResult(new OfficeEvaluateResult(EvaluateType.NotMet, ""));
        schema.setPlaceResult(new PlaceEvaluateResult(EvaluateType.NotMet, ""));
        schema.setPersonMonthsResult(new PersonMonthsEvaluateResult(false, ""));
        schema.setWantList(List.of(skill(EvaluateType.NotMet)));
        schema.setOtherList(List.of(other(EvaluateType.NotMet, "")));

        assertThat(schema.getMatchScore()).isEqualTo(0);
      }

      /**
       * 評価の一覧が未設定の場合に、既定の配点で80点を返すことを確かめる。 一覧が無い項目は既定の配点で計算し、評価の無い候補の点数を一定に保つことを守る。
       *
       * @given 必須、尚可、その他の一覧が未設定で、単価と出社要件と場所は未評価、人月工数は未評価である
       * @when getMatchScore を呼び出す
       * @then 80点を返す
       */
      @Test
      void 評価の一覧が未設定の場合_既定の配点で80点を返す() {
        assertThat(new MatchEvaluateResponseSchema().getMatchScore()).isEqualTo(80);
      }

      /**
       * 評価の一覧が空の場合に、一覧が未設定の時と同じ既定の配点で80点を返すことを確かめる。 空の一覧と未設定の一覧を同じに扱い、点数の計算結果を揃えることを守る。
       *
       * @given 必須、尚可、その他の一覧が空で、単価と出社要件と場所は未評価、人月工数は未評価である
       * @when getMatchScore を呼び出す
       * @then 80点を返す
       */
      @Test
      void 評価の一覧が空の場合_既定の配点で80点を返す() {
        MatchEvaluateResponseSchema schema = new MatchEvaluateResponseSchema();
        schema.setMustList(List.of());
        schema.setWantList(List.of());
        schema.setOtherList(List.of());

        assertThat(schema.getMatchScore()).isEqualTo(80);
      }

      /**
       * 評価の値が未設定の項目がある場合に、その項目を0点として計算することを確かめる。 評価の値が無い項目を満点として扱うと、根拠の無い点数が付くことを守る。
       *
       * @given 必須、尚可、その他の評価の値が未設定で、単価と出社要件と場所の評価の値が未設定、人月工数は未設定である
       * @when getMatchScore を呼び出す
       * @then 人月工数の未評価による5点だけを返す
       */
      @Test
      void 評価の値が未設定の項目がある場合_その項目を0点として5点を返す() {
        MatchEvaluateResponseSchema schema = new MatchEvaluateResponseSchema();
        schema.setMustList(List.of(skill(null)));
        schema.setPriceResult(null);
        schema.setOfficeResult(new OfficeEvaluateResult(null, ""));
        schema.setPlaceResult(new PlaceEvaluateResult(null, ""));
        schema.setWantList(List.of(skill(null)));
        schema.setOtherList(List.of(other(null, "")));

        assertThat(schema.getMatchScore()).isEqualTo(5);
      }
    }
  }

  @Nested
  class getMustEvaluationText {

    @Nested
    class 正常系 {

      /**
       * 必須スキルの評価がある場合に、見出しと評価の内容を並べた文字列を返すことを確かめる。 評価の根拠を画面と通知に表示するため、コメントを含めて形式を固定することを守る。
       *
       * @given 必須スキルが Java で、評価が FullyMet、コメントが "3年" である
       * @when getMustEvaluationText を呼び出す
       * @then 見出しと {@code "Java：◎(3年)"} の行を含む文字列を返す
       */
      @Test
      void 必須スキルの評価がある場合_見出しと評価の行を含む文字列を返す() {
        MatchEvaluateResponseSchema schema = new MatchEvaluateResponseSchema();
        schema.setMustList(List.of(namedSkill("Java", EvaluateType.FullyMet, "3年")));

        assertThat(schema.getMustEvaluationText()).isEqualTo("■必須\n・Java：◎(3年)\n");
      }

      /**
       * 必須スキルの評価が未設定の場合に、記号の代わりに "-" を表示することを確かめる。 評価の無い項目を空欄にすると、判定漏れと区別できなくなることを守る。
       *
       * @given 必須スキルが Java で、評価の値が未設定である
       * @when getMustEvaluationText を呼び出す
       * @then "Java：-" の行を含む文字列を返す
       */
      @Test
      void 必須スキルの評価の値が未設定の場合_ハイフンを表示した文字列を返す() {
        MatchEvaluateResponseSchema schema = new MatchEvaluateResponseSchema();
        schema.setMustList(List.of(namedSkill("Java", null, null)));

        assertThat(schema.getMustEvaluationText()).isEqualTo("■必須\n・Java：-\n");
      }
    }

    @Nested
    class 境界値 {

      /**
       * 必須スキルの一覧が未設定の場合に、空文字を返すことを確かめる。 見出しだけを出すと、評価が存在するように読まれることを守る。
       *
       * @given 必須スキルの一覧が未設定である
       * @when getMustEvaluationText を呼び出す
       * @then 空文字を返す
       */
      @Test
      void 必須スキルの一覧が未設定の場合_空文字を返す() {
        assertThat(new MatchEvaluateResponseSchema().getMustEvaluationText()).isEmpty();
      }

      /**
       * 必須スキルの一覧が空の場合に、空文字を返すことを確かめる。 空の一覧で見出しだけを出さないことを守る。
       *
       * @given 必須スキルの一覧が空である
       * @when getMustEvaluationText を呼び出す
       * @then 空文字を返す
       */
      @Test
      void 必須スキルの一覧が空の場合_空文字を返す() throws Exception {
        MatchEvaluateResponseSchema schema = new MatchEvaluateResponseSchema();
        schema.setMustList(List.of());

        assertThat(schema.getMustEvaluationText()).isEmpty();
      }
    }
  }

  @Nested
  class getWantEvaluationText {

    @Nested
    class 正常系 {

      /**
       * 尚可スキルの評価がある場合に、見出しと評価の内容を並べた文字列を返すことを確かめる。 必須と同じ形式で表示するため、尚可の見出しと行の形式を固定することを守る。
       *
       * @given 尚可スキルが Spring で、評価が PartiallyMet、コメントが "類似" である
       * @when getWantEvaluationText を呼び出す
       * @then 見出しと {@code "Spring：〇(類似)"} の行を含む文字列を返す
       */
      @Test
      void 尚可スキルの評価がある場合_見出しと評価の行を含む文字列を返す() {
        MatchEvaluateResponseSchema schema = new MatchEvaluateResponseSchema();
        schema.setWantList(List.of(namedSkill("Spring", EvaluateType.PartiallyMet, "類似")));

        assertThat(schema.getWantEvaluationText()).isEqualTo("■尚可\n・Spring：〇(類似)\n");
      }
    }

    @Nested
    class 境界値 {

      /**
       * 尚可スキルの一覧が空の場合に、空文字を返すことを確かめる。 尚可の見出しだけを出すと、評価が存在するように読まれることを守る。
       *
       * @given 尚可スキルの一覧が空である
       * @when getWantEvaluationText を呼び出す
       * @then 空文字を返す
       */
      @Test
      void 尚可スキルの一覧が空の場合_空文字を返す() {
        MatchEvaluateResponseSchema schema = new MatchEvaluateResponseSchema();
        schema.setWantList(List.of());

        assertThat(schema.getWantEvaluationText()).isEmpty();
      }

      /**
       * 尚可スキルの評価の値が未設定の場合に、ハイフンを表示することを確かめる。 評価の無い尚可スキルを空欄にせず、判定漏れと区別できるようにすることを守る。
       *
       * @given 尚可スキルが Spring で、評価の値が未設定である
       * @when getWantEvaluationText を呼び出す
       * @then Spring の行にハイフンを表示した文字列を返す
       */
      @Test
      void 尚可スキルの評価の値が未設定の場合_ハイフンを表示した文字列を返す() throws Exception {
        MatchEvaluateResponseSchema schema = new MatchEvaluateResponseSchema();
        schema.setWantList(List.of(namedSkill("Spring", null, null)));

        assertThat(schema.getWantEvaluationText()).isEqualTo("■尚可\n・Spring：-\n");
      }
    }
  }

  @Nested
  class getPlaceEvaluationText {

    @Nested
    class 正常系 {

      /**
       * 場所の評価にコメントがある場合に、コメントを表示することを確かめる。 場所の根拠は記号よりコメントの方が判断しやすいため、コメントを優先して表示することを守る。
       *
       * @given 場所の評価が PartiallyMet で、コメントが "東京近郊" である
       * @when getPlaceEvaluationText を呼び出す
       * @then 場所の見出しと "東京近郊" を含む文字列を返す
       */
      @Test
      void コメントがある場合_コメントを表示した文字列を返す() {
        MatchEvaluateResponseSchema schema = new MatchEvaluateResponseSchema();
        schema.setPlaceResult(new PlaceEvaluateResult(EvaluateType.PartiallyMet, "東京近郊"));

        assertThat(schema.getPlaceEvaluationText()).isEqualTo("■場所：東京近郊\n");
      }
    }

    @Nested
    class 境界値 {

      /**
       * コメントが空で評価がある場合に、記号を表示することを確かめる。 コメントが無い場合は評価の記号で結果を示すことを守る。
       *
       * @given 場所の評価が PartiallyMet で、コメントが空文字である
       * @when getPlaceEvaluationText を呼び出す
       * @then 記号の "〇" を表示した文字列を返す
       */
      @Test
      void コメントが空で評価がある場合_記号を表示した文字列を返す() {
        MatchEvaluateResponseSchema schema = new MatchEvaluateResponseSchema();
        schema.setPlaceResult(new PlaceEvaluateResult(EvaluateType.PartiallyMet, ""));

        assertThat(schema.getPlaceEvaluationText()).isEqualTo("■場所：〇\n");
      }

      /**
       * コメントも評価も無い場合に、ハイフンを表示することを確かめる。 評価の無い場所を空欄で出すと、判定漏れと区別できなくなることを守る。
       *
       * @given 場所の評価の値が未設定で、コメントが空文字である
       * @when getPlaceEvaluationText を呼び出す
       * @then ハイフンを表示した文字列を返す
       */
      @Test
      void コメントも評価も無い場合_ハイフンを表示した文字列を返す() {
        MatchEvaluateResponseSchema schema = new MatchEvaluateResponseSchema();
        schema.setPlaceResult(new PlaceEvaluateResult(null, ""));

        assertThat(schema.getPlaceEvaluationText()).isEqualTo("■場所：-\n");
      }

      /**
       * 場所の評価が無い場合に、空文字を返すことを確かめる。 場所を評価していない候補で場所の見出しを出すと、評価が存在するように読まれることを守る。
       *
       * @given 場所の評価が未設定である
       * @when getPlaceEvaluationText を呼び出す
       * @then 空文字を返す
       */
      @Test
      void 場所の評価が未設定の場合_空文字を返す() {
        assertThat(new MatchEvaluateResponseSchema().getPlaceEvaluationText()).isEmpty();
      }
    }
  }

  @Nested
  class getOfficeEvaluationText {

    @Nested
    class 正常系 {

      /**
       * 出社要件の評価にコメントがある場合に、コメントを添えて表示することを確かめる。 出社の条件の内容を、評価の記号と一緒に確認できるようにすることを守る。
       *
       * @given 出社要件の評価が FullyMet で、コメントが "週2まで" である
       * @when getOfficeEvaluationText を呼び出す
       * @then 記号と "週2まで" を添えた文字列を返す
       */
      @Test
      void コメントがある場合_記号とコメントを表示した文字列を返す() {
        MatchEvaluateResponseSchema schema = new MatchEvaluateResponseSchema();
        schema.setOfficeResult(new OfficeEvaluateResult(EvaluateType.FullyMet, "週2まで"));

        assertThat(schema.getOfficeEvaluationText()).isEqualTo("■出社要件：◎（週2まで）\n");
      }

      /**
       * 出社要件の評価にコメントが無い場合に、記号だけを表示することを確かめる。 空の括弧を出すと、コメントがあるように読まれることを守る。
       *
       * @given 出社要件の評価が FullyMet で、コメントが空文字である
       * @when getOfficeEvaluationText を呼び出す
       * @then 記号だけを表示した文字列を返す
       */
      @Test
      void コメントが空の場合_記号だけを表示した文字列を返す() {
        MatchEvaluateResponseSchema schema = new MatchEvaluateResponseSchema();
        schema.setOfficeResult(new OfficeEvaluateResult(EvaluateType.FullyMet, ""));

        assertThat(schema.getOfficeEvaluationText()).isEqualTo("■出社要件：◎\n");
      }
    }

    @Nested
    class 境界値 {

      /**
       * 出社要件の評価の値が未設定の場合に、ハイフンを表示することを確かめる。 評価の無い出社要件を空欄で出すと、判定漏れと区別できなくなることを守る。
       *
       * @given 出社要件の評価の値が未設定で、コメントが空文字である
       * @when getOfficeEvaluationText を呼び出す
       * @then ハイフンを表示した文字列を返す
       */
      @Test
      void 評価の値が未設定の場合_ハイフンを表示した文字列を返す() {
        MatchEvaluateResponseSchema schema = new MatchEvaluateResponseSchema();
        schema.setOfficeResult(new OfficeEvaluateResult(null, ""));

        assertThat(schema.getOfficeEvaluationText()).isEqualTo("■出社要件：-\n");
      }

      /**
       * 出社要件の評価が無い場合に、空文字を返すことを確かめる。 出社要件を評価していない候補で見出しを出すと、評価が存在するように読まれることを守る。
       *
       * @given 出社要件の評価が未設定である
       * @when getOfficeEvaluationText を呼び出す
       * @then 空文字を返す
       */
      @Test
      void 出社要件の評価が未設定の場合_空文字を返す() {
        assertThat(new MatchEvaluateResponseSchema().getOfficeEvaluationText()).isEmpty();
      }
    }
  }

  @Nested
  class getOtherEvaluationText {

    @Nested
    class 正常系 {

      /**
       * その他の評価がある場合に、項目ごとに記号とコメントを並べた文字列を返すことを確かめる。 その他の条件は1件ずつ判断するため、項目ごとの行の形式を固定することを守る。
       *
       * @given その他の評価が英語は FullyMet で "ビジネス可"、リモートは NotMet で空のコメントである
       * @when getOtherEvaluationText を呼び出す
       * @then 見出しと、英語とリモートの行を並べた文字列を返す
       */
      @Test
      void その他の評価がある場合_項目ごとの行を並べた文字列を返す() {
        MatchEvaluateResponseSchema schema = new MatchEvaluateResponseSchema();
        schema.setOtherList(
            List.of(
                other(EvaluateType.FullyMet, "ビジネス可", "英語"),
                other(EvaluateType.NotMet, "", "リモート")));

        assertThat(schema.getOtherEvaluationText()).isEqualTo("■その他：\n・英語：◎（ビジネス可）\n・リモート：×\n");
      }

      /**
       * その他の評価の値が未設定で不明の場合に、ハイフンを表示することを確かめる。 不明の評価を他の記号と区別して表示することを守る。
       *
       * @given その他の評価が Unknown で、コメントが空文字である
       * @when getOtherEvaluationText を呼び出す
       * @then 不明を表すハイフンを表示した文字列を返す
       */
      @Test
      void 評価が不明の場合_ハイフンを表示した文字列を返す() {
        MatchEvaluateResponseSchema schema = new MatchEvaluateResponseSchema();
        schema.setOtherList(List.of(other(EvaluateType.Unknown, "", "夜勤")));

        assertThat(schema.getOtherEvaluationText()).isEqualTo("■その他：\n・夜勤：-\n");
      }
    }

    @Nested
    class 境界値 {

      /**
       * その他の評価の一覧が空の場合に、空文字を返すことを確かめる。 見出しだけを出すと、評価が存在するように読まれることを守る。
       *
       * @given その他の評価の一覧が空である
       * @when getOtherEvaluationText を呼び出す
       * @then 空文字を返す
       */
      @Test
      void その他の評価の一覧が空の場合_空文字を返す() {
        MatchEvaluateResponseSchema schema = new MatchEvaluateResponseSchema();
        schema.setOtherList(List.of());

        assertThat(schema.getOtherEvaluationText()).isEmpty();
      }

      /**
       * その他の評価の値が未設定の場合に、ハイフンを表示することを確かめる。 評価の無いその他の項目を空欄にせず、判定漏れと区別できるようにすることを守る。
       *
       * @given その他の評価の値が未設定で、コメントが空文字である
       * @when getOtherEvaluationText を呼び出す
       * @then 夜勤の行にハイフンを表示した文字列を返す
       */
      @Test
      void その他の評価の値が未設定の場合_ハイフンを表示した文字列を返す() throws Exception {
        MatchEvaluateResponseSchema schema = new MatchEvaluateResponseSchema();
        schema.setOtherList(List.of(other(null, "", "夜勤")));

        assertThat(schema.getOtherEvaluationText()).isEqualTo("■その他：\n・夜勤：-\n");
      }
    }
  }

  @Nested
  class toNotificationSummary {

    @Nested
    class 正常系 {

      /**
       * 全ての項目を評価している場合に、通知用の要約を返すことを確かめる。 通知は一行で判断できる形にするため、項目の並びと記号を固定することを守る。
       *
       * @given 必須が全て FullyMet、単価を満たし、出社要件が FullyMet、場所が PartiallyMet である
       * @when toNotificationSummary を呼び出す
       * @then 必須、単価、出社、場所の記号を並べた要約を返す
       */
      @Test
      void 全ての項目を評価している場合_記号を並べた要約を返す() {
        MatchEvaluateResponseSchema schema = matchingSchema();
        schema.setPlaceResult(new PlaceEvaluateResult(EvaluateType.PartiallyMet, ""));

        assertThat(schema.toNotificationSummary()).isEqualTo("必須◎ 単価◎ 出社◎ 場所〇");
      }

      /**
       * 必須に一部だけ満たす項目があり、単価を満たさない場合に、対応する記号を表示することを確かめる。 通知で不一致の理由が分かるように、部分一致と不一致の記号を区別することを守る。
       *
       * @given 必須に FullyMet と PartiallyMet があり、単価の評価が false である
       * @when toNotificationSummary を呼び出す
       * @then 必須の記号を "〇"、単価の記号を "×" として要約を返す
       */
      @Test
      void 必須に一部だけ満たす項目があり単価を満たさない場合_不一致の記号を含む要約を返す() {
        MatchEvaluateResponseSchema schema = new MatchEvaluateResponseSchema();
        schema.setMustList(List.of(skill(EvaluateType.FullyMet), skill(EvaluateType.PartiallyMet)));
        schema.setPriceResult(new PriceEvaluateResult(false, ""));

        assertThat(schema.toNotificationSummary()).isEqualTo("必須〇 単価×");
      }
    }

    @Nested
    class 境界値 {

      /**
       * 出社要件と場所の評価が無い場合に、その項目を要約から省くことを確かめる。 評価していない項目を記号で示すと、判定した結果と区別できなくなることを守る。
       *
       * @given 必須と単価だけが評価されていて、出社要件と場所は未設定である
       * @when toNotificationSummary を呼び出す
       * @then 出社と場所を含まない要約を返す
       */
      @Test
      void 出社要件と場所の評価が無い場合_その項目を省いた要約を返す() {
        MatchEvaluateResponseSchema schema = new MatchEvaluateResponseSchema();
        schema.setMustList(List.of(skill(EvaluateType.FullyMet)));
        schema.setPriceResult(new PriceEvaluateResult(true, ""));

        assertThat(schema.toNotificationSummary()).isEqualTo("必須◎ 単価◎");
      }

      /**
       * 出社要件と場所の評価の値が未設定の場合に、その項目を要約から省くことを確かめる。 値の無い評価を記号にすると、判定の結果があるように読まれることを守る。
       *
       * @given 出社要件と場所の評価の値が未設定である
       * @when toNotificationSummary を呼び出す
       * @then 出社と場所を含まない要約を返す
       */
      @Test
      void 出社要件と場所の評価の値が未設定の場合_その項目を省いた要約を返す() {
        MatchEvaluateResponseSchema schema = new MatchEvaluateResponseSchema();
        schema.setOfficeResult(new OfficeEvaluateResult(null, ""));
        schema.setPlaceResult(new PlaceEvaluateResult(null, ""));

        assertThat(schema.toNotificationSummary()).isEmpty();
      }

      /**
       * 全ての項目が未設定の場合に、空文字を返すことを確かめる。 通知に何も出さないことを明示し、空の要約を送らないようにすることを守る。
       *
       * @given 全ての評価が未設定である
       * @when toNotificationSummary を呼び出す
       * @then 空文字を返す
       */
      @Test
      void 全ての項目が未設定の場合_空文字を返す() {
        assertThat(new MatchEvaluateResponseSchema().toNotificationSummary()).isEmpty();
      }

      /**
       * 必須スキルの一覧が空の場合に、必須を省いた要約を返すことを確かめる。 空の一覧を全て満たしたものとして表示しないことを守る。
       *
       * @given 必須スキルの一覧が空で、単価を満たす
       * @when toNotificationSummary を呼び出す
       * @then 必須を含まず単価の記号だけを含む要約を返す
       */
      @Test
      void 必須スキルの一覧が空の場合_必須を省いた要約を返す() throws Exception {
        MatchEvaluateResponseSchema schema = new MatchEvaluateResponseSchema();
        schema.setMustList(List.of());
        schema.setPriceResult(new PriceEvaluateResult(true, ""));

        assertThat(schema.toNotificationSummary()).isEqualTo("単価◎");
      }
    }
  }

  @Nested
  class toEvaluiationText {

    @Nested
    class 正常系 {

      /**
       * 全ての評価がある場合に、マッチ度と項目ごとの評価を並べた文字列を返すことを確かめる。 評価の根拠を一覧で確認できる形にし、マッチ度と内訳が同じ表示になることを守る。
       *
       * @given 必須、尚可、出社要件、場所、人月工数、その他の評価が揃っている
       * @when toEvaluiationText を呼び出す
       * @then マッチ度75点と、各項目の見出しと行を並べた文字列を返す
       */
      @Test
      void 全ての評価がある場合_マッチ度と項目ごとの行を並べた文字列を返す() {
        MatchEvaluateResponseSchema schema = new MatchEvaluateResponseSchema();
        schema.setMustList(List.of(namedSkill("Java", EvaluateType.FullyMet, "3年")));
        schema.setWantList(List.of(namedSkill("Spring", EvaluateType.PartiallyMet, "類似")));
        schema.setPriceResult(new PriceEvaluateResult(true, ""));
        schema.setOfficeResult(new OfficeEvaluateResult(EvaluateType.FullyMet, "週2"));
        schema.setPlaceResult(new PlaceEvaluateResult(EvaluateType.PartiallyMet, ""));
        schema.setPersonMonthsResult(new PersonMonthsEvaluateResult(true, "1年"));
        schema.setOtherList(List.of(other(EvaluateType.NotMet, "", "英語")));

        assertThat(schema.toEvaluiationText())
            .isEqualTo(
                "マッチ度：75点\n"
                    + "■必須\n・Java：◎(3年)\n"
                    + "■尚可\n・Spring：〇(類似)\n"
                    + "■出社要件：◎（週2）\n"
                    + "■場所：〇\n"
                    + "■人月工数：○（1年）\n"
                    + "■その他：\n・英語：×");
      }

      /**
       * 人月工数のコメントが空の場合に、人月工数の行を含めないことを確かめる。 根拠の無い人月工数の判定を表示しないことを守る。
       *
       * @given 人月工数の評価がコメントの空文字である
       * @when toEvaluiationText を呼び出す
       * @then 人月工数の見出しを含まない文字列を返す
       */
      @Test
      void 人月工数のコメントが空の場合_人月工数の行を含まない文字列を返す() {
        MatchEvaluateResponseSchema schema = new MatchEvaluateResponseSchema();
        schema.setPersonMonthsResult(new PersonMonthsEvaluateResult(false, ""));

        assertThat(schema.toEvaluiationText()).doesNotContain("■人月工数");
      }

      /**
       * 人月工数を満たさない場合に、人月工数の記号に不一致を表す記号を表示することを確かめる。 工数の不足を利用者が一目で判断できるように表示することを守る。
       *
       * @given 人月工数の評価が false で、コメントが "3か月不足" である
       * @when toEvaluiationText を呼び出す
       * @then 人月工数の行に不一致の記号を表示した文字列を返す
       */
      @Test
      void 人月工数を満たさない場合_人月工数の行に不一致の記号を表示した文字列を返す() {
        MatchEvaluateResponseSchema schema = new MatchEvaluateResponseSchema();
        schema.setPersonMonthsResult(new PersonMonthsEvaluateResult(false, "3か月不足"));

        assertThat(schema.toEvaluiationText()).contains("■人月工数：×（3か月不足）");
      }

      /**
       * 文字列が2000文字を超える場合に、2000文字に切り詰めることを確かめる。 文字列の長さは AI と通知の上限に合わせるため、超過分を切り捨てることを守る。
       *
       * @given その他の評価が300件あり、それぞれが20文字程度である
       * @when toEvaluiationText を呼び出す
       * @then 2000文字に切り詰めた文字列を返す
       */
      @Test
      void 文字列が2000文字を超える場合_2000文字に切り詰めた文字列を返す() {
        List<OtherEvaluateResult> others = new ArrayList<>();
        for (int i = 0; i < 300; i++) {
          others.add(other(EvaluateType.FullyMet, "あ".repeat(10), "い".repeat(10)));
        }
        MatchEvaluateResponseSchema schema = new MatchEvaluateResponseSchema();
        schema.setOtherList(others);

        assertThat(schema.toEvaluiationText()).hasSize(2000);
      }
    }
  }

  /**
   * 全ての必須項目を満たす応答を作る.
   *
   * @return 応答
   */
  private static MatchEvaluateResponseSchema matchingSchema() {
    MatchEvaluateResponseSchema schema = new MatchEvaluateResponseSchema();
    schema.setMustList(List.of(skill(EvaluateType.FullyMet)));
    schema.setPriceResult(new PriceEvaluateResult(true, ""));
    schema.setOfficeResult(new OfficeEvaluateResult(EvaluateType.FullyMet, ""));
    return schema;
  }

  /**
   * 評価のみを持つスキル評価を作る.
   *
   * @param result 評価
   * @return スキル評価
   */
  private static SkillEvaluateResult skill(final EvaluateType result) {
    return namedSkill("スキル", result, "");
  }

  /**
   * 項目名とコメントを持つスキル評価を作る.
   *
   * @param perspective 項目名
   * @param result 評価
   * @param comment コメント
   * @return スキル評価
   */
  private static SkillEvaluateResult namedSkill(
      final String perspective, final EvaluateType result, final String comment) {
    return new SkillEvaluateResult(perspective, result, comment);
  }

  /**
   * その他の評価を作る.
   *
   * @param result 評価
   * @param comment コメント
   * @param perspective 項目名
   * @return その他の評価
   */
  private static OtherEvaluateResult other(
      final EvaluateType result, final String comment, final String perspective) {
    return new OtherEvaluateResult(perspective, result, comment);
  }

  /**
   * 項目名を指定せず、その他の評価を作る.
   *
   * @param result 評価
   * @param comment コメント
   * @return その他の評価
   */
  private static OtherEvaluateResult other(final EvaluateType result, final String comment) {
    return other(result, comment, "その他");
  }
}
