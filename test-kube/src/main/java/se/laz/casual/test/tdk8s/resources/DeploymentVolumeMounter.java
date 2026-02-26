/*
 * Copyright (c) 2026, The casual project. All rights reserved.
 *
 * This software is licensed under the MIT license, https://opensource.org/licenses/MIT
 */

package se.laz.casual.test.tdk8s.resources;

import io.fabric8.kubernetes.api.model.apps.Deployment;

import java.util.function.Supplier;

public class DeploymentVolumeMounter extends VolumeMounter<Deployment>
{
    public DeploymentVolumeMounter( Deployment resource )
    {
        super( resource );
    }

    @Override
    protected Supplier<Integer> resolveContainerIndex()
    {
        return ()-> ContainerFinder.findIndexOfContainerWithName( resource, this.containerName );
    }

    @Override
    protected Deployment editResourceVolumes()
    {
        return this.resource.edit()
                .editSpec()
                    .editTemplate()
                        .editSpec()
                            .addNewVolume()
                                .withName( this.volumeName )
                                .withNewConfigMap().withName( this.configMap ).endConfigMap()
                            .endVolume()
                            .editContainer( 0 )
                                .addNewVolumeMount()
                                    .withName( this.volumeName )
                                    .withMountPath( this.mountPath )
                                    .withSubPath( this.subPath )
                                .endVolumeMount()
                            .endContainer()
                        .endSpec()
                    .endTemplate()
                .endSpec()
        .build();
    }
}
