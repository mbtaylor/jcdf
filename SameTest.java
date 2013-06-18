package cdf.test;

import cdf.CdfContent;
import cdf.CdfReader;
import cdf.GlobalAttribute;
import cdf.Variable;
import cdf.VariableAttribute;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SameTest {

    private final File[] files_;
    private final PrintStream out_;
    private int nerror_;
    private Stack<String> context_;

    public SameTest( File[] files ) {
        files_ = files;
        out_ = System.out;
        context_ = new Stack<String>();
    }

    public void run() throws IOException {
        CdfContent c0 = new CdfReader( files_[ 0 ] ).readCdf();
        context_.clear();
        for ( int i = 1; i < files_.length; i++ ) {
            pushContext( files_[ 0 ].getName(), files_[ i ].getName() );
            compareCdf( c0, new CdfReader( files_[ i ] ).readCdf() );
            popContext();
        }
        if ( nerror_ > 0 ) {
            out_.println( "Error count: " + nerror_ );
        }
    }

    public int getErrorCount() {
        return nerror_;
    }

    private void compareCdf( CdfContent cdf0, CdfContent cdf1 )
            throws IOException {
        pushContext( "Global Attributes" );
        List<Pair<GlobalAttribute>> gattPairs =
            getPairs( cdf0.getGlobalAttributes(),
                      cdf1.getGlobalAttributes() );
        popContext();
        pushContext( "Variable Attributes" );
        List<Pair<VariableAttribute>> vattPairs =
            getPairs( cdf0.getVariableAttributes(),
                      cdf1.getVariableAttributes() );
        popContext();
        pushContext( "Variables" );
        List<Pair<Variable>> varPairs =
            getPairs( cdf0.getVariables(), cdf1.getVariables() );
        popContext();

        pushContext( "Global Attributes" );
        for ( Pair<GlobalAttribute> gattPair : gattPairs ) {
            compareGlobalAttribute( gattPair.item0_, gattPair.item1_ );
        }
        popContext();

        pushContext( "Variable Attributes" );
        for ( Pair<VariableAttribute> vattPair : vattPairs ) {
            compareVariableAttribute( vattPair.item0_, vattPair.item1_,
                                      varPairs );
        }
        popContext();

        pushContext( "Variables" );
        for ( Pair<Variable> varPair : varPairs ) {
            compareVariable( varPair.item0_, varPair.item1_ );
        }
        popContext();
    }

    private void compareGlobalAttribute( GlobalAttribute gatt0,
                                         GlobalAttribute gatt1 ) {
        pushContext( gatt0.getName(), gatt1.getName() );
        compareScalar( gatt0.getName(), gatt1.getName() );
        compareArray( gatt0.getEntries(), gatt1.getEntries() );
        popContext();
    }

    private void compareVariableAttribute( VariableAttribute vatt0,
                                           VariableAttribute vatt1,
                                           List<Pair<Variable>> varPairs ) {
        pushContext( vatt0.getName(), vatt1.getName() );
        compareScalar( vatt0.getName(), vatt1.getName() );
        for ( Pair<Variable> varPair : varPairs ) {
            pushContext( varPair.item0_.getName(), varPair.item1_.getName() );
            compareScalar( vatt0.getEntry( varPair.item0_ ),
                           vatt1.getEntry( varPair.item1_ ) );
            popContext();
        }
        popContext();
    }

    private void compareVariable( Variable var0, Variable var1 )
            throws IOException {
        pushContext( var0.getName(), var1.getName() );
        compareInt( var0.getNum(), var1.getNum() );
        compareScalar( var0.getName(), var1.getName() );
        compareScalar( var0.getDataType(), var1.getDataType() );
        Object work0 = var0.createRawValueArray();
        Object work1 = var1.createRawValueArray();
        int nrec = Math.max( var0.getRecordCount(), var1.getRecordCount() );
        for ( int irec = 0; irec < nrec; irec++ ) {
            pushContext( "rec#" + irec );
            compareValue( var0.readShapedRecord( irec, false, work0 ),
                          var1.readShapedRecord( irec, false, work1 ) );
            compareValue( var0.readShapedRecord( irec, true, work0 ),
                          var1.readShapedRecord( irec, true, work1 ) );
            popContext();
        }
        popContext();
    }

    private void compareInt( int i0, int i1 ) {
        compareScalar( new Integer( i0 ), new Integer( i1 ) );
    }

    private void compareScalar( Object v0, Object v1 ) {
        boolean match = v0 == null ? v1 == null : v0.equals( v1 );
        if ( ! match ) {
            error( "Value mismatch: " + quote( v0 ) + " != " + quote( v1 ) );
        }
    }

    private String quote( Object obj ) {
        return obj instanceof String ? ( "\"" + obj + "\"" )
                                     : String.valueOf( obj );
    }

    private void compareArray( Object arr0, Object arr1 ) {
        int narr0 = Array.getLength( arr0 );
        int narr1 = Array.getLength( arr1 );
        if ( narr0 != narr1 ) {
            error( "Length mismatch: " + narr0 + " != " + narr1 );
        }
        int count = Math.min( narr0, narr1 );
        for ( int i = 0; i < count; i++ ) {
            pushContext( "el#" + i );
            compareScalar( Array.get( arr0, i ), Array.get( arr1, i ) );
            popContext();
        }
    }

    private void compareValue( Object v0, Object v1 ) {
        Object vt = v0 == null ? v1 : v0;
        if ( vt == null ) {
        }
        else if ( vt.getClass().getComponentType() != null ) {
            compareArray( v0, v1 );
        }
        else {
            compareScalar( v0, v1 );
        }
    }

    private void pushContext( String label0, String label1 ) {
        pushContext( label0.equals( label1 ) ? label0
                                             : ( label0 + "/" + label1 ) );
    }

    private void pushContext( String label ) {
        context_.push( label );
    }

    private void popContext() {
        context_.pop();
    }

    private void error( String msg ) {
        out_.println( context_.toString() + ": " + msg );
        nerror_++;
    }
                                            
    private <T> List<Pair<T>> getPairs( T[] arr0, T[] arr1 ) {
        if ( arr1.length != arr0.length ) {
            error( "Array length mismatch: "
                 + arr0.length + " != " + arr1.length );
        }
        int count = Math.min( arr0.length, arr1.length );
        List<Pair<T>> list = new ArrayList<Pair<T>>( count );
        for ( int i = 0; i < count; i++ ) {
            list.add( new Pair<T>( arr0[ i ], arr1[ i ] ) );
        }
        return list;
    }

    private static class Pair<T> {
        final T item0_;
        final T item1_;
        Pair( T item0, T item1 ) {
            item0_ = item0;
            item1_ = item1;
        }
    }

    public static void main( String[] args ) throws IOException {
        Logger.getLogger( "cdf.CdfReader" ).setLevel( Level.WARNING );
        File[] files = new File[ args.length ];
        for ( int i = 0; i < args.length; i++ ) {
            files[ i ] = new File( args[ i ] );
        }
        SameTest test = new SameTest( files );
        test.run();
        if ( test.getErrorCount() > 0 ) {
            System.exit( 1 );
        }
    }
}
