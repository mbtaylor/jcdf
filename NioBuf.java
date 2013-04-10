package cdf;

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

    public long readLong( Pointer ptr ) {
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

    private int toInt( long lvalue ) {
        int ivalue = (int) lvalue;
        if ( ivalue != lvalue ) {
            throw new IllegalArgumentException( "Pointer out of range: "
                                              + lvalue + " >32 bits" );
        }
        return ivalue;
    }
}
