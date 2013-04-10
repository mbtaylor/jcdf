package cdf;

public class Pointer {
    private long value_;

    public Pointer( long value ) {
        value_ = value;
    }

    public long get() {
        return value_;
    }

    public long getAndIncrement( int increment ) {
        long v = value_;
        value_ += increment;
        return v;
    }
   
    public void set( long value ) {
        value_ = value;
    }

    public void increment( int increment ) {
        value_ += increment;
    }
}
