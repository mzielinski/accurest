stub-runner-spring-cloud
========================

Registers the stubs in the provided Service Discovery. It's enough to add the jar

```
io.codearte.accurest:stub-runner-spring-cloud
```

and the Stub Runner autoconfiguration should be picked up.

You can match the artifactId of the stub with the name of your app by using the `stubrunner.stubs.idsToServiceIds:` map.

You can disable Stub Runner Ribbon support by providing: `stubrunner.cloud.ribbon.enabled` equal to `false`
You can disable Stub Runner support by providing: `stubrunner.cloud.enabled` equal to `false`