package copel.sesproductpackage.tools.utspec;

/**
 * テストケース1行（仕様書の1行）を表す値オブジェクト。
 *
 * <p>テストクラス・第1階層（対象メソッド）・種類・テストメソッドの位置と、JavaDoc の4項目を持つ。 パラメータ付きテストは、パラメータ1組ごとに1つのインスタンスにする。
 *
 * @param classFqn テストクラスの完全修飾名（外側のクラス）
 * @param level1 第1階層の名前（対象メソッド）
 * @param kind 第2階層の名前（種類）
 * @param methodName テストメソッドの名前
 * @param rowLabel パラメータの1組を表す表示名。パラメータ付きでない場合は空文字
 * @param rowIndex パラメータ付きテストの0から始まる順番。パラメータ付きでない場合は 0
 * @param file リポジトリ直下からのテストソースの相対パス
 * @param line テストメソッドの行番号
 * @param what JavaDoc の本文1文目（何を確かめるか）
 * @param why JavaDoc の本文2文目（理由）
 * @param given JavaDoc の @given（前提）
 * @param when JavaDoc の @when（操作）
 * @param then JavaDoc の @then（期待結果）
 * @param status 実行結果
 * @author Copel Co., Ltd.
 */
public record TestCase(
    String classFqn,
    String level1,
    String kind,
    String methodName,
    String rowLabel,
    int rowIndex,
    String file,
    int line,
    String what,
    String why,
    String given,
    String when,
    String then,
    TestStatus status) {

  /**
   * surefire のレポートで使うクラス名（{@code 外側$第1階層$種類} の形式）を返す。
   *
   * @return surefire のクラス名
   */
  public String surefireClassName() {
    return classFqn + "$" + level1 + "$" + kind;
  }

  /**
   * 仕様書に出すテストケース名を返す。パラメータ付きテストでは表示名を後ろに付ける。
   *
   * @return テストケース名
   */
  public String displayName() {
    return rowLabel.isEmpty() ? methodName : methodName + " " + rowLabel;
  }

  /**
   * 行の位置を変えた新しいテストケースを返す。リポジトリ直下からのパスに変換する場合に使う。
   *
   * @param newFile 新しいファイルのパス
   * @return ファイルのパスを差し替えたテストケース
   */
  public TestCase withFile(String newFile) {
    return new TestCase(
        classFqn,
        level1,
        kind,
        methodName,
        rowLabel,
        rowIndex,
        newFile,
        line,
        what,
        why,
        given,
        when,
        then,
        status);
  }

  /**
   * 実行結果だけを変えた新しいテストケースを返す。
   *
   * @param newStatus 新しい実行結果
   * @return 実行結果を差し替えたテストケース
   */
  public TestCase withStatus(TestStatus newStatus) {
    return new TestCase(
        classFqn,
        level1,
        kind,
        methodName,
        rowLabel,
        rowIndex,
        file,
        line,
        what,
        why,
        given,
        when,
        then,
        newStatus);
  }
}
