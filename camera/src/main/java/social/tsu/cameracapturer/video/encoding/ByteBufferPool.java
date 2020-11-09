package social.tsu.cameracapturer.video.encoding;


import java.nio.ByteBuffer;

import social.tsu.cameracapturer.internal.utils.Pool;

/**
 * A simple {@link Pool(int, Factory)} implementation for byte buffers.
 */
class ByteBufferPool extends Pool<ByteBuffer> {

    ByteBufferPool(final int bufferSize, int maxPoolSize) {
        super(maxPoolSize, new Factory<ByteBuffer>() {
            @Override
            public ByteBuffer create() {
                return ByteBuffer.allocateDirect(bufferSize);
            }
        });
    }
}
