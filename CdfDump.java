package cdf.util;

import cdf.Buf;
import cdf.CdfReader;
import cdf.Record;
import cdf.RecordFactory;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class CdfDump {

    private final CdfReader crdr_;
    private final PrintStream out_;
    private final boolean writeFields_;

    public CdfDump( CdfReader crdr, PrintStream out, boolean writeFields ) {
        crdr_ = crdr;
        out_ = out;
        writeFields_ = writeFields;
    }

    public void run() {
        Buf buf = crdr_.getBuf();
        RecordFactory recFact = crdr_.getRecordFactory();
        long offset = 8;
        long leng = buf.getLength();
        for ( int ix = 0; offset < leng; ix++ ) {
            Record rec = recFact.createRecord( buf, offset );
            dumpRecord( ix, rec, offset );
            offset += rec.getRecordSize();
        }
    }

    private void dumpRecord( int index, Record rec, long offset ) {
        out_.println( index + ":\t"
                    + rec.getRecordTypeAbbreviation() + "\t"
                    + rec.getRecordType() + "\t"
                    + rec.getRecordSize() + "\t"
                    + "0x" + Long.toHexString( offset ) );
        if ( writeFields_ ) {
            Field[] fields = rec.getClass().getFields();
            for ( int i = 0; i < fields.length; i++ ) {
                Field field = fields[ i ];
                int mods = field.getModifiers();
                if ( Modifier.isFinal( mods ) &&
                     Modifier.isPublic( mods ) && 
                     ! Modifier.isStatic( mods ) ) {
                    String name = field.getName();
                    Object value;
                    try {
                        value = field.get( rec );
                    }
                    catch ( IllegalAccessException e ) {
                        throw new RuntimeException( "Reflection error", e );
                    }
                    out_.println( formatFieldValue( name, value ) );
                }
            }
        }
    }

    private String formatFieldValue( String name, Object value ) {
        StringBuffer sbuf = new StringBuffer();
        sbuf.append( spaces( 4 ) );
        sbuf.append( name )
            .append( ":" );
        sbuf.append( spaces( 20 - sbuf.length() ) );
        if ( value == null ) {
        }
        else if ( value.getClass().isArray() ) {
            int len = Array.getLength( value );
            if ( value instanceof long[] ) {
                long[] larray = (long[]) value;
                for ( int i = 0; i < len; i++ ) {
                    if ( i > 0 ) {
                        sbuf.append( ", " );
                    }
                    sbuf.append( "0x" )
                        .append( Long.toHexString( larray[ i ] ) );
                }
            }
            else {
                for ( int i = 0; i < len; i++ ) {
                    if ( i > 0 ) {
                        sbuf.append( ", " );
                    }
                    sbuf.append( Array.get( value, i ) );
                }
            }
        }
        else {
            if ( value instanceof Long ) {
                sbuf.append( "0x" )
                    .append( Long.toHexString( ((Long) value).longValue() ) );
            }
            else {
                sbuf.append( value.toString() );
            }
        }
        return sbuf.toString();
    }

    private static String spaces( int count ) {
        StringBuffer sbuf = new StringBuffer( count );
        for ( int i = 0; i < count; i++ ) {
            sbuf.append( ' ' );
        }
        return sbuf.toString();
    }

    public static int runMain( String[] args ) throws IOException {
        String usage = new StringBuffer()
           .append( "\n   Usage:" )
           .append( CdfDump.class.getName() )
           .append( " [-help]" )
           .append( " [-fields]" )
           .append( " <cdf-file>" )
           .append( "\n" )
           .toString();

        List<String> argList = new ArrayList<String>( Arrays.asList( args ) );
        File file = null;
        boolean writeFields = false;
        for ( Iterator<String> it = argList.iterator(); it.hasNext(); ) {
            String arg = it.next();
            if ( arg.startsWith( "-h" ) ) {
                it.remove();
                System.out.println( usage );
                return 0;
            }
            else if ( arg.startsWith( "-field" ) ) {
                it.remove();
                writeFields = true;
            }
            else if ( file == null ) {
                it.remove();
                file = new File( arg );
            }
        }
        if ( ! argList.isEmpty() ) {
            System.err.println( "Unused args: " + argList );
            System.err.println( usage );
            return 1;
        }
        if ( file == null ) {
            System.err.println( usage );
            return 1;
        }

        new CdfDump( new CdfReader( file ), System.out, writeFields ).run();
        return 0;
    }

    public static void main( String[] args ) throws IOException {
        int status = runMain( args );
        if ( status != 0 ) {
            System.exit( status );
        }
    }
}
