package cdf;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class VdrVariable implements Variable {

    private final VariableDescriptorRecord vdr_;
    private final Buf buf_;
    private final boolean isZVariable_;
    private final DataReader dataReader_;
    private final int recSize_;
    private final Object rawPadValue_;
    private final Object shapedPadValueRowMajor_;
    private final Object shapedPadValueColumnMajor_;
    private RecordReader recordReader_;

    public VdrVariable( VariableDescriptorRecord vdr, CdfInfo info ) {
        vdr_ = vdr;
        buf_ = vdr.getBuf();
        isZVariable_ = vdr.getRecordType() == 8;
        int dataType = vdr.dataType_;
        int encoding = info.getEncoding();
        int[] dimSizes = isZVariable_ ? vdr.zDimSizes_ : info.getRDimSizes();
        boolean[] dimVarys = vdr.dimVarys_;
        boolean rowMajor = info.getRowMajor();
        int numElems = vdr.numElems_;
        dataReader_ =
            DataReaderFactory.createDataReader( dataType, encoding, dimSizes,
                                                dimVarys, rowMajor, numElems );
        recSize_ = dataReader_.getRecordSize();
        long padOffset = vdr.getPadOffset();
        if ( padOffset >= 0 ) {
            rawPadValue_ =
                dataReader_.readRawValue( buf_, padOffset );
            shapedPadValueRowMajor_ =
                dataReader_.readShapedValue( buf_, padOffset, true );
            shapedPadValueColumnMajor_ =
                dataReader_.readShapedValue( buf_, padOffset, false );
        }
        else {
            rawPadValue_ = null;
            shapedPadValueRowMajor_ = null;
            shapedPadValueColumnMajor_ = null;
        }
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

    public int getMaxRec() {
        return vdr_.maxRec_;
    }

    public DataReader getDataReader() {
        return dataReader_;
    }

    public boolean hasRecord( int irec ) {
        return getRecordReader().hasRecord( irec );
    }

    public Object readRawRecord( int irec ) {
        return getRecordReader().readRawRecord( irec );
    }

    public Object readShapedRecord( int irec, boolean rowMajor ) {
        return getRecordReader().readShapedRecord( irec, rowMajor );
    }

    private RecordReader getRecordReader() {
        if ( recordReader_ == null ) {
            recordReader_ = createRecordReader();
        }
        return recordReader_;
    }

    private RecordReader createRecordReader() {
        RecordMap recMap = RecordMap.createRecordMap( vdr_, recSize_ );
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
        Object readRawRecord( int irec );
        Object readShapedRecord( int irec, boolean rowMajor );
    }

    private class NoVaryRecordReader implements RecordReader {
        private final Object rawValue_;
        private final Object rowMajorValue_;
        private final Object colMajorValue_;
        NoVaryRecordReader( RecordMap recMap ) {
            RecordReader rt = new PadRecordReader( recMap );
            rawValue_ = rt.readRawRecord( 0 );
            rowMajorValue_ = rt.readShapedRecord( 0, true );
            colMajorValue_ = rt.readShapedRecord( 0, false );
        }
        public boolean hasRecord( int irec ) {
            return true;
        }
        public Object readRawRecord( int irec ) {
            return rawValue_;
        }
        public Object readShapedRecord( int irec, boolean rowMajor ) {
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
        public Object readRawRecord( int irec ) {
            int ient = recMap_.getEntryIndex( irec );
            return ient >= 0
                 ? dataReader_
                  .readRawValue( recMap_.getBuf( ient ),
                                 recMap_.getOffset( ient, irec ) )
                 : rawPadValue_;
        }
        public Object readShapedRecord( int irec, boolean rowMajor ) {
            int ient = recMap_.getEntryIndex( irec );
            return ient >= 0
                 ? dataReader_
                  .readShapedValue( recMap_.getBuf( ient ),
                                    recMap_.getOffset( ient, irec ),
                                    rowMajor )
                 : ( rowMajor ? shapedPadValueRowMajor_
                              : shapedPadValueColumnMajor_ );
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
        public Object readRawRecord( int irec ) {
            int ient = recMap_.getEntryIndex( irec );
            if ( ient >= 0 ) {
                return dataReader_
                      .readRawValue( recMap_.getBuf( ient ),
                                     recMap_.getOffset( ient, irec ) );
            }
            else if ( ient == -1 ) {
                return rawPadValue_;
            }
            else {
                int iPrevEnt = -ient - 2;
                long offset = recMap_.getFinalOffsetInEntry( iPrevEnt );
                return dataReader_
                      .readRawValue( recMap_.getBuf( iPrevEnt ), offset );
            }
        }
        public Object readShapedRecord( int irec, boolean rowMajor ) {
            int ient = recMap_.getEntryIndex( irec );
            if ( ient >= 0 ) {
                return dataReader_
                      .readShapedValue( recMap_.getBuf( ient ),
                                        recMap_.getOffset( ient, irec ),
                                        rowMajor );
            }
            else if ( ient == -1 ) {
                return rowMajor ? shapedPadValueRowMajor_
                                : shapedPadValueColumnMajor_;
            }
            else {
                int iPrevEnt = -ient - 2;
                long offset = recMap_.getFinalOffsetInEntry( iPrevEnt );
                return dataReader_
                      .readShapedValue( recMap_.getBuf( iPrevEnt ), offset,
                                        rowMajor );
            }
        }
    }
}
