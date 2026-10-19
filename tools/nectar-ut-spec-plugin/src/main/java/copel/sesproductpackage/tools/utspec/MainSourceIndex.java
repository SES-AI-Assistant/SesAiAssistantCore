package copel.sesproductpackage.tools.utspec;

import java.util.List;
import java.util.Map;

/**
 * src/main/java を走査した結果の索引。本体クラスの一覧、除外の一覧、本体ソースの違反を持つ。
 *
 * @param bodies 完全修飾名から本体クラスへの対応
 * @param exclusions {@code @CoverageExcludedGenerated} による除外の一覧
 * @param violations 本体ソースで検出した違反（構文エラー、除外の理由の欠落）
 * @author Copel Co., Ltd.
 */
public record MainSourceIndex(
    Map<String, BodyClass> bodies, List<ExclusionEntry> exclusions, List<Violation> violations) {

  /** 本体の対応、除外、違反の一覧を防御的にコピーして保持する。走査の結果を後続の検査で変えないようにするため。 */
  public MainSourceIndex {
    bodies = Map.copyOf(bodies);
    exclusions = List.copyOf(exclusions);
    violations = List.copyOf(violations);
  }

  /**
   * 除外の1件を表す値オブジェクト。仕様書の「除外クラス一覧」に出す。
   *
   * @param target 除外された対象（クラスの完全修飾名、またはクラス名#メンバー名）
   * @param reason 除外の理由（{@code @CoverageExcludedGenerated} の reason）
   */
  public record ExclusionEntry(String target, String reason) {}
}
