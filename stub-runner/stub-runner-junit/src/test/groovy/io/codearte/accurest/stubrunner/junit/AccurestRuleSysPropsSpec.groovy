package io.codearte.accurest.stubrunner.junit

import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.ClassRule
import spock.lang.Shared
import spock.lang.Specification

/**
 * @author Marcin Grzejszczak
 */
class AccurestRuleSysPropsSpec extends Specification {

	@BeforeClass
	void setProps() {
		System.properties.setProperty("stubrunner.stubs.repository.root", AccurestRuleSysPropsSpec.getResource("/m2repo").toURI().toString())
		System.properties.setProperty("stubrunner.stubs.classifier", 'classifier that will be overridden')
	}

	@AfterClass
	void cleanupProps() {
		System.getProperties().setProperty("stubrunner.stubs.repository.root", "");
		System.getProperties().setProperty("stubrunner.stubs.classifier", "stubs");
	}

	@ClassRule @Shared AccurestRule rule = new AccurestRule()
			.downloadStub("io.codearte.accurest.stubs", "loanIssuance", "stubs")
			.downloadStub("io.codearte.accurest.stubs:fraudDetectionServer:stubs")

	def 'should start WireMock servers'() {
		expect: 'WireMocks are running'
			rule.findStubUrl('io.codearte.accurest.stubs', 'loanIssuance') != null
			rule.findStubUrl('loanIssuance') != null
			rule.findStubUrl('loanIssuance') == rule.findStubUrl('io.codearte.accurest.stubs', 'loanIssuance')
			rule.findStubUrl('io.codearte.accurest.stubs:fraudDetectionServer') != null
		and: 'Stubs were registered'
			"${rule.findStubUrl('loanIssuance').toString()}/name".toURL().text == 'loanIssuance'
			"${rule.findStubUrl('fraudDetectionServer').toString()}/name".toURL().text == 'fraudDetectionServer'
		cleanup:
			System.properties.setProperty("stubrunner.stubs.repository.root", "")
			System.properties.setProperty("stubrunner.stubs.classifier", 'stubs')
	}
}
