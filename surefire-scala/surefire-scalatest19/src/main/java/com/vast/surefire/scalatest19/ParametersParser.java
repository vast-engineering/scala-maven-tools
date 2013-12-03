package com.vast.surefire.scalatest19;

import org.apache.maven.surefire.booter.ProviderParameterNames;

import java.util.Properties;

/**
 * @author David Pratt (dpratt@vast.com)
 */
class ParametersParser {

    private static final int DEFAULT_THREADCOUNT = 5;

    private final Properties props;

    public ParametersParser(Properties props) {
        this.props = props;
    }

    public boolean shouldExecuteParallel() {
        return props.getProperty(ProviderParameterNames.PARALLEL_PROP) != null;
    }

    public int getThreadCount() {
        String rawCount =  props.getProperty(ProviderParameterNames.THREADCOUNT_PROP);

        Integer count;
        if(rawCount != null) {
            try {
                count = Integer.parseInt(rawCount);
            } catch(NumberFormatException e) {
                count = DEFAULT_THREADCOUNT;
            }
        } else {
            count = DEFAULT_THREADCOUNT;
        }
        return count;
    }

}
