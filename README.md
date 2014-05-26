# turboshrimp

[![Build Status](https://travis-ci.org/wiseman/turboshrimp.png?branch=master)](https://travis-ci.org/wiseman/turboshrimp) [![Coverage Status](https://coveralls.io/repos/wiseman/turboshrimp/badge.png?branch=master)](https://coveralls.io/r/wiseman/turboshrimp?branch=master)

Turboshrimp is a clojure API for the Parrot AR.Drone.

This code was originally forked from the awesome work gigasquid/Carin
Meier did with [clj-drone](https://github.com/gigasquid/clj-drone).

My changes are mostly about turning the code into a full-featured
library for writing drone applications (like
[node-ar-drone](https://github.com/felixge/node-ar-drone)), with the
following specific goals:

* Keeping the focus on straightforward drone control: I removed the
  OpenCV dependency and the goal/belief-driven programming API.  Those
  are good things, but I think they should be in separate libraries.

* Enhancing the ability to control multiple drones and receive
  telemetry from multiple drones: Replacing single, global vars with
  per-drone data structures.

* Adding a clean way for applications to process drone telemetry, and
  parsing the full set of navdata options from the drone: GPS,
  magneto, vision, etc.


## Testing

```
$ lein test
```


## License

Copyright © Carin Meier, Copyright © 2014 John Wiseman

Distributed under the [Eclipse Public License
v1.0](http://www.eclipse.org/legal/epl-v10.html).
