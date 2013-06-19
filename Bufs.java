package cdf;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public class Bufs {

    private static final int BANK_SIZE = 1 << 30;

    private Bufs() {
    }

    public static Buf createBuf( ByteBuffer byteBuffer,
                                 boolean isBit64, boolean isBigendian ) {
        return new SimpleNioBuf( byteBuffer, isBit64, isBigendian );
    }

    public static Buf createBuf( ByteBuffer[] byteBuffers,
                                 boolean isBit64, boolean isBigendian ) {
        return byteBuffers.length == 1
             ? createBuf( byteBuffers[ 0 ], isBit64, isBigendian )
             : BankBuf.createMultiBankBuf( byteBuffers, isBit64, isBigendian );
    }

    public static Buf createBuf( File file,
                                 boolean isBit64, boolean isBigendian )
            throws IOException {
        FileChannel channel = new FileInputStream( file ).getChannel();
        long leng = file.length();
        if ( leng <= Integer.MAX_VALUE ) {
            int ileng = (int) leng;
            ByteBuffer bbuf =
                channel.map( FileChannel.MapMode.READ_ONLY, 0, ileng );
            return createBuf( bbuf, isBit64, isBigendian );
        }
        else {
            return BankBuf.createMultiBankBuf( channel, leng, BANK_SIZE,
                                               isBit64, isBigendian );
        }
    }

    public static InputStream createByteBufferInputStream( ByteBuffer bbuf ) {
        return new ByteBufferInputStream( bbuf );
    }

    // Utility methods to read arrays of data from buffers.
    // We work differently according to whether we are in fact reading
    // single value or multiple values.  This is because NIO Buffer
    // classes have absolute read methods for scalar reads, but only
    // relative read methods for array reads (i.e. you need to position
    // a pointer and then do the read).  For thread safety we need to
    // synchronize in that case to make sure somebody else doesn't
    // reposition before the read takes place.

    // For the array reads, we also recast the ByteBuffer to a Buffer of
    // the appropriate type for the data being read.  This is probably(?)
    // more efficient than doing multiple scalar reads.

    static String readAsciiString( ByteBuffer bbuf, int ioff, int nbyte ) {
        byte[] abuf = new byte[ nbyte ];
        synchronized ( bbuf ) {
            bbuf.position( ioff );
            bbuf.get( abuf, 0, nbyte );
        }
        StringBuffer sbuf = new StringBuffer( nbyte );
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

    static void readBytes( ByteBuffer bbuf, int ioff, int count, byte[] a ) {
        if ( count == 1 ) {
            a[ 0 ] = bbuf.get( ioff );
        }
        else {
            synchronized ( bbuf ) {
                bbuf.position( ioff );
                bbuf.get( a, 0, count );
            }
        }
    }

    static void readShorts( ByteBuffer bbuf, int ioff, int count, short[] a ) {
        if ( count == 1 ) {
            a[ 0 ] = bbuf.getShort( ioff );
        }
        else {
            synchronized ( bbuf ) {
                bbuf.position( ioff );
                bbuf.asShortBuffer().get( a, 0, count );
            }
        }
    }

    static void readInts( ByteBuffer bbuf, int ioff, int count, int[] a ) {
        if ( count == 1 ) {
            a[ 0 ] = bbuf.getInt( ioff );
        }
        else {
            synchronized ( bbuf ) {
                bbuf.position( ioff );
                bbuf.asIntBuffer().get( a, 0, count );
            }
        }
    }

    static void readLongs( ByteBuffer bbuf, int ioff, int count, long[] a ) {
        if ( count == 1 ) {
            a[ 0 ] = bbuf.getLong( ioff );
        }
        else {
            synchronized ( bbuf ) {
                bbuf.position( ioff );
                bbuf.asLongBuffer().get( a, 0, count );
            }
        }
    }

    static void readFloats( ByteBuffer bbuf, int ioff, int count, float[] a ) {
        if ( count == 1 ) {
            a[ 0 ] = bbuf.getFloat( ioff );
        }
        else {
            synchronized ( bbuf ) {
                bbuf.position( ioff );
                bbuf.asFloatBuffer().get( a, 0, count );
            }
        }
    }

    static void readDoubles( ByteBuffer bbuf, int ioff, int count,
                             double[] a ) {
        if ( count == 1 ) {
            a[ 0 ] = bbuf.getDouble( ioff );
        }
        else {
            synchronized ( bbuf ) {
                bbuf.position( ioff );
                bbuf.asDoubleBuffer().get( a, 0, count );
            }
        }
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
