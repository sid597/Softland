# The browser's face — verification

[Source entry](../src-server-browser/README.md).

This tree holds the checks of the browser's face that drive a running server
from outside, and what they print. Each runs against a server that
`softland.browser.main` started with `--stand-in-google`, and needs the
browser's program built first:

```sh
clojure -M:server-browser -m shadow.cljs.devtools.cli release softland-browser
clojure -M:server-browser -m softland.browser.main --stand-in-google --port 8401
```

| File | What it checks |
|---|---|
| [drive.cjs](drive.cjs) | With headless Chrome, driven by Puppeteer: a sign-in through the stand-in form of `/`; her yes to a program on the real yes page; her id on `/app`; a live read of `:chair-7`; an act written from the page and accepted, and the live read showing it; a second act, and the live read changing with no reload; a post of `/sign-out` from the page's session, and the open page turning to "You are not signed in." with no reload. |
| [origin.cjs](origin.cjs) | A socket opened with `Origin: http://evil.example` is closed at once with 1008 and sent nothing; one opened with the server's own origin stays open until the script closes it. Electric's server sends nothing until the client speaks, so that an accepted socket runs the program is `drive.cjs`'s to show. |

Run them from the repository root, while the server runs:

```sh
node test-server-browser/drive.cjs http://localhost:8401
node test-server-browser/origin.cjs http://localhost:8401
```

Each prints what it saw at each step, then `PASSED` or `FAILED`, and exits with
a status that is not zero when a step fails. `drive.cjs` needs
`node_modules/puppeteer` and Chrome at `/usr/bin/google-chrome` (or the path
in `CHROME`); `origin.cjs` needs `node_modules/ws`. Each run of `drive.cjs`
signs in as the stand-in's default account, so a second run against the same
server finds the acts of the first in the live read.

`drive.cjs` prints the `Origin` that Chrome sends with each post, and fails
when a form's post is not taken. On 20 November 2026 it caught that the
server's pages were given with `Referrer-Policy: no-referrer`, under which
Chrome writes `Origin: null` on a form's post to its own site, so the server
refused its own forms; the policy is now
`softland.server.http/referrer-policy`. The proof over HTTP could not have
seen it: its client writes the `Origin` that it is told to.
