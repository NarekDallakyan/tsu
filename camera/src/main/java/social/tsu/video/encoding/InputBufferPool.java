package social.tsu.video.encoding;


import social.tsu.internal.utils.Pool;

/**
 * A simple {@link Pool(int, Factory)} implementation for input buffers.
 */
public class InputBufferPool extends Pool<InputBuffer> {

    public InputBufferPool() {
        super(Integer.MAX_VALUE, new Factory<InputBuffer>() {
            @Override
            public InputBuffer create() {
                return new InputBuffer();
            }
        });
    }
}
