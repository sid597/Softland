// The check of the socket's Origin rule, against a server that runs
// `softland.browser.main`: an upgrade whose Origin is another site's is closed
// at once with 1008 and sent nothing, and one whose Origin is the server's own
// stays open until this script closes it. It opens the same path that the
// page's program opens (`/app`), with no cookie. Electric's server sends nothing
// until the client speaks (its heartbeat comes every 45 s), so that the accepted
// socket runs the program is shown by drive.cjs, not here.
//
//   node test-server-browser/origin.cjs [origin]    # default http://localhost:8401
//
// It needs `node_modules/ws` in the repository.

const WebSocket = require('ws');

const origin = process.argv[2] || 'http://localhost:8401';
const url = `${origin.replace(/^http/, 'ws')}/app`;

// Opens a socket with the given Origin and watches it for `ms`. Gives what it saw.
function attempt(withOrigin, ms) {
  return new Promise((resolve) => {
    const seen = { origin: withOrigin, opened: false, messages: 0, firstBytes: 0, closed: null, error: null };
    const socket = new WebSocket(url, { headers: { Origin: withOrigin } });
    socket.on('upgrade', (response) => { seen.status = response.statusCode; });
    socket.on('open', () => { seen.opened = true; });
    socket.on('message', (data) => {
      seen.messages += 1;
      if (seen.messages === 1) seen.firstBytes = data.length;
    });
    socket.on('close', (code, reason) => {
      seen.closed = { code, reason: reason.toString() };
      clearTimeout(timer);
      resolve(seen);
    });
    socket.on('error', (error) => { seen.error = error.message; });
    const timer = setTimeout(() => { socket.close(1000); }, ms);
  });
}

(async () => {
  const evil = await attempt('http://evil.example', 3000);
  console.log('Origin http://evil.example:', JSON.stringify(evil));
  const own = await attempt(origin, 3000);
  console.log(`Origin ${origin}:`, JSON.stringify(own));
  const refused = evil.closed && evil.closed.code === 1008 && evil.messages === 0;
  // 1000 is the code of this script's own close, after the wait.
  const accepted = own.opened && own.closed && own.closed.code === 1000;
  console.log(refused ? 'PASSED' : 'FAILED', 'an upgrade from another origin is closed with 1008 and sent nothing');
  console.log(accepted ? 'PASSED' : 'FAILED', "an upgrade from the server's own origin stays open until the client closes it");
  process.exit(refused && accepted ? 0 : 1);
})();
