package cdf;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

public class CdfReader {

    private static final Logger logger_ =
        Logger.getLogger( CdfReader.class.getName() );

    public CdfContent readCdf( Buf buf ) {
        Pointer ptr = new Pointer( 0 );

        // Read the CDF magic number bytes.
        int magic1 = buf.readInt( ptr );
        int magic2 = buf.readInt( ptr );

        // Work out from that what variant (if any) of the CDF format
        // this file implements.
        CdfVariant variant = decodeMagic( magic1, magic2 );
        if ( variant == null ) {
            String msg = new StringBuffer()
                .append( "Unrecognised magic numbers: " )
                .append( "0x" )
                .append( Integer.toHexString( magic1 ) )
                .append( ", " )
                .append( "0x" )
                .append( Integer.toHexString( magic2 ) )
                .toString();
            throw new CdfFormatException( msg );
        }
        logger_.info( "CDF magic number for " + variant.label_ );

        // Versions prior to v3 have essentially the same format but
        // use 4-byte file offsets instead of 8-byte ones.
        // We can easily accommodate this by using a buffer reader that
        // reads 4 bytes for offsets instead of 8.
        if ( ! variant.longOffsets_ ) {
            final Buf buf0 = buf;
            buf = new WrapperBuf( buf ) {
                public long readOffset( Pointer ptr ) {
                    return buf0.readInt( ptr );
                }
            };
        }

        // Get CDF and Global descriptor records.
        CdfDescriptorRecord cdr =
            RecordFactory.createRecord( buf, ptr.get(),
                                        CdfDescriptorRecord.class );
        boolean isSingleFile = Record.hasBit( cdr.flags_, 1 );
        if ( ! isSingleFile ) {
            throw new CdfFormatException( "Multi-file CDFs not supported" );
        }
        GlobalDescriptorRecord gdr =
            RecordFactory.createRecord( buf, cdr.gdrOffset_,
                                        GlobalDescriptorRecord.class );

        // Store global format information.
        int encoding = cdr.encoding_;
        boolean rowMajor = Record.hasBit( cdr.flags_, 0 );
        int[] rDimSizes = gdr.rDimSizes_;
        CdfInfo cdfInfo = new CdfInfo( encoding, rowMajor, rDimSizes );

        // Read the rVariable and zVariable records.
        VariableDescriptorRecord[] rvdrs =
            walkVariableList( buf, gdr.nrVars_, gdr.rVdrHead_ );
        VariableDescriptorRecord[] zvdrs =
            walkVariableList( buf, gdr.nzVars_, gdr.zVdrHead_ );

        // Read the attributes records (global and variable attributes
        // are found in the same list).
        AttributeDescriptorRecord[] adrs =
            walkAttributeList( buf, gdr.numAttr_, gdr.adrHead_ );

        VariableDescriptorRecord[] vdrs = arrayConcat( rvdrs, zvdrs );
        final Variable[] variables = new Variable[ vdrs.length ];
        for ( int iv = 0; iv < vdrs.length; iv++ ) {
            variables[ iv ] = createVariable( vdrs[ iv ], cdfInfo );
        }

        List<GlobalAttribute> globalAtts = new ArrayList<GlobalAttribute>();
        List<VariableAttribute> varAtts = new ArrayList<VariableAttribute>();
        for ( int ia = 0; ia < adrs.length; ia++ ) {
            AttributeDescriptorRecord adr = adrs[ ia ];
            Entry[] grEntries =
                walkEntryList( buf, adr.nGrEntries_, adr.agrEdrHead_,
                               adr.maxGrEntry_, cdfInfo );
            Entry[] zEntries =
                walkEntryList( buf, adr.nZEntries_, adr.azEdrHead_,
                               adr.maxZEntry_, cdfInfo );
            boolean isGlobal = Record.hasBit( adr.scope_, 0 );
            if ( isGlobal ) {
                globalAtts.add( createGlobalAttribute( adr, grEntries,
                                                       zEntries ) );
            }
            else {
                varAtts.add( createVariableAttribute( adr, grEntries,
                                                      zEntries ) );
            }
        }

        final GlobalAttribute[] gAtts =
            globalAtts.toArray( new GlobalAttribute[ 0 ] );
        final VariableAttribute[] vAtts =
            varAtts.toArray( new VariableAttribute[ 0 ] );
        return new CdfContent() {
            public GlobalAttribute[] getGlobalAttributes() {
                return gAtts;
            }
            public VariableAttribute[] getVariableAttributes() {
                return vAtts;
            }
            public Variable[] getVariables() {
                return variables;
            }
        };
    }

    private CdfVariant decodeMagic( int magic1, int magic2 ) {
        final String label;
        final boolean longOffsets;
        final boolean compressed;
        if ( magic1 == 0xcdf30001 ) {
            label = "V3";
            longOffsets = true;
            if ( magic2 == 0x0000ffff ) {
                compressed = false;
            }
            else if ( magic2 == 0xcccc0001 ) {
                compressed = true;
            }
            else {
                return null;
            }
        }
        else if ( magic1 == 0xcdf26002 ) {  // version 2.6/2.7
            label = "V2.6/2.7";
            longOffsets = false;
            if ( magic2 == 0x0000ffff ) {
                compressed = false;
            }
            else if ( magic2 == 0xcccc0001 ) {
                compressed = true;
            }
            else {
                return null;
            }
        }
        else if ( magic1 == 0x0000ffff ) { // pre-version 2.6
            label = "pre-V2.6";
            longOffsets = false;
            if ( magic2 == 0x0000ffff ) {
                compressed = false;
            }
            else {
                return null;
            }
        }
        else {
            return null;
        }
        return new CdfVariant( label, longOffsets, compressed );
    }

    private VariableDescriptorRecord[] walkVariableList( Buf buf, int nvar,
                                                         long head ) {
        VariableDescriptorRecord[] vdrs = new VariableDescriptorRecord[ nvar ];
        long off = head;
        for ( int iv = 0; iv < nvar; iv++ ) {
            VariableDescriptorRecord vdr =
                RecordFactory.createRecord( buf, off,
                                            VariableDescriptorRecord.class );
            vdrs[ iv ] = vdr;
            off = vdr.vdrNext_;
        }
        return vdrs;
    }

    private AttributeDescriptorRecord[] walkAttributeList( Buf buf, int natt,
                                                           long head ) {
        AttributeDescriptorRecord[] adrs =
            new AttributeDescriptorRecord[ natt ];
        long off = head;
        for ( int ia = 0; ia < natt; ia++ ) {
            AttributeDescriptorRecord adr =
                RecordFactory.createRecord( buf, off,
                                            AttributeDescriptorRecord.class );
            adrs[ ia ] = adr;
            off = adr.adrNext_;
        }
        return adrs;
    }

    private Entry[] walkEntryList( Buf buf, int nent, long head, int maxent,
                                   CdfInfo info ) {
        Entry[] entries = new Entry[ Math.max( 0, maxent ) ];
        long off = head;
        for ( int ie = 0; ie < nent; ie++ ) {
            AttributeEntryDescriptorRecord aedr =
                RecordFactory
               .createRecord( buf, off, AttributeEntryDescriptorRecord.class );
            entries[ aedr.num_ ] = createEntry( aedr, info );
            off = aedr.aedrNext_;
        }
        return entries;
    }

    private Entry createEntry( AttributeEntryDescriptorRecord aedr,
                               CdfInfo info ) {
        int dataType = aedr.dataType_;
        int encoding = info.getEncoding();
        int[] dimSizes = new int[ 0 ];
        int[] dimVarys = new int[ 0 ];
        boolean rowMajor = false;
        int numElems = aedr.numElems_;
        final DataReader dataReader =
            DataReaderFactory.createDataReader( dataType, encoding, dimSizes,
                                                dimVarys, rowMajor, numElems );

        final Object value =
            dataReader.readRawValue( aedr.getBuf(), aedr.getValueOffset() );
        return new Entry() {
            public DataReader getDataReader() {
                return dataReader;
            }
            public Object getValue() {
                return value;
            }
        };
    }

    private Variable createVariable( VariableDescriptorRecord vdr,
                                     CdfInfo info ) {
        return new VdrVariable( vdr, info );
    }

    private GlobalAttribute
            createGlobalAttribute( AttributeDescriptorRecord adr,
                                   Entry[] grEntries, Entry[] zEntries ) {
        final Entry[] entries = arrayConcat( grEntries, zEntries );
        final String name = adr.name_;
        return new GlobalAttribute() {
            public String getName() {
                return name;
            }
            public Entry[] getEntries() {
                return entries;
            }
        };
    }

    private VariableAttribute
            createVariableAttribute( AttributeDescriptorRecord adr,
                                     final Entry[] grEntries,
                                     final Entry[] zEntries ) {
        final String name = adr.name_;
        return new VariableAttribute() {
            public String getName() {
                return name;
            }
            public Entry getEntry( Variable variable ) {
                return ( variable.isZVariable() ? zEntries : grEntries )
                       [ variable.getNum() ];
            }
        };
    }

    /**
     * Concatenates two arrays to form a single one.
     *
     * @param  a1  first array
     * @param  a2  second array
     * @return  concatenated array
     */
    private static <T> T[] arrayConcat( T[] a1, T[] a2 ) {
        int count = a1.length + a2.length;
        List<T> list = new ArrayList<T>( count );
        list.addAll( Arrays.asList( a1 ) );
        list.addAll( Arrays.asList( a2 ) );
        Class eClazz = a1.getClass().getComponentType();
        @SuppressWarnings("unchecked")
        T[] result =
            (T[]) list.toArray( (Object[]) Array.newInstance( eClazz, count ) );
        return result;
    }

    private static class CdfVariant {
        final String label_;
        final boolean longOffsets_;
        final boolean compressed_;
        CdfVariant( String label, boolean longOffsets, boolean compressed ) {
            label_ = label;
            longOffsets_ = longOffsets;
            compressed_ = compressed;
        }
    }
}
