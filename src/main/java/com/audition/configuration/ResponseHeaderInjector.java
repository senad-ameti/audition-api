package com.audition.configuration;

import io.micrometer.tracing.CurrentTraceContext;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class ResponseHeaderInjector implements Filter {

    private static final String TRACE_ID_HEADER_NAME = "traceparent";
    public static final String DEFAULT = "00";
    private final Tracer tracer;

    public ResponseHeaderInjector(final Tracer tracer) {
        this.tracer = tracer;
    }

    @Override
    public void doFilter(
        final ServletRequest req,
        final ServletResponse res,
        final FilterChain chain
    ) throws IOException, ServletException {

        final HttpServletResponse response = (HttpServletResponse) res;
        if (!response.getHeaderNames().contains(TRACE_ID_HEADER_NAME)) {
            if (Optional.of(tracer).map(Tracer::currentTraceContext).map(CurrentTraceContext::context).isEmpty()) {
                chain.doFilter(req, res);
                return;
            }

            final TraceContext context = tracer.currentTraceContext().context();

            if (context != null) {
                final String traceId = context.traceId();
                final String parentId = context.spanId();
                final String traceParent = DEFAULT + "-" + traceId + "-" + parentId + "-" + DEFAULT;

                response.setHeader(TRACE_ID_HEADER_NAME, traceParent);
            }
        }

        chain.doFilter(req, res);
    }
}
