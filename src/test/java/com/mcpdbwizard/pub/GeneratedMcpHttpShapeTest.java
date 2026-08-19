package com.mcpdbwizard.pub;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.ee10.servlet.FilterHolder;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.server.Server;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;
import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Compiles and exercises the <em>shape</em> of the plaintext arm of the HTTP branch that
 * {@code SAAdminWrangler.generateMcpServerClass} emits.
 *
 * <p>This exists because that arm is otherwise never compiled by anything. {@code MCP_HTTPS} is a
 * generation-time flag, so only one of the two arms is emitted into a given server — and the
 * propfile the live harnesses regenerate ({@code generic_test_23ai}) sets {@code MCP_HTTPS=YES}.
 * A break in the plaintext arm would therefore survive a full green multi-box run and only surface
 * for whoever generated without TLS.
 *
 * <p>The origin rules themselves are covered in {@link McpHttpPolicyTest}; what is pinned here is
 * the wiring — that the emitted construction compiles, that the filter is mappable onto a Jetty
 * context, and that a refused origin produces 403 and does <em>not</em> continue down the chain.
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
class GeneratedMcpHttpShapeTest {

    /** Records what the filter did to the response. */
    private static final class Outcome {
        Integer sentErrorCode;
        boolean chainContinued;
    }

    /** A stand-in for the servlet request carrying just the one header the filter reads. */
    private static HttpServletRequest requestWithOrigin(final String theOrigin) {
        return (HttpServletRequest) Proxy.newProxyInstance(
                GeneratedMcpHttpShapeTest.class.getClassLoader(),
                new Class<?>[]{HttpServletRequest.class},
                new InvocationHandler() {
                    public Object invoke(Object theProxy, Method theMethod, Object[] theArgs) {
                        if ("getHeader".equals(theMethod.getName()) && "Origin".equals(theArgs[0])) {
                            return theOrigin;
                        }
                        return null;
                    }
                });
    }

    private static HttpServletResponse responseRecording(final Outcome theOutcome) {
        return (HttpServletResponse) Proxy.newProxyInstance(
                GeneratedMcpHttpShapeTest.class.getClassLoader(),
                new Class<?>[]{HttpServletResponse.class},
                new InvocationHandler() {
                    public Object invoke(Object theProxy, Method theMethod, Object[] theArgs) {
                        if ("sendError".equals(theMethod.getName())) {
                            theOutcome.sentErrorCode = (Integer) theArgs[0];
                        }
                        return null;
                    }
                });
    }

    /**
     * The filter exactly as emitted, over the policy it is emitted against.
     */
    private static Filter originFilter(final McpHttpPolicy theOriginPolicy) {
        return new Filter() {
            public void doFilter(ServletRequest theReq, ServletResponse theResp, FilterChain theChain)
                    throws java.io.IOException, jakarta.servlet.ServletException {
                String theOrigin = ((HttpServletRequest) theReq).getHeader("Origin");
                if (!theOriginPolicy.isOriginAllowed(theOrigin)) {
                    ((HttpServletResponse) theResp).sendError(403, "Forbidden");
                    return;
                }
                theChain.doFilter(theReq, theResp);
            }
        };
    }

    private static Outcome runFilter(McpHttpPolicy thePolicy, String theOrigin) throws Exception {
        final Outcome theOutcome = new Outcome();
        FilterChain theChain = new FilterChain() {
            public void doFilter(ServletRequest theReq, ServletResponse theResp) {
                theOutcome.chainContinued = true;
            }
        };
        originFilter(thePolicy).doFilter(requestWithOrigin(theOrigin), responseRecording(theOutcome), theChain);
        return theOutcome;
    }

    @Test
    void thePlaintextArmConstructsAndBindsTheChosenInterface() throws Exception {
        // The emitted line. Jetty's no-arg and int constructors bind every interface; this one does
        // not, which is the whole point.
        String theBindHost = McpHttpPolicy.bindHost(null);
        Server theHttpServer = new Server(new InetSocketAddress(theBindHost, 0));

        ServletContextHandler theContext = new ServletContextHandler();
        theContext.setContextPath("/");
        theContext.addFilter(new FilterHolder(originFilter(McpHttpPolicy.fromEnvironment())),
                "/mcp/*", EnumSet.of(DispatcherType.REQUEST));
        theHttpServer.setHandler(theContext);

        assertEquals("127.0.0.1", theBindHost);
        assertEquals(1, theHttpServer.getConnectors().length, "the address constructor adds one connector");
    }

    @Test
    void aRefusedOriginIs403AndGoesNoFurther() throws Exception {
        Outcome theOutcome = runFilter(new McpHttpPolicy(null), "https://evil.example.com");

        assertEquals(Integer.valueOf(403), theOutcome.sentErrorCode,
                "the spec requires 403 for a present, invalid Origin");
        assertFalse(theOutcome.chainContinued, "a refused request must never reach the MCP servlet");
    }

    @Test
    void anAllowedOriginContinuesDownTheChain() throws Exception {
        Outcome theOutcome = runFilter(new McpHttpPolicy(null), "http://localhost:8090");

        assertTrue(theOutcome.chainContinued);
        assertEquals(null, theOutcome.sentErrorCode);
    }

    @Test
    void aClientSendingNoOriginContinuesDownTheChain() throws Exception {
        // Every non-browser MCP client. If this regressed, the filter would break all of them.
        Outcome theOutcome = runFilter(new McpHttpPolicy(null), null);

        assertTrue(theOutcome.chainContinued);
        assertEquals(null, theOutcome.sentErrorCode);
    }
}
