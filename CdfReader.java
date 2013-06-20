package cdf;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

/**
 * Examines a CDF file and provides methods to access its contents.
 *
 * <p>Constructing an instance of this class reads enough of a file
 * to identify it as a CDF and work out how to access its records.
 * Most of the actual contents are only read from the data buffer
 * as required.
 * Although only the magic numbers and CDR are read during construction,
 * in the case of a file-compressed CDF the whole thing is uncompressed,
 * so it may still be an expensive operation.
 *
 * <p>Having constructed a CdfReader, there are two ways to make use of it,
 * low-level access to the internal records, or high-level access to the
 * attributes and variables.
 *
 * <p>For low-level access to the CDF internal records, use the
 * {@link #getCdr} method to get the CdfDescriptorRecord and use that
 * in conjunction with knowledge of the internal format of CDF files
 * as a starting point to chase pointers around the file constructing
 * other records.  When you have a pointer to another record, you can
 * use the record factory got from {@link #getRecordFactory} to turn
 * it into a typed Record object.
 *
 * <p>For high-level access to the CDF data and metadata,
 * use the {@link #readContent} method, which returns an object from
 * which the attributes and variables that constitute a CDF file
 * can be obtained.
 *
 * @author   Mark Taylor
 * @since    19 Jun 2013
 */
public class CdfReader {

    private final CdfDescriptorRecord cdr_;
    private final Buf buf_;
    private final RecordFactory recordFactory_;

    private static final Logger logger_ =
        Logger.getLogger( CdfReader.class.getName() );

    /** 
     * Constructs a CdfReader from a buffer containing its byte data.
     *
     * @param   buf  buffer containing CDF file
     */
    public CdfReader( Buf buf ) throws IOException {
        Pointer ptr = new Pointer( 0 );

        // Read the CDF magic number bytes.
        int magic1 = buf.readInt( ptr );
        int magic2 = buf.readInt( ptr );
        int offsetRec0 = (int) ptr.get();

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
        logger_.config( "CDF magic number for " + variant.label_ );
        logger_.config( "Whole file compression: " + variant.compressed_ );

        // The length of the pointers and sizes used in CDF files are
        // dependent on the CDF file format version.
        // Notify the buffer which regime is in force for this file.
        // Note that no operations for which this makes a difference have
        // yet taken place.
        buf.setBit64( variant.bit64_ );

        // The lengths of some fields differ according to CDF version.
        // Construct a record factory that does it right.
        recordFactory_ = new RecordFactory( variant.nameLeng_ );

        // Read the CDF Descriptor Record.  This may be the first record,
        // or it may be in a compressed form along with the rest of
        // the internal records.
        if ( variant.compressed_ ) {

            // Work out compression type and location of compressed data.
            CompressedCdfRecord ccr =
                recordFactory_.createRecord( buf, offsetRec0,
                                             CompressedCdfRecord.class );
            CompressedParametersRecord cpr =
                recordFactory_.createRecord( buf, ccr.cprOffset_,
                                             CompressedParametersRecord.class );
            final Compression compress =
                Compression.getCompression( cpr.cType_ );

            // Uncompress the compressed data into a new buffer.
            // The compressed data is the data record of the CCR.
            // When uncompressed it can be treated just like the whole of
            // an uncompressed CDF file, except that it doesn't have the
            // magic numbers (8 bytes) prepended to it.
            // Note however that any file offsets recorded within the file
            // are given as if the magic numbers are present - this is not
            // very clear from the Internal Format Description document,
            // but it appears to be the case from reverse engineering
            // whole-file compressed files.  To work round this, we hack
            // the compression to prepend a dummy 8-byte block to the
            // uncompressed stream it provides.
            final int prepad = offsetRec0;
            assert prepad == 8;
            Compression padCompress =
                    new Compression( "Padded " + compress.getName() ) {
                public InputStream uncompressStream( InputStream in )
                        throws IOException {
                    InputStream in1 =
                        new ByteArrayInputStream( new byte[ prepad ] );
                    InputStream in2 = compress.uncompressStream( in );
                    return new SequenceInputStream( in1, in2 );
                }
            };
            buf = Bufs.uncompress( padCompress, buf, ccr.getDataOffset(),
                                   ccr.uSize_ + prepad );
        }
        cdr_ = recordFactory_.createRecord( buf, offsetRec0,
                                            CdfDescriptorRecord.class );

        // Interrogate CDR for required information.
        boolean isSingleFile = Record.hasBit( cdr_.flags_, 1 );
        if ( ! isSingleFile ) {
            throw new CdfFormatException( "Multi-file CDFs not supported" );
        }
        NumericEncoding encoding =
            NumericEncoding.getEncoding( cdr_.encoding_ );
        Boolean bigEndian = encoding.isBigendian();
        if ( bigEndian == null ) {
            throw new CdfFormatException( "Unsupported encoding " + encoding );
        }
        buf.setEncoding( bigEndian.booleanValue() );
        buf_ = buf;
    }

    /**
     * Constructs a CdfReader from a readable file containig its byte data.
     *
     * @param  file  CDF file
     */
    public CdfReader( File file ) throws IOException {
        this( Bufs.createBuf( file, true, true ) );
    }

    /**
     * Returns the buffer containing the uncompressed record stream for
     * this reader's CDF file.
     * This will be the buffer originally submitted at construction time
     * only if the CDF does not use whole-file compression.
     *
     * @return   buffer containing CDF records
     */
    public Buf getBuf() {
        return buf_;
    }

    /** 
     * Returns a RecordFactory that can be applied to this reader's Buf 
     * to construct CDF Record objects.
     *
     * @return  record factory
     */
    public RecordFactory getRecordFactory() {
        return recordFactory_;
    }

    /**
     * Returns the CDF Descriptor Record object for this reader's CDF.
     *
     * @return  CDF Descriptor Record
     */
    public CdfDescriptorRecord getCdr() {
        return cdr_;
    }

    /** 
     * Returns an object which provides a high-level read-only
     * representation of the data and metadata contained in this reader's CDF.
     *
     * <p>This method reads attribute metadata and entries
     * and variable metadata.
     * Record data for variables is not read except by the relevant calls
     * on {@link Variable} objects.
     *
     * @return    CDF content object
     */
    public CdfContent readContent() throws IOException {
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

        // Collect the rVariables and zVariables into a single list.
        // Turn the rVariable and zVariable records into a single list of
        // Variable objects.
        VariableDescriptorRecord[] vdrs = arrayConcat( rvdrs, zvdrs );
        final Variable[] variables = new Variable[ vdrs.length ];
        for ( int iv = 0; iv < vdrs.length; iv++ ) {
            variables[ iv ] = createVariable( vdrs[ iv ], cdfInfo );
        }

        // Read the attributes records (global and variable attributes
        // are found in the same list).
        AttributeDescriptorRecord[] adrs =
            walkAttributeList( buf, gdr.numAttr_, gdr.adrHead_ );

        // Read the entries for all the attributes, and turn the records
        // with their entries into two lists, one of global attributes and
        // one of variable attributes.
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
                // grEntries are gEntries
                globalAtts.add( createGlobalAttribute( adr, grEntries,
                                                       zEntries ) );
            }
            else {
                // grEntries are rEntries
                varAtts.add( createVariableAttribute( adr, grEntries,
                                                      zEntries ) );
            }
        }
        final GlobalAttribute[] gAtts =
            globalAtts.toArray( new GlobalAttribute[ 0 ] );
        final VariableAttribute[] vAtts =
            varAtts.toArray( new VariableAttribute[ 0 ] );

        // Return the result as a single object containing more or less
        // everything there is to know about the data and metadata
        // contained in the CDF.
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

    /**
     * Follows a linked list of Variable Descriptor Records
     * and returns an array of them.
     *
     * @param  data buffer
     * @param  nvar  number of VDRs in list
     * @param  head  offset into buffer of first VDR
     * @return  list of VDRs
     */
    private VariableDescriptorRecord[] walkVariableList( Buf buf, int nvar,
                                                         long head )
            throws IOException {
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

    /**
     * Follows a linked list of Attribute Descriptor Records
     * and returns an array of them.
     *
     * @param  buf  data buffer
     * @param  natt  number of ADRs in list
     * @param  head  offset into buffer of first ADR
     * @return  list of ADRs
     */
    private AttributeDescriptorRecord[] walkAttributeList( Buf buf, int natt,
                                                           long head )
            throws IOException {
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

    /**
     * Follows a linked list of Attribute Entry Descriptor Records
     * and returns an array of entry values.
     *
     * @param   buf  data buffer
     * @param   head   offset into buffer of first AEDR
     * @param   maxent  upper limit of entry count
     * @param   info   global information about the CDF file
     * @return  entry values
     */
    private Object[] walkEntryList( Buf buf, int nent, long head, int maxent,
                                    CdfInfo info )
            throws IOException {
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

    /**
     * Obtains the value of an entry from an Atribute Entry Descriptor Record.
     *
     * @param  aedr  attribute entry descriptor record
     * @param  info  global information about the CDF file
     * @return   entry value
     */
    private Object getEntryValue( AttributeEntryDescriptorRecord aedr,
                                  CdfInfo info ) throws IOException {
        DataType dataType = DataType.getDataType( aedr.dataType_ );
        int numElems = aedr.numElems_;
        final DataReader dataReader = new DataReader( dataType, numElems, 1 );
        Object va = dataReader.createValueArray();
        dataReader.readValue( aedr.getBuf(), aedr.getValueOffset(), va );

        // Majority is not important since this is a scalar value.
        // The purpose of using the shaper is just to turn the array
        // element into a (probably Number or String) Object.
        Shaper shaper = Shaper.createShaper( dataType, new int[ 0 ],
                                             new boolean[ 0 ], true );
        return shaper.shape( va, true );
    }

    /**
     * Turns a Variable Descriptor Record into a Variable.
     *
     * @param   vdr  record
     * @param   info  global CDF information
     * @return  variable object
     */
    private Variable createVariable( VariableDescriptorRecord vdr,
                                     CdfInfo info ) throws IOException {
        return new VdrVariable( vdr, info, recordFactory_ );
    }

    /**
     * Turns an Attribute Descriptor Record plus entries
     * into a Global Attribute object.
     * The g/rEntries and zEntries are collected together in a single list,
     * on the grounds that users are not likely to be much interested
     * in the difference.
     *
     * @param  adr  record
     * @param  gEntries  gEntry values for this attribute
     * @param  zEntries  zEntry values for this attribute
     * @return  global attribute object
     */
    private GlobalAttribute
            createGlobalAttribute( AttributeDescriptorRecord adr,
                                   Object[] gEntries, Object[] zEntries ) {
        final Object[] entries = arrayConcat( gEntries, zEntries );
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

    /**
     * Turns an Attribute Descriptor Record plus entries
     * into a Variable Atttribute object.
     *
     * @param   adr   record
     * @param   rEntries   rEntry values for this attribute
     * @param   zEntries   zEntry values for this attribute
     * @return  variable attribute object
     */
    private VariableAttribute
            createVariableAttribute( AttributeDescriptorRecord adr,
                                     final Object[] rEntries,
                                     final Object[] zEntries ) {
        final String name = adr.name_;
        return new VariableAttribute() {
            public String getName() {
                return name;
            }
            public Object getEntry( Variable variable ) {
                Object[] entries = variable.isZVariable() ? zEntries
                                                          : rEntries;
                int ix = variable.getNum();
                return ix < entries.length ? entries[ ix ] : null;
            }
        };
    }

    /**
     * Examines a byte array to see if it looks like the start of a CDF file.
     *
     * @param   intro  byte array, at least 8 bytes if available
     * @return  true iff the first 8 bytes of <code>intro</code> are
     *          a CDF magic number
     */
    public static boolean isMagic( byte[] intro ) {
        if ( intro.length < 8 ) {
            return false;
        }
        return decodeMagic( readInt( intro, 0 ), readInt( intro, 4 ) ) != null;
    }

    /**
     * Reads an 4-byte big-endian integer from a byte array.
     *
     * @param  b  byte array
     * @param  ioff   index into <code>b</code> of integer start
     * @return   int value
     */
    private static int readInt( byte[] b, int ioff ) {
        return ( b[ ioff++ ] & 0xff ) << 24
             | ( b[ ioff++ ] & 0xff ) << 16
             | ( b[ ioff++ ] & 0xff ) <<  8
             | ( b[ ioff++ ] & 0xff ) <<  0;
    }

    /**
     * Interprets two integer values as the magic number sequence at the
     * start of a CDF file, and returns an object encoding the information
     * about CDF encoding specifics.
     *
     * @param   magic1  big-endian int at CDF file offset 0x00
     * @param   magic2  big-endian int at CDF file offset 0x04
     * @return  object describing CDF encoding specifics,
     *          or null if this is not a recognised CDF magic number
     */
    private static CdfVariant decodeMagic( int magic1, int magic2 ) {
        final String label;
        final boolean bit64;
        final int nameLeng;
        final boolean compressed;
        if ( magic1 == 0xcdf30001 ) {  // version 3.0 - 3.4 (3.*?)
            label = "V3";
            bit64 = true;
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
            bit64 = false;
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
            bit64 = false;
            nameLeng = 64; // true as far as I know
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
        return new CdfVariant( label, bit64, nameLeng, compressed );
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

    /**
     * Encapsulates CDF encoding details as determined from the magic number.
     */
    private static class CdfVariant {
        final String label_;
        final boolean bit64_;
        final int nameLeng_;
        final boolean compressed_;

        /**
         * Constructor.
         *
         * @param  label  short string indicating CDF format version number
         * @param  bit64  true for 8-bit pointers, false for 4-bit pointers
         * @param  nameLeng  number of bytes used for attribute and variable
         *                   names
         * @param  compressed true iff the CDF file uses whole-file compression
         */
        CdfVariant( String label, boolean bit64, int nameLeng,
                    boolean compressed ) {
            label_ = label;
            bit64_ = bit64;
            nameLeng_ = nameLeng;
            compressed_ = compressed;
        }
    }
}
