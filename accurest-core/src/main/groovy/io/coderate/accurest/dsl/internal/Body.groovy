package io.coderate.accurest.dsl.internal
import groovy.transform.CompileStatic
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString

@CompileStatic
@ToString(includePackage = false, includeFields = true)
@EqualsAndHashCode(includeFields = true)
class Body {

    private final Map<String, DslProperty> body

    Body() {
        this.body = [:]
    }

    Body(Map<String, DslProperty> body) {
        this.body = body
    }

    Map<String, Object> forClientSide() {
        return body.collectEntries {
            Map.Entry<String, DslProperty> entry -> [(entry.key) : entry.value.clientValue]
        } as Map<String, Object>
    }

    Map<String, Object> forServerSide() {
        return body.collectEntries {
            Map.Entry<String, DslProperty> entry -> [(entry.key) : entry.value.serverValue]
        } as Map<String, Object>
    }
}