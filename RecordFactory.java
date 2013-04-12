package cdf;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RecordFactory {

    private static final Map<Integer,TypedRecordFactory> factoryMap_ =
        createFactoryMap();

    public static Record createRecord( Buf buf, long offset ) {
        Pointer ptr = new Pointer( offset );
        long recSize = buf.readOffset( ptr );
        int recType = buf.readInt( ptr );
        RecordPlan plan = new RecordPlan( offset, recSize, recType, buf );
        TypedRecordFactory tfact = factoryMap_.get( recType );
        if ( tfact == null ) {
            throw new CdfFormatException( "Unknown record type " + recType );
        }
        else {
            return tfact.createRecord( plan );
        }
    }

    public static <R extends Record> R createRecord( Buf buf, long offset,
                                                     Class<R> clazz ) {
        Record rec = createRecord( buf, offset );
        if ( clazz.isInstance( rec ) ) {
            return clazz.cast( rec );
        }
        else {
            String msg = new StringBuffer()
                .append( "Unexpected record type at " )
                .append( "0x" )
                .append( Long.toHexString( offset ) )
                .append( "; got " )
                .append( rec.getClass().getName() )
                .append( " not " )
                .append( clazz.getName() )
                .toString();
            throw new CdfFormatException( msg );
        }
    }

    private static Map<Integer,TypedRecordFactory> createFactoryMap() {
        Map<Integer,TypedRecordFactory> map =
            new HashMap<Integer,TypedRecordFactory>();
        List<Class<? extends Record>> recordClasses =
            new ArrayList<Class<? extends Record>>();
        recordClasses.add( CdfDescriptorRecord.class );
        recordClasses.add( GlobalDescriptorRecord.class );
        recordClasses.add( AttributeDescriptorRecord.class );
        recordClasses.add( AttributeEntryDescriptorRecord.GrVariant.class );
        recordClasses.add( AttributeEntryDescriptorRecord.ZVariant.class );
        recordClasses.add( VariableDescriptorRecord.RVariant.class );
        recordClasses.add( VariableDescriptorRecord.ZVariant.class );
        recordClasses.add( VariableIndexRecord.class );
        recordClasses.add( VariableValuesRecord.class );
        recordClasses.add( CompressedCdfRecord.class );
        recordClasses.add( CompressedParametersRecord.class );
        recordClasses.add( SparsenessParametersRecord.class );
        recordClasses.add( CompressedVariableValuesRecord.class );
        recordClasses.add( UnusedInternalRecord.class );
        for ( Class<? extends Record> clazz : recordClasses ) {
            final TypedRecordFactory<?> trf;
            try {
                trf = createTypedRecordFactory( clazz );
            }
            catch ( NoSuchMethodException e ) {
                throw createBadRecordException( clazz, e );
            }
            catch ( NoSuchFieldException e ) {
                throw createBadRecordException( clazz, e );
            }
            catch ( IllegalAccessException e ) {
                throw createBadRecordException( clazz, e );
            }
            map.put( trf.getRecordType(), trf );
        }
        int[] recTypes = new int[ map.size() ];
        int irt = 0;
        for ( int recType : map.keySet() ) {
            recTypes[ irt++ ] = recType;
        }
        Arrays.sort( recTypes );
        assert Arrays.equals( recTypes, new int[] { -1, 1, 2, 3, 4, 5, 6, 7,
                                                    8, 9, 10, 11, 12, 13 } );
        return Collections.unmodifiableMap( map );
    }

    private static <R extends Record> TypedRecordFactory<R>
                   createTypedRecordFactory( Class<R> clazz )
            throws NoSuchMethodException, NoSuchFieldException,
                   IllegalAccessException {
        return new TypedRecordFactory<R>( clazz );
    }

    private static RuntimeException
                   createBadRecordException( Class<? extends Record> clazz,
                                             Throwable error ) {
        return new RuntimeException( "Badly specified CDF Record class "
                                   + "(programming error)", error );
    }

    private static class TypedRecordFactory<R extends Record> {

        private final Class<R> clazz_;
        private final Constructor<R> constructor_;
        private final int recordType_;

        TypedRecordFactory( Class<R> clazz )
                throws NoSuchMethodException, NoSuchFieldException,
                       IllegalAccessException {
            clazz_ = clazz;
            constructor_ = clazz.getConstructor( RecordPlan.class );
            recordType_ = clazz.getField( "RECORD_TYPE" ).getInt( null );
        }

        int getRecordType() {
            return recordType_;
        }

        Record createRecord( RecordPlan plan ) {
            try {
                return constructor_.newInstance( plan );
            }
            catch ( IllegalAccessException e ) {
                throw new AssertionError( e );
            }
            catch ( InstantiationException e ) {
                throw new AssertionError( e );
            }
            catch ( InvocationTargetException e ) {
                throw new RuntimeException( e );
            }
        }
    }
}
