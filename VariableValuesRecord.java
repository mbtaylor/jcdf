package cdf;

public class VariableValuesRecord extends Record {

    public static final int RECORD_TYPE = 7;

    public VariableValuesRecord( RecordPlan plan ) {
        super( plan, RECORD_TYPE );
    }
}
