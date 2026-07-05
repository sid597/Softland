#!/usr/bin/env bash
# install-rama.sh — install, run, and manage Rama on macOS or Linux
#
# Works on:
#   - macOS (Apple Silicon or Intel) — uses Homebrew
#   - Ubuntu / Debian Linux (apt-based) — uses Adoptium repo
#
# Usage:
#   ./install-rama.sh             install + configure (default; does NOT start)
#   ./install-rama.sh start       start ZK + Conductor + Supervisor in background
#   ./install-rama.sh stop        stop all daemons
#   ./install-rama.sh restart     stop then start
#   ./install-rama.sh status      show daemon status
#   ./install-rama.sh logs        tail daemon logs
#   ./install-rama.sh nuke        remove install dir (data dir preserved)
#
# Environment overrides:
#   RAMA_VERSION   default: 1.6.0
#   RAMA_HOME      default: ~/rama
#   RAMA_DATA      default: ~/rama/data
#   RAMA_PROFILE   default: auto (auto | small | medium | large)
#                  small  = 8 GB or less   (worker 1.5G, conductor 768M, supervisor 512M)
#                  medium = 9-16 GB        (worker 2G,   conductor 1G,   supervisor 1G)
#                  large  = 17+ GB         (worker 4G,   conductor 1G,   supervisor 1G)

set -euo pipefail

# ============================================================================
# Configuration
# ============================================================================

RAMA_VERSION="${RAMA_VERSION:-1.6.0}"
RAMA_HOME="${RAMA_HOME:-$HOME/rama}"
RAMA_DATA="${RAMA_DATA:-$HOME/rama/data}"
RAMA_PROFILE="${RAMA_PROFILE:-auto}"
RAMA_DOWNLOAD_URL="https://redplanetlabs.s3.us-west-2.amazonaws.com/rama/rama-${RAMA_VERSION}.zip"

INSTALL_DIR="$RAMA_HOME/rama-${RAMA_VERSION}"
LOG_DIR="$RAMA_HOME/logs"
PID_DIR="$RAMA_HOME/pids"

# ============================================================================
# Output helpers
# ============================================================================

c_blue()   { printf '\033[34m%s\033[0m' "$*"; }
c_green()  { printf '\033[32m%s\033[0m' "$*"; }
c_yellow() { printf '\033[33m%s\033[0m' "$*"; }
c_red()    { printf '\033[31m%s\033[0m' "$*"; }

log()  { echo "$(c_blue '[rama]')  $*"; }
ok()   { echo "$(c_green '[ ok ]') $*"; }
warn() { echo "$(c_yellow '[warn]') $*"; }
err()  { echo "$(c_red '[err ]') $*" >&2; exit 1; }

# ============================================================================
# Platform detection
# ============================================================================

OS="$(uname -s)"
ARCH="$(uname -m)"

is_mac()   { [ "$OS" = "Darwin" ]; }
is_linux() { [ "$OS" = "Linux" ]; }

detect_ram_gb() {
  if is_mac; then
    echo $(($(sysctl -n hw.memsize) / 1024 / 1024 / 1024))
  elif is_linux; then
    echo $(($(grep MemTotal /proc/meminfo | awk '{print $2}') / 1024 / 1024))
  else
    echo "0"
  fi
}

pick_profile() {
  if [ "$RAMA_PROFILE" != "auto" ]; then
    echo "$RAMA_PROFILE"
    return
  fi
  local ram_gb
  ram_gb=$(detect_ram_gb)
  if   [ "$ram_gb" -le 8 ];  then echo "small"
  elif [ "$ram_gb" -le 16 ]; then echo "medium"
  else                            echo "large"
  fi
}

# ============================================================================
# Java install
# ============================================================================

# Non-interactive SSH sessions on macOS do not load ~/.zshrc, so JAVA_HOME
# and the Temurin PATH that Homebrew sets there are invisible. This function
# uses /usr/libexec/java_home (macOS's canonical JDK registry query) to find
# any installed JDK 21 and exports JAVA_HOME + PATH for the rest of this
# script's run, regardless of the calling shell's environment.
setup_java_env() {
  if is_mac; then
    if [ -z "${JAVA_HOME:-}" ] || ! [ -x "${JAVA_HOME:-}/bin/java" ]; then
      local jh
      jh=$(/usr/libexec/java_home -v 21 2>/dev/null) || return 0
      export JAVA_HOME="$jh"
    fi
    case ":$PATH:" in
      *":$JAVA_HOME/bin:"*) ;;
      *) export PATH="$JAVA_HOME/bin:$PATH" ;;
    esac
  fi
}

java_is_21() {
  command -v java >/dev/null 2>&1 || return 1
  java -version 2>&1 | grep -q '"21\.'
}

install_java_mac() {
  command -v brew >/dev/null || err "Homebrew not found. Install from https://brew.sh first."
  if brew list --cask temurin@21 >/dev/null 2>&1; then
    ok "Temurin 21 already installed via brew"
  else
    log "Installing Temurin 21 via brew (this can take a minute)..."
    brew install --cask temurin@21
  fi
}

install_java_linux() {
  command -v apt-get >/dev/null \
    || err "Only apt-based Linux is supported by this script. On RHEL/Fedora/Arch, install Temurin 21 manually first."
  log "Installing Temurin 21 via apt..."
  sudo apt-get update -qq
  sudo apt-get install -y -qq wget gnupg unzip python3 lsb-release ca-certificates
  if [ ! -f /etc/apt/keyrings/adoptium.gpg ]; then
    sudo mkdir -p /etc/apt/keyrings
    wget -qO- https://packages.adoptium.net/artifactory/api/gpg/key/public \
      | sudo gpg --dearmor -o /etc/apt/keyrings/adoptium.gpg
  fi
  if [ ! -f /etc/apt/sources.list.d/adoptium.list ]; then
    echo "deb [signed-by=/etc/apt/keyrings/adoptium.gpg] https://packages.adoptium.net/artifactory/deb $(lsb_release -cs) main" \
      | sudo tee /etc/apt/sources.list.d/adoptium.list >/dev/null
    sudo apt-get update -qq
  fi
  sudo apt-get install -y -qq temurin-21-jdk
}

install_java() {
  if java_is_21; then
    ok "Java 21 already on PATH: $(java -version 2>&1 | head -1)"
    return
  fi
  if   is_mac;   then install_java_mac
  elif is_linux; then install_java_linux
  else err "Unsupported OS: $OS"
  fi
  java_is_21 || err "Java 21 install failed (java -version still not showing 21). Check JAVA_HOME / PATH."
  ok "Java 21 installed"
}

# ============================================================================
# Rama install
# ============================================================================

install_rama() {
  mkdir -p "$RAMA_HOME" "$RAMA_DATA" "$LOG_DIR" "$PID_DIR"
  cd "$RAMA_HOME"

  if [ -d "$INSTALL_DIR" ]; then
    ok "Rama ${RAMA_VERSION} already extracted at $INSTALL_DIR"
    return
  fi

  if [ ! -f "rama-${RAMA_VERSION}.zip" ]; then
    log "Downloading rama-${RAMA_VERSION}.zip from S3..."
    curl -fLO "$RAMA_DOWNLOAD_URL" || err "Download failed: $RAMA_DOWNLOAD_URL"
  else
    ok "rama-${RAMA_VERSION}.zip already downloaded"
  fi

  log "Extracting into $INSTALL_DIR..."
  unzip -q -o "rama-${RAMA_VERSION}.zip" -d "$INSTALL_DIR"

  if is_mac; then
    log "Stripping Gatekeeper quarantine on bundled native libraries..."
    xattr -dr com.apple.quarantine "$INSTALL_DIR" 2>/dev/null || true
  fi

  ok "Rama ${RAMA_VERSION} extracted to $INSTALL_DIR"
}

# ============================================================================
# Config
# ============================================================================

write_config() {
  local profile worker_heap conductor_heap supervisor_heap
  profile=$(pick_profile)

  case "$profile" in
    small)
      worker_heap="1536m"; conductor_heap="768m";  supervisor_heap="512m"  ;;
    medium)
      worker_heap="2048m"; conductor_heap="1024m"; supervisor_heap="1024m" ;;
    large)
      worker_heap="4096m"; conductor_heap="1024m"; supervisor_heap="1024m" ;;
    *)
      err "Unknown profile: $profile (use small | medium | large | auto)" ;;
  esac

  log "Profile: $profile (detected $(detect_ram_gb) GB RAM)"

  cat > "$INSTALL_DIR/rama.yaml" <<EOF
zookeeper.servers: ["localhost"]
conductor.host: "localhost"
local.dir: "$RAMA_DATA"
supervisor.port.range: [7000, 8000]

worker.childopts: "-Xmx${worker_heap} -XX:MaxRAMPercentage=20"
conductor.childopts: "-Xmx${conductor_heap}"
supervisor.childopts: "-Xmx${supervisor_heap}"
EOF

  ok "Wrote $INSTALL_DIR/rama.yaml ($profile profile)"
}

# ============================================================================
# Daemon management
# ============================================================================

daemon_pid_file() { echo "$PID_DIR/$1.pid"; }
daemon_log_file() { echo "$LOG_DIR/$1.log"; }

daemon_running() {
  local pidfile pid
  pidfile=$(daemon_pid_file "$1")
  [ -f "$pidfile" ] || return 1
  pid=$(cat "$pidfile")
  kill -0 "$pid" 2>/dev/null
}

start_daemon() {
  local name="$1" cmd="$2" pidfile logfile
  pidfile=$(daemon_pid_file "$name")
  logfile=$(daemon_log_file "$name")

  if daemon_running "$name"; then
    warn "$name already running (pid $(cat "$pidfile"))"
    return
  fi

  log "Starting $name..."
  cd "$INSTALL_DIR"

  if is_mac; then
    # caffeinate -i prevents idle sleep while the wrapped process is alive
    nohup caffeinate -i ./rama "$cmd" > "$logfile" 2>&1 &
  else
    nohup ./rama "$cmd" > "$logfile" 2>&1 &
  fi

  echo $! > "$pidfile"
  ok "$name started (pid $(cat "$pidfile"), log $logfile)"
}

stop_daemon() {
  local name="$1" pidfile pid i
  pidfile=$(daemon_pid_file "$name")

  if ! daemon_running "$name"; then
    warn "$name not running"
    rm -f "$pidfile"
    return
  fi

  pid=$(cat "$pidfile")
  log "Stopping $name (pid $pid)..."
  kill "$pid" 2>/dev/null || true

  i=0
  while kill -0 "$pid" 2>/dev/null && [ $i -lt 15 ]; do
    sleep 1; i=$((i+1))
  done

  if kill -0 "$pid" 2>/dev/null; then
    warn "$name didn't exit gracefully, sending SIGKILL"
    kill -9 "$pid" 2>/dev/null || true
  fi

  rm -f "$pidfile"
  ok "$name stopped"
}

# ============================================================================
# Subcommands
# ============================================================================

cmd_install() {
  log "Platform:     $OS $ARCH"
  log "Rama version: $RAMA_VERSION"
  log "Install dir:  $INSTALL_DIR"
  log "Data dir:     $RAMA_DATA"
  echo
  install_java
  install_rama
  write_config
  echo
  ok  "Install complete."
  log "Start the cluster:  $0 start"
  log "Then visit:         http://localhost:8888"
}

cmd_start() {
  [ -d "$INSTALL_DIR" ] || err "Rama not installed. Run: $0 install"
  start_daemon zookeeper devZookeeper
  log "Waiting 4s for ZooKeeper to bind..."
  sleep 4
  start_daemon conductor conductor
  log "Waiting 4s for Conductor to bind..."
  sleep 4
  start_daemon supervisor supervisor

  echo
  ok  "All daemons started."
  log "Cluster UI:  http://localhost:8888"
  log "Tail logs:   $0 logs"
  log "Status:      $0 status"
  log "Stop:        $0 stop"
}

cmd_stop() {
  stop_daemon supervisor
  stop_daemon conductor
  stop_daemon zookeeper
}

cmd_status() {
  local d
  for d in zookeeper conductor supervisor; do
    if daemon_running "$d"; then
      ok   "$d running (pid $(cat "$(daemon_pid_file "$d")"))"
    else
      warn "$d NOT running"
    fi
  done
}

cmd_logs() {
  if [ ! -d "$LOG_DIR" ] || [ -z "$(ls -A "$LOG_DIR" 2>/dev/null)" ]; then
    err "No logs found in $LOG_DIR"
  fi
  tail -f "$LOG_DIR"/*.log
}

cmd_nuke() {
  log "Stopping any running daemons..."
  cmd_stop || true
  log "Removing $INSTALL_DIR (data dir $RAMA_DATA preserved)..."
  rm -rf "$INSTALL_DIR" "$LOG_DIR" "$PID_DIR"
  rm -f "$RAMA_HOME/rama-${RAMA_VERSION}.zip"
  ok "Removed."
}

# ============================================================================
# Dispatch
# ============================================================================

main() {
  # Make Java discoverable regardless of whether we were invoked from an
  # interactive shell (which loaded ~/.zshrc) or a non-interactive one
  # (which didn't). Safe to call repeatedly; no-op if already set up.
  setup_java_env

  case "${1:-install}" in
    install) cmd_install ;;
    start)   cmd_start ;;
    stop)    cmd_stop ;;
    restart) cmd_stop; sleep 2; cmd_start ;;
    status)  cmd_status ;;
    logs)    cmd_logs ;;
    nuke)    cmd_nuke ;;
    -h|--help|help|*)
      cat <<EOF
Usage: $0 [command]

Commands:
  install    Install Java 21 + download/extract Rama + write config (default)
  start      Start ZooKeeper, Conductor, Supervisor in background
  stop       Stop all daemons
  restart    Stop then start
  status     Show daemon status
  logs       Tail daemon logs
  nuke       Remove install (preserves data dir)

Environment overrides:
  RAMA_VERSION    default: 1.6.0
  RAMA_HOME       default: ~/rama
  RAMA_DATA       default: ~/rama/data
  RAMA_PROFILE    default: auto  (small | medium | large | auto)
EOF
      ;;
  esac
}

main "$@"
