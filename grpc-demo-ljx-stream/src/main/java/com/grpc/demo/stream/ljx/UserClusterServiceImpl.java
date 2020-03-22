package com.grpc.demo.stream.ljx;

import com.google.common.collect.Lists;
import io.grpc.stub.StreamObserver;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import java.util.stream.IntStream;

public class UserClusterServiceImpl extends UserClusterServiceGrpc.UserClusterServiceImplBase {

    @Override
    public void queryUids(QueryCondition request, StreamObserver<QueryResult> responseObserver){
        lazyQuery(responseObserver);
        responseObserver.onCompleted();
    }

    /**
     * 多次onNext, 实现流式接口
     *
     * @param observer
     * @return
     */
    private void lazyQuery(StreamObserver<QueryResult> observer){
        List<Supplier<List<Long>>> handles = supplierList();
        AtomicInteger count = new AtomicInteger();

        for (Supplier<List<Long>> handle : handles) {
//            if (count.incrementAndGet() == 5) {
//                try {
//                    TimeUnit.SECONDS.sleep(60);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//            }
            QueryResult result = QueryResult.newBuilder().setStatus(QueryResult.Status.SUCCESS).addAllUids(handle.get()).build();
            observer.onNext(result);
        }
    }

    AtomicLong atomicLong = new AtomicLong(0);

    private Supplier<List<Long>> supplier() {
        return () -> {
            List<Long> list = new ArrayList<>();
            for (int i = 0; i < 10; i++) {
                list.add(atomicLong.incrementAndGet());
            }
            return list;
        };
    }

    private List<Supplier<List<Long>>> supplierList() {
        List<Supplier<List<Long>>> list = Lists.newArrayList();
        IntStream.range(0, 10).forEach((t) -> list.add(supplier()));
        return list;
    }
}
