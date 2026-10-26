#!/usr/bin/env bash
# decision-1: how many tracked .clj/.cljc/.md files are larger than one fact's value
# may be (65,536 bytes, src-store/softland/store/door/declaration.clj:84-90). A run, not code.
cd /mnt/data/projects/Softland || exit 1
git ls-files '*.clj' '*.cljc' '*.md' | while read -r f; do [ -f "$f" ] && echo "$(wc -c < "$f") $f"; done > /tmp/d1-sizes.$$
echo "HEAD $(git rev-parse --short HEAD)"
echo "files: $(wc -l < /tmp/d1-sizes.$$)"
echo "over 65536 bytes: $(awk '$1>65536' /tmp/d1-sizes.$$ | wc -l) (code $(awk '$1>65536 && $2 ~ /\.cljc?$/' /tmp/d1-sizes.$$ | wc -l), md $(awk '$1>65536 && $2 ~ /\.md$/' /tmp/d1-sizes.$$ | wc -l))"
echo "in src-store over the bound:"; awk '$1>65536 && $2 ~ /^src-store\//' /tmp/d1-sizes.$$
rm -f /tmp/d1-sizes.$$
