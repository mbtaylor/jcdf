package cdf;

public class VariableValuesRecord extends Record {

    private final long recordsOffset_;
    public VariableValuesRecord( RecordPlan plan ) {
        super( plan, "VVR", 7 );
        Pointer ptr = plan.createContentPointer();
        recordsOffset_ = ptr.get();
    }

    public long getRecordsOffset() {
        return recordsOffset_;
    }
}
