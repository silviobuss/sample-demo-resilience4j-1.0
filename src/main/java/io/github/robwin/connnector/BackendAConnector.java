package io.github.robwin.connnector;


import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.robwin.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpServerErrorException;

/**
 * This Connector shows how to use the CircuitBreaker annotation.
 */
@CircuitBreaker(name = "backendA")
@Component(value = "backendAConnector")
public class BackendAConnector implements Connector {

    @Override
    public String failure() {
        throw new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR, "This is a remote exception");
    }

    @Override
    public String ignoreException() {
        throw new BusinessException("This exception is ignored by the CircuitBreaker of backend A");
    }

    @Override
    public String success() {
        return "Hello World from backend A";
    }

    @Override
    @CircuitBreaker(name = "backendA", fallbackMethod = "fallback")
    public String failureWithFallback() {
        return failure();
    }

    private String fallback(Throwable ex) {
        return "Recovered with fallback: " + ex.getMessage();
    }
}
