package cdf;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public abstract class Record {

    private final RecordPlan plan_;
    private final Logger logger_ = Logger.getLogger( Record.class.getName() );

    protected Record( RecordPlan plan ) {
        plan_ = plan;
    }

    protected Record( RecordPlan plan, int fixedType ) {
        this( plan );
        int planType = plan.getRecordType();
        if ( planType != fixedType ) {
            throw new IllegalArgumentException( "Incorrect record type ("
                                              + planType + " != " + fixedType );
        }
    }

    public long getRecordSize() {
        return plan_.getRecordSize();
    }

    public int getRecordType() {
        return plan_.getRecordType();
    }

    protected int checkIntValue( int actualValue, int fixedValue ) {
        if ( actualValue != fixedValue ) {
            warnFormat( "Unexpected fixed value " + actualValue + " != "
                       + fixedValue );
        }
        return actualValue;
    }

    protected void checkEndRecord( Pointer ptr ) {
        long readCount = plan_.getReadCount( ptr );
        long recSize = getRecordSize();
        if ( readCount != recSize ) {
            warnFormat( "Bytes read in record not equal to record size ("
                      + readCount + " != " + recSize + ")" );
        }
    }

    protected DataType getDataType( int itype ) {

        // Dummy implementation, need to actually write this.
        return null;
    }

    protected void warnFormat( String msg ) {
        assert false : msg;
        logger_.warning( msg );
    }

    /**
     * Reads a moderately-sized integer array.
     * If it's bulk data, we should use a different method.
     */
    public static int[] readIntArray( Buf buf, Pointer ptr, int count ) {
        int[] array = new int[ count ];
        for ( int i = 0; i < count; i++ ) {
            array[ i ] = buf.readInt( ptr );
        }
        return array;
    }

    /**
     * Reads a moderately-sized offset array.
     * If it's bulk data, we should use a different method.
     */
    public static long[] readOffsetArray( Buf buf, Pointer ptr, int count ) {
        long[] array = new long[ count ];
        for ( int i = 0; i < count; i++ ) {
            array[ i ] = buf.readOffset( ptr );
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

    public static boolean hasBit( int flags, int ibit ) {
        return ( flags >> ibit ) % 2 == 1;
    }
}
