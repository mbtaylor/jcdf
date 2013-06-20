package cdf;

public interface GlobalAttribute {
    String getName();

    /**
     * This contains the gEntries and zEntries in a single list.
     */
    Object[] getEntries();
}
