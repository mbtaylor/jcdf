package cdf;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;

public class NioBuf implements Buf {

    private final ByteBuffer byteBuf_;

    public NioBuf( ByteBuffer byteBuf ) {
        byteBuf_ = byteBuf;
    }

    public int readInt( Pointer ptr ) {
        return byteBuf_.getInt( toInt( ptr.getAndIncrement( 4 ) ) );
    }

    public long readOffset( Pointer ptr ) {
        return byteBuf_.getLong( toInt( ptr.getAndIncrement( 8 ) ) );
    }

    public String readAsciiString( Pointer ptr, int nbyte ) {
        byte[] abuf = new byte[ nbyte ];
        byteBuf_.get( abuf, toInt( ptr.getAndIncrement( nbyte ) ), nbyte );
        StringBuilder sbuf = new StringBuilder( nbyte );
        for ( int i = 0; i < nbyte; i++ ) {
            byte b = abuf[ i ];
            if ( b == 0 ) {
                break;
            }
            else {
                sbuf.append( (char) b );
            }
        }
        return sbuf.toString();
    }

    public InputStream createInputStream( long offset ) {
        ByteBuffer strmBuf = byteBuf_.duplicate();
        strmBuf.position( (int) offset );
        return new ByteBufferInputStream( strmBuf );
    }

    private int toInt( long lvalue ) {
        int ivalue = (int) lvalue;
        if ( ivalue != lvalue ) {
            throw new IllegalArgumentException( "Pointer out of range: "
                                              + lvalue + " >32 bits" );
        }
        return ivalue;
    }

    /**
     * Tedious.  You'd think there was an implementation of this in the
     * J2SE somewhere, but I can't see one.
     */
    private static class ByteBufferInputStream extends InputStream {
        private final ByteBuffer bbuf_;

        ByteBufferInputStream( ByteBuffer bbuf ) {
            bbuf_ = bbuf;
        }

        @Override
        public int read() {
            return bbuf_.remaining() > 0 ? bbuf_.get() : -1;
        }

        @Override
        public int read( byte[] b ) {
            return read( b, 0, b.length );
        }

        @Override
        public int read( byte[] b, int off, int len ) {
            if ( len == 0 ) {
                return 0;
            }
            int remain = bbuf_.remaining();
            if ( remain == 0 ) {
                return -1;
            }
            else {
                int nr = Math.min( remain, len );
                bbuf_.get( b, off, nr );
                return nr;
            }
        }

        @Override
        public boolean markSupported() {
            return true;
        }

        @Override
        public void mark( int readLimit ) {
            bbuf_.mark();
        }

        @Override
        public void reset() {
            bbuf_.reset();
        }

        @Override
        public long skip( long n ) {
            int nsk = (int) Math.min( n, bbuf_.remaining() );
            bbuf_.position( bbuf_.position() + nsk );
            return nsk;
        }

        @Override
        public int available() {
            return bbuf_.remaining();
        }
    }
}
