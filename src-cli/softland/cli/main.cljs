(ns softland.cli.main
  "The entry of the program `softland`: it reads the command line into a
  plan and hands it on, to the MCP server for `mcp` and to the commands for
  everything else. A wrong command line is told on standard error, with
  a pointer to `softland help`, and the exit status is 2.

  Takes the arguments of the command line. Gives nothing; it sets the
  process's exit status and lets Node end when the work is done, so that
  what was written to standard output is all written.

  State. None."
  (:require [softland.cli.commands :as commands]
            [softland.cli.mcp :as mcp]
            [softland.cli.node :as node]
            [softland.cli.pure :as pure]))

(defn -main
  "Takes the arguments of the command line, as texts. Runs the command that
  they name. The exit status is the command's: 0 when the server answered
  200 and the result holds no `:could-not`, 1 otherwise, 2 for a wrong
  command line."
  [& args]
  (let [plan (pure/plan-of args)]
    (cond
      (:wrong plan)
      (do (node/err! (str "softland: " (:wrong plan) " (`softland help` lists the commands)"))
          (set! (.-exitCode js/process) 2))

      (= :mcp (:command plan))
      (mcp/serve! (:server plan))

      :else
      (.then (commands/run plan)
             (fn [status] (set! (.-exitCode js/process) status))))))
