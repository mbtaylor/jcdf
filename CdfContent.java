package cdf;

public interface CdfContent {
    GlobalAttribute[] getGlobalAttributes();
    VariableAttribute[] getVariableAttributes();
    Variable[] getVariables();
}
