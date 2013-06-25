package cdf;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Provides all the data and metadata in a CDF file in a high-level
 * read-only easy to use form.
 *
 * @author   Mark Taylor
 * @since    20 Jun 2013
 */
public class CdfContent {

    private final CdfInfo cdfInfo_;
    private final GlobalAttribute[] globalAtts_;
    private final VariableAttribute[] variableAtts_;
    private final Variable[] variables_;

    /**
     * Constructs a CdfContent from a CdfReader.
     * This reads the attribute metadata and entries and variable metadata.
     * Record data for variables is not read at construction time.
     *
     * @param  crdr  object which knows how to read CDF records
     */
    public CdfContent( CdfReader crdr ) throws IOException {

        // Get basic information from reader.
        Buf buf = crdr.getBuf();
        RecordFactory recordFact = crdr.getRecordFactory();
        CdfDescriptorRecord cdr = crdr.getCdr();

        // Get global descriptor record.
        GlobalDescriptorRecord gdr =
            recordFact.createRecord( buf, cdr.gdrOffset_,
                                     GlobalDescriptorRecord.class );

        // Store global format information.
        boolean rowMajor = Record.hasBit( cdr.flags_, 0 );
        int[] rDimSizes = gdr.rDimSizes_;
        cdfInfo_ = new CdfInfo( rowMajor, rDimSizes );

        // Read the rVariable and zVariable records.
        VariableDescriptorRecord[] rvdrs =
            walkVariableList( buf, recordFact, gdr.nrVars_, gdr.rVdrHead_ );
        VariableDescriptorRecord[] zvdrs =
            walkVariableList( buf, recordFact, gdr.nzVars_, gdr.zVdrHead_ );

        // Collect the rVariables and zVariables into a single list.
        // Turn the rVariable and zVariable records into a single list of
        // Variable objects.
        VariableDescriptorRecord[] vdrs = arrayConcat( rvdrs, zvdrs );
        variables_ = new Variable[ vdrs.length ];
        for ( int iv = 0; iv < vdrs.length; iv++ ) {
            variables_[ iv ] = new Variable( vdrs[ iv ], cdfInfo_, recordFact );
        }

        // Read the attributes records (global and variable attributes
        // are found in the same list).
        AttributeDescriptorRecord[] adrs =
            walkAttributeList( buf, recordFact, gdr.numAttr_, gdr.adrHead_ );

        // Read the entries for all the attributes, and turn the records
        // with their entries into two lists, one of global attributes and
        // one of variable attributes.
        List<GlobalAttribute> gAttList = new ArrayList<GlobalAttribute>();
        List<VariableAttribute> vAttList = new ArrayList<VariableAttribute>();
        for ( int ia = 0; ia < adrs.length; ia++ ) {
            AttributeDescriptorRecord adr = adrs[ ia ];
            Object[] grEntries =
                walkEntryList( buf, recordFact,
                               adr.nGrEntries_, adr.maxGrEntry_,
                               adr.agrEdrHead_, cdfInfo_ );
            Object[] zEntries =
                walkEntryList( buf, recordFact,
                               adr.nZEntries_, adr.maxZEntry_,
                               adr.azEdrHead_, cdfInfo_ );
            boolean isGlobal = Record.hasBit( adr.scope_, 0 );
            if ( isGlobal ) {
                // grEntries are gEntries
                Object[] gEntries = arrayConcat( grEntries, zEntries );
                gAttList.add( new GlobalAttribute( adr.name_, gEntries ) );
            }
            else {
                // grEntries are rEntries
                vAttList.add( new VariableAttribute( adr.name_, grEntries,
                                                     zEntries ) );
            }
        }
        globalAtts_ = gAttList.toArray( new GlobalAttribute[ 0 ] );
        variableAtts_ = vAttList.toArray( new VariableAttribute[ 0 ] );
    }

    /**
     * Returns the global attributes.
     *
     * @return  global attribute array, in order
     */
    public GlobalAttribute[] getGlobalAttributes() {
        return globalAtts_;
    }

    /**
     * Returns the variable attributes.
     *
     * @return   variable attribute array, in order
     */
    public VariableAttribute[] getVariableAttributes() {
        return variableAtts_;
    }

    /**
     * Returns the variables.
     *
     * @return  variable array, in order
     */
    public Variable[] getVariables() {
        return variables_;
    }

    /**
     * Returns some global information about the CDF file.
     *
     * @return  CDF info
     */
    public CdfInfo getCdfInfo() {
        return cdfInfo_;
    }

    /**
     * Follows a linked list of Variable Descriptor Records
     * and returns an array of them.
     *
     * @param  buf   data buffer
     * @param  recordFact  record factory
     * @param  nvar  number of VDRs in list
     * @param  head  offset into buffer of first VDR
     * @return  list of VDRs
     */
    private static VariableDescriptorRecord[]
            walkVariableList( Buf buf, RecordFactory recordFact,
                              int nvar, long head ) throws IOException {
        VariableDescriptorRecord[] vdrs = new VariableDescriptorRecord[ nvar ];
        long off = head;
        for ( int iv = 0; iv < nvar; iv++ ) {
            VariableDescriptorRecord vdr =
                recordFact.createRecord( buf, off,
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
     * @param  recordFact  record factory
     * @param  natt  number of ADRs in list
     * @param  head  offset into buffer of first ADR
     * @return  list of ADRs
     */
    private static AttributeDescriptorRecord[]
            walkAttributeList( Buf buf, RecordFactory recordFact,
                               int natt, long head ) throws IOException {
        AttributeDescriptorRecord[] adrs =
            new AttributeDescriptorRecord[ natt ];
        long off = head;
        for ( int ia = 0; ia < natt; ia++ ) {
            AttributeDescriptorRecord adr =
                recordFact.createRecord( buf, off,
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
     * @param   recordFact  record factory
     * @param   nent  number of entries
     * @param   maxient  largest entry index (AEDR num field value)
     * @param   head   offset into buffer of first AEDR
     * @param   info   global information about the CDF file
     * @return  entry values
     */
    private static Object[] walkEntryList( Buf buf, RecordFactory recordFact,
                                           int nent, int maxient,
                                           long head, CdfInfo info )
            throws IOException {
        Object[] entries = new Object[ maxient + 1 ];
        long off = head;
        for ( int ie = 0; ie < nent; ie++ ) {
            AttributeEntryDescriptorRecord aedr =
                recordFact.createRecord( buf, off,
                                         AttributeEntryDescriptorRecord.class );
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
    private static Object getEntryValue( AttributeEntryDescriptorRecord aedr,
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
}
