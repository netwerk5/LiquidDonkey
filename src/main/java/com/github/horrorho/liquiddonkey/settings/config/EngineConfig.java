/*
 * The MIT License
 *
 * Copyright 2015 Ahseya.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.github.horrorho.liquiddonkey.settings.config;

import com.github.horrorho.liquiddonkey.settings.props.Parsers;
import com.github.horrorho.liquiddonkey.settings.Property;
import com.github.horrorho.liquiddonkey.settings.props.Props;
import net.jcip.annotations.Immutable;
import net.jcip.annotations.ThreadSafe;

/**
 * EngineConfig configuration.
 *
 * @author Ahseya
 */
@Immutable
@ThreadSafe
public final class EngineConfig {

    public static EngineConfig newInstance(Props<Property> props) {
        Parsers parsers = Property.parsers();
        boolean isAggressive = props.get(Property.ENGINE_AGGRESSIVE, parsers::asBoolean);

        return EngineConfig.newInstance(
                isAggressive,
                props.get(Property.ENGINE_BATCH_SIZE_MINIMUM_BYTES, parsers::asLong),
                isAggressive ? props.get(Property.ENGINE_CHUNK_LIST_DOWNLOAD_RETRY_AGGRESSIVE, parsers::asInteger)
                        : props.get(Property.ENGINE_CHUNK_LIST_DOWNLOAD_RETRY, parsers::asInteger),
                props.get(Property.ENGINE_PERSISTENT, parsers::asBoolean),
                props.get(Property.ENGINE_RETRY_DELAY_MS, parsers::asInteger),
                props.get(Property.ENGINE_THREAD_STAGGER_DELAY, parsers::asInteger),
                props.get(Property.ENGINE_THREAD_COUNT, parsers::asInteger),
                props.get(Property.ENGINE_FORCE_OVERWRITE, parsers::asBoolean),
                props.get(Property.ENGINE_SET_LAST_MODIFIED_TIMESTAMP, parsers::asBoolean));
    }

    static EngineConfig newInstance(boolean isAggressive,
            long batchSizeMinimumBytes,
            int chunkListDownloadRetry,
            boolean isPersistent,
            int retryDelay,
            int threadStaggerDelay,
            int threadCount,
            boolean toForceOverwrite,
            boolean toSetLastModifiedTimestamp) {

        return new EngineConfig(isAggressive,
                batchSizeMinimumBytes,
                chunkListDownloadRetry,
                isPersistent,
                retryDelay,
                threadStaggerDelay,
                threadCount,
                toForceOverwrite,
                toSetLastModifiedTimestamp);
    }

    private final boolean isAggressive;
    private final long batchSizeMinimumBytes;
    private final int chunkListDownloadRetry;
    private final boolean isPersistent;
    private final int retryDelay;
    private final int threadStaggerDelay;
    private final int threadCount;
    private final boolean toForceOverwrite;
    private final boolean toSetLastModifiedTimestamp;

    EngineConfig(
            boolean isAggressive,
            long batchSizeMinimumBytes,
            int chunkListDownloadRetry,
            boolean isPersistent,
            int retryDelay,
            int threadStaggerDelay,
            int threadCount,
            boolean toForceOverwrite,
            boolean toSetLastModifiedTimestamp) {

        this.isAggressive = isAggressive;
        this.batchSizeMinimumBytes = batchSizeMinimumBytes;
        this.chunkListDownloadRetry = chunkListDownloadRetry;
        this.isPersistent = isPersistent;
        this.retryDelay = retryDelay;
        this.threadStaggerDelay = threadStaggerDelay;
        this.threadCount = threadCount;
        this.toForceOverwrite = toForceOverwrite;
        this.toSetLastModifiedTimestamp = toSetLastModifiedTimestamp;
    }

    public boolean isAggressive() {
        return isAggressive;
    }

    public long batchSizeMinimumBytes() {
        return batchSizeMinimumBytes;
    }

    public int chunkListDownloadRetry() {
        return chunkListDownloadRetry;
    }

    public boolean isPersistent() {
        return isPersistent;
    }

    public int retryDelay() {
        return retryDelay;
    }

    public int threadStaggerDelay() {
        return threadStaggerDelay;
    }

    public int threadCount() {
        return threadCount;
    }

    public boolean toForceOverwrite() {
        return toForceOverwrite;
    }

    public boolean toSetLastModifiedTimestamp() {
        return toSetLastModifiedTimestamp;
    }

    @Override
    public String toString() {
        return "EngineConfig{" + "isAggressive=" + isAggressive + ", batchSizeMinimumBytes=" + batchSizeMinimumBytes
                + ", chunkListDownloadRetry=" + chunkListDownloadRetry + ", isPersistent=" + isPersistent
                + ", retryDelay=" + retryDelay + ", threadStaggerDelay=" + threadStaggerDelay
                + ", threadCount=" + threadCount + ", toForceOverwrite=" + toForceOverwrite
                + ", toSetLastModifiedTimestamp=" + toSetLastModifiedTimestamp + '}';
    }
}