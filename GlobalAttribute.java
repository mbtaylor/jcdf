package uk.ac.bristol.star.cdf;

/**
 * Provides the description and entry values
 * for CDF attribute with global scope.
 *
 * <p>The gEntries and zEntries are combined in a single list,
 * on the grounds that users are not likely to be much interested
 * in the difference.
 *
 * @author   Mark Taylor
 * @since    20 Jun 2013
 */
public class GlobalAttribute {

    private final String name_;
    private final Object[] entries_;

    /**
     * Constructor.
     *
     * @param   name   attribute name
     * @param   entries  attribute entries
     */
    public GlobalAttribute( String name, Object[] entries ) {
        name_ = name;
        entries_ = entries;
    }

    /**
     * Returns this attribute's name.
     *
     * @return   attribute name
     */
    public String getName() {
        return name_;
    }

    /**
     * Returns this attribute's entry values.
     *
     * @return  entry values for this attribute
     */
    public Object[] getEntries() {
        return entries_;
    }
}
