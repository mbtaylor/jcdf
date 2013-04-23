package cdf;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class RecordFactory {

    private final Map<Integer,TypedRecordFactory> factoryMap_;

    public RecordFactory( int nameLeng ) {
        factoryMap_ = createFactoryMap( nameLeng );
    }

    public Record createRecord( Buf buf, long offset ) {
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

    public <R extends Record> R createRecord( Buf buf, long offset,
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

    private static Map<Integer,TypedRecordFactory>
            createFactoryMap( final int nameLeng ) {
        Map<Integer,TypedRecordFactory> map =
            new HashMap<Integer,TypedRecordFactory>();
        map.put( 1, new TypedRecordFactory() {
            public Record createRecord( RecordPlan plan ) {
                return new CdfDescriptorRecord( plan );
            }
        } );
        map.put( 2, new TypedRecordFactory() {
            public Record createRecord( RecordPlan plan ) {
                return new GlobalDescriptorRecord( plan );
            }
        } );
        map.put( 4, new TypedRecordFactory() {
            public Record createRecord( RecordPlan plan ) {
                return new AttributeDescriptorRecord( plan, nameLeng );
            }
        } );
        map.put( 5, new TypedRecordFactory() {
            public Record createRecord( RecordPlan plan ) {
                return new AttributeEntryDescriptorRecord.GrVariant( plan );
            }
        } );
        map.put( 9, new TypedRecordFactory() {
            public Record createRecord( RecordPlan plan ) {
                return new AttributeEntryDescriptorRecord.ZVariant( plan );
            }
        } );
        map.put( 3, new TypedRecordFactory() {
            public Record createRecord( RecordPlan plan ) {
                return new VariableDescriptorRecord.RVariant( plan, nameLeng );
            }
        } );
        map.put( 8, new TypedRecordFactory() {
            public Record createRecord( RecordPlan plan ) {
                return new VariableDescriptorRecord.ZVariant( plan, nameLeng );
            }
        } );
        map.put( 6, new TypedRecordFactory() {
            public Record createRecord( RecordPlan plan ) {
                return new VariableIndexRecord( plan );
            }
        } );
        map.put( 7, new TypedRecordFactory() {
            public Record createRecord( RecordPlan plan ) {
                return new VariableValuesRecord( plan );
            }
        } );
        map.put( 10, new TypedRecordFactory() {
            public Record createRecord( RecordPlan plan ) {
                return new CompressedCdfRecord( plan );
            }
        } );
        map.put( 11, new TypedRecordFactory() {
            public Record createRecord( RecordPlan plan ) {
                return new CompressedParametersRecord( plan );
            }
        } );
        map.put( 12, new TypedRecordFactory() {
            public Record createRecord( RecordPlan plan ) {
                return new SparsenessParametersRecord( plan );
            }
        } );
        map.put( 13, new TypedRecordFactory() {
            public Record createRecord( RecordPlan plan ) {
                return new CompressedVariableValuesRecord( plan );
            }
        } );
        map.put( -1, new TypedRecordFactory() {
            public Record createRecord( RecordPlan plan ) {
                return new UnusedInternalRecord( plan );
            }
        } );
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

    private static interface TypedRecordFactory<R extends Record> {
        Record createRecord( RecordPlan plan );
    }
}
