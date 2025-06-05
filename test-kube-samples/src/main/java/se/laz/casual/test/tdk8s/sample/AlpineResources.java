/*
 * Copyright (c) 2025, The casual project. All rights reserved.
 *
 * This software is licensed under the MIT license, https://opensource.org/licenses/MIT
 */

package se.laz.casual.test.tdk8s.sample;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;

import java.util.Map;

public final class AlpineResources
{
    private AlpineResources()
    {
    }

    public static final Map<String, String> SELECTOR = Map.of( "app", "alpine" );

    public static final String SIMPLE_ALPINE_POD_NAME = "alpine-test";

    public static final Pod SIMPLE_ALPINE_POD = new PodBuilder()
            .withNewMetadata()
            .withName( SIMPLE_ALPINE_POD_NAME )
            .addToLabels( SELECTOR )
            .endMetadata()
            .withNewSpec()
            .addNewContainer()
            .withName( "alpine" )
            .withImage( "alpine:3.21.3" )
            .withCommand( "sh", "-c", "echo \"up\";trap exit SIGTERM;while true\ndo\n  sleep 0.001\ndone" )
            .endContainer()
            .endSpec()
            .build();
}
