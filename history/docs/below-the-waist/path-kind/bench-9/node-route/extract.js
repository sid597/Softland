// Extract the bench's pure declarations (before the GL plumbing) into a Node module.
const fs = require('fs');
const src = fs.readFileSync(process.argv[2], 'utf8');
const lines = src.split('\n');
const start = lines.findIndex(l => l.includes("'use strict';"));
const end = lines.findIndex(l => l.includes('// ---------- GL plumbing ----------'));
let body = lines.slice(start + 1, end).join('\n');
// names to export: every top-level function and const in the slice
const names = new Set();
for (const m of body.matchAll(/^  (?:function|class) ([A-Za-z_$][\w$]*)/gm)) names.add(m[1]);
for (const m of body.matchAll(/^  const ([A-Za-z_$][\w$]*) *=/gm)) names.add(m[1]);
for (const m of body.matchAll(/^  const \{ ([^}]*) \}/gm)) for (const n of m[1].split(',')) names.add(n.trim());
body = "'use strict';\nconst performance = globalThis.performance;\n" + body + '\nmodule.exports = { ' + [...names].join(', ') + ' };\n';
fs.writeFileSync(process.argv[3], body);
console.log('exported', names.size, 'names');
