# marlin

A simple REST api for interacting with a file system, using redis as a backend

# Build

You will need [Leiningen][1] 2+ installed.

The following will build and run a standalone jar for marlin
```bash
git clone https://github.com/cryptic-io/marlin.git
cd marlin
lein uberjar
java -jar target/marlin*standalone.jar -h
```

Once built you can copy/move this standalone jar file wherever you like.

[1]: https://github.com/technomancy/leiningen

# Basics

marlin is designed as a very simple REST API that can be put on top of a filesystem and allow
external services access to the files being housed.

## Features/Goals

The design goals of marlin are:

* Ability to PUT files into a filesystem which will automatically partition the directory heirarchy
  in a sane way, while at the same time ensuring that the file sent is what was intended (through a
  SHA1 hash which is required to be sent with the file).

* Ability to GET files with no fuss

* Ability to GET metadata about files (their size and sha1 hash) with no fuss

* Ability to DELETE files

* Ability to have multiple instances of marlin running on a single machine, each with its own upload
  point on the filesystem

* SSL support (thanks [ring](https://github.com/ring-clojure/ring)!!!)

* Dead simple configuration and deployment

## Configuration

Once you've compiled marlin you can use the `-d` flag to have it dump default configuration (complete
with comments) to stdout. If you save this configuration and change it, you can have marlin load it
using the `-c` flag.

Marlin requires a running copy of redis (2.6+) to function. Some form of disk persistence is optimal,
which one you decide to go with (snapshot or append-log) will depend on your expected usage. Note that
marlin has (or, it will soon) a command to sync what's in redis with what's actually present on the
filesystem, so technically persistance isn't needed. But if you have a lot of files this could take a
while, so weigh your options.

# Usage

marlin operates an HTTP-REST-API on the port specified in the configuration. The following calls are
supported:

`PUT /<filename>?hash=<filehash>`

Puts the data in the body of the request in a file called `filename`, and double-checks that the data
has the sha1 hash of `filehash`. Returns 200 on success.

`GET /all?json=[0|1]`

Returns the list of all filenames currently being housed. If json is `0` (or ommitted) the list is
simply newline separated, otherwise it comes in the form of a json list.

`GET /<filename>`

Returns the contents of `filename`, or 404 if it doesn't exist.

`GET /<filename>/all?json=[0|1]`

Returns a map of all the attributes marlin has in redis about `filename`, or 404 if it doesn't
exists. If `json` is not set the map is returned as a newline separated list of `key value` lines.

`GET /<filename>/<attribute>`

Returns the value `filename`'s `attribute` in redis. Current supported attributes are `size` and
`hash`. Returns 404 if the file or attribute don't exist.

`DELETE /<filename>?delay=<delay>`

Delete `filename`, and all of its meta-data in redis, from the filesystem, if it exists. Always
returns 200. If `delay` is specified marlin will wait that many milliseconds before executing
the command.

## TODO

* Gooder logging

# License

Copyright © 2013 Cryptic IO LLC
