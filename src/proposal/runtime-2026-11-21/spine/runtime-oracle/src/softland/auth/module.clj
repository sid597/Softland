(ns softland.auth.module
  "Non-production test shim for the runtime oracle phase: minimal valid Rama
  module named `Auth` so `softland.runtime.main/in-process-cluster` launches
  cleanly while Session 1 builds the real `src-auth/`. Never used in
  production or real-auth runs."
  (:require [com.rpl.rama :as rama]))

(rama/defmodule Auth
  [setup topologies]
  (rama/declare-depot setup *oracle-noop (rama/hash-by identity)))
