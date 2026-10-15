(ns softland.runtime.page
  "The one page of the runtime: Google's sign-in button for someone who is
  not signed in, and, once she is, who she is and small forms for what she
  does. Pure code: it takes values and gives a text of HTML. It reads no
  clock, takes no random source and calls nothing.

  Plain HTML and a few lines of JavaScript, with no build tool and no
  framework (the build's page; BUILD.md, Part 3). The page loads Google
  Identity Services, `https://accounts.google.com/gsi/client`, and calls
  `google.accounts.id.initialize({client_id, callback})` and
  `google.accounts.id.renderButton` (Sid's decision 4 of 15 October 2026:
  Google's button, with no client secret). The client id is put in a data
  attribute of the button's element and read from there, so no value is
  written into the script.

  Google's callback gives the page a credential, the ID token. The page then
  makes sure a `g_csrf_token` cookie exists, and sets a random one when
  Google has not, and posts a form to `/sign-in` with `credential`,
  `g_csrf_token` (the cookie's value) and `invitation`, read from the
  page's fragment, `#invitation=<code>`. The code is in the fragment and
  not the query because a browser never sends a fragment, while the page,
  served over http with `Referrer-Policy: no-referrer-when-downgrade`
  (which Google asks for), would send its whole URL, query and all, as the
  Referer of each request to Google's https script and button. The
  runtime refuses a sign-in whose cookie and body
  differ or lack the token (BUILD.md, \"For the builder\"). The form is
  posted by the browser itself, not by `fetch`, so the browser follows the
  runtime's answer, takes its cookie and shows the page again. Signed in,
  the same button posts to `/link` and links a second Google account to
  her (BUILD.md, default 3: a second account is linked only while she is
  signed in).

  Signed in, the page shows her id, kind and name, and forms that post to
  the runtime's routes as `application/x-www-form-urlencoded`: her name; an
  agent or a program made (name, description); a right given in her own
  layer (to whom, which keys, as patterns, `:*` by default); a token made,
  shown once on the page that answers; the token requests that wait for her,
  each with its approve button; a token ended; a note written into her
  layer; her layer read back; sign out. What a form did is shown at the top
  of the page that answers, as the runtime's answer in EDN. The layout and
  the words on the page are a session's (builder F), plain on purpose.

  Inputs and results, by call:

    render   {:client-id :me :pending :said :error} -> a text of HTML
    escape   anything -> its printed text with & < > \" ' escaped

  What `render` takes:

    :client-id  the Google client id, a text
    :me         nil when no one is signed in; else what the runtime said of
                her (`softland.runtime.http`): :id, :kind, :name, :session,
                :own-layer, and whatever else it holds, shown as EDN
    :pending    the token requests that wait for her: a sequence of maps,
                each with :request, its id; shown as EDN
    :said       nil, or {:what text :answer value}: what the last form did
    :shown      nil, or {:what text :text text}: a text shown once, such as
                a token

  State and effects. None.

  Callers. `softland.runtime.http`, for `GET /` and for the answer to each
  form that the page posts."
  (:require [clojure.pprint :as pprint]
            [clojure.string :as str]))

(defn escape
  "Takes anything. Gives its text (`str`, and \"\" for nil) with `&`, `<`,
  `>`, `\"` and `'` written as HTML entities, so that it is safe inside an
  element and inside a quoted attribute."
  [x]
  (-> (if (nil? x) "" (str x))
      (str/replace "&" "&amp;")
      (str/replace "<" "&lt;")
      (str/replace ">" "&gt;")
      (str/replace "\"" "&quot;")
      (str/replace "'" "&#39;")))

(defn- edn-text
  "Takes a value. Gives it pretty-printed as EDN, with no length limit."
  [v]
  (binding [*print-length* nil *print-level* nil *print-meta* false]
    (str/trimr (with-out-str (pprint/pprint v)))))

(def ^:private style
  "The page's few rules of style: legible, and no more."
  "body{font-family:system-ui,sans-serif;max-width:46rem;margin:1.5rem auto;padding:0 1rem;line-height:1.4}
section{border-top:1px solid #ccc;padding:.6rem 0}
pre{background:#f4f4f4;padding:.5rem;overflow-x:auto;white-space:pre-wrap;word-break:break-all}
label{display:block;margin:.3rem 0}
input[type=text],textarea{width:100%;box-sizing:border-box;font:inherit}
.said{border:1px solid #888;padding:.5rem;margin-bottom:1rem}
.once{border:2px solid #b60;padding:.5rem;margin-bottom:1rem}")

(def ^:private script
  "The page's script: the `g_csrf_token` cookie, Google's callback, and the
  button. The callback posts a form, to the action that the button's
  element names, with the credential, the cookie's value and the
  invitation's code from the fragment (`#invitation=<code>`)."
  "function csrfToken() {
  var m = document.cookie.match(/(?:^|;\\s*)g_csrf_token=([^;]+)/);
  if (m) return m[1];
  var a = new Uint8Array(16); crypto.getRandomValues(a);
  var v = Array.prototype.map.call(a, function (b) { return ('0' + b.toString(16)).slice(-2); }).join('');
  document.cookie = 'g_csrf_token=' + v + '; Path=/; SameSite=Lax';
  return v;
}
function onCredential(response) {
  var el = document.getElementById('google-button');
  var f = document.createElement('form');
  f.method = 'POST'; f.action = el.dataset.action;
  var fields = [['credential', response.credential],
                ['g_csrf_token', csrfToken()],
                ['invitation', new URLSearchParams(location.hash.slice(1)).get('invitation') || '']];
  fields.forEach(function (kv) {
    var i = document.createElement('input');
    i.type = 'hidden'; i.name = kv[0]; i.value = kv[1]; f.appendChild(i);
  });
  document.body.appendChild(f); f.submit();
}
window.onload = function () {
  var el = document.getElementById('google-button');
  google.accounts.id.initialize({client_id: el.dataset.clientId, callback: onCredential});
  google.accounts.id.renderButton(el, {type: 'standard', theme: 'outline', size: 'large'});
};")

(defn- form
  "Takes a route, a button's label and the inner HTML of the fields. Gives
  a form that posts to the route."
  [action label & fields]
  (str "<form method=\"post\" action=\"" (escape action) "\">"
       (apply str fields)
       "<button type=\"submit\">" (escape label) "</button></form>"))

(defn- field
  "Takes a field's name, its label and its default text. Gives a labelled
  one-line text field."
  [nm label default]
  (str "<label>" (escape label) " <input type=\"text\" name=\"" (escape nm)
       "\" value=\"" (escape default) "\"></label>"))

(defn- hidden [nm v]
  (str "<input type=\"hidden\" name=\"" (escape nm) "\" value=\"" (escape v) "\">"))

(defn- google-button
  "Takes the client id and the route that the credential goes to. Gives the
  element that Google's button is rendered into."
  [client-id action]
  (str "<div id=\"google-button\" data-client-id=\"" (escape client-id)
       "\" data-action=\"" (escape action) "\"></div>"))

(defn- said-box
  "Takes :said and :shown. Gives the boxes at the top of the page: a text
  shown once, then what the last form did."
  [said shown]
  (str (when shown
         (str "<div class=\"once\"><strong>" (escape (:what shown))
              "</strong> Copy it now: it is shown this once and kept nowhere in readable form.<pre>"
              (escape (:text shown)) "</pre></div>"))
       (when said
         (str "<div class=\"said\"><strong>" (escape (:what said)) "</strong><pre>"
              (escape (edn-text (:answer said))) "</pre></div>"))))

(defn- signed-out
  "The page for someone who is not signed in: Google's button."
  [client-id]
  (str "<p>Not signed in. A first sign-in needs an invitation: open the link"
       " that you were given, then sign in with Google.</p>"
       (google-button client-id "/sign-in")))

(defn- signed-in
  "The page for a signed-in person: who she is, and the forms."
  [client-id me pending]
  (str
   "<section><h2>Who you are</h2><pre>" (escape (edn-text me)) "</pre>"
   (form "/sign-out" "Sign out")
   "</section>"

   "<section><h2>Your name</h2>"
   (form "/name" "Change my name" (field "name" "New name (a-z, 0-9, - and _, first a letter)" (:name me)))
   "</section>"

   "<section><h2>Make an agent or a program</h2>"
   (form "/agent" "Make it"
         "<label>Kind <select name=\"kind\"><option value=\"agent\">agent</option>"
         "<option value=\"program\">program</option></select></label>"
         (field "name" "Name" "")
         "<label>Description (its instructions)<textarea name=\"description\" rows=\"3\"></textarea></label>")
   "</section>"

   "<section><h2>Give a right in your own layer</h2>"
   (form "/right/give" "Give"
         (field "to" "To (an agent's or a program's id)" "")
         (field "allows" "Keys it may write: patterns, by spaces (:* every plain key, :note/* a namespace, :note/text one key), or see; blank is :*" ":*"))
   (form "/right/end" "End the right"
         (field "of" "Of (whose right in your layer ends)" ""))
   "</section>"

   "<section><h2>Tokens</h2>"
   (form "/agent/token" "Make its token"
         (field "agent" "For (an agent's or a program's id)" ""))
   "<h3>Requests that wait for you</h3>"
   (if (seq pending)
     (apply str
            (for [r pending]
              (str "<pre>" (escape (edn-text r)) "</pre>"
                   (form "/token/approve" "Approve" (hidden "request" (:request r))))))
     "<p>None.</p>")
   (form "/agent/token/end" "End the token"
         (field "identity" "Whose token (the agent's or the program's id, the second part of the token)" "")
         (field "session" "Its session (the third part of the token)" ""))
   "</section>"

   "<section><h2>Your layer</h2>"
   (form "/act" "Write the note"
         "<label>A note<textarea name=\"note\" rows=\"3\"></textarea></label>")
   (form "/read" "Read my layer back")
   "</section>"

   "<section><h2>Link another Google account</h2>"
   (google-button client-id "/link")
   "</section>"))

(defn render
  "Takes {:client-id :me :pending :said :shown} (the namespace docstring
  says what each holds). Gives the page as a text of HTML: the sign-in
  button when `:me` is nil, else who she is and the forms; what the last
  form did on top."
  [{:keys [client-id me pending said shown]}]
  (str "<!doctype html><html lang=\"en\"><head><meta charset=\"utf-8\">"
       "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">"
       "<title>Softland runtime</title><style>" style "</style>"
       "<script src=\"https://accounts.google.com/gsi/client\" async></script>"
       "<script>" script "</script></head><body>"
       "<h1>Softland runtime</h1>"
       "<p>On localhost. The store is thrown away when the runtime stops.</p>"
       (said-box said shown)
       (if me (signed-in client-id me pending) (signed-out client-id))
       "</body></html>"))
