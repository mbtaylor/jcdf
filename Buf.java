package cdf;

public interface Buf {

    /**
     * Reads a signed 4-byte integer, big-endian byte ordering.
     */
    int readInt( Offset offset );

    /**
     * Reads a signed 8-byte integer, big-endian byte ordering.
     */
    long readLong( Offset offset );

    /**
     * Reads a fixed number of bytes interpreting them as ASCII characters
     * and returns the result as a string.
     * An 0x00 terminates the sequence.
     *
     * <p>Requirements as per CDF Descriptor Record Copyright field.
     */
    String readAsciiString( Offset offset, int nbyte );
}
