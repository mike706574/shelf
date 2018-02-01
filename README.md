# shelf

[![Clojars Project](https://img.shields.io/clojars/v/fun.mike/shelf.svg)](https://clojars.org/fun.mike/shelf)

A small abstraction around executing shell commands, either locally or over SSH.

I had some code that relied on shell commands, and I wasn't sure if I would be deploying it in a place where those shell commands were available locally or if my code would have to SSH to another box. I made this so I wouldn't have to care. It's probably extremely broken.

## Copyright and License

The use and distribution terms for this software are covered by the
[Eclipse Public License 1.0] which can be found in the file
epl-v10.html at the root of this distribution. By using this softwaer
in any fashion, you are agreeing to be bound by the terms of this
license. You must not remove this notice, or any other, from this
software.

[Eclipse Public License 1.0]: http://opensource.org/licenses/eclipse-1.0.php
