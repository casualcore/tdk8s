/*
 * Copyright (c) 2026, The casual project. All rights reserved.
 *
 * This software is licensed under the MIT license, https://opensource.org/licenses/MIT
 */

package se.laz.casual.test.tdk8s.resources;

import io.fabric8.kubernetes.api.model.Pod;

import java.util.function.Supplier;

public class PodVolumeMounter extends VolumeMounter<Pod>
{
    public PodVolumeMounter( Pod resource )
    {
        super( resource );
    }

    @Override
    protected Supplier<Integer> resolveContainerIndex()
    {
        return ()-> ContainerFinder.findIndexOfContainerWithName( resource, this.containerName );
    }

    @Override
    protected Pod editResourceVolumes()
    {
        return this.resource.edit()
                .editSpec()
                .addNewVolume()
                    .withName( this.volumeName )
                    .withNewConfigMap().withName( this.configMap ).endConfigMap()
                .endVolume()
                .editContainer( this.containerIndex )
                    .addNewVolumeMount()
                        .withName( this.volumeName )
                        .withMountPath( this.mountPath )
                        .withSubPath( this.subPath )
                    .endVolumeMount()
                .endContainer()
                .endSpec()
        .build();
    }
}
