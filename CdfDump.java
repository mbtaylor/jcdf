package cdf.util;

import cdf.Buf;
import cdf.CdfReader;
import cdf.CdfDescriptorRecord;
import cdf.GlobalDescriptorRecord;
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
    private final boolean html_;

    public CdfDump( CdfReader crdr, PrintStream out, boolean writeFields,
                    boolean html ) {
        crdr_ = crdr;
        out_ = out;
        writeFields_ = writeFields;
        html_ = html;
    }

    public void run() throws IOException {
        Buf buf = crdr_.getBuf();
        RecordFactory recFact = crdr_.getRecordFactory();
        long offset = 8;
        long leng = buf.getLength();
        long eof = leng;
        CdfDescriptorRecord cdr = null;
        GlobalDescriptorRecord gdr = null;
        long gdroff = -1;
        if ( html_ ) {
            out_.println( "<html><body><pre>" );
        }
        for ( int ix = 0; offset < eof; ix++ ) {
            Record rec = recFact.createRecord( buf, offset );
            dumpRecord( ix, rec, offset );
            if ( cdr == null && rec instanceof CdfDescriptorRecord ) {
                cdr = (CdfDescriptorRecord) rec;
                gdroff = cdr.gdrOffset_;
            }
            if ( offset == gdroff && rec instanceof GlobalDescriptorRecord ) {
                gdr = (GlobalDescriptorRecord) rec;
                eof = gdr.eof_;
            }
            offset += rec.getRecordSize();
        }
        if ( html_ ) {
            out_.println( "<hr />" );
        }
        long extra = leng - eof;
        if ( extra > 0 ) {
            out_.println( " + " + extra + " bytes after final record" );
        }
        if ( html_ ) {
            out_.println( "</pre></body></html>" );
        }
    }

    private void dumpRecord( int index, Record rec, long offset ) {
        StringBuffer sbuf = new StringBuffer();
        if ( html_ ) {
            sbuf.append( "<hr /><strong>" );
        }
        sbuf.append( index )
            .append( ":\t" )
            .append( rec.getRecordTypeAbbreviation() )
            .append( "\t" )
            .append( rec.getRecordType() )
            .append( "\t" )
            .append( rec.getRecordSize() )
            .append( "\t" )
            .append( formatOffsetId( offset ) );
        if ( html_ ) {
            sbuf.append( "</strong>" );
        }
        out_.println( sbuf.toString() );
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
                    out_.println( formatFieldValue( adjustFieldName( name ),
                                                    value ) );
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
                    sbuf.append( formatOffsetRef( larray[ i ] ) );
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
            sbuf.append( value instanceof Long && ! name.endsWith( "Size" )
                             ? formatOffsetRef( ((Long) value).longValue() )
                             : value.toString() );
        }
        return sbuf.toString();
    }

    private String formatOffsetId( long offset ) {
        String txt = "0x" + Long.toHexString( offset );
        return html_ ? "<a name='" + txt + "'>" + txt + "</a>"
                     : txt;
    }

    private String formatOffsetRef( long offset ) {
        String txt = "0x" + Long.toHexString( offset );
        return ( html_ && offset != -1L )
             ? "<a href='#" + txt + "'>" + txt + "</a>"
             : txt;
    }

    private String adjustFieldName( String name ) {
        return name.endsWith( "_" )
             ? name.substring( 0, name.length() - 1 )
             : name;
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
           .append( " [-html]" )
           .append( " <cdf-file>" )
           .append( "\n" )
           .toString();

        List<String> argList = new ArrayList<String>( Arrays.asList( args ) );
        File file = null;
        boolean writeFields = false;
        boolean html = false;
        for ( Iterator<String> it = argList.iterator(); it.hasNext(); ) {
            String arg = it.next();
            if ( arg.equals( "-html" ) ) {
                it.remove();
                html = true;
            }
            else if ( arg.startsWith( "-h" ) ) {
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

        new CdfDump( new CdfReader( file ), System.out, writeFields, html )
           .run();
        return 0;
    }

    public static void main( String[] args ) throws IOException {
        int status = runMain( args );
        if ( status != 0 ) {
            System.exit( status );
        }
    }
}
