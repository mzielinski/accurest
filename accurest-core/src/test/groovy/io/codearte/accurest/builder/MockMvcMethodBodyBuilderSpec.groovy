package io.codearte.accurest.builder

import io.codearte.accurest.dsl.GroovyDsl
import io.codearte.accurest.dsl.WireMockStubVerifier
import spock.lang.Issue
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

import java.util.regex.Pattern

/**
 * @author Jakub Kubrynski
 */
class MockMvcMethodBodyBuilderSpec extends Specification implements WireMockStubVerifier {

	@Shared
	GroovyDsl dslWithOptionalsInString = GroovyDsl.make {
		priority 1
		request {
			method 'POST'
			url '/users/password'
			headers {
				header 'Content-Type': 'application/json'
			}
			body(
					email: $(stub(optional(regex(email()))), test('abc@abc.com')),
					callback_url: $(stub(regex(hostname())), test('http://partners.com'))
			)
		}
		response {
			status 404
			headers {
				header 'Content-Type': 'application/json'
			}
			body(
					code: value(stub("123123"), test(optional("123123"))),
					message: "User not found by email = [${value(test(regex(email())), stub('not.existing@user.com'))}]"
			)
		}
	}

	@Shared
	GroovyDsl dslWithOptionals = GroovyDsl.make {
		priority 1
		request {
			method 'POST'
			url '/users/password'
			headers {
				header 'Content-Type': 'application/json'
			}
			body(
					""" {
								"email" : "${value(stub(optional(regex(email()))), test('abc@abc.com'))}",
								"callback_url" : "${value(client(regex(hostname())), server('http://partners.com'))}"
								}
							"""
			)
		}
		response {
			status 404
			headers {
				header 'Content-Type': 'application/json'
			}
			body(
					""" {
								"code" : "${value(stub(123123), test(optional(123123)))}",
								"message" : "User not found by email = [${
						value(server(regex(email())), client('not.existing@user.com'))
					}]"
								}
							"""
			)
		}
	}

	@Unroll
	def "should generate assertions for simple response body with #methodBuilderName"() {
		given:
			GroovyDsl contractDsl = GroovyDsl.make {
				request {
					method "GET"
					url "test"
				}
				response {
					status 200
					body """{
	"property1": "a",
	"property2": "b"
}"""
				}
			}
			MethodBodyBuilder builder = methodBuilder(contractDsl)
			BlockBuilder blockBuilder = new BlockBuilder(" ")
		when:
			builder.appendTo(blockBuilder)
		then:
			blockBuilder.toString().contains("""assertThatJson(parsedJson).field("property1").isEqualTo("a")""")
			blockBuilder.toString().contains("""assertThatJson(parsedJson).field("property2").isEqualTo("b")""")
		and:
			stubMappingIsValidWireMockStub(contractDsl)
		where:
			methodBuilderName           | methodBuilder
			"MockMvcSpockMethodBuilder" | { GroovyDsl dsl -> new MockMvcSpockMethodBodyBuilder(dsl) }
			"MockMvcJUnitMethodBuilder" | { GroovyDsl dsl -> new MockMvcJUnitMethodBodyBuilder(dsl) }
	}

	@Issue("#187")
	@Unroll
	def "should generate assertions for null and boolean values with #methodBuilderName"() {
		given:
			GroovyDsl contractDsl = GroovyDsl.make {
				request {
					method "GET"
					url "test"
				}
				response {
					status 200
					body """{
	"property1": "true",
	"property2": null,
	"property3": false
}"""
				}
			}
			MethodBodyBuilder builder = methodBuilder(contractDsl)
			BlockBuilder blockBuilder = new BlockBuilder(" ")
		when:
			builder.appendTo(blockBuilder)
		then:
			blockBuilder.toString().contains("""assertThatJson(parsedJson).field("property1").isEqualTo("true")""")
			blockBuilder.toString().contains("""assertThatJson(parsedJson).field("property2").isNull()""")
			blockBuilder.toString().contains("""assertThatJson(parsedJson).field("property3").isEqualTo(false)""")
		and:
			stubMappingIsValidWireMockStub(contractDsl)
		where:
			methodBuilderName           | methodBuilder
			"MockMvcSpockMethodBuilder" | { GroovyDsl dsl -> new MockMvcSpockMethodBodyBuilder(dsl) }
			"MockMvcJUnitMethodBuilder" | { GroovyDsl dsl -> new MockMvcJUnitMethodBodyBuilder(dsl) }
	}

	@Issue("#79")
	@Unroll
	def "should generate assertions for simple response body constructed from map with a list with #methodBuilderName"() {
		given:
			GroovyDsl contractDsl = GroovyDsl.make {
				request {
					method "GET"
					url "test"
				}
				response {
					status 200
					body(
							property1: 'a',
							property2: [
									[a: 'sth'],
									[b: 'sthElse']
							]
					)
				}
			}
			MethodBodyBuilder builder = methodBuilder(contractDsl)
			BlockBuilder blockBuilder = new BlockBuilder(" ")
		when:
			builder.appendTo(blockBuilder)
		then:
			blockBuilder.toString().contains("""assertThatJson(parsedJson).field("property1").isEqualTo("a")""")
			blockBuilder.toString().contains("""assertThatJson(parsedJson).array("property2").contains("a").isEqualTo("sth")""")
			blockBuilder.toString().contains("""assertThatJson(parsedJson).array("property2").contains("b").isEqualTo("sthElse")""")
		and:
			stubMappingIsValidWireMockStub(contractDsl)
		where:
			methodBuilderName           | methodBuilder
			"MockMvcSpockMethodBuilder" | { GroovyDsl dsl -> new MockMvcSpockMethodBodyBuilder(dsl) }
			"MockMvcJUnitMethodBuilder" | { GroovyDsl dsl -> new MockMvcJUnitMethodBodyBuilder(dsl) }
	}

	@Issue("#82")
	@Unroll
	def "should generate proper request when body constructed from map with a list #methodBuilderName"() {
		given:
			GroovyDsl contractDsl = GroovyDsl.make {
				request {
					method "GET"
					url "test"
					body(
							items: ['HOP']
					)
				}
				response {
					status 200
				}
			}
			MethodBodyBuilder builder = methodBuilder(contractDsl)
			BlockBuilder blockBuilder = new BlockBuilder(" ")
		when:
			builder.appendTo(blockBuilder)
		then:
			blockBuilder.toString().contains(bodyString)
		and:
			stubMappingIsValidWireMockStub(contractDsl)
		where:
			methodBuilderName           | methodBuilder                                     | bodyString
			"MockMvcSpockMethodBuilder" | { GroovyDsl dsl -> new MockMvcSpockMethodBodyBuilder(dsl) } | """.body('''{\"items\":[\"HOP\"]}''')"""
			"MockMvcJUnitMethodBuilder" | { GroovyDsl dsl -> new MockMvcJUnitMethodBodyBuilder(dsl) } | '.body("{\\"items\\":[\\"HOP\\"]}")'
	}

	@Issue("#88")
	@Unroll
	def "should generate proper request when body constructed from GString with #methodBuilderName"() {
		given:
			GroovyDsl contractDsl = GroovyDsl.make {
				request {
					method "GET"
					url "test"
					body(
							"property1=VAL1"
					)
				}
				response {
					status 200
				}
			}
			MethodBodyBuilder builder = methodBuilder(contractDsl)
			BlockBuilder blockBuilder = new BlockBuilder(" ")
		when:
			builder.appendTo(blockBuilder)
		then:
			blockBuilder.toString().contains(bodyString)
		and:
			stubMappingIsValidWireMockStub(contractDsl)
		where:
			methodBuilderName           | methodBuilder                                     | bodyString
			"MockMvcSpockMethodBuilder" | { GroovyDsl dsl -> new MockMvcSpockMethodBodyBuilder(dsl) } | """.body('''property1=VAL1''')"""
			"MockMvcJUnitMethodBuilder" | { GroovyDsl dsl -> new MockMvcJUnitMethodBodyBuilder(dsl) } | '.body("\\"property1=VAL1\\"")'
	}

	@Issue("185")
	@Unroll
	def "should generate assertions for a response body containing map with integers as keys with #methodBuilderName"() {
		given:
			GroovyDsl contractDsl = GroovyDsl.make {
				request {
					method "GET"
					url "test"
				}
				response {
					status 200
					body(
							property: [
									14: 0.0,
									7 : 0.0
							]
					)
				}
			}
			MethodBodyBuilder builder = methodBuilder(contractDsl)
			BlockBuilder blockBuilder = new BlockBuilder(" ")
		when:
			builder.appendTo(blockBuilder)
		then:
			blockBuilder.toString().contains("""assertThatJson(parsedJson).field("property").field(7).isEqualTo(0.0)""")
			blockBuilder.toString().contains("""assertThatJson(parsedJson).field("property").field(14).isEqualTo(0.0)""")
		and:
			stubMappingIsValidWireMockStub(contractDsl)
		where:
			methodBuilderName           | methodBuilder
			"MockMvcSpockMethodBuilder" | { GroovyDsl dsl -> new MockMvcSpockMethodBodyBuilder(dsl) }
			"MockMvcJUnitMethodBuilder" | { GroovyDsl dsl -> new MockMvcJUnitMethodBodyBuilder(dsl) }
	}

	@Unroll
	def "should generate assertions for array in response body with #methodBuilderName"() {
		given:
			GroovyDsl contractDsl = GroovyDsl.make {
				request {
					method "GET"
					url "test"
				}
				response {
					status 200
					body """[
{
	"property1": "a"
},
{
	"property2": "b"
}]"""
				}
			}
			MethodBodyBuilder builder = methodBuilder(contractDsl)
			BlockBuilder blockBuilder = new BlockBuilder(" ")
		when:
			builder.appendTo(blockBuilder)
		then:
			blockBuilder.toString().contains("""assertThatJson(parsedJson).array().contains("property2").isEqualTo("b")""")
			blockBuilder.toString().contains("""assertThatJson(parsedJson).array().contains("property1").isEqualTo("a")""")
		and:
			stubMappingIsValidWireMockStub(contractDsl)
		where:
			methodBuilderName           | methodBuilder
			"MockMvcSpockMethodBuilder" | { GroovyDsl dsl -> new MockMvcSpockMethodBodyBuilder(dsl) }
			"MockMvcJUnitMethodBuilder" | { GroovyDsl dsl -> new MockMvcJUnitMethodBodyBuilder(dsl) }
	}

	@Unroll
	def "should generate assertions for array inside response body element with #methodBuilderName"() {
		given:
			GroovyDsl contractDsl = GroovyDsl.make {
				request {
					method "GET"
					url "test"
				}
				response {
					status 200
					body """{
	"property1": [
	{ "property2": "test1"},
	{ "property3": "test2"}
	]
}"""
				}
			}
			MethodBodyBuilder builder = methodBuilder(contractDsl)
			BlockBuilder blockBuilder = new BlockBuilder(" ")
		when:
			builder.appendTo(blockBuilder)
		then:
			blockBuilder.toString().contains("""assertThatJson(parsedJson).array("property1").contains("property2").isEqualTo("test1")""")
			blockBuilder.toString().contains("""assertThatJson(parsedJson).array("property1").contains("property3").isEqualTo("test2")""")
		and:
			stubMappingIsValidWireMockStub(contractDsl)
		where:
			methodBuilderName           | methodBuilder
			"MockMvcSpockMethodBuilder" | { GroovyDsl dsl -> new MockMvcSpockMethodBodyBuilder(dsl) }
			"MockMvcJUnitMethodBuilder" | { GroovyDsl dsl -> new MockMvcJUnitMethodBodyBuilder(dsl) }
	}

	@Unroll
	def "should generate assertions for nested objects in response body with #methodBuilderName"() {
		given:
			GroovyDsl contractDsl = GroovyDsl.make {
				request {
					method "GET"
					url "test"
				}
				response {
					status 200
					body '''\
{
	"property1": "a",
	"property2": {"property3": "b"}
}
'''
				}
			}
			MethodBodyBuilder builder = methodBuilder(contractDsl)
			BlockBuilder blockBuilder = new BlockBuilder(" ")
		when:
			builder.appendTo(blockBuilder)
		then:
			blockBuilder.toString().contains("""assertThatJson(parsedJson).field("property2").field("property3").isEqualTo("b")""")
			blockBuilder.toString().contains("""assertThatJson(parsedJson).field("property1").isEqualTo("a")""")
		and:
			stubMappingIsValidWireMockStub(contractDsl)
		where:
			methodBuilderName           | methodBuilder
			"MockMvcSpockMethodBuilder" | { GroovyDsl dsl -> new MockMvcSpockMethodBodyBuilder(dsl) }
			"MockMvcJUnitMethodBuilder" | { GroovyDsl dsl -> new MockMvcJUnitMethodBodyBuilder(dsl) }
	}

	@Unroll
	def "should generate regex assertions for map objects in response body with #methodBuilderName"() {
		given:
			GroovyDsl contractDsl = GroovyDsl.make {
				request {
					method "GET"
					url "test"
				}
				response {
					status 200
					body(
							property1: "a",
							property2: value(
									client('123'),
									server(regex('[0-9]{3}'))
							)
					)
					headers {
						header('Content-Type': 'application/json')
					}
				}
			}
			MethodBodyBuilder builder = methodBuilder(contractDsl)
			BlockBuilder blockBuilder = new BlockBuilder(" ")
		when:
			builder.appendTo(blockBuilder)
		then:
			blockBuilder.toString().contains("""assertThatJson(parsedJson).field("property2").matches("[0-9]{3}")""")
			blockBuilder.toString().contains("""assertThatJson(parsedJson).field("property1").isEqualTo("a")""")
		and:
			stubMappingIsValidWireMockStub(contractDsl)
		where:
			methodBuilderName           | methodBuilder
			"MockMvcSpockMethodBuilder" | { GroovyDsl dsl -> new MockMvcSpockMethodBodyBuilder(dsl) }
			"MockMvcJUnitMethodBuilder" | { GroovyDsl dsl -> new MockMvcJUnitMethodBodyBuilder(dsl) }
	}

	@Unroll
	def "should generate regex assertions for string objects in response body with #methodBuilderName"() {
		given:
			GroovyDsl contractDsl = GroovyDsl.make {
				request {
					method "GET"
					url "test"
				}
				response {
					status 200
					body("""{"property1":"a","property2":"${value(client('123'), server(regex('[0-9]{3}')))}"}""")
					headers {
						header('Content-Type': 'application/json')
					}
				}
			}
			MethodBodyBuilder builder = methodBuilder(contractDsl)
			BlockBuilder blockBuilder = new BlockBuilder(" ")
		when:
			builder.appendTo(blockBuilder)
		then:
			blockBuilder.toString().contains("""assertThatJson(parsedJson).field("property2").matches("[0-9]{3}")""")
			blockBuilder.toString().contains("""assertThatJson(parsedJson).field("property1").isEqualTo("a")""")
		and:
			stubMappingIsValidWireMockStub(contractDsl)
		where:
			methodBuilderName           | methodBuilder
			"MockMvcSpockMethodBuilder" | { GroovyDsl dsl -> new MockMvcSpockMethodBodyBuilder(dsl) }
			"MockMvcJUnitMethodBuilder" | { GroovyDsl dsl -> new MockMvcJUnitMethodBodyBuilder(dsl) }
	}

	@Issue(["#126", "#143"])
	@Unroll
	def "should generate escaped regex assertions for string objects in response body with #methodBuilderName"() {
		given:
			GroovyDsl contractDsl = GroovyDsl.make {
				request {
					method "GET"
					url "test"
				}
				response {
					status 200
					body("""{"property":"  ${value(client('123'), server(regex('\\d+')))}"}""")
					headers {
						header('Content-Type': 'application/json')
					}
				}
			}
			MethodBodyBuilder builder = methodBuilder(contractDsl)
			BlockBuilder blockBuilder = new BlockBuilder(" ")
		when:
			builder.appendTo(blockBuilder)
		then:
			blockBuilder.toString().contains("""assertThatJson(parsedJson).field("property").matches("\\\\d+")""")
		and:
			stubMappingIsValidWireMockStub(contractDsl)
		where:
			methodBuilderName           | methodBuilder
			"MockMvcSpockMethodBuilder" | { GroovyDsl dsl -> new MockMvcSpockMethodBodyBuilder(dsl) }
			"MockMvcJUnitMethodBuilder" | { GroovyDsl dsl -> new MockMvcJUnitMethodBodyBuilder(dsl) }
	}

	@Unroll
	def "should generate a call with an url path and query parameters with #methodBuilderName"() {
		given:
			GroovyDsl contractDsl = GroovyDsl.make {
				request {
					method 'GET'
					urlPath('/users') {
						queryParameters {
							parameter 'limit': $(client(equalTo("20")), server(equalTo("10")))
							parameter 'offset': $(client(containing("20")), server(equalTo("20")))
							parameter 'filter': "email"
							parameter 'sort': equalTo("name")
							parameter 'search': $(client(notMatching(~/^\/[0-9]{2}$/)), server("55"))
							parameter 'age': $(client(notMatching("^\\w*\$")), server("99"))
							parameter 'name': $(client(matching("Denis.*")), server("Denis.Stepanov"))
							parameter 'email': "bob@email.com"
							parameter 'hello': $(client(matching("Denis.*")), server(absent()))
							parameter 'hello': absent()
						}
					}
				}
				response {
					status 200
					body """
					{
						"property1": "a",
						"property2": "b"
					}
					"""
				}
			}
			MethodBodyBuilder builder = methodBuilder(contractDsl)
			BlockBuilder blockBuilder = new BlockBuilder(" ")
		when:
			builder.appendTo(blockBuilder)
			def test = blockBuilder.toString()
		then:
			test.contains('get("/users?limit=10&offset=20&filter=email&sort=name&search=55&age=99&name=Denis.Stepanov&email=bob@email.com")')
			test.contains('assertThatJson(parsedJson).field("property1").isEqualTo("a")')
			test.contains('assertThatJson(parsedJson).field("property2").isEqualTo("b")')
		and:
			stubMappingIsValidWireMockStub(contractDsl)
		where:
			methodBuilderName           | methodBuilder
			"MockMvcSpockMethodBuilder" | { GroovyDsl dsl -> new MockMvcSpockMethodBodyBuilder(dsl) }
			"MockMvcJUnitMethodBuilder" | { GroovyDsl dsl -> new MockMvcJUnitMethodBodyBuilder(dsl) }
	}

	@Issue('#169')
	@Unroll
	def "should generate a call with an url path and query parameters with url containing a pattern with #methodBuilderName"() {
		given:
			GroovyDsl contractDsl = GroovyDsl.make {
				request {
					method 'GET'
					url($(stub(regex('/foo/[0-9]+')), test('/foo/123456'))) {
						queryParameters {
							parameter 'limit': $(client(equalTo("20")), server(equalTo("10")))
							parameter 'offset': $(client(containing("20")), server(equalTo("20")))
							parameter 'filter': "email"
							parameter 'sort': equalTo("name")
							parameter 'search': $(client(notMatching(~/^\/[0-9]{2}$/)), server("55"))
							parameter 'age': $(client(notMatching("^\\w*\$")), server("99"))
							parameter 'name': $(client(matching("Denis.*")), server("Denis.Stepanov"))
							parameter 'email': "bob@email.com"
							parameter 'hello': $(client(matching("Denis.*")), server(absent()))
							parameter 'hello': absent()
						}
					}
				}
				response {
					status 200
					body """
					{
						"property1": "a",
						"property2": "b"
					}
					"""
				}
			}
			MethodBodyBuilder builder = methodBuilder(contractDsl)
			BlockBuilder blockBuilder = new BlockBuilder(" ")
		when:
			builder.appendTo(blockBuilder)
			def test = blockBuilder.toString()
		then:
			test.contains('get("/foo/123456?limit=10&offset=20&filter=email&sort=name&search=55&age=99&name=Denis.Stepanov&email=bob@email.com")')
			test.contains('assertThatJson(parsedJson).field("property1").isEqualTo("a")')
			test.contains('assertThatJson(parsedJson).field("property2").isEqualTo("b")')
		and:
			stubMappingIsValidWireMockStub(contractDsl)
		where:
			methodBuilderName           | methodBuilder
			"MockMvcSpockMethodBuilder" | { GroovyDsl dsl -> new MockMvcSpockMethodBodyBuilder(dsl) }
			"MockMvcJUnitMethodBuilder" | { GroovyDsl dsl -> new MockMvcJUnitMethodBodyBuilder(dsl) }
	}

	@Unroll
	def "should generate test for empty body with #methodBuilderName"() {
		given:
			GroovyDsl contractDsl = GroovyDsl.make {
				request {
					method('POST')
					url("/ws/payments")
					body("")
				}
				response {
					status 406
				}
			}
			MethodBodyBuilder builder = methodBuilder(contractDsl)
			BlockBuilder blockBuilder = new BlockBuilder(" ")
		when:
			builder.appendTo(blockBuilder)
			def test = blockBuilder.toString()
		then:
			test.contains(bodyString)
		and:
			stubMappingIsValidWireMockStub(contractDsl)
		where:
			methodBuilderName           | methodBuilder                                     | bodyString
			"MockMvcSpockMethodBuilder" | { GroovyDsl dsl -> new MockMvcSpockMethodBodyBuilder(dsl) } | ".body('''''')"
			"MockMvcJUnitMethodBuilder" | { GroovyDsl dsl -> new MockMvcJUnitMethodBodyBuilder(dsl) } | ".body(\"\\\"\\\"\")"
	}

	@Unroll
	def "should generate test for String in response body with #methodBuilderName"() {
		given:
			GroovyDsl contractDsl = GroovyDsl.make {
				request {
					method "POST"
					url "test"
				}
				response {
					status 200
					body "test"
				}
			}
			MethodBodyBuilder builder = methodBuilder(contractDsl)
			BlockBuilder blockBuilder = new BlockBuilder(" ")
		when:
			builder.appendTo(blockBuilder)
			def test = blockBuilder.toString()
		then:
			test.contains(bodyDefinitionString)
			test.contains(bodyEvaluationString)
		and:
			stubMappingIsValidWireMockStub(contractDsl)
		where:
			methodBuilderName           | methodBuilder                                     | bodyDefinitionString                                     | bodyEvaluationString
			"MockMvcSpockMethodBuilder" | { GroovyDsl dsl -> new MockMvcSpockMethodBodyBuilder(dsl) } | 'def responseBody = (response.body.asString())'          | 'responseBody == "test"'
			"MockMvcJUnitMethodBuilder" | { GroovyDsl dsl -> new MockMvcJUnitMethodBodyBuilder(dsl) } | 'Object responseBody = (response.getBody().asString());' | 'assertThat(responseBody).isEqualTo("test");'
	}

	@Issue('113')
	@Unroll
	def "should generate regex test for String in response header with #methodBuilderName"() {
		given:
			GroovyDsl contractDsl = GroovyDsl.make {
				request {
					method 'POST'
					url $(client(regex('/partners/[0-9]+/users')), server('/partners/1000/users'))
					headers { header 'Content-Type': 'application/json' }
					body(
							first_name: 'John',
							last_name: 'Smith',
							personal_id: '12345678901',
							phone_number: '500500500',
							invitation_token: '00fec7141bb94793bfe7ae1d0f39bda0',
							password: 'john'
					)
				}
				response {
					status 201
					headers {
						header 'Location': $(client('http://localhost/partners/1000/users/1001'), server(regex('http://localhost/partners/[0-9]+/users/[0-9]+')))
					}
				}
			}
			MethodBodyBuilder builder = methodBuilder(contractDsl)
			BlockBuilder blockBuilder = new BlockBuilder(" ")
		when:
			builder.appendTo(blockBuilder)
			def test = blockBuilder.toString()
		then:
			test.contains(headerEvaluationString)
		and:
			stubMappingIsValidWireMockStub(contractDsl)
		where:
			methodBuilderName           | methodBuilder                                     | headerEvaluationString
			"MockMvcSpockMethodBuilder" | { GroovyDsl dsl -> new MockMvcSpockMethodBodyBuilder(dsl) } | '''response.header('Location') ==~ java.util.regex.Pattern.compile('http://localhost/partners/[0-9]+/users/[0-9]+')'''
			"MockMvcJUnitMethodBuilder" | { GroovyDsl dsl -> new MockMvcJUnitMethodBodyBuilder(dsl) } | 'assertThat(response.header("Location")).matches("http://localhost/partners/[0-9]+/users/[0-9]+");'
	}

	@Issue('115')
	@Unroll
	def "should generate regex with helper method with #methodBuilderName"() {
		given:
			GroovyDsl contractDsl = GroovyDsl.make {
				request {
					method 'POST'
					url $(client(regex('/partners/[0-9]+/users')), server('/partners/1000/users'))
					headers { header 'Content-Type': 'application/json' }
					body(
							first_name: 'John',
							last_name: 'Smith',
							personal_id: '12345678901',
							phone_number: '500500500',
							invitation_token: '00fec7141bb94793bfe7ae1d0f39bda0',
							password: 'john'
					)
				}
				response {
					status 201
					headers {
						header 'Location': $(client('http://localhost/partners/1000/users/1001'), server(regex("^${hostname()}/partners/[0-9]+/users/[0-9]+")))
					}
				}
			}
			MethodBodyBuilder builder = methodBuilder(contractDsl)
			BlockBuilder blockBuilder = new BlockBuilder(" ")
		when:
			builder.appendTo(blockBuilder)
			def test = blockBuilder.toString()
		then:
			test.contains(headerEvaluationString)
		and:
			stubMappingIsValidWireMockStub(contractDsl)
		where:
			methodBuilderName           | methodBuilder                                     | headerEvaluationString
			"MockMvcSpockMethodBuilder" | { GroovyDsl dsl -> new MockMvcSpockMethodBodyBuilder(dsl) } | '''response.header('Location') ==~ java.util.regex.Pattern.compile('^((http[s]?|ftp):\\/)\\/?([^:\\/\\s]+)(:[0-9]{1,5})?/partners/[0-9]+/users/[0-9]+')'''
			"MockMvcJUnitMethodBuilder" | { GroovyDsl dsl -> new MockMvcJUnitMethodBodyBuilder(dsl) } | 'assertThat(response.header("Location")).matches("^((http[s]?|ftp):/)/?([^:/s]+)(:[0-9]{1,5})?/partners/[0-9]+/users/[0-9]+");'
	}

	@Unroll
	def "should work with more complex stuff and jsonpaths with #methodBuilderName"() {
		given:
			GroovyDsl contractDsl = GroovyDsl.make {
				priority 10
				request {
					method 'POST'
					url '/validation/client'
					headers {
						header 'Content-Type': 'application/json'
					}
					body(
							bank_account_number: '0014282912345698765432161182',
							email: 'foo@bar.com',
							phone_number: '100299300',
							personal_id: 'ABC123456'
					)
				}

				response {
					status 200
					body(errors: [
							[property: "bank_account_number", message: "incorrect_format"]
					])
				}
			}
			MethodBodyBuilder builder = methodBuilder(contractDsl)
			BlockBuilder blockBuilder = new BlockBuilder(" ")
		when:
			builder.appendTo(blockBuilder)
			def test = blockBuilder.toString()
		then:
			test.contains("""assertThatJson(parsedJson).array("errors").contains("property").isEqualTo("bank_account_number")""")
			test.contains("""assertThatJson(parsedJson).array("errors").contains("message").isEqualTo("incorrect_format")""")
		and:
			stubMappingIsValidWireMockStub(contractDsl)
		where:
			methodBuilderName           | methodBuilder
			"MockMvcSpockMethodBuilder" | { GroovyDsl dsl -> new MockMvcSpockMethodBodyBuilder(dsl) }
			"MockMvcJUnitMethodBuilder" | { GroovyDsl dsl -> new MockMvcJUnitMethodBodyBuilder(dsl) }
	}

	@Unroll
	def "should work properly with GString url with #methodBuilderName"() {
		given:
			GroovyDsl contractDsl = GroovyDsl.make {

				request {
					method 'PUT'
					url "/partners/${value(client(regex('^[0-9]*$')), server('11'))}/agents/11/customers/09665703Z"
					headers {
						header 'Content-Type': 'application/json'
					}
					body(
							first_name: 'Josef',
					)
				}
				response {
					status 422
				}
			}
			MethodBodyBuilder builder = methodBuilder(contractDsl)
			BlockBuilder blockBuilder = new BlockBuilder(" ")
		when:
			builder.appendTo(blockBuilder)
			def test = blockBuilder.toString()
		then:
			test.contains('''/partners/11/agents/11/customers/09665703Z''')
		and:
			stubMappingIsValidWireMockStub(contractDsl)
		where:
			methodBuilderName           | methodBuilder
			"MockMvcSpockMethodBuilder" | { GroovyDsl dsl -> new MockMvcSpockMethodBodyBuilder(dsl) }
			"MockMvcJUnitMethodBuilder" | { GroovyDsl dsl -> new MockMvcJUnitMethodBodyBuilder(dsl) }
	}

	@Unroll
	def "should resolve properties in GString with regular expression with #methodBuilderName"() {
		given:
			GroovyDsl contractDsl = GroovyDsl.make {
				priority 1
				request {
					method 'POST'
					url '/users/password'
					headers {
						header 'Content-Type': 'application/json'
					}
					body(
							email: $(client(regex(email())), server('not.existing@user.com')),
							callback_url: $(client(regex(hostname())), server('http://partners.com'))
					)
				}
				response {
					status 404
					headers {
						header 'Content-Type': 'application/json'
					}
					body(
							code: 4,
							message: "User not found by email = [${value(server(regex(email())), client('not.existing@user.com'))}]"
					)
				}
			}
			MethodBodyBuilder builder = methodBuilder(contractDsl)
			BlockBuilder blockBuilder = new BlockBuilder(" ")
		when:
			builder.appendTo(blockBuilder)
			def test = blockBuilder.toString()
		then:
			test.contains("""assertThatJson(parsedJson).field("message").matches("User not found by email = \\\\\\\\[[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\\\\\\\.[a-zA-Z]{2,4}\\\\\\\\]")""")
		where:
			methodBuilderName           | methodBuilder
			"MockMvcSpockMethodBuilder" | { GroovyDsl dsl -> new MockMvcSpockMethodBodyBuilder(dsl) }
			"MockMvcJUnitMethodBuilder" | { GroovyDsl dsl -> new MockMvcJUnitMethodBodyBuilder(dsl) }
	}

	@Issue('42')
	@Unroll
	def "should not omit the optional field in the test creation with MockMvcSpockMethodBodyBuilder"() {
		given:
			MethodBodyBuilder builder = new MockMvcSpockMethodBodyBuilder(contractDsl)
			BlockBuilder blockBuilder = new BlockBuilder(" ")
		when:
			builder.appendTo(blockBuilder)
			def test = blockBuilder.toString()
		then:
			test.contains('''"email":"abc@abc.com"''')
			test.contains("""assertThatJson(parsedJson).field("code").matches("(123123)?")""")
			!test.contains('''REGEXP''')
			!test.contains('''OPTIONAL''')
			!test.contains('''OptionalProperty''')
		where:
			contractDsl << [dslWithOptionals, dslWithOptionalsInString]
	}

	@Issue('42')
	@Unroll
	def "should not omit the optional field in the test creation with MockMvcJUnitMethodBodyBuilder"() {
		given:
			MethodBodyBuilder builder = new MockMvcJUnitMethodBodyBuilder(contractDsl)
			BlockBuilder blockBuilder = new BlockBuilder(" ")
		when:
			builder.appendTo(blockBuilder)
			def test = blockBuilder.toString()
		then:
			test.contains('\\"email\\":\\"abc@abc.com\\"')
			test.contains('assertThatJson(parsedJson).field("code").matches("(123123)?");')
			!test.contains('''REGEXP''')
			!test.contains('''OPTIONAL''')
			!test.contains('''OptionalProperty''')
		where:
			contractDsl << [dslWithOptionals, dslWithOptionalsInString]
	}

	@Issue('72')
	@Unroll
	def "should make the execute method work with #methodBuilderName"() {
		given:
			GroovyDsl contractDsl = GroovyDsl.make {
				request {
					method """PUT"""
					url """/fraudcheck"""
					body("""
                        {
                        "clientPesel":"${value(client(regex('[0-9]{10}')), server('1234567890'))}",
                        "loanAmount":123.123
                        }
                    """
					)
					headers {
						header("""Content-Type""", """application/vnd.fraud.v1+json""")
					}

				}
				response {
					status 200
					body("""{
    "fraudCheckStatus": "OK",
    "rejectionReason": ${value(client(null), server(execute('assertThatRejectionReasonIsNull($it)')))}
}""")
					headers {
						header('Content-Type': 'application/vnd.fraud.v1+json')
						header 'Location': value(
							stub(null),
							test(execute('assertThatLocationIsNull($it)'))
						)
					}
				}
			}
			MethodBodyBuilder builder = methodBuilder(contractDsl)
			BlockBuilder blockBuilder = new BlockBuilder(" ")
		when:
			builder.appendTo(blockBuilder)
			String test = blockBuilder.toString()
		then:
			assertionStrings.each { String assertionString ->
				assert test.contains(assertionString)
			}
		where:
			methodBuilderName           | methodBuilder                                               | assertionStrings
			"MockMvcSpockMethodBuilder" | { GroovyDsl dsl -> new MockMvcSpockMethodBodyBuilder(dsl) } | ['''assertThatRejectionReasonIsNull(parsedJson.read('$.rejectionReason'))''', '''assertThatLocationIsNull(response.header('Location'))''']
			"MockMvcJUnitMethodBuilder" | { GroovyDsl dsl -> new MockMvcJUnitMethodBodyBuilder(dsl) } | ['''assertThatRejectionReasonIsNull(parsedJson.read("$.rejectionReason"))''', '''assertThatLocationIsNull(response.header("Location"))''']
	}

	@Unroll
	def "should support inner map and list definitions with #methodBuilderName"() {
		given:

			Pattern PHONE_NUMBER = Pattern.compile(/[+\w]*/)
			Pattern ANYSTRING = Pattern.compile(/.*/)
			Pattern NUMBERS = Pattern.compile(/[\d\.]*/)
			Pattern DATETIME = ANYSTRING

			GroovyDsl contractDsl = GroovyDsl.make {
				request {
					method "PUT"
					url "/v1/payments/e86df6f693de4b35ae648464c5b0dc09/client_data"
					headers {
						header('Content-Type': 'application/json')
					}
					body(
							client: [
									first_name   : $(stub(regex(onlyAlphaUnicode())), test('Denis')),
									last_name    : $(stub(regex(onlyAlphaUnicode())), test('FakeName')),
									email        : $(stub(regex(email())), test('fakemail@fakegmail.com')),
									fax          : $(stub(PHONE_NUMBER), test('+xx001213214')),
									phone        : $(stub(PHONE_NUMBER), test('2223311')),
									data_of_birth: $(stub(DATETIME), test('2002-10-22T00:00:00Z'))
							],
							client_id_card: [
									id           : $(stub(ANYSTRING), test('ABC12345')),
									date_of_issue: $(stub(ANYSTRING), test('2002-10-02T00:00:00Z')),
									address      : [
											street : $(stub(ANYSTRING), test('Light Street')),
											city   : $(stub(ANYSTRING), test('Fire')),
											region : $(stub(ANYSTRING), test('Skys')),
											country: $(stub(ANYSTRING), test('HG')),
											zip    : $(stub(NUMBERS), test('658965'))
									]
							],
							incomes_and_expenses: [
									monthly_income         : $(stub(NUMBERS), test('0.0')),
									monthly_loan_repayments: $(stub(NUMBERS), test('100')),
									monthly_living_expenses: $(stub(NUMBERS), test('22'))
							],
							additional_info: [
									allow_to_contact: $(stub(optional(regex(anyBoolean()))), test('true'))
							]
					)
				}
				response {
					status 200
					headers {
						header('Content-Type': 'application/json')
					}
				}
			}
			MethodBodyBuilder builder = methodBuilder(contractDsl)
			BlockBuilder blockBuilder = new BlockBuilder(" ")
		when:
			builder.appendTo(blockBuilder)
			def test = blockBuilder.toString()
		then:
			test.contains bodyString
			!test.contains("clientValue")
			!test.contains("cursor")
		where:
			methodBuilderName           | methodBuilder                                     | bodyString
			"MockMvcSpockMethodBuilder" | { GroovyDsl dsl -> new MockMvcSpockMethodBodyBuilder(dsl) } | '"street":"Light Street"'
			"MockMvcJUnitMethodBuilder" | { GroovyDsl dsl -> new MockMvcJUnitMethodBodyBuilder(dsl) } | '\\"street\\":\\"Light Street\\"'

	}

	@Unroll
	def "shouldn't generate unicode escape characters with #methodBuilderName"() {
		given:
			Pattern ONLY_ALPHA_UNICODE = Pattern.compile(/[\p{L}]*/)

			GroovyDsl contractDsl = GroovyDsl.make {
				request {
					method "PUT"
					url "/v1/payments/e86df6f693de4b35ae648464c5b0dc09/енев"
					headers {
						header('Content-Type': 'application/json')
					}
					body(
							client: [
									first_name: $(stub(ONLY_ALPHA_UNICODE), test('Пенева')),
									last_name : $(stub(ONLY_ALPHA_UNICODE), test('Пенева'))
							]
					)
				}
				response {
					status 200
					headers {
						header('Content-Type': 'application/json')
					}
				}
			}
			MethodBodyBuilder builder = methodBuilder(contractDsl)
			BlockBuilder blockBuilder = new BlockBuilder(" ")
		when:
			builder.appendTo(blockBuilder)
			def test = blockBuilder.toString()
		then:
			!test.contains("\\u041f")
		where:
			methodBuilderName           | methodBuilder
			"MockMvcSpockMethodBuilder" | { GroovyDsl dsl -> new MockMvcSpockMethodBodyBuilder(dsl) }
			"MockMvcJUnitMethodBuilder" | { GroovyDsl dsl -> new MockMvcJUnitMethodBodyBuilder(dsl) }
	}

	@Issue('177')
	@Unroll
	def "should generate proper test code when having multiline body with #methodBuilderName"() {
		given:
			GroovyDsl contractDsl = GroovyDsl.make {
				request {
					method "PUT"
					url "/multiline"
					body('''hello,
World.''')
				}
				response {
					status 200
				}
			}
			MethodBodyBuilder builder = methodBuilder(contractDsl)
			BlockBuilder blockBuilder = new BlockBuilder(" ")
		when:
			builder.given(blockBuilder)
			def test = blockBuilder.toString()
		then:
			test.contains(bodyString)
		where:
			methodBuilderName           | methodBuilder                                     | bodyString
			"MockMvcSpockMethodBuilder" | { GroovyDsl dsl -> new MockMvcSpockMethodBodyBuilder(dsl) } | """'''hello,
World.'''"""
			"MockMvcJUnitMethodBuilder" | { GroovyDsl dsl -> new MockMvcJUnitMethodBodyBuilder(dsl) } | '\\"hello,\\nWorld.\\"'
	}

	@Issue('180')
	@Unroll
	def "should generate proper test code when having multipart parameters with #methodBuilderName"() {
		given:
			GroovyDsl contractDsl = GroovyDsl.make {
				request {
					method "PUT"
					url "/multipart"
					headers {
						header('content-type', 'multipart/form-data;boundary=AaB03x')
					}
					multipart(
							formParameter: value(client(regex('.+')), server('"formParameterValue"')),
							someBooleanParameter: value(client(regex('(true|false)')), server('true')),
							file: named(value(client(regex('.+')), server('filename.csv')), value(client(regex('.+')), server('file content')))
					)
				}
				response {
					status 200
				}
			}
			MethodBodyBuilder builder = methodBuilder(contractDsl)
			BlockBuilder blockBuilder = new BlockBuilder(" ")
		when:
			builder.appendTo(blockBuilder)
			def test = blockBuilder.toString()
		then:
			for (String requestString : requestStrings) {
				test.contains(requestString)
			}
		where:
			methodBuilderName           | methodBuilder                                     | requestStrings
			"MockMvcSpockMethodBuilder" | { GroovyDsl dsl -> new MockMvcSpockMethodBodyBuilder(dsl) } | ["""'content-type', 'multipart/form-data;boundary=AaB03x'""",
			                                                                                   """.param('formParameter', '"formParameterValue"'""",
			                                                                                   """.param('someBooleanParameter', 'true')""",
			                                                                                   """.multiPart('file', 'filename.csv', 'file content'.bytes)"""]
			"MockMvcJUnitMethodBuilder" | { GroovyDsl dsl -> new MockMvcJUnitMethodBodyBuilder(dsl) } | ['"content-type", "multipart/form-data;boundary=AaB03x"',
			                                                                                   '.param("formParameter", "\\"formParameterValue\\"")',
			                                                                                   '.param("someBooleanParameter", "true")',
			                                                                                   '.multiPart("file", "filename.csv", "file content".getBytes());']
	}

	@Issue('180')
	@Unroll
	def "should generate proper test code when having multipart parameters with named as map with #methodBuilderName"() {
		given:
			GroovyDsl contractDsl = GroovyDsl.make {
				request {
					method "PUT"
					url "/multipart"
					multipart(
							formParameter: value(client(regex('".+"')), server('"formParameterValue"')),
							someBooleanParameter: value(client(regex('(true|false)')), server('true')),
							file: named(
									name: value(client(regex('.+')), server('filename.csv')),
									content: value(client(regex('.+')), server('file content')))
					)
				}
				response {
					status 200
				}
			}
			MethodBodyBuilder builder = methodBuilder(contractDsl)
			BlockBuilder blockBuilder = new BlockBuilder(" ")
		when:
			builder.given(blockBuilder)
			def test = blockBuilder.toString()
		then:
			test.contains('.multiPart')
		where:
			methodBuilderName           | methodBuilder
			"MockMvcSpockMethodBuilder" | { GroovyDsl dsl -> new MockMvcSpockMethodBodyBuilder(dsl) }
			"MockMvcJUnitMethodBuilder" | { GroovyDsl dsl -> new MockMvcJUnitMethodBodyBuilder(dsl) }
	}

}
