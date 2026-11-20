#!/bin/sh
# Signs the program in against a server that runs a stand-in for Google, with
# no person: it starts `softland sign-in`, reads the hash out of the link that
# the program shows, signs a browser in through the stand-in and says yes (or
# no) for it. Takes the work folder, and `yes` or `no`. The credentials file
# is the one $SOFTLAND_CREDENTIALS names; the server is $SOFTLAND_SERVER or
# http://localhost:8400. Prints what the program printed; exits with its status.
set -u
work=${1:?a work folder}
say=${2:-yes}
server=${SOFTLAND_SERVER:-http://localhost:8400}
root=$(dirname -- "$(dirname -- "$(readlink -f -- "$0")")")
mkdir -p "$work"
"$root/bin/softland" sign-in --as "softland CLI check" >"$work/sign-in.out" 2>"$work/sign-in.err" &
pid=$!
hash=""
for _ in $(seq 1 50); do
  hash=$(sed -n 's/.*hash=\([0-9a-f]\{64\}\).*/\1/p' "$work/sign-in.err" | head -n 1)
  [ -n "$hash" ] && break
  sleep 0.2
done
[ -n "$hash" ] || { echo "no link was shown" >&2; kill "$pid"; exit 1; }
curl -s -o /dev/null -c "$work/jar" -X POST "$server/sign-in/stand-in" -H "Origin: $server" \
  --data-urlencode 'sub=2000002' --data-urlencode 'email=agent-test@example.com'
echo "yes page: $(curl -s -o /dev/null -w '%{http_code}' -b "$work/jar" -X POST "$server/sign-in/yes" \
  -H "Origin: $server" --data-urlencode "hash=$hash" --data-urlencode "say=$say")" >&2
wait "$pid"
status=$?
cat "$work/sign-in.err" >&2
cat "$work/sign-in.out"
exit $status
