package cdf.util;

import cdf.Buf;
import cdf.CdfReader;
import cdf.Record;
import cdf.RecordFactory;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class CdfDump {

    private final CdfReader crdr_;

    public CdfDump( File file ) throws IOException {
        crdr_ = new CdfReader( file );
    }

    public void run() {
        Buf buf = crdr_.getBuf();
        RecordFactory recFact = crdr_.getRecordFactory();
        long offset = 8;
        long leng = buf.getLength();
        while ( offset < leng ) {
            Record rec = recFact.createRecord( buf, offset );
            dumpRecord( rec );
            offset += rec.getRecordSize();
        }
    }

    private void dumpRecord( Record rec ) {
        System.out.println( rec.getRecordTypeAbbreviation() + "\t"
                          + rec.getRecordType() + "\t"
                          + rec.getRecordSize() );
    }

    public static int runMain( String[] args ) throws IOException {
        String usage = new StringBuffer()
           .append( "\n   Usage:" )
           .append( "\n      " )
           .append( CdfDump.class.getName() )
           .append( "\n         " )
           .append( " [-help]" )
           .append( " <cdf-file>" )
           .append( "\n" )
           .toString();

        List<String> argList = new ArrayList<String>( Arrays.asList( args ) );
        File file = null;
        for ( Iterator<String> it = argList.iterator(); it.hasNext(); ) {
            String arg = it.next();
            if ( arg.startsWith( "-h" ) ) {
                it.remove();
                System.out.println( usage );
                return 0;
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

        new CdfDump( file ).run();
        return 0;
    }

    public static void main( String[] args ) throws IOException {
        int status = runMain( args );
        if ( status != 0 ) {
            System.exit( status );
        }
    }
}
