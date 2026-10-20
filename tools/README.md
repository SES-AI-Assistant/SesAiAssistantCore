# Nectar の Java ビルドツール

Nectar の Java リポジトリ（Lambda・Web API）で、ビルド・UT・静的チェックを同じ形で行うための道具をまとめています。
各リポジトリでのコマンド、検査の中身、新しいリポジトリへの導入手順は、この README に集約しています。

## 1. 全体像

| ツール | 役割 | 各リポジトリでの使い方 |
| :--- | :--- | :--- |
| `nectar-parent` | 親 pom。ビルド設定と、`mvn verify` で行う検査をまとめる | `<parent>` に指定する（これだけで下の3つも使われる） |
| `nectar-bom` | JUnit・Mockito・AssertJ などの版を統一する | 親 pom が読み込む |
| `nectar-test-support` | UT 用の部品（時刻の固定、設定値の差し替え、DB 接続のモック）と、UT 用のログ設定 | 親 pom が `test` スコープで依存に加える |
| `nectar-ut-spec-plugin` | UT がルールどおりか検査し、テスト仕様書 HTML を出力する | 親 pom が `verify` で実行する |

```
各リポジトリの pom.xml ──<parent>──▶ nectar-parent
                                       ├─ import ─▶ nectar-bom（版の統一）
                                       ├─ test 依存 ─▶ nectar-test-support（UT 用の部品）
                                       └─ verify で実行 ─▶ nectar-ut-spec-plugin（UT 仕様の検査）
```

各リポジトリでの作業は、`src/main/java` に本体を書き、`src/test/java` に UT のルール（Nectar のワークスペース直下の `.claude/.log/Java_UT仕様.md`。各リポジトリには含まれない）どおりにテストを書くことだけです。
検査の設定は各リポジトリの pom には書きません。

## 2. コマンド

### 2.1 覚えるのは2つ

| コマンド | いつ使うか | 何をするか |
| :--- | :--- | :--- |
| `mvn spotless:apply` | コミットの前 | 整形を直す（未使用 import の削除、import の並べ替えを含む）。ファイルを書き換える |
| `mvn clean verify` | コミット・PR の前 | CI と同じ全検査を行う。ファイルは書き換えない |

CI（各リポジトリの `.github/workflows/ci.yml`）も `mvn clean verify` だけを実行します。検査の内容は手元と同じなので、手元で BUILD SUCCESS なら CI も通ります（CI 側で GitHub Packages の認証ができ、親 pom・ツールが配布済みであることが前提）。

`clean` は必ず付けます。前回の結果（`target/surefire-reports` や checkstyle のキャッシュ）が残っていると、
削除したテストの結果がテスト仕様書に混ざったり、違反を見逃したりするためです。

### 2.2 補助のコマンド（検査は行わない）

| コマンド | 用途 |
| :--- | :--- |
| `mvn test` | UT だけを実行する（実装中に素早く回す） |
| `mvn test -Dtest=LambdaHandlerTest` | 1クラスだけ実行する |
| `mvn clean package -Dmaven.test.skip=true --settings .github/maven-settings.xml` | Lambda 用 JAR を作る（各リポジトリの `deploy.yml` が使う） |

この他の個別のゴール（`checkstyle:check`、`pmd:check` など）は、`mvn clean verify` に含まれているため、個別に実行する必要はありません。
OpenRewrite（`rewrite:run`）は使いません。未使用 import の削除は `spotless:apply` が行います。

## 3. `mvn clean verify` で検査すること

上から順に実行し、失敗した時点で止まります。

| 順 | フェーズ | 検査 | 失敗したときの対処 | 結果の場所 |
| :-: | :--- | :--- | :--- | :--- |
| 1 | validate | JDK のバージョン（17 以上 23 未満） | `JAVA_HOME` を対応 JDK にする（4.1 参照） | — |
| 2 | test | UT の全 PASS（surefire） | 失敗したテストを直す | `target/surefire-reports/` |
| 3 | verify | 整形（spotless。google-java-format 1.17.0。対象は `src/main/java` と `src/test/java`） | `mvn spotless:apply` を実行する | — |
| 4 | verify | LINT: checkstyle（Google スタイル。警告も失敗。本体のみ） | ログの `[WARN] ファイル:行:列` を直す | `target/checkstyle-result.xml` |
| 5 | verify | LINT: PMD（既定のルールセット）と CPD（コードの重複） | ログに出た箇所を直す | `target/pmd.xml`、`target/cpd.xml` |
| 6 | verify | カバレッジ 100%（JaCoCo。命令と分岐） | 通っていない行・分岐のテストを足す | `target/site/jacoco/index.html` |
| 7 | verify | UT 仕様の検査（命名・JavaDoc・1対1 など）とテスト仕様書 HTML の出力 | ログの `ファイル:行 [UT-xx]` を直す（ルールは `nectar-ut-spec-plugin/README.md`） | `target/nectar-ut-spec/index.html` |
| 8 | verify | バグ検出（spotbugs） | ログに出たバグパターンを直す | `target/spotbugsXml.xml` |

- checkstyle はテストコードを対象にしません。日本語のテスト名と `@Nested` のクラス名が Java の命名規則に合わないためです。テストの品質は 7 の UT 仕様の検査で担保します。
- spotbugs の除外がどうしても必要な場合は、範囲を限定した除外ファイルを作り、そのリポジトリの pom で `excludeFilterFile` を指定します。
- UT ではログを出力しません（`nectar-test-support` 同梱の `logback-ut.xml`）。リポジトリの `src/test/resources` に同名のファイルを置くと、そちらが優先されます。

## 4. 前提（最初に1回だけ）

### 4.1 JDK

Maven が **JDK 17 以上 23 未満** で動いている必要があります。範囲外だと、ビルドの最初に対処方法付きのエラーで止まります。
シェルの `java` と Maven が使う JDK は別のことがあるため、`mvn -version` で確認します。

```bash
mvn -version                                     # Java version を確認する
export JAVA_HOME=$(/usr/libexec/java_home -v 18)  # 範囲外だった場合（macOS）
```

### 4.2 GitHub Packages の認証

親 pom・Core・ツールは、GitHub Packages（`https://maven.pkg.github.com/SES-AI-Assistant/SesAiAssistantCore`）から取得します。
公開パッケージでも、GitHub Packages の Maven はダウンロードに認証が必要です。

1. GitHub で Personal Access Token（classic）を作り、`read:packages` の権限を付ける
2. `~/.m2/settings.xml` に次を書く（server の id は各 pom の repository の id `github` と一致させる）

```xml
<settings>
  <servers>
    <server>
      <id>github</id>
      <username>GitHub のユーザー名</username>
      <password>作ったトークン</password>
    </server>
  </servers>
</settings>
```

CI では、各リポジトリの `.github/maven-settings.xml` が環境変数 `GITHUB_ACTOR` と `GITHUB_TOKEN`（`secrets.GH_PAT`）で同じ設定をします。

## 5. リポジトリへの導入手順

UT をルールどおりに作り直したリポジトリから導入します（導入すると、カバレッジ 100% と UT 仕様の検査が必須になるため）。
お手本は `AwsLambdaLineMssageReceiver` です。

### 5.1 pom.xml

親を指定し、リポジトリ固有の設定だけを書きます。

```xml
<parent>
  <groupId>com.github.SES-AI-Assistant</groupId>
  <artifactId>nectar-parent</artifactId>
  <version>0.0.1-SNAPSHOT</version>
  <relativePath />
</parent>

<!-- 親 pom の取得に必要（親の解決は、親の設定を読む前に行われるため子に書く） -->
<repositories>
  <repository>
    <id>github</id>
    <url>https://maven.pkg.github.com/SES-AI-Assistant/SesAiAssistantCore</url>
    <snapshots>
      <enabled>true</enabled>
    </snapshots>
  </repository>
</repositories>

<dependencies>
  <!-- Core は版を書かない（親 pom で管理） -->
  <dependency>
    <groupId>com.github.SES-AI-Assistant</groupId>
    <artifactId>sesaiassistantcore</artifactId>
  </dependency>
  <!-- AWS Lambda・ログなど、リポジトリ固有の依存 -->
</dependencies>

<build>
  <plugins>
    <!-- Lambda 用 JAR。版は親 pom で管理しているため書かない -->
    <plugin>
      <groupId>org.apache.maven.plugins</groupId>
      <artifactId>maven-shade-plugin</artifactId>
      <executions>
        <execution>
          <phase>package</phase>
          <goals>
            <goal>shade</goal>
          </goals>
          <configuration>
            <transformers>
              <transformer implementation="org.apache.maven.plugins.shade.resource.ManifestResourceTransformer">
                <mainClass>copel.sesproductpackage.xxx.LambdaHandler</mainClass>
              </transformer>
            </transformers>
          </configuration>
        </execution>
      </executions>
    </plugin>
  </plugins>
</build>
```

親 pom が持つため、子の pom から消すもの:

- `maven.compiler.release`、`lombok.version`、`pluginRepositories`、`nectar-bom` の import
- 依存: `lombok`、`nectar-test-support`、JUnit・Mockito（`nectar-test-support` が持ち込む）
- プラグイン: enforcer、compiler、surefire、JaCoCo、`nectar-ut-spec-plugin`、spotless、checkstyle、PMD、spotbugs、OpenRewrite
- ファイル: 空の `spotbugs-exclude.xml`、`src/test/resources/logback-ut.xml`

shade・exec・build-helper は版だけ親 pom で管理しているため、子では `<version>` を書かずに宣言します。

### 5.2 CI

お手本の `.github/workflows/ci.yml` をそのままコピーします。検査は `mvn clean verify` の1ステップで、テスト結果・カバレッジレポート・テスト仕様書を Artifacts に14日間保存します。

### 5.3 ドキュメント

リポジトリの `AGENTS.md` と `README.md` のコマンド記載は、2章の2つのコマンドとし、詳細はこの README を参照させます。

## 6. ツール自身の開発と配布

### 6.1 ビルド

依存の順に `tools/` 配下でビルドします。

```bash
(cd tools/nectar-bom && mvn clean install)
(cd tools/nectar-ut-spec-plugin && mvn clean install)
(cd tools/nectar-test-support && mvn clean install)
(cd tools/nectar-parent && mvn clean install)
```

`nectar-parent` は自分自身にも検査のプラグインが掛かりますが、pom だけでソースが無いため、どの検査も対象なしで通ります（UT 仕様の検査は packaging が pom のとき行いません）。

`nectar.ut.skip=true` を指定すると UT 仕様の検査が止まりますが、ログに1行出るだけで気付きにくいため、各リポジトリの pom や CI では使わないでください。

### 6.2 配布

`.github/workflows/deploy-tools.yml` が、`main` への push のうち `tools/**` に変更があった場合と、手動実行で GitHub Packages に配布します。
配布の順は `nectar-bom` → `nectar-ut-spec-plugin` → `nectar-test-support` → `nectar-parent` です。
`nectar-parent` の配布先は、pom ではなく `deploy-tools.yml` の `altDeploymentRepository` で指定しています（pom に書くと子に継承され、子で `mvn deploy` したときに各リポジトリの成果物が公開されてしまうため）。

各リポジトリの CI は配布済みの `nectar-parent` を取得するため、ツールを変えた場合は、Core の PR を先にマージして `deploy-tools.yml` の成功を確かめてから、各リポジトリの PR をマージします。

### 6.3 版

Core とツールは同じ版（現在 `0.0.1-SNAPSHOT`）で配布しています。版を上げるときは、`nectar-parent` の `nectar.version` と各ツールの pom の `version` を合わせて変えます。

### 6.4 GitHub Packages の料金

Core リポジトリは Public のため、配布したパッケージも公開扱いで、保存量・転送量ともに無料です。
Core リポジトリを Private にすると、Free プランの上限（保存 500MB、転送 月 1GB）が掛かり、CI からの取得も転送量に数えられます（`GITHUB_TOKEN` ではなく PAT で取得しているため）。
