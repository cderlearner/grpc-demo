package com.grpc.demo.stream.ljx;

import io.grpc.*;
import io.grpc.netty.NettyChannelBuilder;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class LjxClient {
    private ManagedChannel managedChannel;
    private int PORT = 8888;

    private void createChannel() {
        managedChannel = NettyChannelBuilder.forAddress("localhost", PORT).usePlaintext(true)
                .intercept(new ClientLogInterceptor(), new DeadlineInterceptor(), new DeadlineInterceptor2())
                .build();
    }

    private void shutdown() {
        if (managedChannel != null) {
            try {
                managedChannel.shutdown().awaitTermination(2, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) throws Exception{
        LjxClient ljxClient = new LjxClient();
        ljxClient.createChannel();
        UserClusterServiceGrpc.UserClusterServiceBlockingStub stub = UserClusterServiceGrpc.newBlockingStub(ljxClient.managedChannel);
        //stub.withWaitForReady();
        //stub.withDeadlineAfter(1, TimeUnit.MINUTES);
        stub.getCallOptions().withDeadlineAfter(1, TimeUnit.MINUTES);

        //add metadata
//        Metadata metadata = new Metadata();
//        metadata.put(Metadata.Key.of("extendKey", Metadata.ASCII_STRING_MARSHALLER), "extendValue");
//        MetadataUtils.attachHeaders(simpleServiceStub, metadata);

        Iterator<QueryResult> it = stub.queryUids(QueryCondition.newBuilder()
                .setClusterType("USER_FOLLOW")      // 设置用户群类型
                .putConditions("uid", String.valueOf(1))
                .build());

        AtomicInteger count = new AtomicInteger();
        for (; it.hasNext(); ) {

            QueryResult queryResult = it.next();              // grpc 流接口
            List<Long> queryResultUidsList = queryResult.getUidsList();

            if (count.incrementAndGet()== 5){
                System.out.println("-------------------");
                TimeUnit.SECONDS.sleep(30);

            }

            System.out.println(queryResultUidsList);
        }

    }

    class ClientRequestInterceptor implements ClientInterceptor {
        @Override
        public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> method,
                                                                   CallOptions callOptions, Channel next) {
            return new ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(next.newCall(method, callOptions)) {
                @Override
                public void start(ClientCall.Listener<RespT> responseListener, Metadata headers) {
                    Metadata.Key<String> metaDataKey = Metadata.Key.of("Client-request", Metadata.ASCII_STRING_MARSHALLER);
                    headers.put(metaDataKey, "Client-request-extend-value");
                    super.start(responseListener, headers);
                }
            };
        }
    }

    class ClientResponseInterceptor implements ClientInterceptor {

        @Override
        public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> method,
                                                                   CallOptions callOptions, Channel next) {
            return new ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(next.newCall(method, callOptions)) {
                @Override
                public void start(Listener<RespT> responseListener, Metadata headers) {
                    super.start(new ForwardingClientCallListener.SimpleForwardingClientCallListener<RespT>(
                            responseListener) {
                        @Override
                        public void onHeaders(Metadata headers) {
                            Metadata.Key<String> metaDataKey = Metadata.Key.of("Server-Request", Metadata.ASCII_STRING_MARSHALLER);
                            System.out.println(headers.get(metaDataKey));
                            super.onHeaders(headers);
                        }
                    }, headers);
                }
            };
        }
    }
}
