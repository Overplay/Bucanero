package io.ourglass.bucanero.api;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by mkahn on 5/30/17.
 */

public class OGHeaderInterceptor implements Interceptor {
    @Override
    public Response intercept(Interceptor.Chain chain) throws IOException {
        Request request = chain.request();
        Request newRequest;

        // Can I get HACKA?
        // TODO: This is shite. Box should authorize somehow.
        newRequest = request.newBuilder()
                .addHeader("Authorization", "x-ogdevice-1234")
                .build();

        return chain.proceed(newRequest);
    }
}