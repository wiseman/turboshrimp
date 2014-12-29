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

  * Flight control seems to work.

  * Telemetry works--even GPS!  Telemetry is sent via a callback.

  * Video is in progress.  There's a first draft PaVE parser (with
    latency reduction), and I've experimented with two different H.264
    decoders:

      * [h264j](https://code.google.com/p/h264j/) is pure Java.  It
        decodes at about 30 FPS on my MacBook Air and has some color
        glitches and occasional errors.

      * [xuggler](http://www.xuggle.com/xuggler) has native code and
         decodes at about 50 fps.  Its output appears to be perfect.

  * It runs on Android.  I don't check every commit, but I do
    periodically test on Android.  This code has flown a drone on an
    Android phone.


## Testing

```
$ lein test
```


## License

Copyright 2014 John Wiseman jjwiseman@gmail.com

Distributed under the [Eclipse Public License
v1.0](http://www.eclipse.org/legal/epl-v10.html).
