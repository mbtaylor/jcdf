package cdf;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public abstract class Record {

    private final long recordSize_;
    private final int recordType_;
    private final Logger logger_ = Logger.getLogger( Record.class.getName() );

    protected Record( long recSize, int recType ) {
        recordSize_ = recSize;
        recordType_ = recType;
    }

    protected Record( long recSize, int recType, int fixedType ) {
        this( recSize, recType );
        if ( recType != fixedType ) {
            throw new IllegalArgumentException( "Incorrect record type ("
                                              + recType + " != " + fixedType );
        }
    }

    protected int checkIntValue( int actualValue, int fixedValue ) {
        if ( actualValue != fixedValue ) {
            String warning = "Unexpected fixed value " + actualValue + " != "
                           + fixedValue;
            assert false : warning;
            logger_.warning( warning );
        }
        return actualValue;
    }

    /**
     * Reads a moderately-sized integer array.
     * If it's bulk data, we should use a different method.
     */
    public static int[] readIntArray( Buf buf, Offset offset, int count ) {
        int[] array = new int[ count ];
        for ( int i = 0; i < count; i++ ) {
            array[ i ] = buf.readInt( offset );
        }
        return array;
    }

    /**
     * Splits an ASCII string into 0x0A-terminated lines.
     * As per CdfDescriptorRecord copyright field.
     */
    public static String[] toLines( String text ) {
        List<String> lines = new ArrayList<String>();

        /* Line ends in regexes are so inscrutable that use of String.split()
         * seems too much trouble.  See Goldfarb's First Law Of Text
         * Processing. */
        int nc = text.length();
        StringBuilder sbuf = new StringBuilder( nc );
        for ( int i = 0; i < nc; i++ ) {
            char c = text.charAt( i );
            if ( c == 0x0a ) {
                lines.add( sbuf.toString() );
                sbuf.setLength( 0 );
            }
            else {
                sbuf.append( c );
            }
        }
        if ( sbuf.length() > 0 ) {
            lines.add( sbuf.toString() );
        }
        return lines.toArray( new String[ 0 ] );
    }
}
