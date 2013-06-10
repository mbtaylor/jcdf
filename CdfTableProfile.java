package cdf.util;

public interface CdfTableProfile {
    /**
     * If true, any variables with a record variance of false will be
     * treated as table parameters.  Otherwise, they will be treated
     * as table columns with the same value for each row.
     */
    boolean invariantVariablesToParameters();

    String getDescriptionAttribute( String[] attNames );
    String getUnitAttribute( String[] attNames );
}
