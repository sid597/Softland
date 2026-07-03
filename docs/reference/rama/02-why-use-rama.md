# Why Use Rama?
> Source: https://redplanetlabs.com/docs/~/why-use-rama.html
> Local Rama reference — raw extraction from Red Planet Labs documentation

# Why use Rama?

Software engineering, more than any other field of engineering, leverages abstraction and automation. Yet there’s a huge disconnect between how long it takes to describe a software application versus how long it takes to build it. This is true for all applications, but it’s especially true for applications running at huge scale like Twitter, Reddit, Gmail, Splunk, Coinbase, Facebook, and so on. There are so many parts of these applications that you can describe in complete detail in no more than a few hours, and yet they take hundreds or thousands of person years to build. How can that be? What happened to abstraction and automation?

So much time is invested in the plumbing of application backends: how data is ingested, processed, stored, and queried. Engineers dedicate huge amounts of time on tasks like gluing components together, working around impedance mismatches, and dealing with dizzying amounts of configuration. Rather than working on high level product features, time is spent working on dozens upon dozens of low-level tasks that after great effort eventually mold into a product.

Rama is a paradigm shift that rethinks from the ground up how applications should be built. More than anything else Rama’s breakthrough is it’s the first time there’s been a cohesive model for building software applications, no matter the scale. Rama excels for development teams who care about:

- Building application backends end-to-end in up to 100x less code than otherwise
- [Easy integration](integrating.html) with other tools (databases, queues, etc.)
- Iterating on ideas extremely quickly
- Reducing operational burden by significantly lowering the amount of infrastructure needed
- Lowering complexity

Rama is great for developing applications with any or all of the following needs:

- Realtime, transactional applications
- Realtime analytics applications
- Highly reactive applications
- Wide variety of indexing needs
- [Strong fault-tolerance and data consistency guarantees](acid.html)
- Scale to huge numbers of reads and writes
- High performance

Rama eliminates huge swaths of complexity from application backend development, including:

- Needing multiple databases
- Using and operating dozens of disparate tools for one application
- Building ad-hoc deploys for each application
- Low-level glue code for things like serialization and routing
- Impedance mismatches between database models and application models
- Dozens of narrow APIs in potentially different languages

To start learning Rama so you can gain these huge benefits (and have a lot of fun in the process!), start with [our tutorial](tutorial1.html).