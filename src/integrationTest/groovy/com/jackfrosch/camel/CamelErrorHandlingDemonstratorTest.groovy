package com.jackfrosch.camel

import org.apache.camel.*
import org.apache.camel.builder.NotifyBuilder
import org.apache.camel.builder.RouteBuilder
import org.apache.camel.component.mock.MockEndpoint
import org.apache.camel.model.OnExceptionDefinition
import org.apache.camel.processor.interceptor.Tracer
import org.apache.camel.test.junit4.CamelTestSupport
import org.junit.Before
import org.junit.Test

/**
 * This test illustrates Camel error handling, redelivery policy configuration, and redelivery logging options
 */
class CamelErrorHandlingDemonstratorTest extends CamelTestSupport {
    boolean ROUTE_COMPLETED = Boolean.TRUE
    boolean ROUTE_FAILED = Boolean.FALSE
    Processor SIMULATED_ERROR_PROCESSOR = { throw new RuntimeException("A simulated error has occurred") }

    static class CustomException extends RuntimeException {
        int code
        CustomException(int code, String message, Throwable cause) {
            super(message, cause)
            this.code = code
        }
    }

    @Before
    void setup() {
        super.setUp()
    }

    @Test
    void "When no error occurs, message makes it to the success endpoint"() {
        context.addRoutes(createNoErrorHandlerRoute({ Exchange exch -> exch.in.body = 77 }))
        NotifyBuilder notifier = createNotifier('start', 'mock:success', ROUTE_COMPLETED)

        MockEndpoint mockEp = getMockEndpoint('mock:success')
        mockEp.expectedBodiesReceived(77)

        template.sendBody('direct:start', 'Test input data')

        assert notifier.matches()
        assertMockEndpointsSatisfied()
    }

    @Test
    void "The default error handler yields no retries and the execption propagates back up the route"() {
        context.addRoutes(createNoErrorHandlerRoute(SIMULATED_ERROR_PROCESSOR))

        NotifyBuilder notifier = createNotifier('start', 'mock:success', ROUTE_FAILED)

        MockEndpoint mockEp = getMockEndpoint('mock:success')
        mockEp.expectedMessageCount(0)
        mockEp.resultMinimumWaitTime = 100

        Exchange errorExchange = null
        try {
            template.sendBody('direct:start', 'Test input data')
        } catch (CamelExecutionException e) {                        // Default error handler propagates exception back to caller, so we catch it here
            errorExchange = e.exchange
        }

        assert notifier.matchesMockWaitTime()
        assertMockEndpointsSatisfied()

        assertEquals(0, errorExchange.in.getHeader('CamelRedeliveryCounter', Integer))
        assertTrue("Exchange failed", errorExchange.failed)
        assertEquals('A simulated error has occurred', errorExchange.exception.message)
        assertEquals('Test input data', errorExchange.in.body)
        assertFalse("Error not handled", errorExchange.getProperty('CamelErrorHandlerHandled', Boolean))
        MessageHistory msgHistory = (errorExchange.getProperty('CamelMessageHistory', List))[0] as MessageHistory
        assertTrue("Should fail < 100ms", msgHistory.elapsed < 100)
    }

    @Test
    void "The configured default error handler with a redelivery policy set yields expected number of retries and original message detours"() {
        context.addRoutes(createConfiguredDefaultErrorHandlerRoute(SIMULATED_ERROR_PROCESSOR))

        NotifyBuilder notifier = createNotifier('start', 'mock:success', ROUTE_FAILED)

        MockEndpoint mockEp = getMockEndpoint('mock:success')
        mockEp.expectedMessageCount(0)
        mockEp.resultMinimumWaitTime = 100

        Exchange errorExchange = null
        try {
            template.sendBody('direct:start', 'Test input data')
        } catch (CamelExecutionException e) {                        // Default error handler propagates exception back to caller, so we catch it here
            errorExchange = e.exchange
        }

        assert notifier.matchesMockWaitTime()
        assertMockEndpointsSatisfied()

        assertEquals(4, errorExchange.in.getHeader('CamelRedeliveryCounter', Integer))
        assertTrue("Exchange failed", errorExchange.failed)
        assertEquals('A simulated error has occurred', errorExchange.exception.message)
        assertEquals('Test input data', errorExchange.in.body)
        assertFalse("Error not handled", errorExchange.getProperty('CamelErrorHandlerHandled', Boolean))
        MessageHistory msgHistory = (errorExchange.getProperty('CamelMessageHistory', List))[0] as MessageHistory
        assertTrue("Should fail in ~400ms. Actual: ${msgHistory.elapsed}", (400L..500L).contains(msgHistory.elapsed))
    }

    @Test
    void "An exception is handled and yields expected retries and is detoured"() {
        context.addRoutes(createRouteWithExceptionHandled({ throw new IOException("Simulated connection error") }, 4, 100))

        NotifyBuilder failureNotifier = createNotifier('start', 'mock:success', ROUTE_FAILED)
        NotifyBuilder errorNotifier = createNotifier('start', 'mock:errorDetour', ROUTE_COMPLETED)

        MockEndpoint mockEp = getMockEndpoint('mock:success')
        mockEp.expectedMessageCount(0)
        mockEp.resultMinimumWaitTime = 100

        MockEndpoint detourEp = getMockEndpoint('mock:errorDetour')
        detourEp.expectedMessageCount(1)
        detourEp.resultMinimumWaitTime = 100


        template.sendBody('direct:start', 'Test input data')

        assert failureNotifier.matchesMockWaitTime()
        assert errorNotifier.matchesMockWaitTime()

        assertMockEndpointsSatisfied()

        Exchange detourExchange = detourEp.exchanges[0]
        assertFalse("Detour Exchange successed", detourExchange.failed)
        assertEquals('Simulated connection error', detourExchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception).message)
        assertEquals('java.io.IOException thrown and handled', detourExchange.in.body)
        MessageHistory msgHistory = (detourExchange.getProperty('CamelMessageHistory', List))[0] as MessageHistory
        assertTrue("Should fail retries in ~400ms. Actual: ${msgHistory.elapsed}", (400L..500L).contains(msgHistory.elapsed))
    }


    @Test
    void "An exception is handled and yields expected retries and is detoured after maximum # of retries which occur in backoff retry period"() {
        final int numberOfRetries = 8
        final long redeliveryDelay = 100
        final double backoffMultiplier = 2.0

        context.addRoutes(createRouteWithExceptionHandled({ throw new IOException("Simulated connection error") },
                numberOfRetries, redeliveryDelay, backoffMultiplier))

        NotifyBuilder failureNotifier = createNotifier('start', 'mock:success', ROUTE_FAILED)
        NotifyBuilder errorNotifier = createNotifier('start', 'mock:errorDetour', ROUTE_COMPLETED)

        MockEndpoint mockEp = getMockEndpoint('mock:success')
        mockEp.expectedMessageCount(0)
        mockEp.resultMinimumWaitTime = 100

        MockEndpoint detourEp = getMockEndpoint('mock:errorDetour')
        detourEp.expectedMessageCount(1)
        detourEp.resultMinimumWaitTime = 100


        template.sendBody('direct:start', 'Test input data')

        assert failureNotifier.matchesMockWaitTime()
        assert errorNotifier.matchesMockWaitTime()

        assertMockEndpointsSatisfied()

        Exchange detourExchange = detourEp.exchanges[0]
        assertFalse("Detour Exchange successed", detourExchange.failed)
        assertEquals('Simulated connection error', detourExchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception).message)
        assertEquals('java.io.IOException thrown and handled', detourExchange.in.body)
        MessageHistory msgHistory = (detourExchange.getProperty('CamelMessageHistory', List))[0] as MessageHistory

        long expectedRetryPeriod = redeliveryDelay * backoffMultiplier ** numberOfRetries
        log.debug "Estimated retry period: ${expectedRetryPeriod}"

        long earliestExpected = expectedRetryPeriod - redeliveryDelay
        long latestExpected = expectedRetryPeriod + redeliveryDelay
        assertTrue("Should fail retries in ~${expectedRetryPeriod}. Actual: ${msgHistory.elapsed}",
                (earliestExpected..latestExpected).contains(msgHistory.elapsed))
    }

    @Test
    void "An exception is handled and yields expected # of retries and is detoured after maximum # of retries following delay pattern"() {
        context.addRoutes(createRouteWithExceptionHandled({ throw new IOException("Simulated connection error") },
                10, 100, 0.0, '0:100;1:200;2:300;3:500;8:1000')) // note: no spaces between elements of delay pattern

        NotifyBuilder failureNotifier = createNotifier('start', 'mock:success', ROUTE_FAILED)
        NotifyBuilder errorNotifier = createNotifier('start', 'mock:errorDetour', ROUTE_COMPLETED)

        MockEndpoint mockEp = getMockEndpoint('mock:success')
        mockEp.expectedMessageCount(0)
        mockEp.resultMinimumWaitTime = 100

        MockEndpoint detourEp = getMockEndpoint('mock:errorDetour')
        detourEp.expectedMessageCount(1)
        detourEp.resultMinimumWaitTime = 100

        template.sendBody('direct:start', 'Test input data')

        assert failureNotifier.matchesMockWaitTime()
        assert errorNotifier.matchesMockWaitTime()

        assertMockEndpointsSatisfied()

        Exchange detourExchange = detourEp.exchanges[0]
        assertFalse("Detour Exchange successed", detourExchange.failed)
        assertEquals('Simulated connection error', detourExchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception).message)
        assertEquals('java.io.IOException thrown and handled', detourExchange.in.body)
        MessageHistory msgHistory = (detourExchange.getProperty('CamelMessageHistory', List))[0] as MessageHistory

        long expectedRetryPeriod = 100 + 200 + 300 + 500 * 5 + 1000 * 3
        log.debug "Estimated retry period: ${expectedRetryPeriod}"

        long earliestExpected = expectedRetryPeriod - expectedRetryPeriod / 10
        long latestExpected = expectedRetryPeriod + expectedRetryPeriod / 10
        assertTrue("Should fail retries in ~${expectedRetryPeriod}. Actual: ${msgHistory.elapsed}",
                (earliestExpected..latestExpected).contains(msgHistory.elapsed))
    }

    @Test
    void "A configured default error handler with exception ignored is not detoured"() {
        context.addRoutes(createRouteWithExceptionIgnored({ Exchange exch -> exch.in.body = 77; throw new ValidationException(exch, "Validation failed. foo = bar") }))
        NotifyBuilder notifier = createNotifier('start', 'mock:success', ROUTE_COMPLETED)

        MockEndpoint mockEp = getMockEndpoint('mock:success')
        mockEp.expectedMessageCount(1)
        mockEp.expectedBodiesReceived(77)

        template.sendBody('direct:start', 'Test input data')

        assert notifier.matches()
        assertMockEndpointsSatisfied()
    }

    @Test
    void "Doing a try/catch during processing and wrapping it in a custom exception still works; it just hides root exception making us work harder"() {
        Processor processor = new Processor() {
            @Override
            void process(Exchange exchange) throws Exception {
                try {
                    String someValue = exchange.in.getHeader('SomeValue', String)
                    exchange.in.headers.TransformedValue = someValue.toUpperCase()
                } catch (Exception e) {
                    CustomException ex2 = new CustomException(77, 'A 77 error occurred', e)
                    log.error(ex2.message, ex2) // This causes the exception to be logged with every redelivery - even though we configured the route only to log stacktrace when exhausted
                    throw ex2
                }
            }
        }
        context.addRoutes(createRouteWithExceptionHandled((processor)))

        NotifyBuilder errorNotifier = createNotifier('start', 'mock:errorDetour', ROUTE_COMPLETED)

        MockEndpoint detourEp = getMockEndpoint('mock:errorDetour')
        detourEp.expectedMessageCount(1)
        detourEp.resultMinimumWaitTime = 100

        template.sendBody('direct:start', 'Test input data')

        assert errorNotifier.matchesMockWaitTime()

        assertMockEndpointsSatisfied()

        Exchange detourExchange = detourEp.exchanges[0]
        assertFalse("Detour Exchange successed", detourExchange.failed)
        assertEquals('A 77 error occurred', detourExchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception).message)
        assertEquals('Cannot invoke method toUpperCase() on null object', detourExchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception).cause.message)
        assertEquals('com.jackfrosch.camel.CamelErrorHandlingDemonstratorTest$CustomException thrown and handled', detourExchange.in.body)
    }

    @Test
    void "Not doing a try/catch at all during processing is clean for unchecked exceptions, but we get no retries"() {
        Processor processor = new Processor() {
            @Override
            void process(Exchange exchange) throws Exception {
                String someValue = exchange.in.getHeader('SomeValue', String)
                exchange.in.headers.TransformedValue = someValue.toUpperCase()
            }
        }
        context.addRoutes(createRouteWithExceptionHandled((processor)))

        NotifyBuilder errorNotifier = createNotifier('start', 'mock:errorDetour', ROUTE_COMPLETED)

        MockEndpoint detourEp = getMockEndpoint('mock:errorDetour')
        detourEp.expectedMessageCount(1)
        detourEp.resultMinimumWaitTime = 100

        template.sendBody('direct:start', 'Test input data')

        assert errorNotifier.matchesMockWaitTime()

        assertMockEndpointsSatisfied()

        Exchange detourExchange = detourEp.exchanges[0]
        assertFalse("Detour Exchange successed", detourExchange.failed)
        assertEquals('Cannot invoke method toUpperCase() on null object',
                detourExchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception).message)
        assertEquals('java.lang.NullPointerException thrown and handled', detourExchange.in.body)
    }

    @Test
    void "Not doing a try/catch at all during processing is clean for checked exceptions, and we get retries"() {
        Processor processor = new Processor() {
            @Override
            void process(Exchange exchange) throws Exception {
                new File('FooBar_abc123', 'DataFile.xml').readLines()
            }
        }
        context.addRoutes(createRouteWithExceptionHandled((processor)))

        NotifyBuilder errorNotifier = createNotifier('start', 'mock:errorDetour', ROUTE_COMPLETED)

        MockEndpoint detourEp = getMockEndpoint('mock:errorDetour')
        detourEp.expectedMessageCount(1)
        detourEp.resultMinimumWaitTime = 100

        template.sendBody('direct:start', 'Test input data')

        assert errorNotifier.matchesMockWaitTime()

        assertMockEndpointsSatisfied()

        Exchange detourExchange = detourEp.exchanges[0]
        assertFalse("Detour Exchange successed", detourExchange.failed)
        assertEquals('FooBar_abc123/DataFile.xml (No such file or directory)',
                detourExchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception).message)
        assertEquals('java.io.FileNotFoundException thrown and handled', detourExchange.in.body)
    }

    protected RouteBuilder createNoErrorHandlerRoute(Processor processor) {
        new RouteBuilder() {
            @Override
            void configure() {
                context.tracing = false

                from("direct:start")
                        .routeId("start")
                        .process(processor)
                        .to("mock:success")
            }
        }
    }

    protected RouteBuilder createConfiguredDefaultErrorHandlerRoute(Processor processor) {
        new RouteBuilder() {
            @Override
            void configure() {
                context.tracing = false

                errorHandler(defaultErrorHandler()
                        .useOriginalMessage()
                        .maximumRedeliveries(4)
                        .redeliveryDelay(100)
                        .retryAttemptedLogLevel(LoggingLevel.WARN))


                from("direct:start")
                        .routeId("start")
                        .process(processor)
                        .to("mock:success")
            }
        }
    }

    // Optionally use backOffMultiplier or delayPattern. If both used, delayPattern drives the redelivery.
    // Also illustrates logging configuration
    protected RouteBuilder createRouteWithExceptionHandled(Processor processor, int maxRedeliveries = 4, long redeliveryDelay = 1000,
                                                           double backOffMultiplier = 0.00, String delayPattern = '') {
        final Processor errorProcessor = new Processor() {
            @Override
            void process(Exchange exchange) {
                Exception ex = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception)
                exchange.in.body = ex.class.name + " thrown and handled"
            }
        }

        new RouteBuilder() {
            @Override
            void configure() {
                Tracer tracer = new Tracer()
                tracer.logLevel = LoggingLevel.DEBUG    // we can dial down Tracer logging to DEBUG
                context.addInterceptStrategy(tracer)

                context.tracing = true

                onException(RuntimeException)
                        .routeId('uncheckedExceptionHandler')
                        .handled(true)
                        .maximumRedeliveries(0)
                        .logHandled(true)           // Shows the stacktrace exception was handled
                        .process(errorProcessor)
                        .to("mock:errorDetour")


                OnExceptionDefinition excDef = onException(Exception)
                        .routeId('checkedExceptionHandler')
                        .handled(true)
                        .logHandled(true)           // Shows the stacktrace after tries exhausted and exception was handled
                        .logExhausted(true)         // This results in stacktrace for final failure
                        .logRetryAttempted(true)    // Logs each attempt
                        .logRetryStackTrace(false)  // If true, we'll get stacktrace for every retry attempt
                        .retryAttemptedLogLevel(LoggingLevel.WARN)
                        .retriesExhaustedLogLevel(LoggingLevel.ERROR)
                        .maximumRedeliveries(maxRedeliveries)
                        .redeliveryDelay(redeliveryDelay)
                        .process(errorProcessor)
                        .to("mock:errorDetour")
                if(backOffMultiplier) { excDef.backOffMultiplier(backOffMultiplier) }
                if(delayPattern) { excDef.delayPattern(delayPattern) }

                errorHandler(defaultErrorHandler()
                        .useOriginalMessage()
                        .maximumRedeliveries(4)
                        .redeliveryDelay(redeliveryDelay)
                        .retryAttemptedLogLevel(LoggingLevel.WARN))


                from("direct:start")
                        .routeId("start")
                        .process(processor)
                        .to("mock:success")
            }
        }
    }

    // Retries are attempted, but if exhausted, we handle it and continue on to end
    protected RouteBuilder createRouteWithExceptionIgnored(Processor processor) {
        new RouteBuilder() {
            @Override
            void configure() {
                context.tracing = false

                onException(ValidationException)
                        .routeId('validationExceptionHandler')
                        .log(LoggingLevel.WARN, 'Validation error occurred: Exception: ${exception}')
                        .continued(true)    // Logs the exception that caused the retries
                        .logContinued(true)

                errorHandler(defaultErrorHandler()
                        .useOriginalMessage()
                        .maximumRedeliveries(4)
                        .redeliveryDelay(100)
                        .retryAttemptedLogLevel(LoggingLevel.WARN))


                from("direct:start")
                        .routeId("start")
                        .process(processor)
                        .to("mock:success")
            }
        }
    }

    protected NotifyBuilder createNotifier(String fromRouteId, String toRouteUri, boolean completed) {

        NotifyBuilder builder = new NotifyBuilder(context).fromRoute(fromRouteId)
                .wereSentTo(toRouteUri)

        builder = completed ? builder.whenCompleted(1) : builder.whenFailed(0)

        builder.create()
    }

}
