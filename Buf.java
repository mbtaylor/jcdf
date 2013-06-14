package cdf;

import java.io.IOException;
import java.io.InputStream;

public interface Buf {

    /**
     * Reads a single byte, returning a value in the range 0..255.
     */
    int readUnsignedByte( Pointer ptr ) throws IOException;

    /**
     * Reads a signed 4-byte integer, big-endian byte ordering.
     */
    int readInt( Pointer ptr ) throws IOException;

    /**
     * Reads a file offset or size.
     * This is a signed integer with big-endian byte ordering.
     * For CDF V3 it is 8 bytes, and for earlier versions it is 4 bytes.
     */
    long readOffset( Pointer ptr ) throws IOException;

    /**
     * Reads a fixed number of bytes interpreting them as ASCII characters
     * and returns the result as a string.
     * An 0x00 terminates the sequence.
     *
     * <p>Requirements as per CDF Descriptor Record Copyright field.
     */
    String readAsciiString( Pointer ptr, int nbyte ) throws IOException;

    void setBit64( boolean isBit64 );
    boolean isBit64();
    void setEncoding( boolean isBigendian );
    boolean isBigendian();

    void readDataBytes( long offset, int count, byte[] array )
            throws IOException;
    void readDataShorts( long offset, int count, short[] array )
            throws IOException;
    void readDataInts( long offset, int count, int[] array )
            throws IOException;
    void readDataLongs( long offset, int count, long[] array )
            throws IOException;
    void readDataFloats( long offset, int count, float[] array )
            throws IOException;
    void readDataDoubles( long offset, int count, double[] array )
            throws IOException;

    InputStream createInputStream( long offset );
    Buf fillNewBuf( long count, InputStream in ) throws IOException;

    long getLength();
}
