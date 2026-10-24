// CI の検査結果を、GitHub Actions の画面で読みやすい形にして出す。依存ライブラリは使わない（Node.js 18 以上）。
//
// mvn verify が出したレポートを読み、次の3つを出力する。mvn の成否は変えない（成否は mvn のステップが決める）。
//   1. 違反箇所の注釈（::error / ::warning）。実行画面の上部と、PR の Files changed の該当行に出る
//   2. Job Summary（実行画面の Summary）。検査ごとの結果、UT のクラス別結果、カバレッジのファイル別表
//   3. ログ末尾の要約。UT の結果と、jest の text レポートに似たカバレッジ表
//
// 使い方（リポジトリのルートで実行する）: node .github/scripts/ci-report.mjs <mvn のログファイル>
//
// 読むファイル（いずれも mvn clean verify が target に出す。無ければ「未実行」にする）:
//   surefire-reports/TEST-*.xml, site/jacoco/jacoco.xml, checkstyle-result.xml, pmd.xml, cpd.xml, spotbugsXml.xml
//   spotless はレポートを出さないため、mvn のログから違反ファイルを読む

import fs from 'node:fs';

const WORKSPACE = (process.env.GITHUB_WORKSPACE || process.cwd()).replace(/\\/g, '/').replace(/\/$/, '');
const MAX_ANNOTATIONS = 10; // 1つの検査が出す注釈の上限（GitHub は1ステップ10件・1ジョブ50件までしか表示しない）
const MAX_ROWS = 100; // Summary の1つの表に載せる行数の上限
const logPath = process.argv[2];
const log = logPath && fs.existsSync(logPath) ? fs.readFileSync(logPath, 'utf8') : '';

// ---- 小さな XML パーサ（レポートは整形式のため、木構造にできれば十分） ----

const decode = (s) =>
  s
    .replace(/&lt;/g, '<')
    .replace(/&gt;/g, '>')
    .replace(/&quot;/g, '"')
    .replace(/&apos;/g, "'")
    .replace(/&#(\d+);/g, (_, n) => String.fromCodePoint(Number(n)))
    .replace(/&#x([0-9a-f]+);/gi, (_, n) => String.fromCodePoint(parseInt(n, 16)))
    .replace(/&amp;/g, '&');

function parseXml(src) {
  const root = { name: '#root', attrs: {}, children: [], text: '' };
  const stack = [root];
  const token =
    /<!--[\s\S]*?-->|<\?[\s\S]*?\?>|<!DOCTYPE[^>]*>|<!\[CDATA\[([\s\S]*?)\]\]>|<(\/?)([^\s/>]+)((?:\s+[^\s=/>]+\s*=\s*(?:"[^"]*"|'[^']*'))*)\s*(\/?)>|([^<]+)/g;
  for (let m; (m = token.exec(src)); ) {
    const top = stack[stack.length - 1];
    if (m[1] !== undefined) {
      top.text += m[1];
    } else if (m[3]) {
      if (m[2]) {
        stack.pop();
      } else {
        const attrs = {};
        for (const a of m[4].matchAll(/([^\s=]+)\s*=\s*(?:"([^"]*)"|'([^']*)')/g)) {
          attrs[a[1]] = decode(a[2] ?? a[3]);
        }
        const node = { name: m[3], attrs, children: [], text: '' };
        top.children.push(node);
        if (!m[5]) stack.push(node);
      }
    } else if (m[6]) {
      top.text += decode(m[6]);
    }
  }
  return root;
}

const kids = (node, name) => node.children.filter((c) => c.name === name);
const all = (node, name) => node.children.flatMap((c) => (c.name === name ? [c] : []).concat(all(c, name)));
const load = (file) => (fs.existsSync(file) ? parseXml(fs.readFileSync(file, 'utf8')) : null);

// ---- 出力の道具 ----

const rel = (p) => {
  const n = p.replace(/\\/g, '/');
  return n.startsWith(`${WORKSPACE}/`) ? n.slice(WORKSPACE.length + 1) : n;
};
const escData = (s) => String(s).replace(/%/g, '%25').replace(/\r/g, '%0D').replace(/\n/g, '%0A');
const escProp = (s) => escData(s).replace(/:/g, '%3A').replace(/,/g, '%2C');
const cell = (s) => String(s ?? '').replace(/\|/g, '\\|').replace(/\r?\n/g, ' ').replace(/</g, '&lt;');

function annotate(level, { file, line, col, title }, message) {
  const props = [];
  if (file) props.push(`file=${escProp(file)}`);
  if (file && line) props.push(`line=${line}`);
  if (file && line && col) props.push(`col=${col}`);
  if (title) props.push(`title=${escProp(title)}`);
  console.log(`::${level} ${props.join(',')}::${escData(message)}`);
}

const pct = (covered, missed) => {
  const total = covered + missed;
  return total === 0 ? 100 : Math.floor((covered / total) * 1000) / 10; // 切り上がって 100 に見えないよう切り捨てる
};
const fmtPct = (v) => (Number.isInteger(v) ? String(v) : v.toFixed(1));

const summary = []; // Job Summary（Markdown）
const overview = []; // 検査ごとの結果の一覧。{ name, state: 'ok' | 'ng' | 'skip', detail }
const STATE = { ok: '✅ OK', ng: '❌ NG', skip: '⏭️ 未実行' };

// 静的解析の違反の一覧を、注釈と Summary の表にする
function reportViolations(name, items, hint) {
  if (items === null) {
    overview.push({ name, state: 'skip', detail: '前段の検査の失敗などで実行されていません' });
    return;
  }
  overview.push({ name, state: items.length ? 'ng' : 'ok', detail: items.length ? `${items.length} 件の違反` : '違反なし' });
  items.slice(0, MAX_ANNOTATIONS).forEach((v) => annotate('error', { file: v.file, line: v.line, col: v.col, title: `${name}: ${v.rule}` }, v.msg));
  if (!items.length) return;
  summary.push(`### ❌ ${name}（${items.length} 件）`, '');
  if (hint) summary.push(hint, '');
  summary.push('| 場所 | ルール | 内容 |', '| :-- | :-- | :-- |');
  items.slice(0, MAX_ROWS).forEach((v) => summary.push(`| \`${cell(v.file)}${v.line ? `:${v.line}` : ''}\` | ${cell(v.rule)} | ${cell(v.msg)} |`));
  if (items.length > MAX_ROWS) summary.push(`| … | | 他 ${items.length - MAX_ROWS} 件（ログと注釈を参照） |`);
  summary.push('');
}

// ---- UT（surefire） ----

function reportUnitTests() {
  const dir = 'target/surefire-reports';
  const files = fs.existsSync(dir) ? fs.readdirSync(dir).filter((f) => /^TEST-.*\.xml$/.test(f)) : [];
  if (!files.length) {
    overview.push({ name: 'UT（surefire）', state: 'skip', detail: 'テスト結果がありません（コンパイルの失敗など）' });
    return;
  }
  const byClass = new Map(); // @Nested のクラス（Foo$bar$正常系）は外側のクラスにまとめる
  const failures = [];
  for (const f of files) {
    const suite = load(`${dir}/${f}`).children[0];
    const top = suite.attrs.name.split('$')[0];
    const row = byClass.get(top) ?? { tests: 0, failed: 0, skipped: 0, time: 0 };
    row.tests += Number(suite.attrs.tests);
    row.failed += Number(suite.attrs.failures) + Number(suite.attrs.errors);
    row.skipped += Number(suite.attrs.skipped);
    row.time += Number(suite.attrs.time);
    byClass.set(top, row);
    for (const tc of kids(suite, 'testcase')) {
      const bad = kids(tc, 'failure')[0] ?? kids(tc, 'error')[0];
      if (bad) failures.push({ top, name: [...tc.attrs.classname.split('$').slice(1), tc.attrs.name].join(' › '), bad });
    }
  }
  const sum = (key) => [...byClass.values()].reduce((a, r) => a + r[key], 0);
  const [tests, failed, skipped] = [sum('tests'), sum('failed'), sum('skipped')];
  overview.push({
    name: 'UT（surefire）',
    state: failed ? 'ng' : 'ok',
    detail: `${tests} 件中 成功 ${tests - failed - skipped} / 失敗 ${failed} / スキップ ${skipped}（${byClass.size} クラス）`,
  });

  failures.forEach((f, i) => {
    const simple = f.top.split('.').pop();
    const testFile = `src/test/java/${f.top.replace(/\./g, '/')}.java`;
    const line = (f.bad.text.match(new RegExp(`\\(${simple}\\.java:(\\d+)\\)`)) ?? [])[1];
    const msg = (f.bad.attrs.message ?? f.bad.attrs.type ?? '').split('\n')[0];
    if (i < MAX_ANNOTATIONS) annotate('error', { file: testFile, line, title: `UT 失敗: ${f.name}` }, msg);
    f.testFile = testFile;
  });

  if (failures.length) {
    summary.push(`### ❌ UT の失敗（${failures.length} 件）`, '');
    failures.slice(0, MAX_ROWS).forEach((f) => {
      const stack = f.bad.text.split('\n').slice(0, 15).join('\n');
      summary.push(`<details><summary><code>${cell(f.top.split('.').pop())}</code> ${cell(f.name)}</summary>`, '', '```', stack, '```', '', '</details>', '');
    });
  }
  summary.push(`### UT の結果（${tests} 件 / ${byClass.size} クラス）`, '');
  const rows = [...byClass.entries()].sort(([a], [b]) => a.localeCompare(b));
  const table = ['| テストクラス | 件数 | 失敗 | スキップ | 時間(秒) |', '| :-- | --: | --: | --: | --: |'];
  rows.forEach(([name, r]) => table.push(`| ${r.failed ? '❌' : '✅'} \`${name.split('.').pop()}\` | ${r.tests} | ${r.failed} | ${r.skipped} | ${r.time.toFixed(2)} |`));
  summary.push('<details><summary>クラス別の結果を表示</summary>', '', ...table, '', '</details>', '');
  console.log(`UT: ${tests} 件 / 失敗 ${failed} / スキップ ${skipped}（${byClass.size} クラス）`);
}

// ---- 整形（spotless）: レポートが無いため mvn のログから読む ----

function reportSpotless() {
  const name = '整形（spotless）';
  if (!/--- spotless-maven-plugin:[\d.]+:check/.test(log)) return reportViolations(name, null);
  const lines = log.split(/\r?\n/).map((l) => l.replace(/^\[(ERROR|WARNING|INFO)\]\s?/, ''));
  const start = lines.findIndex((l) => l.includes('had format violations'));
  const items = [];
  if (start >= 0) {
    for (let i = start + 1; i < lines.length && !lines[i].startsWith('Run '); i++) {
      const m = lines[i].match(/^ {2,6}([^\s@+-].*\.java)\s*$/);
      if (!m) continue;
      const hunk = lines.slice(i + 1, i + 4).find((l) => /@@ -\d+/.test(l));
      items.push({ file: rel(m[1]), line: hunk ? Number(hunk.match(/@@ -(\d+)/)[1]) : undefined, rule: 'google-java-format', msg: '書式が google-java-format と異なります' });
    }
  }
  reportViolations(name, items, '修正は `mvn spotless:apply` を実行してコミットする。');
}

// ---- LINT・バグ検出（checkstyle / PMD / CPD / SpotBugs） ----

function reportCheckstyle() {
  const xml = load('target/checkstyle-result.xml');
  const items = xml && all(xml, 'file').flatMap((f) =>
    kids(f, 'error').map((e) => ({
      file: rel(f.attrs.name),
      line: Number(e.attrs.line),
      col: Number(e.attrs.column) || undefined,
      rule: (e.attrs.source ?? '').split('.').pop().replace(/Check$/, ''),
      msg: e.attrs.message,
    })));
  reportViolations('LINT（checkstyle）', items);
}

function reportPmd() {
  const xml = load('target/pmd.xml');
  const items = xml && all(xml, 'file').flatMap((f) =>
    kids(f, 'violation').map((v) => ({
      file: rel(f.attrs.name),
      line: Number(v.attrs.beginline),
      col: Number(v.attrs.begincolumn) || undefined,
      rule: v.attrs.rule,
      msg: v.text.trim(),
    })));
  reportViolations('LINT（PMD）', items);
}

function reportCpd() {
  const xml = load('target/cpd.xml');
  const items = xml && all(xml, 'duplication').flatMap((d) => {
    const files = kids(d, 'file');
    return files.map((f, i) => ({
      file: rel(f.attrs.path),
      line: Number(f.attrs.line),
      rule: `${d.attrs.lines} 行 / ${d.attrs.tokens} トークン`,
      msg: `重複コード。同じ内容: ${files.filter((_, j) => j !== i).map((o) => `${rel(o.attrs.path)}:${o.attrs.line}`).join(', ')}`,
    }));
  });
  reportViolations('LINT（CPD: コード重複）', items);
}

function reportSpotBugs() {
  const xml = load('target/spotbugsXml.xml');
  const items = xml && all(xml.children[0], 'BugInstance').map((b) => {
    const src = kids(b, 'SourceLine').find((s) => s.attrs.start) ?? all(b, 'SourceLine').find((s) => s.attrs.start);
    return {
      file: src ? `src/main/java/${src.attrs.sourcepath}` : undefined,
      line: src ? Number(src.attrs.start) : undefined,
      rule: `${b.attrs.category}/${b.attrs.type}`,
      msg: (kids(b, 'LongMessage')[0]?.text ?? b.attrs.type).trim(),
    };
  });
  reportViolations('バグ検出（SpotBugs）', items);
}

// ---- カバレッジ（JaCoCo） ----

const ranges = (nums) => {
  const out = [];
  for (let i = 0; i < nums.length; ) {
    let j = i;
    while (j + 1 < nums.length && nums[j + 1] === nums[j] + 1) j++;
    out.push(i === j ? `${nums[i]}` : `${nums[i]}-${nums[j]}`);
    i = j + 1;
  }
  return out;
};

function reportCoverage() {
  const name = 'カバレッジ（JaCoCo）';
  const xml = load('target/site/jacoco/jacoco.xml');
  if (!xml) return overview.push({ name, state: 'skip', detail: 'レポートがありません（UT の失敗など）' });
  const report = xml.children.find((c) => c.name === 'report');
  const counter = (node, type) => {
    const c = kids(node, 'counter').find((k) => k.attrs.type === type);
    return { covered: Number(c?.attrs.covered ?? 0), missed: Number(c?.attrs.missed ?? 0) };
  };
  const row = (node) => ({
    instr: pct(counter(node, 'INSTRUCTION').covered, counter(node, 'INSTRUCTION').missed),
    branch: pct(counter(node, 'BRANCH').covered, counter(node, 'BRANCH').missed),
    line: pct(counter(node, 'LINE').covered, counter(node, 'LINE').missed),
    method: pct(counter(node, 'METHOD').covered, counter(node, 'METHOD').missed),
  });
  const total = row(report);
  const files = kids(report, 'package').flatMap((p) =>
    kids(p, 'sourcefile').map((s) => {
      const uncovered = kids(s, 'line').filter((l) => Number(l.attrs.mi) > 0 || Number(l.attrs.mb) > 0).map((l) => Number(l.attrs.nr));
      return { path: `${p.attrs.name}/${s.attrs.name}`, ...row(s), uncovered };
    }));
  files.sort((a, b) => a.path.localeCompare(b.path));
  const lacking = files.filter((f) => f.instr < 100 || f.branch < 100);

  const ok = total.instr === 100 && total.branch === 100;
  overview.push({ name, state: ok ? 'ok' : 'ng', detail: `命令 ${fmtPct(total.instr)}% / 分岐 ${fmtPct(total.branch)}%（要求は両方 100%）` });
  lacking.slice(0, MAX_ANNOTATIONS).forEach((f) =>
    annotate('error', { file: `src/main/java/${f.path}`, line: f.uncovered[0], title: 'カバレッジ不足' }, `命令 ${fmtPct(f.instr)}% / 分岐 ${fmtPct(f.branch)}%。未カバーの行: ${ranges(f.uncovered).join(', ')}`));

  const mdRow = (label, r, unc) => `| ${label} | ${fmtPct(r.instr)} | ${fmtPct(r.branch)} | ${fmtPct(r.line)} | ${fmtPct(r.method)} | ${unc} |`;
  const head = ['| ファイル | 命令 % | 分岐 % | 行 % | メソッド % | 未カバーの行 |', '| :-- | --: | --: | --: | --: | :-- |'];
  summary.push(`### ${ok ? '✅' : '❌'} カバレッジ（全体: 命令 ${fmtPct(total.instr)}% / 分岐 ${fmtPct(total.branch)}%）`, '');
  if (lacking.length) {
    summary.push(`100% に届いていないファイル（${lacking.length} 件）`, '', ...head);
    lacking.slice(0, MAX_ROWS).forEach((f) => summary.push(mdRow(`⚠️ \`${cell(f.path)}\``, f, `\`${ranges(f.uncovered).join(', ')}\``)));
    summary.push('');
  }
  summary.push(`<details><summary>全ファイルのカバレッジ（${files.length} 件）を表示</summary>`, '', ...head);
  files.forEach((f) => summary.push(mdRow(`\`${cell(f.path)}\``, f, f.uncovered.length ? `\`${ranges(f.uncovered).join(', ')}\`` : '')));
  summary.push('', '</details>', '');

  // ログ末尾の要約（jest の text レポートに似せる）
  const width = Math.min(70, Math.max(10, ...files.map((f) => f.path.length)));
  const clip = (s) => (s.length > width ? `…${s.slice(s.length - width + 1)}` : s.padEnd(width));
  const line = (a, ...n) => `${a} | ${n.map((v) => String(v).padStart(8)).join(' | ')} |`;
  const bar = `${'-'.repeat(width)}-|${'-'.repeat(10)}|${'-'.repeat(10)}|${'-'.repeat(10)}|${'-'.repeat(10)}|-------------------`;
  console.log('::group::カバレッジ（ファイル別）');
  console.log(bar);
  console.log(`${'File'.padEnd(width)} | ${['% Instr', '% Branch', '% Lines', '% Funcs'].map((h) => h.padStart(8)).join(' | ')} | Uncovered Line #s`);
  console.log(bar);
  console.log(`${line('All files'.padEnd(width), fmtPct(total.instr), fmtPct(total.branch), fmtPct(total.line), fmtPct(total.method))}`);
  files.forEach((f) => console.log(`${line(clip(f.path), fmtPct(f.instr), fmtPct(f.branch), fmtPct(f.line), fmtPct(f.method))} ${ranges(f.uncovered).join(',')}`));
  console.log(bar);
  console.log('::endgroup::');
  console.log(`カバレッジ: 命令 ${fmtPct(total.instr)}% / 分岐 ${fmtPct(total.branch)}% / 100% 未満のファイル ${lacking.length} 件`);
}

// ---- コンパイルエラーなど、上のどれにも当たらない失敗 ----

function reportOtherErrors() {
  if (!log) return;
  const errors = log.split(/\r?\n/).filter((l) => l.startsWith('[ERROR]'));
  if (!errors.length) return;
  for (const l of errors) {
    const m = l.match(/^\[ERROR\] (.+\.java):\[(\d+),(\d+)\] (.*)$/); // javac のエラー
    if (m) annotate('error', { file: rel(m[1]), line: m[2], col: m[3], title: 'コンパイルエラー' }, m[4]);
  }
  const hasKnown = overview.some((o) => o.state === 'ng');
  if (hasKnown) return; // 検査の違反は上で出しているため、重複して載せない
  summary.push('### ❌ ビルドのエラー', '', '```', ...errors.slice(0, 40).map((l) => l.replace(/^\[ERROR\]\s?/, '')), '```', '');
  overview.push({ name: 'ビルド', state: 'ng', detail: 'エラーの詳細は Summary とログを参照（UT 仕様の検査の違反もここに出る）' });
}

// ---- 実行 ----

reportUnitTests();
reportSpotless();
reportCheckstyle();
reportPmd();
reportCpd();
reportCoverage();
reportSpotBugs();
reportOtherErrors();

const failedAny = overview.some((o) => o.state === 'ng');
const head = [
  `## ${failedAny ? '❌' : '✅'} CI 検査結果`,
  '',
  '| 検査 | 結果 | 内容 |',
  '| :-- | :-- | :-- |',
  ...overview.map((o) => `| ${o.name} | ${STATE[o.state]} | ${cell(o.detail)} |`),
  '',
];
console.log('\n=== CI 検査結果 ===');
overview.forEach((o) => console.log(`${STATE[o.state]}  ${o.name}: ${o.detail}`));

if (process.env.GITHUB_STEP_SUMMARY) {
  fs.appendFileSync(process.env.GITHUB_STEP_SUMMARY, [...head, ...summary].join('\n') + '\n');
}
