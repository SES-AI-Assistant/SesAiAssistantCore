package copel.sesproductpackage.tools.utspec;

import java.util.List;

/**
 * 本体クラス（src/main/java の型）の検査に必要な情報を表す値オブジェクト。
 *
 * <p>テストの第1階層の名前が本体に存在するかを判定するため、宣言されたメソッド・コンストラクタの 期待する名前（{@link #expectedNames()}）を持つ。
 *
 * @param packageName パッケージ名（デフォルトパッケージは空）
 * @param simpleName クラス名（外側のクラス）
 * @param relativePath リポジトリ直下からのソースの相対パス
 * @param line 型宣言の行番号
 * @param expectedNames 第1階層に書いてよい名前の一覧（オーバーロード時は型付きの名前）
 * @param excluded {@code @CoverageExcludedGenerated} が付いているなら true
 * @author Copel Co., Ltd.
 */
public record BodyClass(
    String packageName,
    String simpleName,
    String relativePath,
    int line,
    List<String> expectedNames,
    boolean excluded) {
  /** 期待する名前の一覧を防御的にコピーして保持する。外部の変更が検査結果に影響しないようにするため。 */
  public BodyClass {
    expectedNames = List.copyOf(expectedNames);
  }

  /**
   * 完全修飾名を返す。テストクラスとの対応付けに使う。
   *
   * @return 完全修飾名（デフォルトパッケージの場合はクラス名だけ）
   */
  public String fqn() {
    return packageName.isEmpty() ? simpleName : packageName + "." + simpleName;
  }
}
