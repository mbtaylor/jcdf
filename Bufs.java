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
