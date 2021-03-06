package io.codearte.accurest.stubrunner

import groovy.transform.CompileStatic

/**
 * Manages lifecycle of multiple {@link StubRunner} instances.
 *
 * @see StubRunner
 */
@CompileStatic
class BatchStubRunner implements StubRunning {

	private final Iterable<StubRunner> stubRunners

	BatchStubRunner(Iterable<StubRunner> stubRunners) {
		this.stubRunners = stubRunners
	}

	@Override
	RunningStubs runStubs() {
		Map<StubConfiguration, Integer> appsAndPorts = stubRunners.inject([:]) { Map<StubConfiguration, Integer> acc, StubRunner value ->
			acc.putAll(value.runStubs().namesAndPorts)
			return acc
		} as Map<StubConfiguration, Integer>
		return new RunningStubs(appsAndPorts)
	}

	@Override
	URL findStubUrl(String groupId, String artifactId) {
		return stubRunners.findResult(null) { StubRunner stubRunner ->
			return stubRunner.findStubUrl(groupId, artifactId)
		} as URL
	}

	@Override
	URL findStubUrl(String ivyNotation) {
		String[] splitString = ivyNotation.split(":")
		if (splitString.length > 3) {
			throw new IllegalArgumentException("$ivyNotation is invalid")
		} else if (splitString.length == 2) {
			return findStubUrl(splitString[0], splitString[1])
		}
		return findStubUrl(null, splitString[0])
	}

	@Override
	RunningStubs findAllRunningStubs() {
		return new RunningStubs(stubRunners.collect { StubRunner runner -> runner.findAllRunningStubs() })
	}

	@Override
	void close() throws IOException {
		stubRunners.each {
			it.close()
		}
	}
}
