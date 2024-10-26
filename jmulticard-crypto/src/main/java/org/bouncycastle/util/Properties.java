package org.bouncycastle.util;

import java.math.BigInteger;
import java.security.AccessControlException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.Security;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

/**
 * Utility method for accessing properties values - properties can be set in java.security,
 * thread local, and system properties. They are checked for in the same order with
 * checking stopped as soon as a value is found.
 */
public class Properties {

    private Properties()
    {
    }

    private static final ThreadLocal threadProperties = new ThreadLocal();

    /**
     * Return whether a particular override has been set to true.
     *
     * @param propertyName the property name for the override.
     * @return true if the property is set to "true", false otherwise.
     */
    public static boolean isOverrideSet(final String propertyName) {
        try {
            return isSetTrue(getPropertyValue(propertyName));
        }
        catch (final AccessControlException e) {
            return false;
        }
    }

    /**
     * Return whether a particular override has been set to false.
     *
     * @param propertyName the property name for the override.
     * @param isTrue true if the override should be true, false otherwise.
     * @return true if the property is set to the value of isTrue, false otherwise.
     */
    public static boolean isOverrideSetTo(final String propertyName, final boolean isTrue) {
        try {
            final String propertyValue = getPropertyValue(propertyName);
            if (isTrue) {
                return isSetTrue(propertyValue);
            }
            return isSetFalse(propertyValue);
        }
        catch (final AccessControlException e) {
            return false;
        }
    }

    /**
     * Enable the specified override property for the current thread only.
     *
     * @param propertyName the property name for the override.
     * @param enable true if the override should be enabled, false if it should be disabled.
     * @return true if the override was already set true, false otherwise.
     */
    public static boolean setThreadOverride(final String propertyName, final boolean enable) {
        final boolean isSet = isOverrideSet(propertyName);

        Map localProps = (Map)threadProperties.get();
        if (localProps == null) {
            localProps = new HashMap();

            threadProperties.set(localProps);
        }

        localProps.put(propertyName, enable ? "true" : "false");

        return isSet;
    }

    /**
     * Remove any value for the specified override property for the current thread only.
     *
     * @param propertyName the property name for the override.
     * @return true if the override was already set true in thread local, false otherwise.
     */
    public static boolean removeThreadOverride(final String propertyName)
    {
        final Map localProps = (Map)threadProperties.get();
        if (localProps != null)
        {
            final String p = (String)localProps.remove(propertyName);
            if (p != null)
            {
                if (localProps.isEmpty())
                {
                    threadProperties.remove();
                }

                return "true".equals(Strings.toLowerCase(p));
            }
        }

        return false;
    }

    /**
     * Return propertyName as an integer, defaultValue used if not defined.
     *
     * @param propertyName name of property.
     * @param defaultValue integer to return if property not defined.
     * @return value of property, or default if not found, as an int.
     */
    public static int asInteger(final String propertyName, final int defaultValue)
    {
        final String p = getPropertyValue(propertyName);

        if (p != null)
        {
            return Integer.parseInt(p);
        }

        return defaultValue;
    }

    /**
     * Return propertyName as a BigInteger.
     *
     * @param propertyName name of property.
     * @return value of property as a BigInteger, null if not defined.
     */
    public static BigInteger asBigInteger(final String propertyName)
    {
        final String p = getPropertyValue(propertyName);

        if (p != null)
        {
            return new BigInteger(p);
        }

        return null;
    }

    public static Set<String> asKeySet(final String propertyName)
    {
        final Set<String> set = new HashSet<>();

        final String p = getPropertyValue(propertyName);

        if (p != null)
        {
            final StringTokenizer sTok = new StringTokenizer(p, ",");
            while (sTok.hasMoreElements())
            {
                set.add(Strings.toLowerCase(sTok.nextToken()).trim());
            }
        }

        return Collections.unmodifiableSet(set);
    }

    /**
     * Return the String value of the property propertyName. Property valuation
     * starts with java.security, then thread local, then system properties.
     *
     * @param propertyName name of property.
     * @return value of property as a String, null if not defined.
     */
    public static String getPropertyValue(final String propertyName)
    {
        final String val = (String)AccessController.doPrivileged(new PrivilegedAction()
        {
            @Override
			public Object run()
            {
                return Security.getProperty(propertyName);
            }
        });
        if (val != null)
        {
            return val;
        }

        final Map localProps = (Map)threadProperties.get();
        if (localProps != null)
        {
            final String p = (String)localProps.get(propertyName);
            if (p != null)
            {
                return p;
            }
        }

        return (String)AccessController.doPrivileged(new PrivilegedAction()
        {
            @Override
			public Object run()
            {
                return System.getProperty(propertyName);
            }
        });
    }

    private static boolean isSetFalse(final String p)
    {
        if (p == null || p.length() != 5)
        {
            return false;
        }

        return (p.charAt(0) == 'f' || p.charAt(0) == 'F')
            && (p.charAt(1) == 'a' || p.charAt(1) == 'A')
            && (p.charAt(2) == 'l' || p.charAt(2) == 'L')
            && (p.charAt(3) == 's' || p.charAt(3) == 'S')
            && (p.charAt(4) == 'e' || p.charAt(4) == 'E');
    }

    private static boolean isSetTrue(final String p)
    {
        if (p == null || p.length() != 4)
        {
            return false;
        }

        return (p.charAt(0) == 't' || p.charAt(0) == 'T')
            && (p.charAt(1) == 'r' || p.charAt(1) == 'R')
            && (p.charAt(2) == 'u' || p.charAt(2) == 'U')
            && (p.charAt(3) == 'e' || p.charAt(3) == 'E');
    }
}
