package copel.sesproductpackage.core.api.gpt.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** {@link SkillsheetLinkJudgeRequestSchema} の単体テスト。 */
class SkillsheetLinkJudgeRequestSchemaTest {

  @Nested
  class toString {

    @Nested
    class 正常系 {

      /**
       * ファイル名と担当者の一覧を、スネークケースの JSON 文字列に変換することを確かめる。 判定の依頼内容をログに残す形式が、AI への送信形式と同じ名前で揃うことを守る。
       *
       * @given ファイル名が "skillsheet.xlsx" で、担当者 P001 の要約が "要約" である
       * @when toString を呼び出す
       * @then スネークケースの JSON 文字列を返す
       */
      @Test
      void ファイル名と担当者が設定されている場合_JSON文字列を返す() {
        SkillsheetLinkJudgeRequestSchema.PersonInfo person =
            new SkillsheetLinkJudgeRequestSchema.PersonInfo();
        person.setPersonId("P001");
        person.setSummary("要約");
        SkillsheetLinkJudgeRequestSchema schema = new SkillsheetLinkJudgeRequestSchema();
        schema.setFileName("skillsheet.xlsx");
        schema.setPersons(List.of(person));

        assertThat(schema.toString())
            .isEqualTo(
                "{\"file_name\":\"skillsheet.xlsx\",\"persons\":[{\"person_id\":\"P001\",\"summary\":\"要約\"}]}");
      }
    }

    @Nested
    class 境界値 {

      /**
       * 担当者の一覧が未設定の場合に、担当者の項目を null として出力することを確かめる。 担当者が無い依頼でも、ファイル名だけは判定の材料として残すので、その仕様を守る。
       *
       * @given ファイル名が "a.xlsx" で、担当者の一覧が未設定である
       * @when toString を呼び出す
       * @then ファイル名を含み、担当者の項目が null の JSON 文字列を返す
       */
      @Test
      void 担当者の一覧が未設定の場合_担当者をnullにしたJSON文字列を返す() {
        SkillsheetLinkJudgeRequestSchema schema = new SkillsheetLinkJudgeRequestSchema();
        schema.setFileName("a.xlsx");

        assertThat(schema.toString()).isEqualTo("{\"file_name\":\"a.xlsx\",\"persons\":null}");
      }
    }
  }
}
