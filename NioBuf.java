package cdf;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.ArrayList;

public class NioBuf implements Buf {

    private final ByteBuffer byteBuf_;
    private final ByteBuffer dataBuf_;
    private boolean isBigendian_;

    public NioBuf( ByteBuffer byteBuf, boolean isBigendian ) {
        byteBuf_ = byteBuf;
        dataBuf_ = byteBuf.duplicate();
        setEncoding( isBigendian );
    }

    public long getLength() {
        return byteBuf_.capacity();
    }

    public int readUnsignedByte( Pointer ptr ) {
        return byteBuf_.get( toInt( ptr.getAndIncrement( 1 ) ) ) & 0xff;
    }

    public int readInt( Pointer ptr ) {
        return byteBuf_.getInt( toInt( ptr.getAndIncrement( 4 ) ) );
    }

    public long readOffset( Pointer ptr ) {
        return byteBuf_.getLong( toInt( ptr.getAndIncrement( 8 ) ) );
    }

    public String readAsciiString( Pointer ptr, int nbyte ) {
        byte[] abuf = new byte[ nbyte ];
        synchronized ( byteBuf_ ) {
            byteBuf_.position( toInt( ptr.getAndIncrement( nbyte ) ) );
            byteBuf_.get( abuf, 0, nbyte );
        }
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

    public synchronized void setEncoding( boolean bigend ) {
        dataBuf_.order( bigend ? ByteOrder.BIG_ENDIAN
                               : ByteOrder.LITTLE_ENDIAN );
        isBigendian_ = bigend;
    }

    public boolean isBigendian() {
        return isBigendian_;
    }

    public void readDataBytes( long offset, int count, byte[] array ) {
        if ( count == 1 ) {
            array[ 0 ] = dataBuf_.get( toInt( offset ) );
        }
        else {
            synchronized ( dataBuf_ ) {
                dataBuf_.position( toInt( offset ) );
                dataBuf_.get( array, 0, count );
            }
        }
    }

    public void readDataShorts( long offset, int count, short[] array ) {
        if ( count == 1 ) {
            array[ 0 ] = dataBuf_.getShort( toInt( offset ) );
        }
        else {
            synchronized ( dataBuf_ ) {
                dataBuf_.position( toInt( offset ) );
                dataBuf_.asShortBuffer().get( array, 0, count );
            }
        }
    }

    public void readDataInts( long offset, int count, int[] array ) {
        if ( count == 1 ) {
            array[ 0 ] = dataBuf_.getInt( toInt( offset ) );
        }
        else {
            synchronized ( dataBuf_ ) {
                dataBuf_.position( toInt( offset ) );
                dataBuf_.asIntBuffer().get( array, 0, count );
            }
        }
    }

    public void readDataLongs( long offset, int count, long[] array ) {
        if ( count == 1 ) {
            array[ 0 ] = dataBuf_.getLong( toInt( offset ) );
        }
        else {
            synchronized ( dataBuf_ ) {
                dataBuf_.position( toInt( offset ) );
                dataBuf_.asLongBuffer().get( array, 0, count );
            }
        }
    }

    public void readDataFloats( long offset, int count, float[] array ) {
        if ( count == 1 ) {
            array[ 0 ] = dataBuf_.getFloat( toInt( offset ) );
        }
        else {
            synchronized ( dataBuf_ ) {
                dataBuf_.position( toInt( offset ) );
                dataBuf_.asFloatBuffer().get( array, 0, count );
            }
        }
    }

    public void readDataDoubles( long offset, int count, double[] array ) {
        if ( count == 1 ) {
            array[ 0 ] = dataBuf_.getDouble( toInt( offset ) );
        }
        else {
            synchronized ( dataBuf_ ) {
                dataBuf_.position( toInt( offset ) );
                dataBuf_.asDoubleBuffer().get( array, 0, count );
            }
        }
    }

    public InputStream createInputStream( long offset ) {
        ByteBuffer strmBuf = byteBuf_.duplicate();
        strmBuf.position( (int) offset );
        return new ByteBufferInputStream( strmBuf );
    }

    public static NioBuf createBuf( File file, boolean isBigendian )
            throws IOException {
        int fleng = toInt( file.length() );
        ByteBuffer bbuf = new FileInputStream( file )
                         .getChannel()
                         .map( FileChannel.MapMode.READ_ONLY, 0, fleng );
        return new NioBuf( bbuf, isBigendian );
    }

    private static int toInt( long lvalue ) {
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
