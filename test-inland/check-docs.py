#!/usr/bin/env python3
"""Check the Inland documentation repair without executing product namespaces.

Compare a named Git baseline with current runtime/test forms; inspect the folder
hierarchy and local file links. Owns no product process, data or build output.
"""
import ast
import json
from pathlib import Path
import re
import subprocess
import sys

ROOT = Path(__file__).resolve().parent.parent
BASELINE = sys.argv[1] if len(sys.argv) > 1 else '8494613'
RUNTIME = Path('src-inland/softland/inland')
JVM_TEST = Path('test-inland/softland/inland/test_runner.clj')
MAPS = [Path(p) for p in [
    'src-inland/README.md', 'src-inland/softland/README.md',
    'src-inland/softland/inland/README.md', 'resources/inland/README.md',
    'test-inland/README.md', 'test-inland/softland/README.md',
    'test-inland/softland/inland/README.md']]

# Read with both feature sets. Never require/evaluate a product namespace.
# Reader-generated ids and regex object identity differ between two reads;
# normalize those representations while preserving their executable content.
CLOJURE_CHECK = r'''
(require '[clojure.data.json :as json] '[clojure.walk :as walk]
         '[clojure.tools.reader :as reader] '[clojure.tools.reader.reader-types :as rt])
(def input (json/read-str (slurp *in*) :key-fn keyword))
(defn forms [source feature]
  (binding [reader/*read-eval* false reader/*data-readers* {'js #(tagged-literal 'js %)}]
    (with-open [reader (rt/indexing-push-back-reader source)]
      (loop [out []]
        (let [value (reader/read {:eof ::end :read-cond :allow :features #{feature}} reader)]
          (if (= ::end value) out (recur (conj out value))))))))
(def declarations #{'ns 'defn 'defn- 'e/defn})
(defn normalize [x]
  (walk/postwalk
    (fn [v]
      (cond
        (and (seq? v) (contains? declarations (first v)) (string? (nth v 2 nil)))
        (concat (take 2 v) (drop 3 v))
        (symbol? v) (symbol (clojure.string/replace (str v) #"__\d+(?=(?:__auto__|#)$)" "__GENERATED"))
        (instance? java.util.regex.Pattern v) [:reader/regex (.pattern v) (.flags v)]
        (instance? clojure.lang.TaggedLiteral v) [:reader/tag (.-tag v) (normalize (.-form v))]
        :else v)) x))
(def results
  (vec (for [{:keys [path before after]} input feature [:clj :cljs]]
    (let [a (forms before feature) b (forms after feature)
          ds (filter #(and (seq? %) (contains? declarations (first %))) b)
          missing (mapv #(str (second %)) (remove #(and (string? (nth % 2 nil)) (seq (nth % 2))) ds))]
      (when-not (= (normalize a) (normalize b))
        (throw (ex-info "Executable forms changed" {:path path :feature feature})))
      (when (seq missing) (throw (ex-info "Missing documentation" {:path path :feature feature :names missing})))
      {:path path :feature feature :namespaces (count (filter #(= 'ns (first %)) ds))
       :functions (count (remove #(= 'ns (first %)) ds))}))))
(println (json/write-str results))
'''


def before(path):
    """Read this allowed file at baseline from Git; do not inspect other content."""
    return subprocess.check_output(['git', 'show', f'{BASELINE}:{path}'], cwd=ROOT, text=True)


def python_tree(source):
    """Python source → AST without declaration docstrings; no module execution."""
    tree = ast.parse(source)
    for node in ast.walk(tree):
        if isinstance(node, (ast.Module, ast.FunctionDef, ast.AsyncFunctionDef, ast.ClassDef)):
            if node.body and isinstance(node.body[0], ast.Expr):
                value = node.body[0].value
                if isinstance(value, ast.Constant) and isinstance(value.value, str):
                    node.body.pop(0)
    return ast.dump(tree, include_attributes=False)


def main():
    """Assert coverage/equivalence/links and print a bounded receipt; fail on mismatch."""
    files = sorted(p.relative_to(ROOT) for p in (ROOT / RUNTIME).glob('*')
                   if p.suffix in {'.clj', '.cljc', '.cljs'}) + [JVM_TEST, Path('resources/inland/seed.edn')]
    payload = [{'path': str(p), 'before': before(p), 'after': (ROOT / p).read_text()} for p in files]
    result = subprocess.run(['clj', '-M:inland', '-e', CLOJURE_CHECK], cwd=ROOT,
                            input=json.dumps(payload), capture_output=True, text=True, check=True)
    rows = json.loads(result.stdout.strip().splitlines()[-1])
    counts = [r for r in rows if r['feature'] == 'cljs' and r['path'].startswith(str(RUNTIME))]
    assert len(counts) == 19
    assert sum(r['namespaces'] for r in counts) == 19
    assert sum(r['functions'] for r in counts) == 141
    assert next(r for r in rows if r['path'] == str(JVM_TEST))['functions'] == 6
    launcher = (ROOT / 'bin/inland').read_text()
    assert python_tree(before('bin/inland')) == python_tree(launcher), 'Launcher execution changed'
    py_functions = [n for n in ast.walk(ast.parse(launcher)) if isinstance(n, ast.FunctionDef)]
    assert len(py_functions) == 15 and all(ast.get_docstring(n) for n in py_functions)
    # Permit only added whole-line JSDoc; require all remaining bytes to match.
    browser = (ROOT / 'test-inland/browser.mjs').read_text()
    without_docs = re.sub(r'^/\*\*[^\n]*\*/\n', '', browser, flags=re.M)
    assert without_docs == before('test-inland/browser.mjs'), 'Browser driver execution changed'
    js_helpers = re.findall(r'^const (\w+)\s*=.*=>', browser, re.M)
    assert all(re.search(r'/\*\*[^\n]*\*/\nconst '+name+r'\s*=', browser) for name in js_helpers)
    links = 0
    documents = MAPS + [Path('src/app/client/README.md'), Path('docs/build-softland-in-softland/HANDOFF.md')]
    for document in documents:
        source = (ROOT / document).read_text()
        for target in re.findall(r'\[[^\]]*\]\(([^)]+)\)', source):
            if re.match(r'^[a-zA-Z][\w+.-]*:', target) or target.startswith('#'):
                continue
            target = target.split('#', 1)[0]
            assert (ROOT / document.parent / target).exists(), f'Broken link: {document} → {target}'
            links += 1
    for p in files:
        parent = ROOT / p.parent
        assert (parent / 'README.md').exists(), f'Missing containing map: {p}'
    # Every immediate runtime namespace is reachable from its containing map.
    runtime_map = (ROOT / RUNTIME / 'README.md').read_text()
    assert all(f']({p.name})' in runtime_map for p in files if p.parent == RUNTIME)
    print(f'PASS: 19 runtime namespace and 141 function contracts; 6 JVM helpers; {len(py_functions)} launcher functions; {len(js_helpers)} browser helpers')
    print(f'PASS: {len(files)} Clojure/CLJS/EDN files equivalent in both reader branches to {BASELINE}; Python AST and JavaScript body unchanged')
    print(f'PASS: 7 colocated maps, {links} local file links, and immediate runtime child coverage')
    print('No product namespace, browser, cluster, or provider was executed.')


if __name__ == '__main__':
    try:
        main()
    except subprocess.CalledProcessError as error:
        print(error.stderr or str(error), file=sys.stderr)
        raise SystemExit(1)
