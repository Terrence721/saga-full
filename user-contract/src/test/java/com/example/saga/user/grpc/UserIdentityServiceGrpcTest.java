package com.example.saga.user.grpc;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import javax.annotation.Nullable;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserIdentityServiceGrpcTest {

    private Channel fakeChannel() {
        return new Channel() {
            @Override
            public String authority() { return "fake"; }
                        @Override
                        public <ReqT, RespT> io.grpc.ClientCall<ReqT, RespT> newCall(
                                        io.grpc.MethodDescriptor<ReqT, RespT> methodDescriptor, CallOptions callOptions) {
                                return new io.grpc.ClientCall<ReqT, RespT>() {
                    @SuppressWarnings("unchecked")
                    @Override
                                        public void start(io.grpc.ClientCall.Listener<RespT> listener, io.grpc.Metadata headers) {
                        String fullName = String.valueOf(methodDescriptor.getFullMethodName());
                        if (fullName.endsWith("/Login")) {
                            listener.onMessage((RespT) LoginResponse.getDefaultInstance());
                        } else {
                            listener.onMessage((RespT) ValidateTokenResponse.getDefaultInstance());
                        }
                        listener.onClose(Status.OK, new io.grpc.Metadata());
                    }
                    @Override
                    public void request(int messages) { }
                    @Override
                    public void cancel(@Nullable String message, @Nullable Throwable cause) { }
                    @Override
                    public void halfClose() { }
                    @Override
                                        public void sendMessage(ReqT message) { }
                };
            }
        };
    }

    @Test
    void privateConstructorCanBeInstantiated() throws Exception {
        var constructor = UserIdentityServiceGrpc.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        assertThat(constructor.newInstance()).isNotNull();
    }

    @Test
    void getLoginMethod_andValidateTokenMethod_areConfigured() {
        assertThat(UserIdentityServiceGrpc.getLoginMethod().getFullMethodName())
                .isEqualTo("saga.user.v1.UserIdentityService/Login");
        assertThat(UserIdentityServiceGrpc.getValidateTokenMethod().getFullMethodName())
                .isEqualTo("saga.user.v1.UserIdentityService/ValidateToken");
    }

    @Test
    void serviceDescriptor_containsBothMethods() {
        assertThat(UserIdentityServiceGrpc.SERVICE_NAME)
                .isEqualTo("saga.user.v1.UserIdentityService");
        assertThat(UserIdentityServiceGrpc.getServiceDescriptor().getMethods())
                .hasSize(2);
    }

    @Test
    void newStub_factory_methods_returnStubs() {
        Channel channel = fakeChannel();
        UserIdentityServiceGrpc.UserIdentityServiceStub asyncStub =
                UserIdentityServiceGrpc.newStub(channel);
        UserIdentityServiceGrpc.UserIdentityServiceBlockingStub blockingStub =
                UserIdentityServiceGrpc.newBlockingStub(channel);
        UserIdentityServiceGrpc.UserIdentityServiceFutureStub futureStub =
                UserIdentityServiceGrpc.newFutureStub(channel);

        assertThat(asyncStub).isNotNull();
        assertThat(blockingStub).isNotNull();
        assertThat(futureStub).isNotNull();
    }

    @Test
    void implBase_bindService_exposesService() {
        UserIdentityServiceGrpc.UserIdentityServiceImplBase impl =
                new UserIdentityServiceGrpc.UserIdentityServiceImplBase() {};
        assertThat(impl.bindService()).isNotNull();
    }

    @Test
    void asyncService_defaultMethods_callUnimplemented() {
        UserIdentityServiceGrpc.AsyncService service =
                new UserIdentityServiceGrpc.UserIdentityServiceImplBase() {};
        RecordingObserver<LoginResponse> loginObserver = new RecordingObserver<>();
        service.login(LoginRequest.getDefaultInstance(), loginObserver);
        assertThat(loginObserver.error).isInstanceOf(StatusRuntimeException.class);
        assertThat(((StatusRuntimeException) loginObserver.error).getStatus().getCode())
                .isEqualTo(Status.UNIMPLEMENTED.getCode());

        RecordingObserver<ValidateTokenResponse> tokenObserver = new RecordingObserver<>();
        service.validateToken(ValidateTokenRequest.getDefaultInstance(), tokenObserver);
        assertThat(tokenObserver.error).isInstanceOf(StatusRuntimeException.class);
        assertThat(((StatusRuntimeException) tokenObserver.error).getStatus().getCode())
                .isEqualTo(Status.UNIMPLEMENTED.getCode());
    }

    @Test
    void blockingStub_login_returnsResponse() {
        UserIdentityServiceGrpc.UserIdentityServiceBlockingStub stub =
                UserIdentityServiceGrpc.newBlockingStub(fakeChannel());
        assertThat(stub.login(LoginRequest.getDefaultInstance())).isNotNull();
    }

    @Test
    void blockingStub_validateToken_returnsResponse() {
        UserIdentityServiceGrpc.UserIdentityServiceBlockingStub stub =
                UserIdentityServiceGrpc.newBlockingStub(fakeChannel());
        assertThat(stub.validateToken(ValidateTokenRequest.getDefaultInstance())).isNotNull();
    }

    @Test
    void futureStub_login_returnsResponse() throws Exception {
        UserIdentityServiceGrpc.UserIdentityServiceFutureStub stub =
                UserIdentityServiceGrpc.newFutureStub(fakeChannel());
        assertThat(stub.login(LoginRequest.getDefaultInstance()).get()).isNotNull();
    }

    @Test
    void futureStub_validateToken_returnsResponse() throws Exception {
        UserIdentityServiceGrpc.UserIdentityServiceFutureStub stub =
                UserIdentityServiceGrpc.newFutureStub(fakeChannel());
        assertThat(stub.validateToken(ValidateTokenRequest.getDefaultInstance()).get()).isNotNull();
    }

    @Test
    void asyncStub_login_andValidateToken_invokeClientPath() {
        UserIdentityServiceGrpc.UserIdentityServiceStub stub =
                UserIdentityServiceGrpc.newStub(fakeChannel());
        RecordingObserver<LoginResponse> loginObserver = new RecordingObserver<>();
        stub.login(LoginRequest.getDefaultInstance(), loginObserver);
        RecordingObserver<ValidateTokenResponse> tokenObserver = new RecordingObserver<>();
        stub.validateToken(ValidateTokenRequest.getDefaultInstance(), tokenObserver);
        assertThat(loginObserver.error).isNull();
        assertThat(tokenObserver.error).isNull();
    }

    @Test
    void methodHandlers_unaryInvoke_coversBothMethods() throws Exception {
        Class<?> handlerClass = Class.forName("com.example.saga.user.grpc.UserIdentityServiceGrpc$MethodHandlers");
        var constructor = handlerClass.getDeclaredConstructor(UserIdentityServiceGrpc.AsyncService.class, int.class);
        constructor.setAccessible(true);

        UserIdentityServiceGrpc.AsyncService service = new UserIdentityServiceGrpc.UserIdentityServiceImplBase() {};
        var invoke = handlerClass.getMethod("invoke", Object.class, StreamObserver.class);

        RecordingObserver<LoginResponse> loginObserver = new RecordingObserver<>();
        Object loginHandler = constructor.newInstance(service, 0);
        invoke.invoke(loginHandler, LoginRequest.getDefaultInstance(), loginObserver);
        assertThat(loginObserver.error).isInstanceOf(StatusRuntimeException.class);

        RecordingObserver<ValidateTokenResponse> tokenObserver = new RecordingObserver<>();
        Object tokenHandler = constructor.newInstance(service, 1);
        invoke.invoke(tokenHandler, ValidateTokenRequest.getDefaultInstance(), tokenObserver);
        assertThat(tokenObserver.error).isInstanceOf(StatusRuntimeException.class);
    }

    @Test
    void methodHandlers_streamingInvoke_throwsAssertionError() throws Exception {
        Class<?> handlerClass = Class.forName("com.example.saga.user.grpc.UserIdentityServiceGrpc$MethodHandlers");
        var constructor = handlerClass.getDeclaredConstructor(UserIdentityServiceGrpc.AsyncService.class, int.class);
        constructor.setAccessible(true);
        UserIdentityServiceGrpc.AsyncService service = new UserIdentityServiceGrpc.UserIdentityServiceImplBase() {};
        Object handler = constructor.newInstance(service, 0);
        var invoke = handlerClass.getMethod("invoke", StreamObserver.class);

        assertThatThrownBy(() -> invoke.invoke(handler, new RecordingObserver<>()))
                .isInstanceOf(java.lang.reflect.InvocationTargetException.class)
                .hasCauseInstanceOf(AssertionError.class);
    }

    @Test
    void stubBuildMethods_canBeInvokedByReflection() throws Exception {
        Channel channel = fakeChannel();
        CallOptions options = CallOptions.DEFAULT;
        String[] classNames = {
                "com.example.saga.user.grpc.UserIdentityServiceGrpc$UserIdentityServiceStub",
                "com.example.saga.user.grpc.UserIdentityServiceGrpc$UserIdentityServiceBlockingStub",
                "com.example.saga.user.grpc.UserIdentityServiceGrpc$UserIdentityServiceFutureStub"
        };
        for (String className : classNames) {
            Class<?> clazz = Class.forName(className);
            var constructor = clazz.getDeclaredConstructor(Channel.class, CallOptions.class);
            constructor.setAccessible(true);
            Object stub = constructor.newInstance(channel, options);
            var build = clazz.getDeclaredMethod("build", Channel.class, CallOptions.class);
            build.setAccessible(true);
            assertThat(build.invoke(stub, channel, options)).isNotNull();
        }
    }

    @Test
    void methodDescriptorSuppliers_returnDescriptors() {
        assertThat(UserIdentityServiceGrpc.getServiceDescriptor()).isNotNull();
        assertThat(UserIdentityServiceGrpc.getLoginMethod()).isNotNull();
        assertThat(UserIdentityServiceGrpc.getValidateTokenMethod()).isNotNull();
    }

    @Test
    void methodHandlers_defaultUnaryCase_throwsAssertionError() throws Exception {
        Class<?> handlerClass = Class.forName("com.example.saga.user.grpc.UserIdentityServiceGrpc$MethodHandlers");
        var constructor = handlerClass.getDeclaredConstructor(UserIdentityServiceGrpc.AsyncService.class, int.class);
        constructor.setAccessible(true);
        UserIdentityServiceGrpc.AsyncService service = new UserIdentityServiceGrpc.UserIdentityServiceImplBase() {};
        Object handler = constructor.newInstance(service, 2); // invalid methodId hits default
        var invoke = handlerClass.getMethod("invoke", Object.class, StreamObserver.class);

        assertThatThrownBy(() -> invoke.invoke(handler, LoginRequest.getDefaultInstance(), new RecordingObserver<>()))
                .isInstanceOf(java.lang.reflect.InvocationTargetException.class)
                .hasCauseInstanceOf(AssertionError.class);
    }

    @Test
    void descriptorSuppliers_returnDescriptors() throws Exception {
        Class<?> fileSupplierClass = Class.forName("com.example.saga.user.grpc.UserIdentityServiceGrpc$UserIdentityServiceFileDescriptorSupplier");
        var fileConstructor = fileSupplierClass.getDeclaredConstructor();
        fileConstructor.setAccessible(true);
        Object fileSupplier = fileConstructor.newInstance();

        var getFileDescriptor = fileSupplier.getClass().getMethod("getFileDescriptor");
        Object fileDescriptor = getFileDescriptor.invoke(fileSupplier);
        assertThat(fileDescriptor).isNotNull();

        var getServiceDescriptor = fileSupplier.getClass().getMethod("getServiceDescriptor");
        Object serviceDescriptor = getServiceDescriptor.invoke(fileSupplier);
        assertThat(serviceDescriptor).isNotNull();

        Class<?> methodSupplierClass = Class.forName("com.example.saga.user.grpc.UserIdentityServiceGrpc$UserIdentityServiceMethodDescriptorSupplier");
        var methodConstructor = methodSupplierClass.getDeclaredConstructor(String.class);
        methodConstructor.setAccessible(true);
        Object methodSupplier = methodConstructor.newInstance("Login");

        var getMethodDescriptor = methodSupplier.getClass().getMethod("getMethodDescriptor");
        Object methodDescriptor = getMethodDescriptor.invoke(methodSupplier);
        assertThat(methodDescriptor).isNotNull();
    }

    private static class RecordingObserver<T> implements StreamObserver<T> {
        Object error;
        @Override
        public void onNext(T value) { }
        @Override
        public void onError(Throwable t) { error = t; }
        @Override
        public void onCompleted() { }
    }
}

