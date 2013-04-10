package cdf;

import java.nio.ByteBuffer;
import java.util.ArrayList;

public class NioBuf implements Buf {

    private final ByteBuffer byteBuf_;

    public NioBuf( ByteBuffer byteBuf ) {
        byteBuf_ = byteBuf;
    }

    public int readInt( Offset offset ) {
        return byteBuf_.getInt( offset.getAndIncrement( 4 ) );
    }

    public long readLong( Offset offset ) {
        return byteBuf_.getLong( offset.getAndIncrement( 8 ) );
    }

    public String readAsciiString( Offset offset, int nbyte ) {
        byte[] abuf = new byte[ nbyte ];
        byteBuf_.get( abuf, offset.getAndIncrement( nbyte ), nbyte );
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
}
