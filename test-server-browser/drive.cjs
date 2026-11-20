// The check of the browser's face with a real browser: headless Chrome, driven
// by Puppeteer, against a server that runs `softland.browser.main`.
//
// It signs in through the stand-in form of `/`, says yes to a program on the
// real yes page, opens `/app`, watches the
// entity :chair-7, writes :chair-7 :colour :green and waits until the act shows
// as accepted and the watched table shows :colour with :green; writes :blue and
// waits for the table to show :blue with no reload; then posts `/sign-out` from
// the page's own session and waits for the open page to say that she is not
// signed in, with no reload. It prints what it saw at each step, and exits with
// a status that is not zero when a step does not pass in its time.
//
//   node test-server-browser/drive.cjs [origin]    # default http://localhost:8401
//
// It needs `node_modules/puppeteer` in the repository and Chrome at
// /usr/bin/google-chrome (or CHROME=path).

const puppeteer = require('puppeteer');

const origin = process.argv[2] || 'http://localhost:8401';
const chrome = process.env.CHROME || '/usr/bin/google-chrome';
const stepMs = 20000;

function saw(step, what) {
  console.log(`${step}: ${what}`);
}

// Waits until fn, run in the page, gives a truthy value; gives that value.
async function until(page, step, fn, arg) {
  try {
    const handle = await page.waitForFunction(fn, { timeout: stepMs, polling: 100 }, arg);
    return await handle.jsonValue();
  } catch (error) {
    const text = await page.evaluate(() => document.body.innerText).catch(() => '(no page)');
    console.log(`FAILED ${step}: not seen within ${stepMs} ms. The page said:\n${text}`);
    throw error;
  }
}

async function fill(page, selector, text) {
  await page.waitForSelector(selector, { timeout: stepMs });
  await page.$eval(selector, (el) => { el.value = ''; });
  await page.type(selector, text);
}

async function write(page, entity, key, value) {
  await fill(page, '#write input[name=entity]', entity);
  await fill(page, '#write input[name=key]', key);
  await fill(page, '#write input[name=value]', value);
  await page.click('#write button[type=submit]');
}

(async () => {
  const browser = await puppeteer.launch({
    executablePath: chrome,
    headless: true,
    args: ['--no-sandbox', '--disable-gpu'],
  });
  let failed = false;
  try {
    const page = await browser.newPage();
    page.on('console', (message) => console.log(`  (console ${message.type()}) ${message.text()}`));
    page.on('pageerror', (error) => console.log(`  (page error) ${error.message}`));

    // 1. Sign in through the stand-in form on `/`.
    await page.goto(`${origin}/`, { waitUntil: 'load' });
    saw('open /', JSON.stringify(await page.evaluate(() => document.body.innerText.slice(0, 200))));
    // The Origin header that Chrome really sends with each post, as the network
    // stack wrote it (Puppeteer's own request object does not show it).
    const client = await page.target().createCDPSession();
    await client.send('Network.enable');
    const posted = new Map();
    client.on('Network.requestWillBeSent', (e) => {
      if (e.request.method === 'POST') posted.set(e.requestId, new URL(e.request.url).pathname);
    });
    client.on('Network.requestWillBeSentExtraInfo', (e) => {
      if (posted.has(e.requestId)) {
        const origin = Object.entries(e.headers).find(([k]) => k.toLowerCase() === 'origin');
        console.log(`  (Chrome posted ${posted.get(e.requestId)} with Origin: ${origin ? origin[1] : '(none)'})`);
      }
    });

    const [response] = await Promise.all([
      page.waitForNavigation({ waitUntil: 'load' }),
      page.click('form[action^="/sign-in/stand-in"] button'),
    ]);
    saw('the stand-in form posted', `${response.status()} ${JSON.stringify(await page.evaluate(() => document.body.innerText.slice(0, 200)))}`);
    // The form's post must be taken as it is: a real browser decides what
    // `Origin` it writes on a form's post from the page's referrer policy, and
    // the server holds each post that rests on a cookie, or that signs
    // somebody in, to its own origin.
    if (response.status() !== 200) throw new Error(`the stand-in form's post was not taken: ${response.status()}`);
    saw('signed in', JSON.stringify(await page.evaluate(() => document.body.innerText.slice(0, 200))));

    // 1b. Her yes to a program, through the real page and its form: the post
    // rests on her cookie and must carry the server's own origin.
    const hash = 'ab'.repeat(32);
    await page.goto(`${origin}/sign-in/yes?hash=${hash}&what=${encodeURIComponent('the browser check')}`, { waitUntil: 'load' });
    saw('the yes page', JSON.stringify(await page.evaluate(() => document.body.innerText.slice(0, 160))));
    const [yes] = await Promise.all([
      page.waitForNavigation({ waitUntil: 'load' }),
      page.click('form[action="/sign-in/yes"] button[value=yes]'),
    ]);
    saw('her yes posted', `${yes.status()} ${JSON.stringify(await page.evaluate(() => document.body.innerText.slice(0, 120)))}`);
    if (yes.status() !== 200 && yes.status() !== 409) throw new Error(`her yes was not taken: ${yes.status()}`);

    // 2. Open the program; her id and her own layer show.
    await page.goto(`${origin}/app`, { waitUntil: 'load' });
    const who = await until(page, 'her id', () => {
      const el = document.querySelector('#who');
      return el && el.textContent && el.textContent !== '…' ? el.textContent : null;
    });
    saw('her id shows', who);
    const layer = await until(page, 'her own layer', () => {
      const el = document.querySelector('#own-layer');
      return el && /^[0-9a-f-]{36}$/.test(el.textContent) ? el.textContent : null;
    });
    saw('her own layer shows', layer);

    // 3. Watch :chair-7.
    await fill(page, '#watch input[name=entity]', ':chair-7');
    await page.click('#watch button[type=submit]');
    await until(page, 'the watch opens', () => !!document.querySelector('#watched'));
    saw('watching', JSON.stringify(await page.evaluate(() => document.querySelector('#watched').parentElement.innerText)));

    // 4. Write :chair-7 :colour :green; the act is accepted and the table shows it.
    await write(page, ':chair-7', ':colour', ':green');
    const first = await until(page, 'the first act accepted', () => {
      const li = document.querySelectorAll('#acts li')[0];
      return li && /accepted|refused|could not|nothing-yet|not-mine/.test(li.textContent) ? li.textContent : null;
    });
    saw('first act', first);
    if (!/accepted/.test(first)) throw new Error(`the first act was not accepted: ${first}`);
    const green = await until(page, 'the table shows :green', () => {
      const t = document.querySelector('#watched');
      return t && /:colour/.test(t.innerText) && /:green/.test(t.innerText) ? t.innerText : null;
    });
    saw('watched table', JSON.stringify(green));

    // 5. Write :chair-7 :colour :blue; the table changes with no reload.
    const navigations = [];
    page.on('framenavigated', (frame) => { if (frame === page.mainFrame()) navigations.push(frame.url()); });
    await write(page, ':chair-7', ':colour', ':blue');
    const second = await until(page, 'the second act answered', () => {
      const li = document.querySelectorAll('#acts li')[1];
      return li && /accepted|refused|could not|nothing-yet|not-mine/.test(li.textContent) ? li.textContent : null;
    });
    saw('second act', second);
    const blue = await until(page, 'the table shows :blue', () => {
      const t = document.querySelector('#watched');
      return t && /:colour/.test(t.innerText) && /:blue/.test(t.innerText) ? t.innerText : null;
    });
    saw('watched table', JSON.stringify(blue));
    saw('navigations since the first write', JSON.stringify(navigations));

    // 6. Sign out from the page's own session; the open page turns with no reload.
    const signOut = await page.evaluate(async () => {
      const r = await fetch('/sign-out', {
        method: 'POST', credentials: 'same-origin', redirect: 'manual',
      });
      return `${r.type} ${r.status}`;
    });
    saw('posted /sign-out', signOut);
    const out = await until(page, 'the page says not signed in', () => {
      const el = document.querySelector('#standing');
      return el && /You are not signed in\./.test(el.textContent) ? el.textContent : null;
    });
    saw('the open page', out);
    saw('navigations since the first write', JSON.stringify(navigations));
    if (navigations.length) throw new Error('the page navigated');
    saw('PASSED', 'every step');
  } catch (error) {
    failed = true;
    console.log(`FAILED: ${error.message}`);
  } finally {
    await browser.close();
  }
  process.exit(failed ? 1 : 0);
})();
