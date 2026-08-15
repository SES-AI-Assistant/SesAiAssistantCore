package copel.sesproductpackage.core.api.gpt.entity;

import copel.sesproductpackage.core.api.gpt.schema.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AIによるスキルシートマッチ判定結果エンティティ.
 *
 * @author Copel Co., Ltd.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SkillsheetLinkJudgeResponseSchema {
  @Schema(
    title = "判定結果",
    description ="要求された要員リスト(persons)の中に、要求された指定のファイル名の経歴書の持ち主にとなる要員が存在するかどうか。存在すればtrue、しなければfalse"
  )
  private boolean have = false;
  @Schema(
    title = "該当者の要員ID",
    description ="指定のファイル名の経歴書の持ち主にとなる要員のpersonIdを10文字で指定する",
    maxLength = 10,
    minLength = 10
  )
  private String personId = null;
}
