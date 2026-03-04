/*
 * Copyright (c) 2026, The casual project. All rights reserved.
 *
 * This software is licensed under the MIT license, https://opensource.org/licenses/MIT
 */

package se.laz.casual.test.tdk8s.resources;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.apps.Deployment;

import java.util.List;

/**
 * Helper to find the index of a container with the provided name.
 */
public class ContainerFinder
{
    private ContainerFinder()
    {
    }

    public static int findIndexOfContainerWithName( Pod pod, String name )
    {
        List<Container> containers = pod.getSpec().getContainers();
        return findIndexOfContainerWithName( containers, name );
    }

    public static int findIndexOfContainerWithName( Deployment deployment, String name )
    {
        List<Container> containers = deployment.getSpec().getTemplate().getSpec().getContainers();
        return findIndexOfContainerWithName( containers, name );
    }

    private static int findIndexOfContainerWithName( List<Container> containers, String name )
    {
        for( int i=0; i < containers.size(); i++ )
        {
            if( containers.get( i ).getName().equals( name ) )
            {
                return i;
            }
        }
        return -1;
    }
}
