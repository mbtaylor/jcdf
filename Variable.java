package cdf;

import java.io.IOException;

/**
 * Provides the metadata and record data for a CDF variable.
 * This interface does not currently support data reading in such
 * a flexible way as the official CDF interface.
 * You can read a record's worth of data at a time using either
 * {@link #readRawRecord readRawRecord} or
 * {@link #readShapedRecord readShapedRecord}.
 *
 * @author   Mark Taylor
 * @since    20 Jun 2013
 */
public interface Variable {

    /**
     * Returns this variable's name.
     *
     * @return   variable name
     */
    String getName();

    /**
     * Returns the index number within the CDF of this variable.
     *
     * @return  variable num
     */
    int getNum();

    /**
     * Indicates whether this variable is a zVariable or rVariable.
     *
     * @return   true for zVariable, false for rVariable
     */
    boolean isZVariable();

    /**
     * Returns a short text string describing the type, shape and variance
     * of this variable.
     *
     * @return  text summary of variable characteristics
     */
    String getSummary();

    /**
     * Returns the upper limit of records that may have values.
     * The actual number of records may be lower than this in case of sparsity.
     *
     * @return   maximum record count
     */
    int getRecordCount();

    /**
     * Returns the data type of this variable.
     *
     * @return  data type
     */
    DataType getDataType();

    /**
     * Returns an object that knows about the array dimensions
     * of the data values.
     *
     * @return  shaper
     */
    Shaper getShaper();

    /**
     * Indicates whether this variable has a value which is fixed for all
     * records or can vary per record.
     *
     * @return   false for fixed, true for varying
     */
    boolean getRecordVariance();

    /**
     * Indicates whether a real distinct file-based record exists for
     * the given index.
     * Reading a record will give you a result in any case, but if this
     * returns false it will be some kind of fixed or default value.
     */
    boolean hasRecord( int irec ) throws IOException;

    /**
     * Creates a workspace array suitable for use with this variable's
     * reading methods.
     * The returned array is a 1-dimensional array of a primitive type
     * or of String.
     *
     * @return  workspace array for data reading
     */
    Object createRawValueArray();

    /**
     * Reads the data from a single record into a supplied raw value array.
     * The values are read into the supplied array in the order in which
     * they are stored in the data stream, that is depending on the row/column
     * majority of the CDF.
     * <p>The raw value array is as obtained from {@link #createRawValueArray}.
     *
     * @param  irec  record index
     * @param  rawValueArray  workspace array, as created by the
     *                        <code>createRawValueArray</code> method
     */
    void readRawRecord( int irec, Object rawValueArray ) throws IOException;

    /**
     * Reads the data from a single record and returns it as an object
     * of a suitable type for this variable.
     * If the variable type a scalar, then the return value will be
     * one of the primitive wrapper types (Integer etc),
     * otherwise it will be an array of primitive or String values.
     * If the majority of the stored data does not match the
     * <code>rowMajor</code> argument, the array elements will be
     * rordered appropriately.
     * If some of the dimension variances are false, the values will
     * be duplicated accordingly.
     * The Shaper returned from the {@link #getShaper} method
     * can provide more information on the return value from this method.
     *
     * <p>The workspace is as obtained from {@link #createRawValueArray}.
     *
     * @param  irec  record index
     * @param  rowMajor  required majority of output array; true for row major,
     *                   false for column major; only has an effect for
     *                   dimensionality &gt;=2
     * @param  rawValueArrayWorkspace  workspace array, as created by the
     *                                 <code>createRawValueArray</code> method
     * @return   a new object (not the same object as <code>rawValueArray</code>
     */
    Object readShapedRecord( int irec, boolean rowMajor,
                             Object rawValueArrayWorkspace ) throws IOException;
}
