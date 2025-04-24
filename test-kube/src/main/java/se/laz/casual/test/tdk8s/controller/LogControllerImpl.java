/*
 * Copyright (c) 2025, The casual project. All rights reserved.
 *
 * This software is licensed under the MIT license, https://opensource.org/licenses/MIT
 */

package se.laz.casual.test.tdk8s.controller;

import io.fabric8.kubernetes.client.dsl.PodResource;
import se.laz.casual.test.tdk8s.store.ResourceNotFoundException;

public class LogControllerImpl implements LogController
{
    private final ResourceLookupController lookupController;

    public LogControllerImpl( ResourceLookupController lookupController )
    {
        this.lookupController = lookupController;
    }

    @Override
    public String getLog( String pod )
    {
        PodResource resource = getPodResource( pod );

        return resource.getLog();
    }

    @Override
    public String getLogTail( String pod, int lines )
    {
        PodResource resource = getPodResource( pod );

        return resource.tailingLines( lines ).getLog();
    }

    @Override
    public String getLogSince( String pod, String sinceTime )
    {
        PodResource resource = getPodResource( pod );

        return resource.sinceTime( sinceTime ).getLog();
    }

    private PodResource getPodResource( String pod )
    {
        return lookupController.getPodResource( pod )
                .orElseThrow( () -> new ResourceNotFoundException( "Unable to find pod " + pod ) );
    }
}
