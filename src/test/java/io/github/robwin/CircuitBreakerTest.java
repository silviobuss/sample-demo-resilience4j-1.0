package io.github.robwin;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import io.vavr.collection.Stream;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

import static io.github.resilience4j.circuitbreaker.CircuitBreaker.State;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		classes = Application.class)
@DirtiesContext
public class CircuitBreakerTest {

	private static final String BACKEND_A = "backendA";
	private static final String BACKEND_B = "backendB";

	@Autowired
	private CircuitBreakerRegistry circuitBreakerRegistry;

	@Autowired
	private TestRestTemplate restTemplate;

	@Autowired
	private RetryRegistry retryRegistry;

	@Before
	public void setUp(){
		circuitBreakerRegistry.circuitBreaker(BACKEND_A).reset();
		circuitBreakerRegistry.circuitBreaker(BACKEND_B).reset();
		resetRetry(BACKEND_A);
		resetRetry(BACKEND_B);
	}

	private void resetRetry(String name) {
		Retry old = retryRegistry.retry(name);
		retryRegistry.remove(name);
		retryRegistry.retry(name, old.getRetryConfig());
	}

	@Test
	public void shouldOpenAndCloseBackendACircuitBreakerToCheckMinimumNumberOfCalls() throws InterruptedException {
		// When
		Stream.rangeClosed(1,5).forEach((count) -> produceFailure(BACKEND_A));
		// Then
		checkHealthStatus(BACKEND_A, State.OPEN);
		// waitDurationInOpenState
		Thread.sleep(2000);
		// Check Change to closed again
		Stream.rangeClosed(1,3).forEach((count) -> produceSuccess(BACKEND_A));
		checkHealthStatus(BACKEND_A, State.CLOSED);
	}

	@Test
	public void shouldOpenAndCloseBackendACircuitBreakerToCheckSlidingWindowSize() throws InterruptedException {
		// When
		Stream.rangeClosed(1,10).forEach((count) -> produceSuccess(BACKEND_A));
		Stream.rangeClosed(1,4).forEach((count) -> produceFailure(BACKEND_A));
		Stream.rangeClosed(1,10).forEach((count) -> produceSuccess(BACKEND_A));
		checkHealthStatus(BACKEND_A, State.CLOSED);
		Stream.rangeClosed(1,5).forEach((count) -> produceFailure(BACKEND_A));
		// Then
		checkHealthStatus(BACKEND_A, State.OPEN);
		//waitDurationInOpenState
		Thread.sleep(2000);
		// Check Change to closed again
		Stream.rangeClosed(1,3).forEach((count) -> produceSuccess(BACKEND_A));
		checkHealthStatus(BACKEND_A, State.CLOSED);
	}

	@Test
	public void shouldRetryThreeTimes() {
		produceFailure(BACKEND_B);
		checkNumberOfFailedCallsMetrics(BACKEND_B, 1l);
	}

	@Test
	public void shouldRetryThreeTimesAndOpenCircuitBreaker() {
		Stream.rangeClosed(1,2).forEach((count) -> produceSuccess(BACKEND_B));
		Stream.rangeClosed(1,2).forEach((count) -> produceFailure(BACKEND_B));
		checkHealthStatus(BACKEND_B, State.OPEN);
		checkNumberOfFailedCallsMetrics(BACKEND_B, 2l);
	}

	private void checkNumberOfFailedCallsMetrics(String backend, Long count) {
		assertThat(retryRegistry.retry(backend).getMetrics().getNumberOfFailedCallsWithRetryAttempt()).isEqualTo(count);
	}

	private void checkHealthStatus(String circuitBreakerName, State state) {
		CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(circuitBreakerName);
		assertThat(circuitBreaker.getState()).isEqualTo(state);
	}

	private void produceFailure(String backend) {
		ResponseEntity<String> response = restTemplate.getForEntity("/" + backend + "/failure", String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
	}

	private void produceSuccess(String backend) {
		ResponseEntity<String> response = restTemplate.getForEntity("/" + backend + "/success", String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
	}
}
