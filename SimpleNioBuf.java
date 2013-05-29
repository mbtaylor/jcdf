package cdf;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

public class SimpleNioBuf implements Buf {

    private final ByteBuffer byteBuf_;
    private final ByteBuffer dataBuf_;
    private boolean isBit64_;
    private boolean isBigendian_;

    public SimpleNioBuf( ByteBuffer byteBuf, boolean isBit64,
                         boolean isBigendian ) {
        byteBuf_ = byteBuf;
        dataBuf_ = byteBuf.duplicate();
        setBit64( isBit64 );
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
        return isBit64_
             ? byteBuf_.getLong( toInt( ptr.getAndIncrement( 8 ) ) )
             : (long) byteBuf_.getInt( toInt( ptr.getAndIncrement( 4 ) ) );
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

    public synchronized void setBit64( boolean isBit64 ) {
        isBit64_ = isBit64;
    }

    public synchronized void setEncoding( boolean bigend ) {
        dataBuf_.order( bigend ? ByteOrder.BIG_ENDIAN
                               : ByteOrder.LITTLE_ENDIAN );
        isBigendian_ = bigend;
    }

    public boolean isBigendian() {
        return isBigendian_;
    }

    public boolean isBit64() {
        return isBit64_;
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
        return Bufs.createByteBufferInputStream( strmBuf );
    }

    public Buf fillNewBuf( long count, InputStream in ) throws IOException {
        int icount = toInt( count );
        ByteBuffer bbuf = ByteBuffer.allocateDirect( icount );
        ReadableByteChannel chan = Channels.newChannel( in );
        while ( icount > 0 ) {
            int nr = chan.read( bbuf );
            if ( nr < 0 ) {
                throw new EOFException();
            }
            else { 
                icount -= nr;
            }
        }
        return new SimpleNioBuf( bbuf, isBit64_, isBigendian_ );
    }

    private static int toInt( long lvalue ) {
        int ivalue = (int) lvalue;
        if ( ivalue != lvalue ) {
            throw new IllegalArgumentException( "Pointer out of range: "
                                              + lvalue + " >32 bits" );
        }
        return ivalue;
    }
}
