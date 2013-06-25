package cdf.test;

import cdf.CdfContent;
import cdf.CdfReader;
import cdf.GlobalAttribute;
import cdf.Variable;
import cdf.VariableAttribute;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Tests the contents of two of the example files
 * (example1.cdf and example2.cdf) from the samples directory of the
 * CDF distribution.  The assertions in this file were written by
 * examining the output of cdfdump by eye.
 */
public class ExampleTest {

    private static boolean assertionsOn_;

    public void testExample1( File ex1file ) throws IOException {
        CdfContent content = new CdfContent( new CdfReader( ex1file ) );

        GlobalAttribute[] gatts = content.getGlobalAttributes();
        assert gatts.length == 1;
        GlobalAttribute gatt0 = gatts[ 0 ];
        assert "TITLE".equals( gatt0.getName() );
        assert Arrays.equals( new String[] { "CDF title", "Author: CDF" },
                              gatt0.getEntries() );

        VariableAttribute[] vatts = content.getVariableAttributes();
        assert vatts.length == 2;
        assert "FIELDNAM".equals( vatts[ 0 ].getName() );
        assert "UNITS".equals( vatts[ 1 ].getName() );

        Variable[] vars = content.getVariables();
        assert vars.length == 3;
        assert "Time".equals( vars[ 0 ].getName() );
        assert "Latitude".equals( vars[ 1 ].getName() );
        assert "Image".equals( vars[ 2 ].getName() );
        assert vars[ 0 ].getSummary().matches( "INT4 .* 0:\\[\\] T/" );
        assert vars[ 1 ].getSummary().matches( "INT2 .* 1:\\[181\\] T/T" );
        assert vars[ 2 ].getSummary().matches( "INT4 .* 2:\\[10,20\\] T/TT" );

        assert vatts[ 1 ].getEntry( vars[ 0 ] ).equals( "Hour/Minute" );
        assert vatts[ 1 ].getEntry( vars[ 1 ] ) == null;

        assert readShapedRecord( vars[ 0 ], 0, true )
              .equals( new Integer( 23 ) );
        assert readShapedRecord( vars[ 0 ], 1, true ) == null;
        assert readShapedRecord( vars[ 0 ], 2, true ) == null;
        assert Arrays.equals( (short[]) readShapedRecord( vars[ 1 ], 0, true ),
                              shortSequence( -90, 1, 181 ) );
        assert Arrays.equals( (short[]) readShapedRecord( vars[ 1 ], 0, false ),
                              shortSequence( -90, 1, 181 ) );
        assert readShapedRecord( vars[ 1 ], 1, true ) == null;
        assert readShapedRecord( vars[ 1 ], 2, false ) == null;
        assert Arrays.equals( (int[]) readShapedRecord( vars[ 2 ], 0, true ),
                              intSequence( 0, 1, 200 ) );
        assert Arrays.equals( (int[]) readShapedRecord( vars[ 2 ], 1, true ),
                              intSequence( 200, 1, 200 ) );
        assert Arrays.equals( (int[]) readShapedRecord( vars[ 2 ], 2, true ),
                              intSequence( 400, 1, 200 ) );
        int[] sideways = (int[]) readShapedRecord( vars[ 2 ], 0, false );
        assert sideways[ 0 ] == 0;
        assert sideways[ 1 ] == 20;
        assert sideways[ 2 ] == 40;
        assert sideways[ 10 ] == 1;
        assert sideways[ 199 ] == 199;
    }

    public void testExample2( File ex2file ) throws IOException {
        CdfContent content = new CdfContent( new CdfReader( ex2file ) );

        GlobalAttribute[] gatts = content.getGlobalAttributes();
        assert gatts.length == 1;
        GlobalAttribute gatt0 = gatts[ 0 ];
        assert "TITLE".equals( gatt0.getName() );
        assert "An example CDF (2)."
              .equals( ((String) gatt0.getEntries()[ 0 ]).trim() );

        VariableAttribute[] vatts = content.getVariableAttributes();
        assert vatts.length == 9;
        VariableAttribute fnVatt = vatts[ 0 ];
        VariableAttribute vminVatt = vatts[ 1 ];
        VariableAttribute vmaxVatt = vatts[ 2 ];
        assert fnVatt.getName().equals( "FIELDNAM" );
        assert vminVatt.getName().equals( "VALIDMIN" );
        assert vmaxVatt.getName().equals( "VALIDMAX" );

        Variable[] vars = content.getVariables();
        assert vars.length == 4;
        Variable timeVar = vars[ 0 ];
        Variable lonVar = vars[ 1 ];
        Variable latVar = vars[ 2 ];
        Variable tempVar = vars[ 3 ];
        assert timeVar.getName().equals( "Time" );
        assert lonVar.getName().equals( "Longitude" );
        assert latVar.getName().equals( "Latitude" );
        assert tempVar.getName().equals( "Temperature" );

        assert timeVar.getSummary().matches( "INT4 .* 0:\\[\\] T/" );
        assert lonVar.getSummary().matches( "REAL4 .* 1:\\[2\\] F/T" );
        assert latVar.getSummary().matches( "REAL4 .* 1:\\[2\\] F/T" );
        assert tempVar.getSummary().matches( "REAL4 .* 2:\\[2,2\\] T/TT" );
        assert timeVar.getRecordCount() == 24;
        assert tempVar.getRecordCount() == 24;
        assert lonVar.getRecordCount() == 1;
        assert latVar.getRecordCount() == 1;

        assert ((String) fnVatt.getEntry( timeVar )).trim()
                     .equals( "Time of observation" );
        assert vminVatt.getEntry( timeVar ).equals( new Integer( 0 ) );
        assert vmaxVatt.getEntry( timeVar ).equals( new Integer( 2359 ) );
        assert vminVatt.getEntry( lonVar ).equals( new Float( -180f ) );
        assert vmaxVatt.getEntry( lonVar ).equals( new Float( 180f ) );

        assert readShapedRecord( timeVar, 0, true )
              .equals( new Integer( 0 ) );
        assert readShapedRecord( timeVar, 23, false )
              .equals( new Integer( 2300 ) );

        float[] lonVal = new float[] { -165f, -150f };
        float[] latVal = new float[] { 40f, 30f };
        for ( int irec = 0; irec < 24; irec++ ) {
            assert Arrays.equals( (float[]) readShapedRecord( lonVar, irec,
                                                              true ),
                                  lonVal );
            assert Arrays.equals( (float[]) readShapedRecord( latVar, irec,
                                                              false ),
                                  latVal );
        }
        assert Arrays.equals( (float[]) readShapedRecord( tempVar, 0, true ),
                              new float[] { 20f, 21.7f, 19.2f, 20.7f } );
        assert Arrays.equals( (float[]) readShapedRecord( tempVar, 23, true ),
                              new float[] { 21f, 19.5f, 18.4f, 22f } );
        assert Arrays.equals( (float[]) readShapedRecord( tempVar, 23, false ),
                              new float[] { 21f, 18.4f, 19.5f, 22f } );

    }

    private Object readShapedRecord( Variable var, int irec, boolean rowMajor )
            throws IOException {
        return var.readShapedRecord( irec, rowMajor,
                                     var.createRawValueArray() );
    }

    private short[] shortSequence( int start, int step, int count ) {
        short[] array = new short[ count ];
        for ( int i = 0; i < count; i++ ) {
            array[ i ] = (short) ( start + i * step );
        }
        return array;
    }

    private int[] intSequence( int start, int step, int count ) {
        int[] array = new int[ count ];
        for ( int i = 0; i < count; i++ ) {
            array[ i ] = start + i * step;
        }
        return array;
    }

    private static boolean checkAssertions() {
        assertionsOn_ = true;
        return true;
    }

    /**
     * Main method.  Run with locations of files example1.cdf and example2.cdf
     * as arguments.  Use -help for help.
     * Tests are made using java assertions, so this test must be
     * run with java assertions enabled.  If it's not, it will fail anyway.
     */
    public static void main( String[] args ) throws IOException {
        assert checkAssertions();
        if ( ! assertionsOn_ ) {
            throw new RuntimeException( "Assertions disabled - bit pointless" );
        }
        String usage = "Usage: " + ExampleTest.class.getName()
                     + " example1.cdf example2.cdf";
        if ( args.length != 2 ) {
            System.err.println( usage );
            System.exit( 1 );
        }
        File ex1 = new File( args[ 0 ] );
        File ex2 = new File( args[ 1 ] );
        if ( ! ex1.canRead() || ! ex2.canRead() ) {
            System.err.println( usage );
            System.exit( 1 );
        }
        ExampleTest extest = new ExampleTest();
        extest.testExample1( ex1 );
        extest.testExample2( ex2 );
    }
}
