# Source: Electric Slack thread — e/fn values in data structures, v3 dynamic siting

- Recorded: 2026-07-10, session `design-conversation-analysis` (Sid pasted the thread; timestamps below are Slack-local times, thread date unknown).
- Venue: Clojurians Slack `#hyperfiddle`. Participants: noonian, Dustin Getz (Hyperfiddle), xificurC.
- Why it matters here: it is the direct upstream answer to the assembly-interpreter question — can a registry `{keyword → e/fn}` be built and dispatched at runtime. Feeds SKILL.md entries **U4** and **U5**. Verbatim below; no edits.

---

noonian  [10:57 AM]
Question on higher-order e/fn's: is there any pattern for returning multiple e/fn's from a single factory e/fn? I haven't been able to get that to work by returning a vector/map of anonymous e/fns and I think it's because creating those data structures creates a site binding and electric isn't analyzing the contents of the vector since it's a sited value. Returning a single e/fn as a return value works great.

Context for this is making higher-order UI forms like login/auth. If there are other patterns for this sort of thing I would love to hear about them (dynamic binding?). Example macro in :thread:

This was user error on my part and I've confirmed returning electric functions in data structures works currently. (edited)

13 replies

noonian  [11:06 AM]
(e/defn WrapAuth [Identity Unsecured Secured]
  (let [auth-identity (Identity)]
    (if auth-identity
      (Secured auth-identity)
      (Unsecured))))

#?(:clj
   (defmacro with-fake-auth
     "Binds three caller-named e/fns to a shared per-connection in-memory
   identity atom, then evaluates body with them in scope:

     (with-fake-auth [Identity SignIn! SignOut!]
       (binding [dom/node js/document.body]
         (WrapAuth Identity Unsecured Secured)))"
     [bindings & body]
     (let [[Identity-sym SignIn!-sym SignOut!-sym] bindings
           !auth-identity-ref-sym (gensym "!auth-identity_")
           auth-identity-sym (gensym "auth-identity_")]
       `(e/server
          (let [~!auth-identity-ref-sym (atom nil)
                ~auth-identity-sym (e/watch ~!auth-identity-ref-sym)]
            (e/client
              (let [~Identity-sym (e/fn [] (e/server ~auth-identity-sym))
                    ~SignIn!-sym (e/fn [new-auth-identity]
                                   (e/server (reset! ~!auth-identity-ref-sym new-auth-identity)))
                    ~SignOut!-sym (e/fn []
                                    (e/server (reset! ~!auth-identity-ref-sym nil)))]
                ~@body)))))))

Dustin Getz (Hyperfiddle)  [3:25 PM]
I expect this to work, try asking claude to minimize it
[3:26 PM]
e/fns are values

xificurC  [3:30 PM]
new-auth-identity needs a gensym
[3:31 PM]
I haven't been able to get that to work

please resolve "get to work", is it failing to compile, run, are there exceptions etc

noonian  [11:13 PM]
TBC: the macro approach works, but I couldn't get returning 3 e/fn's in a vector working. I'll post what I want to get working returning multiple efn's after I'm off work. It's possible I am hitting a different issue somewhere.

it compiled, but failed to run with an arity error
usually when I get that, I think it means an efn is getting called as a regular function
(edited)

noonian  [11:31 PM]
I think this is user error, but will post details once I verify. I've confirmed it works returning 3 e/fns in a vector in a simple case

noonian  [12:15 AM]
Confirmed, this works:
(e/defn FakeAuth [!identity-store]
  (let [identity (e/watch !identity-store)]
    {:Identity (e/fn [] identity)
     :SignIn! (e/fn [new-identity] (reset! !identity-store new-identity))
     :SignOut! (e/fn [] (reset! !identity-store nil))}))

I think my issue was actually in the form handling with my login service being concurrent and one branch not finishing before the e/fn returned a value

I am not sure how this fits with my intuition about edges and effects though. I understand e/fn's are values in electric, but I don't think I understand how the analysis works with the concrete data structure return value and whether the returned e/fns are bound to a particular site in any way. I am not sure what I don't understand so I can't articulate it well. Thanks for pushing me to reassess my assumptions and apologies for the poorly stated question

Dustin Getz (Hyperfiddle)  [6:41 PM]
are returned e/fns are bound to a particular site in any way?

no, they are concretely pointers/ids (serializable, you can println them) and these pointers can move between sites. The pointers are interpreted in the context of the common program dag, which both sites understand. Either site can boot a e/fn, keeping in mind that the e/fn contains both client and server regions, so one side or the other is going to incur latency relative to which site booted the e/fn.
[6:43 PM]
As an aside, your program contains {} which expands to a hash-map call, so concretely, you have three e/fn pointer values stored in a clojure hashmap
[6:46 PM]
Also, your e/fn closures do not contain e/client or e/server site markers, so under the electric v3 rule of dynamic site resolution (NOT lexical), the body will be evaluated on the site that boots the fn. And your functions access an unserializable reference (the atom). So you probably intend to site the whole system all on one site. I would recommend you make this explicit by putting e/client inside each of your e/fns.

noonian  [7:15 PM]
Thank you, especially for the concrete pointer note for e/fns. Yeah, I ended up with the explicit server site but was experimenting with making unsited fns/components because I didn't really have good intuition about using them in practice. This helps
[7:17 PM]
Also just reading the todo-mvc patterns more closely and refactoring to the patterns used there improved my structure a lot (and understanding of the service pattern for forms)
