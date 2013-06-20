package cdf;

/**
 * Provides all the data and metadata in a CDF file in an easy to use form.
 *
 * @author   Mark Taylor
 * @since    20 Jun 2013
 */
public interface CdfContent {

    /**
     * Returns the global attributes.
     *
     * @return  global attribute array, in order
     */
    GlobalAttribute[] getGlobalAttributes();

    /**
     * Returns the variable attributes.
     *
     * @return   variable attribute array, in order
     */
    VariableAttribute[] getVariableAttributes();

    /**
     * Returns the variables.
     *
     * @return  variable array, in order
     */
    Variable[] getVariables();
}
