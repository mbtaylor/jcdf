package cdf;

import java.lang.reflect.Array;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

public class CdfReader {

    private final CdfDescriptorRecord cdr_;
    private final Buf buf_;
    private final RecordFactory recordFactory_;

    private static final Logger logger_ =
        Logger.getLogger( CdfReader.class.getName() );

    public CdfReader( Buf buf ) {
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
        logger_.info( "Whole file compression: " + variant.compressed_ );

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

        // The lengths of some fields differ according to CDF version.
        // Get a record factory that does it right.
        recordFactory_ = new RecordFactory( variant.nameLeng_ );

        // Get the CDF Descriptor Record.  This may be the first record,
        // or it may be in a compressed form along with the rest of
        // the internal records.
        long offsetRec0 = ptr.get();
        final CdfDescriptorRecord cdr;
        if ( variant.compressed_ ) {
            CompressedCdfRecord ccr =
                recordFactory_.createRecord( buf, offsetRec0,
                                             CompressedCdfRecord.class );
            CompressedParametersRecord cpr =
                recordFactory_.createRecord( buf, ccr.cprOffset_,
                                             CompressedParametersRecord.class );
            Compression compress = Compression.getCompression( cpr.cType_ );
            buf = compress.uncompress( buf, ccr.getDataOffset(), ccr.uSize_ );
            cdr = recordFactory_
                 .createRecord( buf, 0, CdfDescriptorRecord.class );
        }
        else {
            cdr = recordFactory_.createRecord( buf, offsetRec0,
                                               CdfDescriptorRecord.class );
        }

        // Interrogate CDR for required information.
        boolean isSingleFile = Record.hasBit( cdr.flags_, 1 );
        if ( ! isSingleFile ) {
            throw new CdfFormatException( "Multi-file CDFs not supported" );
        }
        NumericEncoding encoding = NumericEncoding.getEncoding( cdr.encoding_ );
        Boolean bigEndian = encoding.isBigendian();
        if ( bigEndian == null ) {
            throw new CdfFormatException( "Unsupported encoding " + encoding );
        }
        buf.setEncoding( bigEndian.booleanValue() );
        cdr_ = cdr;
        buf_ = buf;
    }

    public CdfReader( File file ) throws IOException {
        this( NioBuf.createBuf( file, true ) );
    }

    public Buf getBuf() {
        return buf_;
    }

    public RecordFactory getRecordFactory() {
        return recordFactory_;
    }

    public CdfDescriptorRecord getCdr() {
        return cdr_;
    }

    public CdfContent readCdf() {
        CdfDescriptorRecord cdr = cdr_;
        Buf buf = buf_;

        // Get global descriptor record.
        GlobalDescriptorRecord gdr =
            recordFactory_.createRecord( buf, cdr.gdrOffset_,
                                         GlobalDescriptorRecord.class );

        // Store global format information.
        boolean rowMajor = Record.hasBit( cdr.flags_, 0 );
        int[] rDimSizes = gdr.rDimSizes_;
        CdfInfo cdfInfo = new CdfInfo( rowMajor, rDimSizes );

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
            Object[] grEntries =
                walkEntryList( buf, adr.nGrEntries_, adr.agrEdrHead_,
                               adr.maxGrEntry_ + 1, cdfInfo );
            Object[] zEntries =
                walkEntryList( buf, adr.nZEntries_, adr.azEdrHead_,
                               adr.maxZEntry_ + 1, cdfInfo );
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

    private VariableDescriptorRecord[] walkVariableList( Buf buf, int nvar,
                                                         long head ) {
        VariableDescriptorRecord[] vdrs = new VariableDescriptorRecord[ nvar ];
        long off = head;
        for ( int iv = 0; iv < nvar; iv++ ) {
            VariableDescriptorRecord vdr =
                recordFactory_.createRecord( buf, off,
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
                recordFactory_.createRecord( buf, off,
                                             AttributeDescriptorRecord.class );
            adrs[ ia ] = adr;
            off = adr.adrNext_;
        }
        return adrs;
    }

    private Object[] walkEntryList( Buf buf, int nent, long head, int maxent,
                                    CdfInfo info ) {
        Object[] entries = new Object[ maxent ];
        long off = head;
        for ( int ie = 0; ie < nent; ie++ ) {
            AttributeEntryDescriptorRecord aedr =
                recordFactory_
               .createRecord( buf, off, AttributeEntryDescriptorRecord.class );
            entries[ aedr.num_ ] = getEntryValue( aedr, info );
            off = aedr.aedrNext_;
        }
        return entries;
    }

    private Object getEntryValue( AttributeEntryDescriptorRecord aedr,
                                  CdfInfo info ) {
        DataType dataType = DataType.getDataType( aedr.dataType_ );
        int numElems = aedr.numElems_;
        final DataReader dataReader = new DataReader( dataType, numElems, 1 );
        Shaper shaper = Shaper.createShaper( dataType, new int[ 0 ],
                                             new boolean[ 0 ], true );
        Object va = dataReader.createValueArray();
        dataReader.readValue( aedr.getBuf(), aedr.getValueOffset(), va );
        return shaper.shape( va, true );
    }

    private Variable createVariable( VariableDescriptorRecord vdr,
                                     CdfInfo info ) {
        return new VdrVariable( vdr, info, recordFactory_ );
    }

    private GlobalAttribute
            createGlobalAttribute( AttributeDescriptorRecord adr,
                                   Object[] grEntries, Object[] zEntries ) {
        final Object[] entries = arrayConcat( grEntries, zEntries );
        final String name = adr.name_;
        return new GlobalAttribute() {
            public String getName() {
                return name;
            }
            public Object[] getEntries() {
                return entries;
            }
        };
    }

    private VariableAttribute
            createVariableAttribute( AttributeDescriptorRecord adr,
                                     final Object[] grEntries,
                                     final Object[] zEntries ) {
        final String name = adr.name_;
        return new VariableAttribute() {
            public String getName() {
                return name;
            }
            public Object getEntry( Variable variable ) {
                Object[] entries = variable.isZVariable() ? zEntries
                                                          : grEntries;
                int ix = variable.getNum();
                return ix < entries.length ? entries[ ix ] : null;
            }
        };
    }

    public static boolean isMagic( byte[] intro ) {
        if ( intro.length < 8 ) {
            return false;
        }
        return decodeMagic( readInt( intro, 0 ), readInt( intro, 4 ) ) != null;
    }

    private static int readInt( byte[] b, int ioff ) {
        return ( b[ ioff++ ] & 0xff ) << 24
             | ( b[ ioff++ ] & 0xff ) << 16
             | ( b[ ioff++ ] & 0xff ) <<  8
             | ( b[ ioff++ ] & 0xff ) <<  0;
    }

    private static CdfVariant decodeMagic( int magic1, int magic2 ) {
        final String label;
        final boolean longOffsets;
        final int nameLeng;
        final boolean compressed;
        if ( magic1 == 0xcdf30001 ) {
            label = "V3";
            longOffsets = true;
            nameLeng = 256;
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
            nameLeng = 64;
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
            nameLeng = 64; // ??
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
        return new CdfVariant( label, longOffsets, nameLeng, compressed );
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
        final int nameLeng_;
        final boolean compressed_;
        CdfVariant( String label, boolean longOffsets, int nameLeng,
                    boolean compressed ) {
            label_ = label;
            longOffsets_ = longOffsets;
            nameLeng_ = nameLeng;
            compressed_ = compressed;
        }
    }
}
