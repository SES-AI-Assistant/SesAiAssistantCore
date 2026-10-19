package copel.sesproductpackage.tools.utspec;

import java.util.List;

/**
 * テスト仕様書 HTML1ファイルに出す内容を表す値オブジェクト。
 *
 * @param repositoryName リポジトリ名
 * @param commitHash コミットハッシュ。取れない場合は null
 * @param repositoryUrl リポジトリの URL。取れない場合は null（行リンクを作らない）
 * @param generatedAt 出力の日時（表示用の文字列）
 * @param passedCount 成功したテストケース数
 * @param failedCount 失敗したテストケース数
 * @param skippedCount スキップしたテストケース数
 * @param coverage JaCoCo の集計結果
 * @param exclusions 除外の一覧
 * @param cases テストケースの一覧（実行結果を含む）
 * @author Copel Co., Ltd.
 */
public record SpecReport(
    String repositoryName,
    String commitHash,
    String repositoryUrl,
    String generatedAt,
    int passedCount,
    int failedCount,
    int skippedCount,
    JacocoSummary coverage,
    List<MainSourceIndex.ExclusionEntry> exclusions,
    List<TestCase> cases) {
  /** 除外の一覧とテストケースの一覧を防御的にコピーして保持する。仕様書の出力中に変わらないようにするため。 */
  public SpecReport {
    exclusions = List.copyOf(exclusions);
    cases = List.copyOf(cases);
  }
}
