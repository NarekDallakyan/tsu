package social.tsu.video.encoding;


import java.nio.ByteBuffer;

import social.tsu.internal.utils.Pool;

/**
 * A simple {@link Pool(int, Factory)} implementation for byte buffers.
 */
public class ByteBufferPool extends Pool<ByteBuffer> {

    public ByteBufferPool(final int bufferSize, int maxPoolSize) {
        super(maxPoolSize, new Factory<ByteBuffer>() {
            @Override
            public ByteBuffer create() {
                return ByteBuffer.allocateDirect(bufferSize);
            }
        });
    }
}
