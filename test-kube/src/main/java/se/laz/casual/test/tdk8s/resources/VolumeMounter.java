/*
 * Copyright (c) 2026, The casual project. All rights reserved.
 *
 * This software is licensed under the MIT license, https://opensource.org/licenses/MIT
 */

package se.laz.casual.test.tdk8s.resources;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import se.laz.casual.test.tdk8s.store.ResourceNotFoundException;

import java.nio.file.Paths;
import java.util.Objects;
import java.util.function.IntSupplier;

/**
 * Mount volumes on k8s resources.
 */
public final class VolumeMounter
{
    private VolumeMounter()
    {
    }

    /**
     * Mount the {@link FileMount} as a volume on the pod resource.
     *
     * @param resource to mount upon.
     * @param mount definition of what to mount.
     * @return updated pod resource.
     * @throws ResourceNotFoundException if container for mount is not found.
     */
    public static Pod mount( Pod resource, FileMount mount )
    {
        validate( resource, mount );

        int index = getContainerIndex( mount, ()-> ContainerFinder.findIndexOfContainerWithName( resource, mount.getContainer() ) );

        return mountContainerAtIndex( resource, mount, index );
    }

    /**
     * Mount the {@link FileMount} as a volume on the deployment resource.
     * @param resource to mount upon.
     * @param mount definition of what to mount.
     * @return updated deployment resource.
     * @throws ResourceNotFoundException if container for mount is not found.
     */
    public static Deployment mount( Deployment resource, FileMount mount )
    {
        validate( resource, mount );

        int index = getContainerIndex( mount, ()-> ContainerFinder.findIndexOfContainerWithName( resource, mount.getContainer() ) );

        return mountContainerAtIndex( resource, mount, index );
    }

    private static void validate( Object resource, FileMount mount )
    {
        Objects.requireNonNull( resource, "Resource is null." );
        Objects.requireNonNull( mount, "File mount is null." );
    }

    private static int getContainerIndex( FileMount mount, IntSupplier containerFinder )
    {
        int index = 0;

        if( mount.getContainer() != null )
        {
            index = containerFinder.getAsInt();
            if( index == -1 )
            {
                throw new ResourceNotFoundException( "Could not find container with name: " + mount.getContainer() );
            }
        }
        return index;
    }

    private static Pod mountContainerAtIndex( Pod resource, FileMount mount, int index )
    {
        return resource.edit()
                .editSpec()
                    .addNewVolume()
                        .withName( mount.getVolume() )
                        .withNewConfigMap().withName( mount.getConfigMap().getMetadata().getName() ).endConfigMap()
                    .endVolume()
                    .editContainer( index )
                        .addNewVolumeMount()
                            .withName( mount.getVolume() )
                            .withMountPath( mount.getMountPath() )
                            .withSubPath( Paths.get( mount.getMountPath() ).getFileName().toString() )
                        .endVolumeMount()
                    .endContainer()
                .endSpec()
            .build();
    }

    private static Deployment mountContainerAtIndex( Deployment resource, FileMount mount, int index )
    {
        return resource.edit()
                .editSpec()
                    .editTemplate()
                        .editSpec()
                            .addNewVolume()
                                .withName( mount.getVolume() )
                                .withNewConfigMap().withName( mount.getConfigMap().getMetadata().getName() ).endConfigMap()
                            .endVolume()
                            .editContainer( index )
                                .addNewVolumeMount()
                                    .withName( mount.getVolume() )
                                    .withMountPath( mount.getMountPath() )
                                    .withSubPath( Paths.get( mount.getMountPath() ).getFileName().toString() )
                                .endVolumeMount()
                            .endContainer()
                        .endSpec()
                    .endTemplate()
                .endSpec()
            .build();
    }
}
