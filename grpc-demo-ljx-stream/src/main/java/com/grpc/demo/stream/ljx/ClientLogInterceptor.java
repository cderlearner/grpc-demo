//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//
package com.grpc.demo.stream.ljx;

import io.grpc.*;
import io.grpc.ForwardingClientCall.SimpleForwardingClientCall;
import io.grpc.Metadata.Key;

public class ClientLogInterceptor implements ClientInterceptor {
    //private static final Logger log = LoggerFactory.getLogger(ClientLogInterceptor.class);
    //private static final IDBox idbox = IDBox.of(11, System.currentTimeMillis(), 42, 5, 12);
    private static final Key<String> traceId;

    public ClientLogInterceptor() {
    }

    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(final MethodDescriptor<ReqT, RespT> methodDescriptor, CallOptions callOptions, Channel channel) {
        final long startTime = System.currentTimeMillis();
        //final String traceIdStr = idbox.getNextId().toString();
        return new SimpleForwardingClientCall<ReqT, RespT>(channel.newCall(methodDescriptor, callOptions.withoutWaitForReady())) {
            public void sendMessage(ReqT message) {
                System.out.println(String.format("|Send-gRpc-trace: %s|method=%s", "1", methodDescriptor.getFullMethodName()));
                super.sendMessage(message);
            }

            public void start(final Listener<RespT> responseListener, Metadata headers) {
                //headers.put(ClientLogInterceptor.traceId, traceIdStr);
                Listener<RespT> listener = new ForwardingClientCallListener<RespT>() {
                    protected Listener<RespT> delegate() {
                        return responseListener;
                    }

                    public void onMessage(RespT message) {
                        long endTime = System.currentTimeMillis();
                        System.out.println(String.format("|Received-gRpc-trace: %s|method=%s, spendTime=%s ms", new Object[]{"1", methodDescriptor.getFullMethodName(), endTime - startTime}));
                        super.onMessage(message);
                    }

                    public void onClose(Status status, Metadata trailers) {
                        if (!status.isOk()) {
                            System.out.println(String.format("|Client-Exception-trace: %s|method=%s, exception=%s", new Object[]{"1", methodDescriptor.getFullMethodName(), status.getDescription()}));
                        }

                        this.delegate().onClose(status, trailers);
                    }
                };
                super.start(listener, headers);
            }
        };
    }

    static {
        traceId = Key.of("traceId", Metadata.ASCII_STRING_MARSHALLER);
    }
}
