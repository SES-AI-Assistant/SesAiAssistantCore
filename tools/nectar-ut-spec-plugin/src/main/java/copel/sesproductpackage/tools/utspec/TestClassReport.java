package copel.sesproductpackage.tools.utspec;

import java.util.List;

/**
 * テストクラス1つ分の検査結果を表す値オブジェクト。
 *
 * @param classFqn テストクラスの完全修飾名。解析に失敗した場合は空
 * @param bodyFqn 対応するはずの本体クラスの完全修飾名。テストクラスの名前が「本体名 + Test」でない場合は null
 * @param relativePath リポジトリ直下からのテストソースの相対パス
 * @param violations 検出した違反の一覧
 * @param cases 仕様書に出すテストケースの一覧
 * @author Copel Co., Ltd.
 */
public record TestClassReport(
    String classFqn,
    String bodyFqn,
    String relativePath,
    List<Violation> violations,
    List<TestCase> cases) {
  /** 違反の一覧とテストケースの一覧を防御的にコピーして保持する。検査の結果を後から変えないようにするため。 */
  public TestClassReport {
    violations = List.copyOf(violations);
    cases = List.copyOf(cases);
  }
}
