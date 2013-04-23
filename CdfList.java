package cdf.util;

import cdf.CdfContent;
import cdf.CdfReader;
import cdf.Entry;
import cdf.GlobalAttribute;
import cdf.Variable;
import cdf.VariableAttribute;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class CdfList {

    private final CdfReader crdr_;
    private final PrintStream out_;

    public CdfList( CdfReader crdr, PrintStream out ) {
        crdr_ = crdr;
        out_ = out;
    }

    public void run() {
        CdfContent cdf = crdr_.readCdf();
        GlobalAttribute[] gAtts = cdf.getGlobalAttributes();
        VariableAttribute[] vAtts = cdf.getVariableAttributes();
        Variable[] vars = cdf.getVariables();
        
        header( "Global Attributes" );
        for ( int iga = 0; iga < gAtts.length; iga++ ) {
            GlobalAttribute gAtt = gAtts[ iga ];
            out_.println( "    " + gAtt.getName() );
            Entry[] entries = gAtt.getEntries();
            for ( int ie = 0; ie < entries.length; ie++ ) {
                out_.println( "        " + entries[ ie ].getValue() );
            }
        }

        for ( int iv = 0; iv < vars.length; iv++ ) {
            Variable var = vars[ iv ];
            header( "Variable " + var.getNum() + ": " + var.getName() );
            for ( int ia = 0; ia < vAtts.length; ia++ ) {
                VariableAttribute vAtt = vAtts[ ia ];
                Entry entry = vAtt.getEntry( var );
                if ( entry != null ) {
                    out_.println( "    " + vAtt.getName()
                                + ":\t" + entry.getValue() );
                }
            }
        }
    }

    private void header( String txt ) {
        out_.println( txt );
        StringBuffer sbuf = new StringBuffer( txt.length() );
        for ( int i = 0; i < txt.length(); i++ ) {
            sbuf.append( '-' );
        }
        out_.println( sbuf.toString() );
    }

    public static int runMain( String[] args ) throws IOException {
        String usage = new StringBuffer()
           .append( "\n   Usage: " )
           .append( CdfList.class.getName() )
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
        }

        new CdfList( new CdfReader( file ), System.out ).run();
        return 0;
    }

    public static void main( String[] args ) throws IOException {
        int status = runMain( args );
        if ( status != 0 ) {
            System.exit( status );
        }
    }
}
