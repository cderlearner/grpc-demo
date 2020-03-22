package com.grpc.demo.stream.ljx;

import io.grpc.*;

import java.util.concurrent.TimeUnit;

public class DeadlineInterceptor implements ClientInterceptor {
    private Integer defaultDeadline = 15;

    public DeadlineInterceptor() {
    }

    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> methodDescriptor, CallOptions callOptions, Channel channel) {
        if (callOptions.getDeadline() == null) {
            callOptions = callOptions.withDeadlineAfter((long) this.defaultDeadline, TimeUnit.SECONDS);
        }

        return channel.newCall(methodDescriptor, callOptions);
    }
}
