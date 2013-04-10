package cdf;

public class Offset {
    private int value_;

    public Offset() {
        this( 0 );
    }

    public Offset( int value ) {
        value_ = value;
    }

    public int get() {
        return value_;
    }

    public int getAndIncrement( int increment ) {
        int v = value_;
        value_ += increment;
        return v;
    }
   
    public void set( int value ) {
        value_ = value;
    }

    public void increment( int increment ) {
        value_ += increment;
    }
}
