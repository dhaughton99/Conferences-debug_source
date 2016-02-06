package com.squareup.okhttp;

import java.io.IOException;
import java.net.CacheResponse;
import java.net.HttpURLConnection;

public interface OkResponseCache {
    void trackConditionalCacheHit();

    void trackResponse(ResponseSource responseSource);

    void update(CacheResponse cacheResponse, HttpURLConnection httpURLConnection) throws IOException;
}
