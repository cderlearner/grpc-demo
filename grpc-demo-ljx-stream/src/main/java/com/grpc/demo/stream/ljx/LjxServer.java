package com.grpc.demo.stream.ljx;

import io.grpc.Server;
import io.grpc.netty.NettyServerBuilder;

import java.util.concurrent.TimeUnit;

public class LjxServer {
    public static void main(String[] args) throws Exception{
        LjxServer server = new LjxServer();
        server.start();
        TimeUnit.MINUTES.sleep(100);
    }

    private int PORT = 8888;
    private Server server;

    private void start() throws Exception {
        server = NettyServerBuilder.forPort(PORT).addService(new UserClusterServiceImpl().bindService())
                .build();
        server.start();

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                System.err.println("*** Shutting down gRPC server since JVM is shutting down");
                LjxServer.this.stop();
                System.err.println("*** Server shut down");
            }
        });
    }

    private void stop() {
        try {
            server.awaitTermination(2, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
