/*
 * Copyright (c) 2025, The casual project. All rights reserved.
 *
 * This software is licensed under the MIT license, https://opensource.org/licenses/MIT
 */

package se.laz.casual.test.tdk8s.controller.logging;

import io.fabric8.kubernetes.client.dsl.PodResource;
import se.laz.casual.test.tdk8s.controller.provisioning.ResourceLookupController;
import se.laz.casual.test.tdk8s.store.ResourceNotFoundException;

public class LogControllerImpl implements LogController
{
    private final ResourceLookupController lookupController;

    public LogControllerImpl( ResourceLookupController lookupController )
    {
        this.lookupController = lookupController;
    }

    @Override
    public String getLog( String resource )
    {
        PodResource pod = getPodResource( resource );

        return pod.getLog();
    }

    @Override
    public String getLogTail( String resource, int lines )
    {
        PodResource pod = getPodResource( resource );

        return pod.tailingLines( lines ).getLog();
    }

    @Override
    public String getLogSince( String resource, String sinceTime )
    {
        PodResource pod = getPodResource( resource );

        return pod.sinceTime( sinceTime ).getLog();
    }

    private PodResource getPodResource( String resource )
    {
        return lookupController.findFirstPodForResource( resource )
                .orElseThrow( () -> new ResourceNotFoundException( "Unable to find Pod for resource: " + resource ) );
    }
}
