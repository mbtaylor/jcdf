package cdf.util;

import cdf.CdfContent;
import cdf.GlobalAttribute;
import cdf.Variable;
import cdf.VariableAttribute;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import uk.ac.starlink.table.AbstractStarTable;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.RowSequence;
import uk.ac.starlink.table.ValueInfo;

public class CdfStarTable extends AbstractStarTable {

    private final Variable[] vars_;
    private final VariableReader[] randomVarReaders_;
    private final int ncol_;
    private final long nrow_;
    private final ColumnInfo[] colInfos_;

    public CdfStarTable( CdfContent content, CdfTableProfile profile ) {
        vars_ = content.getVariables();
        ncol_ = vars_.length;

        // Calculate row count, as the longest record count of any of
        // the variables.
        long nrow = 0;
        for ( int iv = 0; iv < vars_.length; iv++ ) {
            nrow = Math.max( nrow, vars_[ iv ].getRecordCount() );
        }
        nrow_ = nrow;

        // Set up random data access.
        randomVarReaders_ = new VariableReader[ ncol_ ];
        for ( int iv = 0; iv < ncol_; iv++ ) {
            randomVarReaders_[ iv ] = new VariableReader( vars_[ iv ] );
        }

        // Generate table parameters from global attributes.
        GlobalAttribute[] gatts = content.getGlobalAttributes();
        for ( int iga = 0; iga < gatts.length; iga++ ) {
            DescribedValue dval = createParameter( gatts[ iga ] );
            if ( dval != null ) {
                setParameter( dval );
            }
        }

        // Try to work out which attributes represent units and description.
        VariableAttribute[] vatts = content.getVariableAttributes();
        String[] attNames = new String[ vatts.length ];
        for ( int iva = 0; iva < vatts.length; iva++ ) {
            attNames[ iva ] = vatts[ iva ].getName();
        }
        String descAttName = profile.getDescriptionAttribute( attNames );
        String unitAttName = profile.getUnitAttribute( attNames );
        VariableAttribute descAtt = null;
        VariableAttribute unitAtt = null;
        for ( int iva = 0; iva < vatts.length; iva++ ) {
            VariableAttribute vatt = vatts[ iva ];
            String vattName = vatt.getName();
            if ( vattName != null ) {
                if ( vattName.equals( descAttName ) ) {
                    descAtt = vatt;
                }
                else if ( vattName.equals( unitAttName ) ) {
                    unitAtt = vatt;
                }
            }
        }

        // Remove those from the variable attributes to give a miscellaneous
        // attribute list.
        List<VariableAttribute> miscAttList =
            new ArrayList<VariableAttribute>( Arrays.asList( vatts ) );
        miscAttList.remove( descAtt );
        miscAttList.remove( unitAtt );
        colInfos_ = new ColumnInfo[ ncol_ ];
        for ( int icol = 0; icol < ncol_; icol++ ) {
            Variable var = vars_[ icol ];
            Map<String,Object> miscAttMap = new LinkedHashMap<String,Object>();
            for ( VariableAttribute vatt : miscAttList ) {
                Object entry = vatt.getEntry( var );
                if ( entry != null ) {
                    miscAttMap.put( vatt.getName(), entry );
                }
            }
            colInfos_[ icol ] =
                createColumnInfo( var, getStringEntry( descAtt, var ),
                                  getStringEntry( unitAtt, var ), miscAttMap );
        }
    }

    public int getColumnCount() {
        return ncol_;
    }

    public long getRowCount() {
        return nrow_;
    }

    public ColumnInfo getColumnInfo( int icol ) {
        return colInfos_[ icol ];
    }

    public boolean isRandom() {
        return true;
    }

    public Object getCell( long irow, int icol ) {
        return randomVarReaders_[ icol ]
              .readShapedRecord( toRecordIndex( irow ) );
    }

    public RowSequence getRowSequence() {
        final VariableReader[] vrdrs = new VariableReader[ ncol_ ];
        for ( int icol = 0; icol < ncol_; icol++ ) {
            vrdrs[ icol ] = new VariableReader( vars_[ icol ] );
        }
        return new RowSequence() {
            private long irow = -1;
            public boolean next() {
                return ++irow < nrow_;
            }
            public Object getCell( int icol ) {
                return vrdrs[ icol ].readShapedRecord( toRecordIndex( irow ) );
            }
            public Object[] getRow() {
                Object[] row = new Object[ ncol_ ];
                for ( int icol = 0; icol < ncol_; icol++ ) {
                    row[ icol ] = getCell( icol );
                }
                return row;
            }
            public void close() {
            }
        };
    }

    private static DescribedValue createParameter( GlobalAttribute gatt ) {
        String name = gatt.getName();
        Object[] entries = gatt.getEntries();
        int nent = entries.length;
        if ( nent == 0 ) {
            return null;
        }
        else if ( nent == 1 ) {
            Object value = entries[ 0 ];
            if ( value == null ) {
                return null;
            }
            else {
                ValueInfo info =
                    new DefaultValueInfo( name, value.getClass(), null );
                return new DescribedValue( info, value );
            }
        }
        else {
            Object value = entries;
            DefaultValueInfo info =
                new DefaultValueInfo( name, value.getClass(), null );
            info.setShape( new int[] { nent } );
            return new DescribedValue( info, value );
        }
    }

    private static ColumnInfo createColumnInfo( Variable var, String descrip,
                                                String units,
                                                Map<String,Object> attMap ) {
        String name = var.getName();
        Class clazz = var.getShaper().getShapeClass();
        int[] shape = clazz.getComponentType() == null
                    ? null
                    : var.getShaper().getDimSizes();
        ColumnInfo info = new ColumnInfo( name, clazz, descrip );
        info.setUnitString( units );
        info.setShape( shape  );
        List<DescribedValue> auxData = new ArrayList<DescribedValue>();
        for ( Map.Entry<String,Object> attEntry : attMap.entrySet() ) {
            String auxName = attEntry.getKey();
            Object auxValue = attEntry.getValue();
            if ( auxValue != null ) {
                ValueInfo auxInfo =
                    new DefaultValueInfo( auxName, auxValue.getClass() );
                auxData.add( new DescribedValue( auxInfo, auxValue ) );
            }
        }
        info.setAuxData( auxData );
        return info;
    }

    private static String getStringEntry( VariableAttribute att,
                                          Variable var ) {
        Object entry = att == null ? null : att.getEntry( var );
        return entry instanceof String ? (String) entry : null;
    }

    private static class VariableReader {
        private final Variable var_;
        private final Object work_;

        VariableReader( Variable var ) {
            var_ = var;
            work_ = var_.createRawValueArray();
        }

        synchronized Object readShapedRecord( int irec ) {
            return var_.readShapedRecord( irec, false, work_ );
        }
    }

    private static int toRecordIndex( long irow ) {
        int irec = (int) irow;
        if ( irec != irow ) {
            // long record counts not supported in CDF so must be a call error.
            throw new IllegalArgumentException( "Out of range: " + irow );
        }
        return irec;
    }
}
