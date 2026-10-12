"""Put every quotation of a page to the record of Sid's lines.

Run: python3 check_quotes.py <file> [<first line> <last line>]

A paper of this folder gives Sid's words in double quotation marks. This script
takes each quoted run of four words or more from the page, asks the record who
wrote it first (`memtool who`), and prints one row for each run:

  the page's line · whole, head or tail · exact or WORDS-ONLY · the record's
  verdict · his line, if he typed it · the long prompt that holds it, if one
  does · the first session text that holds it · the run

A quotation of more than 14 words is asked as two runs, its first ten words and
its last nine, because the record matches a run of words and a long run is more
often broken by one changed word.

How to read a row. The record matches words, not marks: a comma that differs
does not show.

  exact / sid_typed            he typed that run.
  exact / unknown, with a      the run is in a long prompt of his, where the
  long prompt                  record cannot tell typing from pasting.
  exact / session_only, or     a session wrote that run and he did not. That is
  session_pasted_by_sid        right for a sheet's title or for words that the
                               page gives as a session's, and wrong for words
                               that the page gives as his.
  WORDS-ONLY                   the run as the page has it is nowhere; only its
                               words are, in some order. The quotation was
                               changed.
  head typed, tail not         one quotation whose two ends differ: a session
                               changed the words of his line on the way to the
                               page.

A pass shows that the quoted words are in the record. It does not show that the
page says truly what they mean, or that a sentence without quotation marks is
his. The marks on the page must pair: a page with an odd number of them is
refused.

On 12 October 2026 this ran over `src-store/README.md`, the decisions of 6 to
12 October, before and after the pass of that day. Before: 128 runs, 79 of
them typed by him as the page had them; six quotations had been changed by a
session and were put back to his words. After: 158 runs. 109 are typed by him
as the page has them, and 11 are in two long dictated prompts. The other 38
were read one by one: each is a sheet's title, words of a session, of the
rulings file or of Rama's docs that the page gives as such, or the page
naming one of its own entries.
"""
import re
import subprocess
import sys

MEMTOOL = "/mnt/data/projects/softland-memory/bin/memtool"


def who(run):
    """Ask the record who first wrote `run`. Give (exact?, verdict, his line ids,
    long prompt ids, the first session text)."""
    out = subprocess.run([MEMTOOL, "who", run], capture_output=True, text=True,
                         timeout=120).stdout
    typed = re.search(r"TYPED BY SID[^:]*: (.*)", out)
    long_prompt = re.search(r"IN A LONG PROMPT[^:]*: (.*)", out)
    session = re.search(r"WRITTEN BY A SESSION in (\d+) texts?, first: («\w+» [\d-]+ [\d:]+)", out)
    verdict = re.search(r"BY THE RECORD: who = (\S+)", out)
    typed_text = typed.group(1) if typed else ""
    return ("exact" if "appears nowhere before now" not in out else "WORDS-ONLY",
            verdict.group(1).rstrip(".,") if verdict else "?",
            " ".join(re.findall(r"‹\w+›", typed_text)[:3]) if "no line" not in typed_text else "-",
            " ".join(re.findall(r"‹\w+›", long_prompt.group(1))[:2]) if long_prompt else "-",
            session.group(2) if session else "-")


def main():
    path = sys.argv[1]
    lines = open(path, encoding="utf-8").read().split("\n")
    first = int(sys.argv[2]) if len(sys.argv) > 2 else 1
    last = min(int(sys.argv[3]), len(lines)) if len(sys.argv) > 3 else len(lines)

    text, line_of = "", []          # the span as one string, and each character's line
    for n in range(first - 1, last):
        for ch in lines[n].strip() + " ":
            text += ch
            line_of.append(n + 1)
    if text.count('"') % 2:
        sys.exit("an odd number of quotation marks in lines %d to %d: they do not pair" % (first, last))

    position, seen = 0, set()
    for i, part in enumerate(text.split('"')):
        start = position
        position += len(part) + 1
        if i % 2 == 0:
            continue                 # outside the marks
        quote = " ".join(part.split())
        words = quote.split()
        if len(words) < 4 or quote in seen:
            continue
        seen.add(quote)
        runs = ([("whole", quote)] if len(words) <= 14 else
                [("head", " ".join(words[:10])), ("tail", " ".join(words[-9:]))])
        for name, run in runs:
            run = run.strip(" .,;:?!…")
            exact, verdict, his, long_prompt, session = who(run)
            print("\t".join([str(line_of[min(start, len(line_of) - 1)]), name, exact, verdict,
                             his, long_prompt, session, run[:70]]), flush=True)


if __name__ == "__main__":
    main()
