# Rama Documentation: Introduction
> Source: https://redplanetlabs.com/docs/~/index.html
> Local Rama reference — raw extraction from Red Planet Labs documentation

# Introduction

Rama is a new programming platform implementing a distributed-first paradigm that will radically improve your ability to build applications. By learning Rama you’ll not only add a powerful tool to your development toolkit, you’ll learn a new way of thinking about programming. In the same way that learning about declarative programming helps you become a better programmer if you’ve only been exposed to imperative models, learning Rama’s model will give you a new perspective on problem solving that you’ll be able to apply to any programming challenge.

Rama doesn’t just improve programming for applications that operate at huge scale. Its integrated approach vastly simplifies application development in general. It lets you focus on the logic of your application instead of being encumbered by one low-level hurdle after another. And if your application becomes popular? Well, it already scales.

At a high-level Rama combines what databases and stream processing systems do, except with massively enhanced capabilities. All storage is durable and replicated, and all computation is inherently fault-tolerant. Rama applications are horizontally scalable, and deployment, monitoring, and updating are seamlessly built-in.

This documentation will help you master Rama application development. It will also thoroughly explain the Rama model – a new way of organizing computing resources so that you can seamlessly scale applications while achieving world-class performance and rock-solid consistency guarantees. Along the way you’ll learn Rama’s programming paradigm, a dataflow-oriented approach that brings new levels of expressivity to distributed programming. In Rama, you get to focus on *what* to do rather than *how* to do it.

If you’re eager to jump into some real code, the best place to start is [rama-demo-gallery](https://github.com/redplanetlabs/rama-demo-gallery), an open-source repository of short, self-contained, thoroughly commented examples of applying Rama towards a variety of use cases. These include registering and editing profiles, time-series analytics, transferring money between bank accounts, integrating with external REST APIs, and top-N analytics. Besides the examples being scalable and fault-tolerant, each example also includes unit tests demonstrating how to test Rama applications.

For an even bigger example, you can check out [twitter-scale-mastodon](https://github.com/redplanetlabs/twitter-scale-mastodon). This is an implementation of Mastodon’s backend from scratch that scales to Twitter-scale. It’s also more than 40% less code than Mastodon’s backend implementation and 100x less code than Twitter wrote to build the equivalent. We also published [a post](https://blog.redplanetlabs.com/2023/08/15/how-we-reduced-the-cost-of-building-twitter-at-twitter-scale-by-100x/) doing a deep dive into how `twitter-scale-mastodon` works.

On the Rama team, we love programming. We love the joy that comes from finding elegant solutions to tricky problems, and we love the dopamine hits that come from breathing a project into life. But before Rama those highs were few and far in between, spaced between the lows of configuration hell and constant duct-taping of parts that weren’t designed to fit together. With this guide, we hope to show you how much more fun and productive programming can be.

## How this guide is structured

The [tutorial](tutorial1.html) gently introduces the concepts of Rama and how to apply them towards building real applications: clusters, modules, tasks, depots, PStates, and topologies. On the [last page of the tutorial](tutorial6.html) you’ll see how to combine all these concepts to build the complete backend for a fully scalable social network application in only 180 lines of code.

The [Terminology page](terminology.html) lists the main terminology you’ll see throughout the documentation and when using Rama. It’s meant to be used as a quick reference.

Most of the rest of the pages are deep dives on the various aspects of Rama that were introduced in the tutorial. On those pages you’ll learn more advanced ways the features of Rama can be used.

The [Operating Rama clusters page](operating-rama.html) explains everything you need to know to set up Rama clusters and manage Rama applications as their features and performance needs evolve over time.

The [Integrating with other tools](integrating.html) page explains how to integrate Rama with other tools, whether queues, databases, or other systems.

After working your way through the tutorial, you’ll have enough knowledge to build your own applications. Although there’s a lot more to learn from the other pages, we recommend coming back to them when you encounter use cases that require more advanced features.