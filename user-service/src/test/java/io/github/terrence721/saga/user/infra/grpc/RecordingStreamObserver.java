package io.github.terrence721.saga.user.infra.grpc;

import io.grpc.stub.StreamObserver;

import java.util.ArrayList;
import java.util.List;

// io.grpc.testing.StreamRecorder is deprecated with no official replacement;
// GrpcExecutor always calls onNext/onError/onCompleted synchronously for our
// unary RPCs, so a plain capturing StreamObserver needs no latch/await.
class RecordingStreamObserver<T> implements StreamObserver<T> {

    private final List<T> values = new ArrayList<>();
    private Throwable error;

    @Override
    public void onNext(T value) {
        values.add(value);
    }

    @Override
    public void onError(Throwable t) {
        this.error = t;
    }

    @Override
    public void onCompleted() {
    }

    T firstValue() {
        return values.get(0);
    }

    T firstValueOrNull() {
        return values.isEmpty() ? null : values.get(0);
    }

    Throwable error() {
        return error;
    }
}
