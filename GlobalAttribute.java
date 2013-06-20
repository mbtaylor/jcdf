package cdf;

/**
 * Provides the description and entry values
 * for CDF attribute with global scope.
 *
 * @author   Mark Taylor
 * @since    20 Jun 2013
 */
public interface GlobalAttribute {

    /**
     * Returns this attribute's name.
     *
     * @return   attribute name
     */
    String getName();

    /**
     * Returns this attribute's entry values.
     * The gEntries and zEntries are combined in a single list.
     *
     * @return  entry values for this attribute
     */
    Object[] getEntries();
}
