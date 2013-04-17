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
    private final int compressType_;
    private RecordTable recordTable_;

    public VdrVariable( VariableDescriptorRecord vdr, CdfInfo info ) {
        vdr_ = vdr;
        buf_ = vdr.getBuf();
        isZVariable_ = vdr.getRecordType() == 8;
        int dataType = vdr.dataType_;
        int encoding = info.getEncoding();
        int[] dimSizes = isZVariable_ ? vdr.zDimSizes_ : info.getRDimSizes();
        int[] dimVarys = vdr.dimVarys_;
        boolean rowMajor = info.getRowMajor();
        int numElems = vdr.numElems_;
        dataReader_ =
            DataReaderFactory.createDataReader( dataType, encoding, dimSizes,
                                                dimVarys, rowMajor, numElems );
        recSize_ = dataReader_.getRecordSize();
        long padOffset = vdr.getPadOffset();
        if ( padOffset >= 0 ) {
            rawPadValue_ =
                dataReader_.readRawValue( buf_, new Pointer( padOffset ) );
            shapedPadValueRowMajor_ =
                dataReader_.readShapedValue( buf_, new Pointer( padOffset ),
                                             true );
            shapedPadValueColumnMajor_ =
                dataReader_.readShapedValue( buf_, new Pointer( padOffset ),
                                             false );
        }
        else {
            rawPadValue_ = null;
            shapedPadValueRowMajor_ = null;
            shapedPadValueColumnMajor_ = null;
        }

        // Get compression type.  This is missing from the CDF Internal
        // Format Description document, but cdf.h says:
        //    #define NO_COMPRESSION                  0L
        //    #define RLE_COMPRESSION                 1L
        //    #define HUFF_COMPRESSION                2L
        //    #define AHUFF_COMPRESSION               3L
        //    #define GZIP_COMPRESSION                5L
        boolean compress = Record.hasBit( vdr_.flags_, 2 );
        if ( compress && vdr_.cprOrSprOffset_ != -1 ) {
            CompressedParametersRecord cpr =
                RecordFactory.createRecord( vdr_.getBuf(), vdr_.cprOrSprOffset_,
                                            CompressedParametersRecord.class );
            compressType_ = cpr.cType_;
        }
        else {
            compressType_ = 0;
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
        return getRecordTable().hasRecord( irec );
    }

    public Object readRawRecord( int irec ) {
        return getRecordTable().readRawRecord( irec );
    }

    public Object readShapedRecord( int irec, boolean rowMajor ) {
        return getRecordTable().readShapedRecord( irec, rowMajor );
    }

    private RecordTable getRecordTable() {
        if ( recordTable_ == null ) {
            recordTable_ = createRecordTable();
        }
        return recordTable_;
    }

    private RecordTable createRecordTable() {
        boolean recVary = Record.hasBit( vdr_.flags_, 0 );
        boolean hasPad = Record.hasBit( vdr_.flags_, 1 );

        // Get sparse records type.  This is missing from the CDF Internal
        // Format Description document, but cdf.h says:
        //    #define NO_SPARSERECORDS                0L
        //    #define PAD_SPARSERECORDS               1L
        //    #define PREV_SPARSERECORDS              2L
        int sRecords = vdr_.sRecords_;
        if ( ! recVary ) {
            return new NoVaryRecordTable();
        }
        else if ( sRecords == 1 ) {
            return new PreviousRecordTable();
        }
        else {
            return new PadRecordTable();
        }
    }

    private interface RecordTable {
        boolean hasRecord( int irec );
        Object readRawRecord( int irec );
        Object readShapedRecord( int irec, boolean rowMajor );
    }

    private class NoVaryRecordTable implements RecordTable {
        private final Object rawValue_;
        private final Object rowMajorValue_;
        private final Object colMajorValue_;
        NoVaryRecordTable() {
            RecordTable rt = new PadRecordTable();
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

    private class PadRecordTable implements RecordTable {
        private final IndexTable indexTable_;
        PadRecordTable() {
            indexTable_ = createIndexTable();
        }
        public boolean hasRecord( int irec ) {
            return indexTable_.getEntryIndex( irec ) >= 0;
        }
        public Object readRawRecord( int irec ) {
            int ient = indexTable_.getEntryIndex( irec );
            return ient >= 0
                 ? dataReader_
                  .readRawValue( vdr_.getBuf(),
                                 new Pointer( indexTable_
                                             .getOffset( ient, irec ) ) )
                 : rawPadValue_;
        }
        public Object readShapedRecord( int irec, boolean rowMajor ) {
            int ient = indexTable_.getEntryIndex( irec );
            return ient >= 0
                 ? dataReader_
                  .readShapedValue( vdr_.getBuf(),
                                    new Pointer( indexTable_
                                                .getOffset( ient, irec ) ),
                                    rowMajor )
                 : ( rowMajor ? shapedPadValueRowMajor_
                              : shapedPadValueColumnMajor_ );
        }
    }

    private class PreviousRecordTable implements RecordTable {
        private final IndexTable indexTable_;
        PreviousRecordTable() {
            indexTable_ = createIndexTable();
        }
        public boolean hasRecord( int irec ) {
            return indexTable_.getEntryIndex( irec ) >= 0;
        }
        public Object readRawRecord( int irec ) {
            int ient = indexTable_.getEntryIndex( irec );
            if ( ient >= 0 ) {
                return dataReader_
                      .readRawValue( vdr_.getBuf(),
                                     new Pointer( indexTable_
                                                 .getOffset( ient, irec ) ) );
            }
            else if ( ient == -1 ) {
                return rawPadValue_;
            }
            else {
                int iPrevEnt = -ient - 2;
                return dataReader_
                      .readRawValue( vdr_.getBuf(),
                                     new Pointer( indexTable_
                                                 .getLastOffset( ient ) ) );
            }
        }
        public Object readShapedRecord( int irec, boolean rowMajor ) {
            int ient = indexTable_.getEntryIndex( irec );
            if ( ient >= 0 ) {
                return dataReader_
                      .readShapedValue( vdr_.getBuf(),
                                        new Pointer( indexTable_
                                                    .getOffset( ient, irec ) ),
                                        rowMajor );
            }
            else if ( ient == -1 ) {
                return rowMajor ? shapedPadValueRowMajor_
                                : shapedPadValueColumnMajor_;
            }
            else {
                int iPrevEnt = -ient - 2;
                return dataReader_
                      .readShapedValue( vdr_.getBuf(),
                                        new Pointer( indexTable_
                                                    .getLastOffset( ient ) ),
                                        rowMajor );
            }
        }
    }

    private IndexTable createIndexTable() {
        Buf buf = vdr_.getBuf();
        List<IndexEntry> entryList = new ArrayList<IndexEntry>();
        for ( long vxrOffset = vdr_.vxrHead_; vxrOffset != 0; ) {
            VariableIndexRecord vxr = 
                RecordFactory.createRecord( buf, vxrOffset,
                                            VariableIndexRecord.class );
            vxrOffset = vxr.vxrNext_;
            int nent = vxr.nUsedEntries_;
            for ( int ie = 0; ie < nent; ie++ ) {
                entryList.add( new IndexEntry( vxr.first_[ ie ],
                                               vxr.last_[ ie ],
                                               vxr.offset_[ ie ] ) );
            }
        }
        return new IndexTable( entryList.toArray( new IndexEntry[ 0 ] ) );
    }

    private class IndexTable {
        private final int nent_;
        private final int[] firsts_;
        private final int[] lasts_;
        private final long[] offsets_;
        private Block lastBlock_;

        IndexTable( IndexEntry[] entries ) {
            Arrays.sort( entries );
            nent_ = entries.length;
            firsts_ = new int[ nent_ ];
            lasts_ = new int[ nent_ ];
            offsets_ = new long[ nent_ ];
            for ( int ie = 0; ie < nent_; ie++ ) {
                IndexEntry entry = entries[ ie ];
                firsts_[ ie ] = entry.first_;
                lasts_[ ie ] = entry.last_;
                Record record =
                    RecordFactory.createRecord( vdr_.getBuf(), entry.offset_ );
                if ( record instanceof VariableValuesRecord ) {
                    offsets_[ ie ] =
                        ((VariableValuesRecord) record).getContentOffset();
                }
                else {
                    // The VXR doc says this can point to another VXR rather
                    // than a VVR, though I don't really know why that's
                    // necessary.  I should support this.
                    // I think it can maybe also point to a CVVR (compressed)
                    // though it doesn't say that explicitly in the docs.
                    String msg = new StringBuffer()
                       .append( "Non VVR record type (" )
                       .append( record.getRecordType() )
                       .append( ") pointed to by VXR offset" )
                       .append( " not supported" )
                       .toString();
                    throw new CdfFormatException( msg );
                }
            }
            lastBlock_ = calculateEntryBlock( 0 );
        }

        /**
         * If one of the entries contains the given record, return its index.
         * If no entry contains it (the record is in a sparse region),
         * return (-fr-2), where <code>fr</code> is the index of the previous
         * entry.  A value of -1 indicates that the requested record is
         * in a sparse region before the first stored record.
         */
        public int getEntryIndex( int irec ) {
            if ( ! lastBlock_.contains( irec ) ) {
                lastBlock_ = calculateEntryBlock( irec );
            }
            return lastBlock_.ient_;
        }

        private Block calculateEntryBlock( int irec ) {
            int firstIndex = Arrays.binarySearch( firsts_, irec );
            if ( firstIndex >= 0 ) {
                return new Block( firstIndex,
                                  firsts_[ firstIndex ], lasts_[ firstIndex ] );
            }
            else if ( firstIndex == -1 ) {
                return new Block( -firstIndex - 2, 0, firsts_[ 0 ] - 1 );
            }
            else {
                firstIndex = -2 - firstIndex;
            }
            int lastIndex = Arrays.binarySearch( lasts_, irec );
            if ( lastIndex >= 0 ) {
                return new Block( lastIndex,
                                  firsts_[ lastIndex ], lasts_[ lastIndex ] );
            }
            else if ( lastIndex == - nent_ - 1 ) {
                return new Block( -lastIndex,
                                  lasts_[ nent_ - 1 ], Integer.MAX_VALUE );
            }
            else {
                lastIndex = -1 - lastIndex;
            }
            if ( firstIndex == lastIndex ) {
                return new Block( firstIndex,
                                  firsts_[ firstIndex ], lasts_[ firstIndex ] );
            }
            else {
                int low = lasts_[ firstIndex ] + 1;
                int high = firsts_[ lastIndex ] - 1;
                return new Block( -firstIndex - 2,
                                  lasts_[ firstIndex ] + 1,
                                  firsts_[ lastIndex ] - 1 );
            }
        }

        /**
         * The ient parameter must be an entry containing the given record,
         * most likely obtained from a previous call to getEntryIndex.
         */
        public long getOffset( int ient, int irec ) {
            assert irec >= firsts_[ ient ] && irec <= lasts_[ ient ];
            return offsets_[ ient ] + ( irec - firsts_[ ient ] ) * recSize_;
        }

        public long getLastOffset( int ient ) {
            return offsets_[ ient ]
                 + ( lasts_[ ient ] - firsts_[ ient ] + 1 ) * recSize_;
        }

        private class Block {
            final int ient_;
            final int low_;
            final int high_;
            Block( int ient, int low, int high ) {
                low_ = low;
                high_ = high;
                ient_ = ient;
            }
            boolean contains( int irec ) {
                return irec >= low_ && irec <= high_;
            }
        }
    }

    private static class IndexEntry implements Comparable<IndexEntry> {
        final int first_;
        final int last_;
        final long offset_;
        IndexEntry( int first, int last, long offset ) {
            first_ = first;
            last_ = last;
            offset_ = offset;
        }
        public int compareTo( IndexEntry other ) {
            return this.first_ - other.first_;
        }
    }
}
