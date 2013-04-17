package cdf;

import java.io.InputStream;

class WrapperBuf implements Buf {

    private final Buf base_;

    public WrapperBuf( Buf base ) {
        base_ = base;
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

    public InputStream createInputStream( long offset ) {
        return base_.createInputStream( offset );
    }
}
