package cdf.util;

import cdf.CdfContent;
import cdf.CdfReader;
import cdf.DataType;
import cdf.GlobalAttribute;
import cdf.Variable;
import cdf.VariableAttribute;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * Utility to describe a CDF file, optionally with record data.
 * Intended to be used from the commandline via the <code>main</code> method.
 * The output format is somewhat reminiscent of the <code>cdfdump</code>
 * command in the CDF distribution.
 *
 * @author   Mark Taylor
 * @since    21 Jun 2013
 */
public class CdfList {

    private final CdfContent cdf_;
    private final PrintStream out_;
    private final boolean writeData_;

    /**
     * Constructor.
     *
     * @param   cdf   CDF content
     * @param   out   output stream for listing
     * @param   writeData  true if data values as well as metadata are to
     *                     be written
     */
    public CdfList( CdfContent cdf, PrintStream out, boolean writeData ) {
        cdf_ = cdf;
        out_ = out;
        writeData_ = writeData;
    }

    /**
     * Does the work, writing output.
     */
    public void run() throws IOException {

        // Read the CDF.
        GlobalAttribute[] gAtts = cdf_.getGlobalAttributes();
        VariableAttribute[] vAtts = cdf_.getVariableAttributes();
        Variable[] vars = cdf_.getVariables();
        
        // Write global attribute information.
        header( "Global Attributes" );
        for ( int iga = 0; iga < gAtts.length; iga++ ) {
            GlobalAttribute gAtt = gAtts[ iga ];
            out_.println( "    " + gAtt.getName() );
            Object[] entries = gAtt.getEntries();
            for ( int ie = 0; ie < entries.length; ie++ ) {
                out_.println( "        " + entries[ ie ] );
            }
        }

        // Write variable information.
        for ( int iv = 0; iv < vars.length; iv++ ) {
            out_.println();
            Variable var = vars[ iv ];
            header( "Variable " + var.getNum() + ": " + var.getName()
                  + "  ---  " + var.getSummary() );
            for ( int ia = 0; ia < vAtts.length; ia++ ) {
                VariableAttribute vAtt = vAtts[ ia ];
                Object entry = vAtt.getEntry( var );
                if ( entry != null ) {
                    out_.println( "    " + vAtt.getName() + ":\t" + entry );
                }
            }

            // Optionally write variable data as well.
            if ( writeData_ ) {
                DataType dataType = var.getDataType();
                Object abuf = var.createRawValueArray();
                int nrec = var.getRecordCount();
                for ( int ir = 0; ir < nrec; ir++ ) {
                    var.readRawRecord( ir, abuf );
                    StringBuffer sbuf = new StringBuffer();
                    boolean hasRec = var.hasRecord( ir );
                    if ( ! hasRec ) {
                        sbuf.append( '[' );
                    }
                    sbuf.append( Integer.toString( ir ) );
                    if ( ! hasRec ) {
                        sbuf.append( ']' );
                    }
                    sbuf.append( ':' );
                    sbuf.append( '\t' );
                    sbuf.append( formatValues( abuf, dataType ) );
                    out_.println( sbuf.toString() );
                }
            }
        }
    }

    /**
     * Applies string formatting to a value of a given data type.
     *
     * @param  abuf   array buffer containing data
     * @param  dataType  data type for data
     * @return  string representation of value
     */
    private String formatValues( Object abuf, DataType dataType ) {
        StringBuffer sbuf = new StringBuffer();
        if ( abuf == null ) {
        }
        else if ( abuf.getClass().isArray() ) {
            int groupSize = dataType.getGroupSize();
            int len = Array.getLength( abuf );
            for ( int i = 0; i < len; i += groupSize ) {
                if ( i > 0 ) {
                    sbuf.append( ", " );
                }
                sbuf.append( dataType.formatArrayValue( abuf, i ) );
            }
        }
        else {
            sbuf.append( dataType.formatScalarValue( abuf ) );
        }
        return sbuf.toString();
    }

    /**
     * Writes a header to the output listing.
     *
     * @param  txt  header text
     */
    private void header( String txt ) {
        out_.println( txt );
        StringBuffer sbuf = new StringBuffer( txt.length() );
        for ( int i = 0; i < txt.length(); i++ ) {
            sbuf.append( '-' );
        }
        out_.println( sbuf.toString() );
    }

    /**
     * Does the work for the command line tool, handling arguments.
     * Sucess is indicated by the return value.
     *
     * @param  args   command-line arguments
     * @return   0 for success, non-zero for failure
     */
    public static int runMain( String[] args ) throws IOException {

        // Usage string.
        String usage = new StringBuffer()
           .append( "\n   Usage: " )
           .append( CdfList.class.getName() )
           .append( " [-help]" )
           .append( " [-verbose]" ) 
           .append( " [-data]" )
           .append( " <cdf-file>" )
           .append( "\n" )
           .toString();

        // Process arguments.
        List<String> argList = new ArrayList<String>( Arrays.asList( args ) );
        File file = null;
        boolean writeData = false;
        int verb = 0;
        for ( Iterator<String> it = argList.iterator(); it.hasNext(); ) {
            String arg = it.next();
            if ( arg.startsWith( "-h" ) ) {
                it.remove();
                System.out.println( usage );
                return 0;
            }
            else if ( arg.equals( "-verbose" ) || arg.equals( "-v" ) ) {
                it.remove();
                verb++;
            }
            else if ( arg.equals( "+verbose" ) || arg.equals( "+v" ) ) {
                it.remove();
                verb--;
            }
            else if ( arg.equals( "-data" ) ) {
                it.remove();
                writeData = true;
            }
            else if ( file == null ) {
                it.remove();
                file = new File( arg );
            }
        }

        // Validate arguments.
        if ( ! argList.isEmpty() ) {
            System.err.println( "Unused args: " + argList );
            System.err.println( usage );
            return 1;
        }
        if ( file == null ) {
            System.err.println( usage );
            return 1;
        }

        // Configure and run.
        LogUtil.setVerbosity( verb );
        new CdfList( new CdfContent( new CdfReader( file ) ),
                     System.out, writeData ).run();
        return 0;
    }

    /**
     * Main method.  Use -help for arguments.
     */
    public static void main( String[] args ) throws IOException {
        int status = runMain( args );
        if ( status != 0 ) {
            System.exit( status );
        }
    }
}
