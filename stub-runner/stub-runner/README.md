Stub-runner
===========

Runs stubs for service collaborators. Treating stubs as contracts of services allows to use stub-runner as an implementation of 
[Consumer Driven Contracts](http://martinfowler.com/articles/consumerDrivenContracts.html).

### Running stubs

#### Running using main app

You can set the following options to the main class:

```
 -maxp (--maxPort) N            : Maximum port value to be assigned to the
                                  Wiremock instance. Defaults to 15000
                                  (default: 15000)
 -minp (--minPort) N            : Minimal port value to be assigned to the
                                  Wiremock instance. Defaults to 10000
                                  (default: 10000)
 -s (--stubs) VAL               : Comma separated list of Ivy representation of
                                  jars with stubs. Eg. groupid:artifactid1,group
                                  id2:artifactid2:classifier
 -sr (--stubRepositoryRoot) VAL : Location of a Jar containing server where you
                                  keep your stubs (e.g. http://nexus.net/content
                                  /repositories/repository)
 -ss (--stubsSuffix) VAL        : Suffix for the jar containing stubs (e.g.
                                  'stubs' if the stub jar would have a 'stubs'
                                  classifier for stubs: foobar-stubs ).
                                  Defaults to 'stubs' (default: stubs)
 -wo (--workOffline)            : Switch to work offline. Defaults to 'false'
                                  (default: false)

```


#### Building a Fat Jar

Just call the following command:

```
./gradlew stub-runner-root:stub-runner:shadowJar -PfatJar
```

and inside the `build/lib` there will be a Fat Jar with classifier `fatJar` waiting for you to execute. E.g.

```
java -jar stub-runner/stub-runner/build/libs/stub-runner-1.0.1-SNAPSHOT-fatJar.jar -sr http://a.b.com -s a:b:c,d:e,f:g:h 
```

### Stub runner configuration

You can configure the stub runner by either passing the full arguments list with the `-Pargs` like this:

```
./gradlew stub-runner-root:stub-runner:run -Pargs="-c pl -minp 10000 -maxp 10005 -s a:b:c,d:e,f:g:h"
```

or each parameter separately with a `-P` prefix and without the hyphen `-` in the name of the param

```
./gradlew stub-runner-root:stub-runner:run -Pc=pl -Pminp=10000 -Pmaxp=10005 -Ps=a:b:c,d:e,f:g:h
```


#### Stubs

Stubs are defined in JSON documents, whose syntax is defined in [WireMock documentation](http://wiremock.org/stubbing.html)

Example:
```json
{
    "request": {
        "method": "GET",
        "url": "/ping"
    },
    "response": {
        "status": 200,
        "body": "pong",
        "headers": {
            "Content-Type": "text/plain"
        }
    }
}
```

In the provided JAR file we're harvesting all JSON files and try to put them inside running WireMock instance.

#### Viewing registered mappings

Every stubbed collaborator exposes list of defined mappings under `__/admin/` endpoint.