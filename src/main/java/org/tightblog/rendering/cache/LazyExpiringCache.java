/*
* Licensed to the Apache Software Foundation (ASF) under one or more
* contributor license agreements.  The ASF licenses this file to You
* under the Apache License, Version 2.0 (the "License"); you may not
* use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.  For additional information regarding
* copyright in this work, please see the NOTICE file in the top level
* directory of this distribution.
*
* Source file modified from the original ASF source; all changes made
* are also under Apache License.
*/
package org.tightblog.rendering.cache;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.apache.commons.lang3.time.DateUtils;
import com.github.benmanes.caffeine.cache.Cache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class LazyExpiringCache {
    private static Logger log = LoggerFactory.getLogger(LazyExpiringCache.class);

    private String cacheHandlerId;

    private AtomicLong incomingRequests = new AtomicLong();

    private AtomicLong requestsHandledBy304 = new AtomicLong();

    public long getIncomingRequests() {
        return incomingRequests.get();
    }

    public void incrementIncomingRequests() {
        this.incomingRequests.incrementAndGet();
    }

    public long getRequestsHandledBy304() {
        return requestsHandledBy304.get();
    }

    public void incrementRequestsHandledBy304() {
        this.requestsHandledBy304.incrementAndGet();
    }

    public String getCacheHandlerId() {
        return cacheHandlerId;
    }

    public void setCacheHandlerId(String cacheHandlerId) {
        this.cacheHandlerId = cacheHandlerId;
    }

    private int maxEntries;

    public void setMaxEntries(int maxEntries) {
        this.maxEntries = maxEntries;
    }

    public int getMaxEntries() {
        return maxEntries;
    }

    public void invalidateAll() {
        contentCache.invalidateAll();
    }

    public static Logger getLog() {
        return log;
    }

    public long getEstimatedSize() {
        return contentCache == null ? 0 : contentCache.estimatedSize();
    }

    public long getCacheRequestCount() {
        return contentCache == null ? 0 : contentCache.stats().requestCount();
    }

    public long getCacheHitCount() {
        return contentCache == null ? 0 : contentCache.stats().hitCount();
    }

    public long getCacheMissCount() {
        return contentCache == null ? 0 : contentCache.stats().missCount();
    }

    public double getCacheHitRate() {
        return contentCache == null ? 0 : contentCache.stats().hitRate();
    }

    private long timeoutInMS;

    public void setTimeoutSec(int timeoutSec) {
        this.timeoutInMS = timeoutSec * DateUtils.MILLIS_PER_SECOND;
    }

    private Cache<String, Object> contentCache;

    @PostConstruct
    public void init() {
        if (maxEntries > 0) {
            contentCache = Caffeine.newBuilder()
                    .expireAfterWrite(timeoutInMS, TimeUnit.MILLISECONDS)
                    .maximumSize(maxEntries)
                    .recordStats()
                    .build();
        } else {
            contentCache = null;
            log.warn("Cache {} has been DISABLED", cacheHandlerId);
        }
    }

    public CachedContent get(String key, Instant lastModified) {
        if (maxEntries > 0) {
            CachedContent entry = null;
            LazyExpiringCacheEntry lazyEntry = (LazyExpiringCacheEntry) this.contentCache.getIfPresent(key);
            if (lazyEntry != null) {
                entry = lazyEntry.getValue(lastModified);

                if (entry != null) {
                    log.trace("HIT {}", key);
                } else {
                    log.trace("HIT-EXPIRED {}", key);
                }
            } else {
                log.trace("MISS {}", key);
            }
            return entry;
        } else {
            return null;
        }
    }

    public void put(String key, CachedContent value) {
        if (maxEntries > 0) {
            contentCache.put(key, new LazyExpiringCacheEntry(value));
            log.debug("PUT {}", key);
        }
    }
}