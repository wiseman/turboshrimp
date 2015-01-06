# turboshrimp

[![Build Status](https://travis-ci.org/wiseman/turboshrimp.png?branch=master)](https://travis-ci.org/wiseman/turboshrimp) [![Coverage Status](https://coveralls.io/repos/wiseman/turboshrimp/badge.png?branch=master)](https://coveralls.io/r/wiseman/turboshrimp?branch=master)
[![](https://www.codeship.io/projects/bf2c2bd0-d1cb-0131-603c-5af570721319/status)](https://www.codeship.io/projects/bf2c2bd0-d1cb-0131-603c-5af570721319/status)

Turboshrimp is a clojure library for communicating with and
controlling the Parrot AR.Drone.

This code was originally forked from the awesome work gigasquid/Carin
Meier did with [clj-drone](https://github.com/gigasquid/clj-drone).

My changes are mostly about turning the code into a full-featured
library for writing drone applications (like
[node-ar-drone](https://github.com/felixge/node-ar-drone)), with the
following specific goals:

  * Keeping the focus on straightforward drone control: I removed the
    OpenCV dependency and the goal/belief-driven programming API.
    Those are good things, but I think they should be in separate
    libraries.

  * Enhancing the ability to control multiple drones and receive
    telemetry from multiple drones: Replacing single, global vars with
    per-drone data structures.

  * Adding a clean way for applications to process drone telemetry,
    and parsing the full set of navdata options from the drone: GPS,
    magneto, vision, etc.

  * Android compatibility.  I want to be able to use this code on
    Android using [Clojure on Android](http://clojure-android.info/)

I'm basing some of the new work on the
[node-ar-drone](https://github.com/felixge/node-ar-drone) project.


## Current status

  * Flight control works.

  * Telemetry works--even GPS!

  * Video works.  I've tried two different H.264 decoders:

      * [turboshrimp-h264j](https://github.com/wiseman/turboshrimp-h264j)
        uses the pure Java [h264j](https://code.google.com/p/h264j/)
        decoder.  h264j is a little slow and has some color glitches
        and occasional errors.

      * [turboshrimp-xuggler](https://github.com/wiseman/turboshrimp-xuggler)
        uses the [xuggler](http://www.xuggle.com/xuggler) decoder,
        which uses native code.  It's almost twice as fast as h264j
        and its output appears to be perfect.

  * It runs on Android.  I don't check every commit, but I do
    periodically test on Android.  This code has flown a drone on an
    Android phone.


## Example controller

There is an example of using the library to create a simple ground
control station with keyboard control and live video with an overlaid
HUD:
[examples/controller.clj](https://github.com/wiseman/turboshrimp/blob/master/examples/controller.clj)

You can run the example like this:
```
# lein with-profile example run -m controller > debug.log
```

![Alt text](/media/screenshots/turboshrimp-hud.jpg?raw=true "Turboshrimp HUD")

(Hey look, it's Ralph & Dottie!)

Here's a video of the HUD in action: https://www.youtube.com/watch?v=2mOtoYUoiWI

Once the controller has started, you can use the following controls:

Key       | Command
----------|--------
t         | Take off
l         | Land
w/a/s/d   | Forward / turn left / backward/ turn right
shift-a/d |"Strafe" left / right
q / z     | Climb / descend
c         | Switch to forward-facing camera
v         | Switch to downward-facing camera


## Testing

```
$ lein test
```


## License

Copyright 2014, 2015 John Wiseman jjwiseman@gmail.com

Distributed under the [Eclipse Public License
v1.0](http://www.eclipse.org/legal/epl-v10.html).
