# Sid's questions on the path kind — 2026-09-05 session, cleaned verbatim

Transcription fixes and fillers removed only. Order kept. Answers from that session deliberately left out.

## How I am going about this

I will go from each kind to the other kind. If I describe a kind and it is not correct at this level, it means I don't have the correct understanding, and we dig deeper into it. Otherwise not.

(Aside on hit testing, from the text kind:) Hit testing is only for testing and will not be piped into production code. We are not at the position of deciding how to do hit testing, because it is bigger infrastructure: it includes all the other kinds as well, and we are not sure what all the kinds are. Deciding how hit testing should be done is not a concern at this point.

## What I understand the current path kind is

Now the path kind. This is where I'm not sure what the input is. If you treat this as a library, the input is different from how I would think of it as a user. From a user point of view, I am just tracing around the screen. Tracing means that at some rate I am outputting "this is where the pointer is at this instant." At the end of a burst, the burst being marked by pen down and pen up, because that is how we know one event is done, the output is x number of points with their xy coordinates. Then we pass it to the path kind, and the output of it is a shape to be drawn on the screen by the GPU.

The shape is not a rectangle, but between two points is it just a straight line of some width? By straight line I mean there is no concept of a line; we are just saying make things ten pixels wide, mark a ten-pixel-wide dot around this xy point we got. So if I am given three points, what is the algorithm for defining this? One way is take the points as pairs, a rolling window, and for each pair draw a line between them of some width, but don't fill the insides, just mark the border, because the inside might be some color, to be passed as color data maybe in future. It seems currently we don't.

You mentioned it gives a hash and a tessellated mesh. I don't know what that means. What is the dependency? Is there an expectation of how many points are to be passed? What does contour mean? Zoom level: I think you have to calculate, based on some zoom level, how thick or thin a line looks when drawn at that scene. I also heard someone say in a session that there is no concept of curves and that is missing. What does that mean? What is the difference between a curve and what we are doing right now? Maybe I need theory to understand that.

## Digging: Slug, curves, subsets

What is Slug? Is it an implementation only geared towards glyphs? Maybe the broader question is: what is a subset of what? Currently we draw rectangles first and then do tessellation, but we would have to do the same for curves as well, the tessellation part. So in the middle there is just: do we draw rectangles or a curve. Are the algorithms to draw a rectangle and a curve complementary? Can a rectangle be drawn with the data we pass to the curve algorithm? Can we pass the curve data to the rectangle algorithm? If we make the tessellation very, very high, we can basically remove the angles.

So there are three things: given a bunch of points, how do we decide if it is a curve or a rectangle? What is the best way to draw them? And why is there a Slug? This is a graphics problem that had forty, fifty years to get solved. I am far behind, and maybe my questions are all wrong.

## Digging: CPU versus GPU, pre-baked shapes

The trade-off is CPU calculation versus GPU calculation. How are we making decisions there, or how should we? On different zoom levels we have to compute again in the CPU case. If this was done on the GPU, it would just be moving the camera; we do the calculation once. Since the future is going to be all GPUs, I think we can make a bet on the GPU side. Although, since it's GPU based, how much memory are we taking out of the GPU? If the user is running a bunch of local agents, what gets priority? (Question for later.)

So for the ink, should we just go with curves? This brings up another point: you said any closed filled outline could go through a Slug kind of thing. What would those be? tldraw, or any canvas-based app, Blender, Figma, Canva, all have pre-baked shapes, and there are many more than pre-baked shapes: UI component libraries are pre-made. Those shapes are fixed. Do they fall into Slug? What would be the right way to think about all of these?

## Why the current path kind is wrong: boundary and fill, abstraction, self-crossing, representation first

Scratch what you read from the decisions docs. Keep what we were discussing in this session. The questions we were having are only related to the path: two things, boundary and fill. Currently with text we do the fill. But for drawing any random line or curve, the code should not presume there is going to be a fill. It could just be an outline. The code should have better abstraction and more options, because this is a committed goal that is going to live. Anything committed should be written so it solves as many things as possible for that function, thinking through its input, its output, and the definition of what it needs to do.

We were figuring out the different ways to draw. The first, which we currently do: draw rectangles, use the CPU to figure out how many triangles for this level, pass the data to the GPU, it renders. The other way: give a very big rectangle and some shape, and the GPU figures out if this point of the rectangle falls into the shape; draw it, otherwise don't. The caveat: fill the inside or just have a boundary, how thick is the boundary, how thick is the inner filling. Those are extra data that have to be passed, but we should not make assumptions and should be open to as broad a set of possibilities as possible.

Applying this principle solves CPU or GPU: I think GPU, because it makes it coherent with text and seems a better abstraction, given there could be many things to draw. For example a border, then some empty area, then a fill area: three things that on the CPU would have to be calculated, on a GPU can be parallelized.

One question: how to handle a curve that crosses itself? Is the algorithm "whatever comes later gets written on top of whatever came before"? Say I was drawing a knitted sweater, where a thread goes over a thread and under a thread, the same thread going on. How will we show that? Maybe that's not possible with the pen; it's a computation option or a selection the user has to make.

Next: preconfigured shapes and user-defined shapes. How do we draw them? Should they be done on GPU? Actually, talking about GPU is not the right way to think about it. We are one level above, since we decided on the ink and how to represent it. The first problem is how to represent the data of "a user is drawing something." Previously we did it in the format the GPU can consume, and now we need a format that a curve can consume. That's sorted.

Going one level up: we are resuming as if this curve drawing algorithm now exists instead of the rectangle one; is that correct? Then let's think through the layer above. The path kind's input is a path represented by a bunch of points. How we got to those points does not matter. It could very well be a closed shape. So maybe the first divergence: will this algorithm be the best one for drawing closed shapes as well, the formula shapes and the outline shapes? Is the sampled shape different from the other two? You said three tiers and tier three is the one we are doing. Assuming we build tier three, it seems this is the general case of the above two. Is it not?

## Is this how the best teams would think about it?

When I read "that is what text does today, checked," I was thinking: are you basing your answers on how it is done today, and that's why it is trying to do it this way? Or are you thinking from the point of view of this is how it should be, and this is already being done in the text case? Both of those lead to different places, so it is very important to think about how it should be done first. If this problem was given to the best team or people to define this problem, what would they reply? You'd say it's nuanced, there are pros and cons, there need to be trade-offs. But I don't know about them. If I knew better, I would be able to ask better and decide better.

It seems my framing of the question, the level of the hierarchy I was asking at, was wrong. Let's assume these four points are the basis of deciding anything in this area, and let's assume the trade-offs. As you pointed out: what tool am I trying to build? The tool I'm trying to build is every tool that can be built. How I want to go about it: there is this concept of the waist. Please don't go looking at the codebase or doc files, just stay here. The waist concept is: what system should exist as code, and what can be defined as an ECS kind of thing. I do want to make painting apps, vector engines, and all the other 2D anything that can be done. That's why I was asking from the tldraw and Figma point of view: these are the kinds of things that exist in those apps; can we build them up from this curve library we were talking about?

I am not sure how to frame my question, where I stand now, or what my mental model should be. Can you show me what my previous mental model was, how the best teams would think about it, and what is the jump I have to make to get from where I was to that understanding?

From the product point of view: I want to start from where we are, at what architecture is to be built in the codebase, and assume that once this is built, we can do all the functionality all the other apps do. An abstraction layer from bottom to top, and at the user's end we can make anything they want, because everything is possible. Maybe this is getting too meta, too product, and would dilute this whole thing.

## Reconciling with the current files

I lost you. Everything can be broken down into what the inputs and outputs are at this layer. If we follow that through, would we not arrive at this path kind? Or is this problem sidestepped, or is the current path kind doing too many things, and the field breaks it down a different way? What the current path does is define some input shape of data that needs to come in; it also has what the fill color would be, what the zoom level is. I think some of these are required by the CPU calculation. I don't know what I'm asking or what I don't understand.

So what are we doing in the current path files? An acknowledgement from my side: I don't know what the renderer does in the current system. Does it belong to your layer zero or not? If it is the current path/* kind, what should its output be? We send that to the renderer, and you also mention a renderer; I am not sure if both these renderers match.
