/*
 * Copyright (c) 2005 Oculus Technologies Corporation, All Rights Reserved
 */
package abbot;

/**
 * @author twall
 *
 */
public interface DynamicLoadingConstants {
    /** Path to classes for dynamic loading; must not be included in
     * java.class.path.
     */
    String DYNAMIC_CLASSPATH = "build/dynamic-test-classes";
    /** Generic class to use for class reloading tests. */
    String DYNAMIC_CLASSNAME = "test.dynamic.ClassForReloading";
    /** {@link java.awt.Component}-based class to use for class reloading tests. */
    String DYNAMIC_COMPONENT_CLASSNAME = "test.dynamic.ComponentClassForLoading";
}
