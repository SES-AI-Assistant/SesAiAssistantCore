package copel.sesproductpackage.tools.utspec;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** {@link GitContext} の単体テスト。 */
class GitContextTest {

  /** 実行に使うディレクトリ。git の無い場所として存在しないパスを使う。 */
  private static final Path NO_REPO = Path.of("no-such-dir-for-git-context");

  @Nested
  class コンストラクタ_Map_Path {

    @Nested
    class 正常系 {

      /**
       * 環境変数を渡して生成すると、その環境変数でリポジトリ名を求めることを確かめる。 テストで環境を固定できるようにする。
       *
       * @given 環境変数に GITHUB_REPOSITORY が owner/repo の値
       * @when repositoryName を実行する
       * @then "repo" を返す
       */
      @Test
      void 環境変数が設定されている場合_環境変数からリポジトリ名を返す() {
        GitContext git = new GitContext(Map.of("GITHUB_REPOSITORY", "owner/repo"), NO_REPO);

        assertThat(git.repositoryName()).isEqualTo("repo");
      }
    }
  }

  @Nested
  class コンストラクタ_Map_Path_String {

    @Nested
    class 正常系 {

      /**
       * 実行するコマンドの名前を差し替えて生成しても、同じ環境変数の判定を使うことを確かめる。 中断の振る舞いを、差し替えたコマンドでテストできるようにする。
       *
       * @given 環境変数に GITHUB_SHA を持ち、コマンド名を sh にして生成した Git の情報
       * @when commitHash を実行する
       * @then 環境変数の値を返す
       */
      @Test
      void コマンド名を指定する場合_環境変数の判定を使った値を返す() {
        GitContext git = new GitContext(Map.of("GITHUB_SHA", "def456"), NO_REPO, "sh");

        assertThat(git.commitHash()).isEqualTo("def456");
      }
    }
  }

  @Nested
  class pathPrefix {

    @Nested
    class 正常系 {

      /**
       * 接頭辞が取れない場合は空文字を返すことを確かめる。 リポジトリ外の実行でもリンクのパスを壊さない。
       *
       * @given git を実行できないディレクトリ
       * @when pathPrefix を実行する
       * @then 空文字を返す
       */
      @Test
      void 取得できない場合_空文字を返す() {
        assertThat(new GitContext(Map.of(), NO_REPO).pathPrefix()).isEmpty();
      }

      /**
       * 実行できる場合は接頭辞が空か、末尾にスラッシュの付いた形式で返すことを確かめる。 実行環境によらず、形式が崩れないことを確かめる。
       *
       * @given 現在のディレクトリを基準にした Git の情報
       * @when pathPrefix を実行する
       * @then 空文字、または末尾にスラッシュの付いた文字列を返す
       */
      @Test
      void 実行できる場合_空文字かスラッシュ付きの文字列を返す() {
        assertThat(new GitContext(Map.of(), Path.of(".")).pathPrefix()).matches("|.*/");
      }
    }
  }

  @Nested
  class repositoryName {

    @Nested
    class 正常系 {

      /**
       * GITHUB_REPOSITORY の末尾をリポジトリ名にすることを確かめる。 仕様書の見出しにリポジトリ名だけを出す。
       *
       * @given GITHUB_REPOSITORY が "org/SesAiAssistantCore"
       * @when repositoryName を実行する
       * @then "SesAiAssistantCore" を返す
       */
      @Test
      void 環境変数がある場合_末尾をリポジトリ名にした値を返す() {
        GitContext git =
            new GitContext(Map.of("GITHUB_REPOSITORY", "org/SesAiAssistantCore"), NO_REPO);

        assertThat(git.repositoryName()).isEqualTo("SesAiAssistantCore");
      }
    }

    @Nested
    class 境界値 {
      /**
       * ルートのパスのように名前を持たない基準ディレクトリは、空のリポジトリ名として扱うことを確かめる。 名前が取れない場合でも例外にせず、仕様書を出力できるようにする。
       *
       * @given 名前を持たないルートのパス
       * @when repositoryName を実行する
       * @then 空文字を返す
       */
      @Test
      void 基準のパスが名前を持たない場合_空文字を返す() {
        assertThat(new GitContext(Map.of(), Path.of("/")).repositoryName()).isEmpty();
      }

      /**
       * 環境変数が無い場合は基準ディレクトリの名前をリポジトリ名にすることを確かめる。 ローカルで実行してもリポジトリ名を出す。
       *
       * @given 環境変数が空で、基準ディレクトリが "/work/repo-name"
       * @when repositoryName を実行する
       * @then "repo-name" を返す
       */
      @Test
      void 環境変数が無い場合_基準ディレクトリの名前を返す() {
        GitContext git = new GitContext(Map.of(), Path.of("/work/repo-name"));

        assertThat(git.repositoryName()).isEqualTo("repo-name");
      }

      /**
       * 空白だけの環境変数は無いものとして扱うことを確かめる。 空の値でリポジトリ名を壊さない。
       *
       * @given GITHUB_REPOSITORY が空白だけの値で、基準ディレクトリが "/work/local"
       * @when repositoryName を実行する
       * @then "local" を返す
       */
      @Test
      void 環境変数が空白だけの場合_基準ディレクトリの名前を返す() {
        GitContext git = new GitContext(Map.of("GITHUB_REPOSITORY", " "), Path.of("/work/local"));

        assertThat(git.repositoryName()).isEqualTo("local");
      }
    }
  }

  @Nested
  class commitHash {

    @Nested
    class 正常系 {

      /**
       * GITHUB_SHA があればそれをコミットハッシュにすることを確かめる。 GitHub Actions の実行と同じハッシュで行リンクを作る。
       *
       * @given GITHUB_SHA が "abc123"
       * @when commitHash を実行する
       * @then "abc123" を返す
       */
      @Test
      void 環境変数がある場合_そのハッシュを返す() {
        GitContext git = new GitContext(Map.of("GITHUB_SHA", "abc123"), NO_REPO);

        assertThat(git.commitHash()).isEqualTo("abc123");
      }
    }

    @Nested
    class 境界値 {

      /**
       * 空白だけの GITHUB_SHA は無いものとして扱うことを確かめる。 空の値でリンクを作らない。
       *
       * @given GITHUB_SHA が空白だけの値で、git を実行できないディレクトリ
       * @when commitHash を実行する
       * @then null を返す
       */
      @Test
      void 環境変数が空白だけの場合_nullを返す() {
        assertThat(new GitContext(Map.of("GITHUB_SHA", " "), NO_REPO).commitHash()).isNull();
      }

      /**
       * git も環境変数も無い場合は null を返すことを確かめる。 取れない情報でリンクを作らない。
       *
       * @given 環境変数が空で、git を実行できないディレクトリ
       * @when commitHash を実行する
       * @then null を返す
       */
      @Test
      void 取得できない場合_nullを返す() {
        assertThat(new GitContext(Map.of(), NO_REPO).commitHash()).isNull();
      }
    }
  }

  @Nested
  class repositoryUrl {

    @Nested
    class 正常系 {

      /**
       * GitHub の環境変数からリポジトリの URL を作ることを確かめる。 行リンクの土台にする。
       *
       * @given GITHUB_SERVER_URL が "https://github.com"、GITHUB_REPOSITORY が "org/repo"
       * @when repositoryUrl を実行する
       * @then "https://github.com/org/repo" を返す
       */
      @Test
      void 環境変数が揃う場合_サーバーとリポジトリをつないだURLを返す() {
        GitContext git =
            new GitContext(
                Map.of("GITHUB_SERVER_URL", "https://github.com", "GITHUB_REPOSITORY", "org/repo"),
                NO_REPO);

        assertThat(git.repositoryUrl()).isEqualTo("https://github.com/org/repo");
      }
    }

    @Nested
    class 境界値 {

      /**
       * サーバーが空白の場合は環境変数を使わず、取得できない場合は null を返すことを確かめる。 不完全な環境変数で URL を作らない。
       *
       * @given GITHUB_SERVER_URL が空白、GITHUB_REPOSITORY が org/repo で、git を実行できないディレクトリ
       * @when repositoryUrl を実行する
       * @then null を返す
       */
      @Test
      void サーバーが空白の場合_nullを返す() {
        GitContext git =
            new GitContext(
                Map.of("GITHUB_SERVER_URL", " ", "GITHUB_REPOSITORY", "org/repo"), NO_REPO);

        assertThat(git.repositoryUrl()).isNull();
      }

      /**
       * リポジトリ名が空白の場合は環境変数を使わず、取得できない場合は null を返すことを確かめる。 不完全な環境変数で URL を作らない。
       *
       * @given GITHUB_SERVER_URL が https の値で、GITHUB_REPOSITORY が空白、git を実行できないディレクトリ
       * @when repositoryUrl を実行する
       * @then null を返す
       */
      @Test
      void リポジトリ名が空白の場合_nullを返す() {
        GitContext git =
            new GitContext(
                Map.of("GITHUB_SERVER_URL", "https://github.com", "GITHUB_REPOSITORY", " "),
                NO_REPO);

        assertThat(git.repositoryUrl()).isNull();
      }

      /**
       * リポジトリ名が無い場合も環境変数を使わず、取得できない場合は null を返すことを確かめる。 片方だけの環境変数で URL を作らない。
       *
       * @given GITHUB_SERVER_URL だけがあり、git を実行できないディレクトリ
       * @when repositoryUrl を実行する
       * @then null を返す
       */
      @Test
      void リポジトリ名が無い場合_nullを返す() {
        GitContext git = new GitContext(Map.of("GITHUB_SERVER_URL", "https://github.com"), NO_REPO);

        assertThat(git.repositoryUrl()).isNull();
      }

      /**
       * 環境変数も git の情報も無い場合は null を返すことを確かめる。 取れない場合は行リンクを作らない。
       *
       * @given 環境変数が空で、git を実行できないディレクトリ
       * @when repositoryUrl を実行する
       * @then null を返す
       */
      @Test
      void 取得できない場合_nullを返す() {
        assertThat(new GitContext(Map.of(), NO_REPO).repositoryUrl()).isNull();
      }
    }
  }

  @Nested
  class normalizeRemote {

    @Nested
    class 正常系 {
      /**
       * ssh 形式の URL を https 形式に変換し、認証情報を取り除くことを確かめる。 ssh の URL をそのまま行リンクに使うとブラウザで開けない。
       *
       * @given 接続先が ssh://git@github.com/org/repo.git である
       * @when normalizeRemote を実行する
       * @then https://github.com/org/repo を返す
       */
      @Test
      void 接続先がssh形式の場合_認証情報を除いたhttps形式の値を返す() {
        assertThat(
                new GitContext(Map.of(), NO_REPO)
                    .normalizeRemote("ssh://git@github.com/org/repo.git"))
            .isEqualTo("https://github.com/org/repo");
      }

      /**
       * URL の user:token@ を取り除き、https の URL を返すことを確かめる。 認証情報を仕様書の行リンクに出さない。
       *
       * @given 接続先が https://user:tok@github.com/org/repo.git である
       * @when normalizeRemote を実行する
       * @then https://github.com/org/repo を返す
       */
      @Test
      void 認証情報が含まれる場合_認証情報を除いた値を返す() {
        assertThat(
                new GitContext(Map.of(), NO_REPO)
                    .normalizeRemote("https://user:tok@github.com/org/repo.git"))
            .isEqualTo("https://github.com/org/repo");
      }

      /**
       * SSH 形式の origin を https の URL にし、末尾の .git を除くことを確かめる。 ブラウザで開ける URL にする。
       *
       * @given 値が "git@github.com:org/repo.git"
       * @when normalizeRemote を実行する
       * @then "https://github.com/org/repo" を返す
       */
      @Test
      void 値がSSH形式の場合_https形式で末尾のgitを除いた値を返す() {
        assertThat(new GitContext(Map.of(), NO_REPO).normalizeRemote("git@github.com:org/repo.git"))
            .isEqualTo("https://github.com/org/repo");
      }

      /**
       * 末尾に .git が無い URL はそのまま返すことを確かめる。 既に規定どおりの形式の URL を変えない。
       *
       * @given 値が "https://github.com/org/repo"
       * @when normalizeRemote を実行する
       * @then "https://github.com/org/repo" を返す
       */
      @Test
      void gitの接尾辞が無い場合_そのままの値を返す() {
        assertThat(new GitContext(Map.of(), NO_REPO).normalizeRemote("https://github.com/org/repo"))
            .isEqualTo("https://github.com/org/repo");
      }

      /**
       * https 形式の origin は末尾の .git だけを除くことを確かめる。 既に https の場合は形式を変えない。
       *
       * @given 値が "https://github.com/org/repo.git"
       * @when normalizeRemote を実行する
       * @then "https://github.com/org/repo" を返す
       */
      @Test
      void 値がhttps形式の場合_末尾のgitを除いた値を返す() {
        assertThat(
                new GitContext(Map.of(), NO_REPO)
                    .normalizeRemote("https://github.com/org/repo.git"))
            .isEqualTo("https://github.com/org/repo");
      }
    }
  }

  @Nested
  class runGit {

    @Nested
    class 正常系 {

      /**
       * git が実行でき、終了コードが 0 の場合は標準出力の先頭行を返すことを確かめる。 コミットハッシュの情報を取り出す。
       *
       * @given git の version を表示させる引数
       * @when runGit を実行する
       * @then 空でない文字列を含む結果を返す
       */
      @Test
      void コマンドの終了コードが0の場合_先頭行を返す() {
        assertThat(new GitContext(Map.of(), Path.of(".")).runGit("--version")).isPresent();
      }
    }

    @Nested
    class 異常系 {

      /**
       * 終了コードが 0 以外の場合は空を返すことを確かめる。 失敗した git の出力を情報として使わない。
       *
       * @given 存在しないサブコマンドの引数
       * @when runGit を実行する
       * @then 空を返す
       */
      @Test
      void コマンドの終了コードが0以外の場合_空を返す() {
        assertThat(new GitContext(Map.of(), Path.of(".")).runGit("no-such-subcommand-for-test"))
            .isEmpty();
      }

      /**
       * 実行中のスレッドが中断された場合は空を返し、中断の状態を戻すことを確かめる。 中断を握りつぶさず、呼び出し元に伝える。
       *
       * @given 出力の後で終了を待つ間に中断されたスレッドと、実行中のコマンド
       * @when runGit を実行する
       * @then 空を返し、スレッドの中断の状態を戻す
       */
      @Test
      void スレッドが中断された場合_空を返す() {
        try {
          Thread.currentThread().interrupt();
          GitContext git = new GitContext(Map.of(), Path.of("."), "sh");

          assertThat(git.runGit("-c", "echo x; exec sleep 5")).isEmpty();
          assertThat(Thread.currentThread().isInterrupted()).isTrue();
        } finally {
          Thread.interrupted();
        }
      }

      /**
       * 実行ディレクトリが存在しない場合は空を返すことを確かめる。 実行に失敗しても検査を止めない。
       *
       * @given 存在しない実行ディレクトリ
       * @when runGit を実行する
       * @then 空を返す
       */
      @Test
      void 実行ディレクトリが無い場合_空を返す() {
        assertThat(new GitContext(Map.of(), NO_REPO).runGit("--version")).isEmpty();
      }
    }
  }
}
