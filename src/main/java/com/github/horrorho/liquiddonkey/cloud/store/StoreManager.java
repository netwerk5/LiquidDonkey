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
package com.github.horrorho.liquiddonkey.cloud.store;

import com.github.horrorho.liquiddonkey.cloud.file.SignatureWriter;
import com.github.horrorho.liquiddonkey.cloud.file.CloudFileWriterResult;
import com.github.horrorho.liquiddonkey.cloud.protobuf.ChunkServer;
import com.github.horrorho.liquiddonkey.cloud.protobuf.ICloud;
import com.github.horrorho.liquiddonkey.exception.BadDataException;
import com.github.horrorho.liquiddonkey.printer.Level;
import com.github.horrorho.liquiddonkey.printer.Printer;
import com.github.horrorho.liquiddonkey.settings.Markers;
import com.github.horrorho.liquiddonkey.util.Bytes;
import com.google.protobuf.ByteString;
import java.io.IOException;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import net.jcip.annotations.ThreadSafe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

/**
 * StoreManager.
 *
 * @author Ahseya
 */
@ThreadSafe
public final class StoreManager {

    public static StoreManager from(
            ChunkServer.FileGroups fileGroups,
            SignatureWriter signatureWriter,
            Printer printer) {

        logger.trace("<< from()");
        logger.debug(marker, "-- from() > fileGroup: {}", fileGroups);

        Map<ByteString, Set<ChunkServer.StorageHostChunkList>> signatureToChunks = new HashMap<>();
        ConcurrentMap<ByteString, List<ChunkListReference>> signatureToChunkListReferenceList
                = new ConcurrentHashMap<>();

        fileGroups.getFileGroupsList().stream().forEach(fileGroup -> {

            List<ChunkServer.StorageHostChunkList> chunkListList = fileGroup.getStorageHostChunkListList();
            Map<Long, ChunkServer.StorageHostChunkList> containerToChunkList = new HashMap<>();
            for (int index = 0; index < chunkListList.size(); index++) {
                containerToChunkList.put((long) index, chunkListList.get(index));
            }

            fileGroup.getFileChecksumChunkReferencesList().stream().forEach(references -> {
                ByteString signature = references.getFileChecksum();
                signatureToChunkListReferenceList.put(signature, new ArrayList<>());
                signatureToChunks.put(signature, new HashSet<>());

                references.getChunkReferencesList().stream().forEach(reference -> {
                    ChunkServer.StorageHostChunkList chunkList
                            = containerToChunkList.get(reference.getContainerIndex());
                    ChunkListReference chunkListReference
                            = ChunkListReference.from(chunkList, (int) reference.getChunkIndex());

                    signatureToChunkListReferenceList.get(signature).add(chunkListReference);
                    signatureToChunks.get(signature).add(chunkList);
                });
            });
        });

        logger.debug(marker, "-- from() > signatureToChunkListReferenceList: {}", signatureToChunkListReferenceList);

        StoreManager chunkManager = new StoreManager(
                MemoryStore.create(),
                BiRef.from(signatureToChunks),
                signatureToChunkListReferenceList,
                ChunkDecrypter::create,
                signatureWriter,
                printer);

        logger.trace(">> from()");
        return chunkManager;
    }

    private static final Logger logger = LoggerFactory.getLogger(StoreManager.class);
    private static final Marker marker = MarkerFactory.getMarker(Markers.STORE);

    private final Store<ChunkServer.StorageHostChunkList> store;
    private final BiRef<ByteString, ChunkServer.StorageHostChunkList> references;
    private final ConcurrentMap<ByteString, List<ChunkListReference>> signatureToChunkListReferenceList;
    private final Supplier<ChunkDecrypter> decrypters;
    private final SignatureWriter signatureWriter;
    private final Printer printer;

    StoreManager(
            Store<ChunkServer.StorageHostChunkList> store,
            BiRef<ByteString, ChunkServer.StorageHostChunkList> references,
            ConcurrentMap<ByteString, List<ChunkListReference>> signatureToChunkListReferenceList,
            Supplier<ChunkDecrypter> decrypters,
            SignatureWriter signatureWriter,
            Printer printer) {

        this.store = Objects.requireNonNull(store);
        this.references = Objects.requireNonNull(references);
        this.signatureToChunkListReferenceList = Objects.requireNonNull(signatureToChunkListReferenceList);
        this.decrypters = Objects.requireNonNull(decrypters);
        this.signatureWriter = Objects.requireNonNull(signatureWriter);
        this.printer = Objects.requireNonNull(printer);
    }

    public void put(ChunkServer.StorageHostChunkList chunkList, byte[] chunkData)
            throws BadDataException, IOException, InterruptedException {

        Objects.requireNonNull(chunkList);
        Objects.requireNonNull(chunkData);

        logger.trace("<< put() < uri: {} length: {}", chunkList.getHostInfo().getUri(), chunkData.length);

        List<byte[]> chunks = decrypters.get().decrypt(chunkList, chunkData);

        if (!store.put(chunkList, chunks)) {
            logger.warn("-- put() > overwritten store container: {}", chunkList.getHostInfo().getUri());
        }

        Map<ByteString, StoreWriter> writers = process(chunkList);
        if (!writers.isEmpty()) {
            writers.keySet().forEach(references::removeKey);
        }

        logger.debug("-- put() > writing signatures: {}", writers.keySet());
        write(writers);

        logger.trace(">> put()");
    }

    void write(Map<ByteString, StoreWriter> writers) throws IOException, InterruptedException {
        try {
            for (ByteString signature : new HashSet<>(writers.keySet())) {
                try (StoreWriter dataWriter = writers.get(signature)) {
                    Map<ICloud.MBSFile, CloudFileWriterResult> results = signatureWriter.write(signature, dataWriter);

                    if (results == null) {
                        logger.warn("-- write() > unreferenced signature: {}", Bytes.hex(signature));
                    } else {
                        results.entrySet().stream().forEach(
                                entry -> printer.println(Level.VV,
                                        "\t" + entry.getKey().getDomain()
                                        + " " + entry.getKey().getRelativePath()
                                        + " " + entry.getValue()));
                    }
                } finally {
                    writers.remove(signature);
                }
            }
        } finally {
            writers.values().stream().forEach(dataWriter -> {
                try {
                    dataWriter.close();
                } catch (IOException ex) {
                    logger.warn("-- write() > exception on close: {}", ex);
                }
            });
        }
    }

    Map<ByteString, StoreWriter> process(ChunkServer.StorageHostChunkList chunkList) {

        Map<ByteString, StoreWriter> writers = references.key(chunkList).stream()
                .map(signature -> new SimpleEntry<>(signature, process(signature)))
                .filter(entry -> entry.getValue() != null)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        return writers;
    }

    StoreWriter process(ByteString signature) {
        List<ChunkListReference> list = signatureToChunkListReferenceList.get(signature);

        // Exit if any chunks are missing.
        if (!list.stream().allMatch(reference -> store.contains(reference.chunkList(), reference.index()))) {
            return null;
        }

        // We have all the chunks, remove reference. Null if another thread beat us to it.
        if (signatureToChunkListReferenceList.remove(signature) == null) {
            return null;
        }

        // Writer.
        List<StoreWriter> writers = list.stream()
                .map(reference -> store.writer(reference.chunkList(), reference.index()))
                .collect(Collectors.toList());

        return CompoundWriter.from(writers);
    }

    public List<ChunkServer.StorageHostChunkList> chunkListList() {
        return new ArrayList<>(references.valueSet());
    }
}
