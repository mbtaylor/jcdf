package cdf;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class VdrVariable implements Variable {

    private final VariableDescriptorRecord vdr_;
    private final Buf buf_;
    private final RecordFactory recFact_;
    private final boolean isZVariable_;
    private final Shaper shaper_;
    private final int rvaleng_;
    private final DataType dataType_;
    private final DataReader dataReader_;
    private final Object padRawValueArray_;
    private final Object shapedPadValueRowMajor_;
    private final Object shapedPadValueColumnMajor_;
    private final String summaryTxt_;
    private RecordReader recordReader_;

    public VdrVariable( VariableDescriptorRecord vdr, CdfInfo info,
                        RecordFactory recFact ) {
        vdr_ = vdr;
        buf_ = vdr.getBuf();
        recFact_ = recFact;
        isZVariable_ = vdr.getRecordType() == 8;
        dataType_ = DataType.getDataType( vdr.dataType_ );
        int[] dimSizes = isZVariable_ ? vdr.zDimSizes_ : info.getRDimSizes();
        boolean[] dimVarys = vdr.dimVarys_;
        boolean rowMajor = info.getRowMajor();
        int numElems = vdr.numElems_;
        shaper_ =
            Shaper.createShaper( dataType_, dimSizes, dimVarys, rowMajor );
        dataReader_ =
            new DataReader( dataType_, numElems, shaper_.getRawItemCount() );
        rvaleng_ = Array.getLength( dataReader_.createValueArray() );
        long padOffset = vdr.getPadOffset();
        String shapeTxt = "";
        String varyTxt = "";
        for ( int idim = 0; idim < dimSizes.length; idim++ ) {
            if ( idim > 0 ) {
                shapeTxt += ',';
            }
            shapeTxt += dimSizes[ idim ];
            varyTxt += dimVarys[ idim ] ? 'T' : 'F';
        }
        if ( padOffset >= 0 ) {
            Object rva = dataReader_.createValueArray();
            dataReader_.readValue( buf_, padOffset, rva );
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
            .append( Record.hasBit( vdr_.flags_, 0 ) ? 'T' : 'F' )
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

    public DataReader getDataReader() {
        return dataReader_;
    }

    public Shaper getShaper() {
        return shaper_;
    }

    public String getSummary() {
        return summaryTxt_;
    }

    public Object createRawValueArray() {
        return dataReader_.createValueArray();
    }

    public boolean hasRecord( int irec ) {
        return getRecordReader().hasRecord( irec );
    }

    public void readRawRecord( int irec, Object rawValueArray ) {
         getRecordReader().readRawRecord( irec, rawValueArray );
    }

    public Object readShapedRecord( int irec, boolean rowMajor,
                                    Object rawValueArrayWorkspace ) {
         return getRecordReader()
               .readShapedRecord( irec, rowMajor, rawValueArrayWorkspace );
    }

    private RecordReader getRecordReader() {
        if ( recordReader_ == null ) {
            recordReader_ = createRecordReader();
        }
        return recordReader_;
    }

    private RecordReader createRecordReader() {
        RecordMap recMap =
            RecordMap.createRecordMap( vdr_, recFact_,
                                       dataReader_.getRecordSize() );
        boolean recVary = Record.hasBit( vdr_.flags_, 0 );
        if ( ! recVary ) {
            return new NoVaryRecordReader( recMap );
        }
        else {
            // Get sparse records type.  This is missing from the CDF Internal
            // Format Description document, but cdf.h says:
            //    #define NO_SPARSERECORDS                0L
            //    #define PAD_SPARSERECORDS               1L
            //    #define PREV_SPARSERECORDS              2L
            int sRecords = vdr_.sRecords_;
            return sRecords == 1 ? new PreviousRecordReader( recMap )
                                 : new PadRecordReader( recMap );
        }
    }

    private interface RecordReader {
        boolean hasRecord( int irec );
        void readRawRecord( int irec, Object rawValueArray );
        Object readShapedRecord( int irec, boolean rowMajor,
                                 Object rawValueArrayWorkspace );
    }

    private class NoVaryRecordReader implements RecordReader {
        private final Object rawValue_;
        private final Object rowMajorValue_;
        private final Object colMajorValue_;
        NoVaryRecordReader( RecordMap recMap ) {
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

    private class PadRecordReader implements RecordReader {
        private final RecordMap recMap_;
        PadRecordReader( RecordMap recMap ) {
            recMap_ = recMap;
        }
        public boolean hasRecord( int irec ) {
            return recMap_.getEntryIndex( irec ) >= 0;
        }
        public void readRawRecord( int irec, Object rawValueArray ) {
            int ient = recMap_.getEntryIndex( irec );
            if ( ient >= 0 ) {
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
                                        Object work ) {
            int ient = recMap_.getEntryIndex( irec );
            if ( ient >= 0 ) {
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
    }

    private class PreviousRecordReader implements RecordReader {
        private final RecordMap recMap_;
        PreviousRecordReader( RecordMap recMap ) {
            recMap_ = recMap;
        }
        public boolean hasRecord( int irec ) {
            return recMap_.getEntryIndex( irec ) >= 0;
        }
        public void readRawRecord( int irec, Object rawValueArray ) {
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
                                        Object work ) {
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
