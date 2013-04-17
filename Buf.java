package cdf;

import java.io.InputStream;

public interface Buf {

    /**
     * Reads a signed 4-byte integer, big-endian byte ordering.
     */
    int readInt( Pointer ptr );

    /**
     * Reads a file offset or size.
     * For CDF V3 this is a signed 8-byte integer, big-endian byte ordering.
     */
    long readOffset( Pointer ptr );

    /**
     * Reads a fixed number of bytes interpreting them as ASCII characters
     * and returns the result as a string.
     * An 0x00 terminates the sequence.
     *
     * <p>Requirements as per CDF Descriptor Record Copyright field.
     */
    String readAsciiString( Pointer ptr, int nbyte );

    InputStream createInputStream( long offset );
}
