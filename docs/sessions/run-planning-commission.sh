#!/bin/bash
# Planning Commission — LLM Roundtable
# Three LLMs have an informal meeting via a shared chat file.
# Run from a terminal that is NOT inside any LLM session.

set -euo pipefail

PROJECT_DIR="/mnt/data/projects/Softland"
CHAT_FILE="$PROJECT_DIR/docs/sessions/2026-02-18-planning-commission.md"
MAX_ROUNDS="${1:-5}"

GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

BRIEF="You are in an informal planning meeting with two other LLMs. The shared chat file is at docs/sessions/2026-02-18-planning-commission.md — read it, read the context docs referenced at the top, then append your response at the bottom. Talk like a colleague. Go on tangents if interesting. Disagree if you disagree. Keep it natural. This is round ROUND_NUM."

echo -e "${YELLOW}Planning Commission — LLM Roundtable${NC}"
echo -e "Chat: $CHAT_FILE"
echo -e "Rounds: $MAX_ROUNDS\n"

for round in $(seq 1 "$MAX_ROUNDS"); do
  echo -e "${YELLOW}--- Round $round ---${NC}\n"

  # Claude
  echo -e "${GREEN}[CLAUDE] ...${NC}"
  PROMPT="${BRIEF//ROUND_NUM/$round} You are CLAUDE."
  ( unset CLAUDECODE; cd "$PROJECT_DIR"; claude -p "$PROMPT" --print --allowedTools "Read,Edit,Glob" 2>/dev/null ) || true
  echo -e "${GREEN}[CLAUDE] done${NC}\n"

  # Gemini
  echo -e "${BLUE}[GEMINI] ...${NC}"
  PROMPT="${BRIEF//ROUND_NUM/$round} You are GEMINI."
  ( cd "$PROJECT_DIR"; gemini -p "$PROMPT" --yolo 2>/dev/null ) || true
  echo -e "${BLUE}[GEMINI] done${NC}\n"

  # Codex
  echo -e "${RED}[CODEX] ...${NC}"
  PROMPT="${BRIEF//ROUND_NUM/$round} You are CODEX."
  ( cd "$PROJECT_DIR"; codex exec "$PROMPT" 2>/dev/null ) || true
  echo -e "${RED}[CODEX] done${NC}\n"
done

echo -e "\n${YELLOW}Done. Read the chat:${NC} $CHAT_FILE"
