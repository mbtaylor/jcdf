package cdf;

import java.io.IOException;
import java.io.InputStream;

public class WrapperBuf implements Buf {

    private final Buf base_;

    public WrapperBuf( Buf base ) {
        base_ = base;
    }

    public int readUnsignedByte( Pointer ptr ) {
        return base_.readUnsignedByte( ptr );
    }

    public int readInt( Pointer ptr ) {
        return base_.readInt( ptr );
    }

    public long readOffset( Pointer ptr ) {
        return base_.readOffset( ptr );
    }

    public String readAsciiString( Pointer ptr, int nbyte ) {
        return base_.readAsciiString( ptr, nbyte );
    }

    public void setEncoding( boolean isBigendian ) {
        base_.setEncoding( isBigendian );
    }

    public boolean isBigendian() {
        return base_.isBigendian();
    }

    public void readDataBytes( long offset, int count, byte[] array ) {
        base_.readDataBytes( offset, count, array );
    }

    public void readDataShorts( long offset, int count, short[] array ) {
        base_.readDataShorts( offset, count, array );
    }

    public void readDataInts( long offset, int count, int[] array ) {
        base_.readDataInts( offset, count, array );
    }

    public void readDataLongs( long offset, int count, long[] array ) {
        base_.readDataLongs( offset, count, array );
    }

    public void readDataFloats( long offset, int count, float[] array ) {
        base_.readDataFloats( offset, count, array );
    }

    public void readDataDoubles( long offset, int count, double[] array ) {
        base_.readDataDoubles( offset, count, array );
    }

    public InputStream createInputStream( long offset ) {
        return base_.createInputStream( offset );
    }

    public Buf fillNewBuf( long count, InputStream in ) throws IOException {
        return base_.fillNewBuf( count, in );
    }

    public long getLength() {
        return base_.getLength();
    }
}
