package copel.sesproductpackage.tools.utspec;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * リポジトリの名前・コミット・URL を、GitHub Actions の環境変数または git コマンドから求める。
 *
 * <p>GitHub Actions の環境変数（GITHUB_SERVER_URL、GITHUB_REPOSITORY、GITHUB_SHA）を優先する。 無い場合は git
 * コマンドで求め、それも取れない場合は null を返す。null の場合は GitHub の行リンクを作らない。
 *
 * @author Copel Co., Ltd.
 */
public class GitContext {

  /** GitHub Actions が設定するサーバーの URL。 */
  private static final String ENV_SERVER_URL = "GITHUB_SERVER_URL";

  /** GitHub Actions が設定するリポジトリ名（owner/repo）。 */
  private static final String ENV_REPOSITORY = "GITHUB_REPOSITORY";

  /** GitHub Actions が設定するコミットハッシュ。 */
  private static final String ENV_SHA = "GITHUB_SHA";

  /** ssh の URL の接頭辞。 */
  private static final String SSH_SCHEME = "ssh://";

  /** scp 形式の URL の接頭辞（git@host:path）。 */
  private static final String SCP_USER_PREFIX = "git@";

  /** https の URL の接頭辞。 */
  private static final String HTTPS_SCHEME = "https://";

  /** リポジトリの URL の末尾に付く接尾辞。 */
  private static final String GIT_SUFFIX = ".git";

  /** 既定の git の実行ファイル名。 */
  private static final String DEFAULT_EXECUTABLE = "git";

  /** 環境変数の一覧。テストで差し替えられるように外から渡す。 */
  private final Map<String, String> env;

  /** git コマンドを実行するディレクトリ（リポジトリ直下）。 */
  private final Path basedir;

  /** 実行するコマンドの名前。通常は git。 */
  private final String executable;

  /**
   * 環境変数の一覧と実行ディレクトリを指定して生成する。実行するコマンドは git とする。
   *
   * @param env 環境変数の一覧（通常は System.getenv()）
   * @param basedir git コマンドを実行するディレクトリ
   */
  public GitContext(Map<String, String> env, Path basedir) {
    this(env, basedir, DEFAULT_EXECUTABLE);
  }

  /**
   * 環境変数の一覧、実行ディレクトリ、実行するコマンドの名前を指定して生成する。
   *
   * <p>コマンドの名前を差し替えられるのは、中断などの振る舞いをテストで確かめるためである。
   *
   * @param env 環境変数の一覧（通常は System.getenv()）
   * @param basedir コマンドを実行するディレクトリ
   * @param executable 実行するコマンドの名前（通常は git）
   */
  public GitContext(Map<String, String> env, Path basedir, String executable) {
    this.env = Map.copyOf(env);
    this.basedir = basedir;
    this.executable = executable;
  }

  /**
   * リポジトリ名を返す。GITHUB_REPOSITORY の末尾、無ければ基準ディレクトリの名前。
   *
   * @return リポジトリ名
   */
  public String repositoryName() {
    String repository = env.get(ENV_REPOSITORY);
    if (repository != null && !repository.isBlank()) {
      return repository.substring(repository.lastIndexOf('/') + 1);
    }
    return Optional.ofNullable(basedir.toAbsolutePath().normalize().getFileName())
        .map(Path::toString)
        .orElse("");
  }

  /**
   * コミットハッシュを返す。GITHUB_SHA、無ければ git rev-parse HEAD の結果。取れない場合は null。
   *
   * @return コミットハッシュ。取れない場合は null
   */
  public String commitHash() {
    String sha = env.get(ENV_SHA);
    if (sha != null && !sha.isBlank()) {
      return sha;
    }
    return runGit("rev-parse", "HEAD").orElse(null);
  }

  /**
   * 基準ディレクトリのリポジトリ直下からの接頭辞を返す。GitHub の行リンクのパスに付けるため。
   *
   * <p>リポジトリの直下で実行した場合は空文字になる。サブディレクトリのリポジトリでは「tools/foo/」の形になる。
   *
   * @return リポジトリ直下からの接頭辞（末尾に / を付ける）。取得できない場合は空文字
   */
  public String pathPrefix() {
    return runGit("rev-parse", "--show-prefix").orElse("");
  }

  /**
   * GitHub のリポジトリの URL を返す。GITHUB_SERVER_URL と GITHUB_REPOSITORY、無ければ origin の URL から求める。
   *
   * @return リポジトリの URL（末尾に / は付けない）。取れない場合は null
   */
  public String repositoryUrl() {
    String server = env.get(ENV_SERVER_URL);
    String repository = env.get(ENV_REPOSITORY);
    if (server != null && !server.isBlank() && repository != null && !repository.isBlank()) {
      return server + "/" + repository;
    }
    return runGit("config", "--get", "remote.origin.url").map(this::normalizeRemote).orElse(null);
  }

  /**
   * origin の URL を、ブラウザで開ける https の URL に変換する。
   *
   * <p>{@code git@host:owner/repo.git} と {@code https://host/owner/repo.git} を対象とし、末尾の .git を除く。
   *
   * @param remote origin の URL
   * @return https の URL
   */
  public String normalizeRemote(String remote) {
    String url = remote.trim();
    if (url.startsWith(SSH_SCHEME)) {
      url = HTTPS_SCHEME + url.substring(SSH_SCHEME.length());
    }
    if (url.startsWith(SCP_USER_PREFIX)) {
      url = HTTPS_SCHEME + url.substring(SCP_USER_PREFIX.length()).replaceFirst(":", "/");
    }
    // 認証情報（user:token@ や git@）を URL から取り除く。ブラウザで開く URL に含めないため
    url = url.replaceFirst("^(https?://)[^/@]+@", "$1");
    if (url.endsWith(GIT_SUFFIX)) {
      url = url.substring(0, url.length() - GIT_SUFFIX.length());
    }
    return url;
  }

  /**
   * コマンドを基準ディレクトリで実行し、標準出力の先頭行を返す。失敗した場合は空。
   *
   * @param args コマンドに渡す引数
   * @return 標準出力の先頭行。コマンドが無い、終了コードが0以外の場合は空
   */
  public Optional<String> runGit(String... args) {
    List<String> command = new ArrayList<>();
    command.add(executable);
    command.addAll(List.of(args));
    ProcessBuilder builder =
        new ProcessBuilder(command)
            .directory(basedir.toFile())
            .redirectError(ProcessBuilder.Redirect.DISCARD);
    try {
      return finish(builder.start());
    } catch (IOException e) {
      return Optional.empty();
    }
  }

  /**
   * 起動したプロセスの先頭行を読み、終了を待つ。待機中に中断された場合はプロセスを止める。
   *
   * @param process 起動済みのプロセス
   * @return 標準出力の先頭行。終了コードが0以外の場合は空
   * @throws IOException 標準出力を読めない場合
   */
  public Optional<String> finish(Process process) throws IOException {
    Optional<String> firstLine;
    try (BufferedReader reader =
        new BufferedReader(
            new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
      firstLine = reader.lines().findFirst().map(String::trim);
    }
    try {
      return process.waitFor() == 0 ? firstLine : Optional.empty();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      process.destroyForcibly();
      return Optional.empty();
    }
  }
}
