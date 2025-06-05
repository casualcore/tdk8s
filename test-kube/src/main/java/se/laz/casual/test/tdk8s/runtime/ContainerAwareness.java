/*
 * Copyright (c) 2025, The casual project. All rights reserved.
 *
 * This software is licensed under the MIT license, https://opensource.org/licenses/MIT
 */

package se.laz.casual.test.tdk8s.runtime;

/**
 * Determine if the currently executing process is running inside a container or not.
 * <p>
 *     This check is based on the expectation that container runtimes will inject an
 *     environment variable of the name "container" into their runtime.
 * </p>
 * <p>
 *     It is also based on the assumption that the integration tests running will not
 *     manually set the "container" environment variable themselves.
 * </p>
 * <p>
 *     I have tested this in k3s, minikube, microk8s and openshift, though struggled to
 *     find any definitive documentation which states this is how it should be.
 * </p>
 */
public class ContainerAwareness
{

    private ContainerAwareness()
    {
    }

    public static final String CONTAINER_ENV = "container";

    private static boolean container = false;

    static
    {
        init();
    }

    static void init()
    {
        String containerEnv = System.getenv( CONTAINER_ENV );
        container = containerEnv != null && !containerEnv.isEmpty();
    }

    public static boolean isInsideContainer()
    {
        return container;
    }
}
