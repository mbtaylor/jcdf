package cdf;

/**
 * Provides the description and per-variable entry values
 * for a CDF attribute with variable scope.
 *
 * @author   Mark Taylor
 * @since    20 Jun 2013
 */
public interface VariableAttribute {

    /**
     * Returns this attribute's name.
     *
     * @return  attribute name
     */
    String getName();

    /**
     * Returns the entry value that a given variable has for this attribute.
     * If the variable has no entry for this attribute, null is returned.
     *
     * @param  variable  CDF variable from the same CDF as this attribute
     * @return   this attribute's value for <code>variable</code>
     */
    Object getEntry( Variable variable );
}
