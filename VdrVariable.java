package cdf;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Variable implementation based on a Variable Description Record.
 *
 * @author   Mark Taylor
 * @since    20 Jun 2013
 */
class VdrVariable implements Variable {

    private final VariableDescriptorRecord vdr_;
    private final Buf buf_;
    private final RecordFactory recFact_;
    private final boolean isZVariable_;
    private final boolean recordVariance_;
    private final Shaper shaper_;
    private final int rvaleng_;
    private final DataType dataType_;
    private final DataReader dataReader_;
    private final Object padRawValueArray_;
    private final Object shapedPadValueRowMajor_;
    private final Object shapedPadValueColumnMajor_;
    private final String summaryTxt_;
    private RecordReader recordReader_;

    /**
     * Constructor.
     *
     * @param   vdr   variable descriptor record for the variable
     * @param   cdfInfo  global CDF information
     * @param   recFact  record factory
     */
    public VdrVariable( VariableDescriptorRecord vdr, CdfInfo info,
                        RecordFactory recFact ) throws IOException {

        // Prepare state for reading data.
        vdr_ = vdr;
        buf_ = vdr.getBuf();
        recFact_ = recFact;
        isZVariable_ = vdr.getRecordType() == 8;
        dataType_ = DataType.getDataType( vdr.dataType_ );
        recordVariance_ = Record.hasBit( vdr_.flags_, 0 );
        int[] dimSizes = isZVariable_ ? vdr.zDimSizes_ : info.getRDimSizes();
        boolean[] dimVarys = vdr.dimVarys_;
        boolean rowMajor = info.getRowMajor();
        int numElems = vdr.numElems_;
        shaper_ =
            Shaper.createShaper( dataType_, dimSizes, dimVarys, rowMajor );
        int nraw = shaper_.getRawItemCount();
        dataReader_ = new DataReader( dataType_, numElems, nraw );
        rvaleng_ = Array.getLength( dataReader_.createValueArray() );

        // Read pad value if present.
        long padOffset = vdr.getPadValueOffset();
        if ( padOffset >= 0 ) {
            DataReader padReader = new DataReader( dataType_, numElems, 1 );
            assert vdr.getPadValueSize() == padReader.getRecordSize();
            Object padValueArray = padReader.createValueArray();
            padReader.readValue( buf_, padOffset, padValueArray );
            Object rva = dataReader_.createValueArray();
            for ( int i = 0; i < nraw; i++ ) {
                System.arraycopy( padValueArray, 0, rva, i, 1 );
            }
            padRawValueArray_ = rva;
            shapedPadValueRowMajor_ = shaper_.shape( padRawValueArray_, true );
            shapedPadValueColumnMajor_ =
                shaper_.shape( padRawValueArray_, false );
        }
        else {
            padRawValueArray_ = null;
            shapedPadValueRowMajor_ = null;
            shapedPadValueColumnMajor_ = null;
        }

        // Assemble a short summary string.
        String shapeTxt = "";
        String varyTxt = "";
        for ( int idim = 0; idim < dimSizes.length; idim++ ) {
            if ( idim > 0 ) {
                shapeTxt += ',';
            }
            shapeTxt += dimSizes[ idim ];
            varyTxt += dimVarys[ idim ] ? 'T' : 'F';
        }
        summaryTxt_ = new StringBuffer()
            .append( dataType_.getName() )
            .append( ' ' )
            .append( isZVariable_ ? "(z)" : "(r)" )
            .append( ' ' )
            .append( dimSizes.length )
            .append( ':' )
            .append( '[' )
            .append( shapeTxt )
            .append( ']' )
            .append( ' ' )
            .append( recordVariance_ ? 'T' : 'F' )
            .append( '/' )
            .append( varyTxt )
            .toString();
    }

    public String getName() {
        return vdr_.name_;
    }

    public int getNum() {
        return vdr_.num_;
    }

    public boolean isZVariable() {
        return isZVariable_;
    }

    public int getRecordCount() {
        return vdr_.maxRec_ + 1;
    }

    public DataType getDataType() {
        return dataType_;
    }

    public Shaper getShaper() {
        return shaper_;
    }

    public boolean getRecordVariance() {
        return recordVariance_;
    }

    public String getSummary() {
        return summaryTxt_;
    }

    public Object createRawValueArray() {
        return dataReader_.createValueArray();
    }

    public boolean hasRecord( int irec ) throws IOException {
        return getRecordReader().hasRecord( irec );
    }

    public void readRawRecord( int irec, Object rawValueArray )
             throws IOException {
         getRecordReader().readRawRecord( irec, rawValueArray );
    }

    public Object readShapedRecord( int irec, boolean rowMajor,
                                    Object rawValueArrayWorkspace )
             throws IOException {
         return getRecordReader()
               .readShapedRecord( irec, rowMajor, rawValueArrayWorkspace );
    }

    /**
     * Returns an object that can read records for this variable.
     * Constructing it requires reading maps of where the record values
     * are stored, which might in principle involve a bit of work,
     * so do it lazily.
     *
     * @return  record reader
     */
    private synchronized RecordReader getRecordReader() throws IOException {
        if ( recordReader_ == null ) {
            recordReader_ = createRecordReader();
        }
        return recordReader_;
    }

    /**
     * Constructs a record reader.
     *
     * @return  new record reader
     */
    private RecordReader createRecordReader() throws IOException {
        RecordMap recMap =
            RecordMap.createRecordMap( vdr_, recFact_,
                                       dataReader_.getRecordSize() );
        if ( ! recordVariance_ ) {
            return new NoVaryRecordReader( recMap );
        }
        else {
            // Get sparse records type.  This is missing from the CDF Internal
            // Format Description document, but cdf.h says:
            //    #define NO_SPARSERECORDS                0L
            //    #define PAD_SPARSERECORDS               1L
            //    #define PREV_SPARSERECORDS              2L
            int sRecords = vdr_.sRecords_;
            return sRecords == 2 ? new PreviousRecordReader( recMap )
                                 : new PadRecordReader( recMap );
        }
    }

    /**
     * Object which can read record values for this variable.
     * This provides the implementations of several of the Variable methods.
     */
    private interface RecordReader {

        /**
         * Indicates whether a real file-based record exists for the given
         * record index.
         *
         * @param  irec  record index
         * @return  true iff a file-based record exists for irec
         */
        boolean hasRecord( int irec );

        /**
         * Reads the data from a single record into a supplied raw value array.
         *
         * @param  irec  record index
         * @param  rawValueArray  workspace array
         */
        void readRawRecord( int irec, Object rawValueArray )
            throws IOException;

        /**
         * Reads the data from a single record and returns it as an object
         * of a suitable type for this variable.
         *
         * @param  irec  record index
         * @param  rowMajor  required majority of output array
         * @param  rawValueArrayWorkspace  workspace array
         * @return   a new object containing shaped result
         */
        Object readShapedRecord( int irec, boolean rowMajor,
                                 Object rawValueArrayWorkspace )
            throws IOException;
    }

    /**
     * RecordReader implementation for non-record-varying variables.
     */
    private class NoVaryRecordReader implements RecordReader {
        private final Object rawValue_;
        private final Object rowMajorValue_;
        private final Object colMajorValue_;

        /**
         * Constructor.
         *
         * @param   recMap  record map
         */
        NoVaryRecordReader( RecordMap recMap ) throws IOException {

            // When record variance is false, the fixed value appears
            // to be located where you woul otherwise expect to find record #0.
            // Read it once and store it in raw, row-major and column-major
            // versions for later use.
            RecordReader rt = new PadRecordReader( recMap );
            rawValue_ = createRawValueArray();
            rt.readRawRecord( 0, rawValue_ );
            rowMajorValue_ = shaper_.shape( rawValue_, true );
            colMajorValue_ = shaper_.shape( rawValue_, false );
        }
        public boolean hasRecord( int irec ) {
            return false;
        }
        public void readRawRecord( int irec, Object rawValueArray ) {
            System.arraycopy( rawValue_, 0, rawValueArray, 0, rvaleng_ );
        }
        public Object readShapedRecord( int irec, boolean rowMajor,
                                        Object work ) {
            return rowMajor ? rowMajorValue_ : colMajorValue_;
        }
    }

    /**
     * RecordReader implementation for record-varying variables
     * with sparse padding or no padding.
     */
    private class PadRecordReader implements RecordReader {
        private final RecordMap recMap_;

        /**
         * Constructor.
         *
         * @param  recMap  record map
         */
        PadRecordReader( RecordMap recMap ) {
            recMap_ = recMap;
        }
        public boolean hasRecord( int irec ) {
            return hasRecord( irec, recMap_.getEntryIndex( irec ) );
        }
        public void readRawRecord( int irec, Object rawValueArray )
                throws IOException {
            int ient = recMap_.getEntryIndex( irec );
            if ( hasRecord( irec, ient ) ) {
                dataReader_.readValue( recMap_.getBuf( ient ),
                                       recMap_.getOffset( ient, irec ),
                                       rawValueArray );
            }
            else {
                System.arraycopy( padRawValueArray_, 0, rawValueArray, 0,
                                  rvaleng_ );
            }
        }
        public Object readShapedRecord( int irec, boolean rowMajor,
                                        Object work )
                throws IOException {
            int ient = recMap_.getEntryIndex( irec );
            if ( hasRecord( irec, ient ) ) {
                dataReader_.readValue( recMap_.getBuf( ient ),
                                       recMap_.getOffset( ient, irec ),
                                       work );
                return shaper_.shape( work, rowMajor );
            }
            else {
                return rowMajor ? shapedPadValueRowMajor_
                                : shapedPadValueColumnMajor_;
            }
        }
        private boolean hasRecord( int irec, int ient ) {
            return ient >= 0 && ient < recMap_.getEntryCount()
                && irec < getRecordCount();
        }
    }

    /**
     * RecordReader implementation for record-varying variables
     * with previous padding.
     */
    private class PreviousRecordReader implements RecordReader {
        private final RecordMap recMap_;

        /**
         * Constructor.
         *
         * @param  recMap  record map
         */
        PreviousRecordReader( RecordMap recMap ) {
            recMap_ = recMap;
        }
        public boolean hasRecord( int irec ) {
            // I'm not sure whether the constraint on getRecordCount ought
            // to be applied here - maybe for previous padding, non-existent
            // records are OK??
            return recMap_.getEntryIndex( irec ) >= 0
                && irec < getRecordCount();
        }
        public void readRawRecord( int irec, Object rawValueArray )
                throws IOException {
            int ient = recMap_.getEntryIndex( irec );
            if ( ient >= 0 ) {
                dataReader_.readValue( recMap_.getBuf( ient ),
                                       recMap_.getOffset( ient, irec ),
                                       rawValueArray );
            }
            else if ( ient == -1 ) {
                System.arraycopy( padRawValueArray_, 0, rawValueArray, 0,
                                  rvaleng_ );
            }
            else {
                int iPrevEnt = -ient - 2;
                long offset = recMap_.getFinalOffsetInEntry( iPrevEnt );
                dataReader_.readValue( recMap_.getBuf( iPrevEnt ), offset,
                                       rawValueArray );
            }
        }
        public Object readShapedRecord( int irec, boolean rowMajor,
                                        Object work )
                throws IOException {
            int ient = recMap_.getEntryIndex( irec );
            if ( ient >= 0 ) {
                dataReader_.readValue( recMap_.getBuf( ient ),
                                       recMap_.getOffset( ient, irec ),
                                       work );
                return shaper_.shape( work, rowMajor );
            }
            else if ( ient == -1 ) {
                return rowMajor ? shapedPadValueRowMajor_
                                : shapedPadValueColumnMajor_;
            }
            else {
                int iPrevEnt = -ient - 2;
                long offset = recMap_.getFinalOffsetInEntry( iPrevEnt );
                dataReader_.readValue( recMap_.getBuf( ient ),
                                       recMap_.getOffset( ient, irec ),
                                       work );
                return shaper_.shape( work, rowMajor );
            }
        }
    }
}
