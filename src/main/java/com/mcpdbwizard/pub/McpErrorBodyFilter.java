package com.mcpdbwizard.pub;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

/**
 * A filter for a generated server's {@code /mcp/*} that rewrites the SDK's transport error bodies --
 * which carry a full Java stack trace -- into plain JSON-RPC errors. See {@link McpErrorBodies}.
 *
 * <p>Only a response whose status is already 400 or above when its body is first written is
 * buffered; the SDK sets the status before it writes. Everything else, including every successful
 * reply and every SSE stream, goes straight through unbuffered, so streaming is unaffected.
 *
 * <p>Loaded only by a generated server's HTTP branch, so a stdio deployment never needs the servlet
 * API on its classpath.
 *
 * Copyright 2003-2026 ATB Consultancy Services Ltd
 * (formerly Orinda Software Ltd, Dublin, Ireland)
 */
public final class McpErrorBodyFilter implements Filter {

    @Override
    public void doFilter(ServletRequest theRequest, ServletResponse theResponse, FilterChain theChain)
            throws IOException, ServletException {
        if (!(theResponse instanceof HttpServletResponse)) {
            theChain.doFilter(theRequest, theResponse);
            return;
        }
        Capturing theWrapper = new Capturing((HttpServletResponse) theResponse);
        theChain.doFilter(theRequest, theWrapper);
        theWrapper.finish();
    }

    /** Buffers the body only when the status says it is an error. */
    static final class Capturing extends HttpServletResponseWrapper {
        private int theStatus = 200;
        private ByteArrayOutputStream theBuffer;
        private PrintWriter theWriter;
        private ServletOutputStream theStream;

        Capturing(HttpServletResponse theResponse) {
            super(theResponse);
        }

        @Override
        public void setStatus(int theCode) {
            theStatus = theCode;
            super.setStatus(theCode);
        }

        private boolean capturing() {
            if (theBuffer == null && theStatus >= 400) {
                theBuffer = new ByteArrayOutputStream();
            }
            return theBuffer != null;
        }

        @Override
        public PrintWriter getWriter() throws IOException {
            if (!capturing()) {
                return super.getWriter();
            }
            if (theWriter == null) {
                theWriter = new PrintWriter(new OutputStreamWriter(theBuffer, StandardCharsets.UTF_8));
            }
            return theWriter;
        }

        @Override
        public ServletOutputStream getOutputStream() throws IOException {
            if (!capturing()) {
                return super.getOutputStream();
            }
            if (theStream == null) {
                theStream = new ServletOutputStream() {
                    @Override
                    public void write(int b) {
                        theBuffer.write(b);
                    }

                    @Override
                    public boolean isReady() {
                        return true;
                    }

                    @Override
                    public void setWriteListener(WriteListener theListener) {
                        // synchronous buffer: nothing to wait for
                    }
                };
            }
            return theStream;
        }

        @Override
        public void flushBuffer() {
            // Deferred to finish(): flushing now would commit the original error body.
        }

        /** Write the buffered body out, rewritten if it is an SDK error. */
        void finish() throws IOException {
            if (theBuffer == null) {
                return;
            }
            if (theWriter != null) {
                theWriter.flush();
            }
            String theBody = theBuffer.toString(StandardCharsets.UTF_8);
            String theClean = McpErrorBodies.jsonRpcErrorFor(theBody);
            byte[] theOut = (theClean != null ? theClean : theBody).getBytes(StandardCharsets.UTF_8);
            HttpServletResponse theReal = (HttpServletResponse) getResponse();
            theReal.setContentLength(theOut.length);
            ServletOutputStream theSink = theReal.getOutputStream();
            theSink.write(theOut);
            theSink.flush();
        }
    }
}
