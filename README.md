# frog Milan Salone: Drone Race

Source code for the Drone Race game presented at frog Milan event for the Salone del Mobile 2016.
This is a racing and shooting game that is based on [Parrot JumpingSumo](http://www.parrot.com/usa/products/minidrones/jumping-race-drone/max/) drones and computer vision.

For a description of the project please check [this article](http://www.google.it)

The game is implemented by:
- An Android Mobile app that acts as remote controller, written mostly in Kotlin language, with parts in RenderScript/C and Java.
- A simple nodeJS based server component, acting as a broker to route messages among the game participants

This codebase was created to support a single event, as a hack journey in the fields of
robotics, computer vision, game design, hastily put together in our spare time.

That means a warning is due: bumpy code ahead - server side especially, code is
kind of messy. Client side, we tried to exploit the functional nature of Kotlin + RxJava,
to explore the potential of the technologies.
