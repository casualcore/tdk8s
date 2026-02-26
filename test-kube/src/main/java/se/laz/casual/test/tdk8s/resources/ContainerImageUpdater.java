/*
 * Copyright (c) 2026, The casual project. All rights reserved.
 *
 * This software is licensed under the MIT license, https://opensource.org/licenses/MIT
 */

package se.laz.casual.test.tdk8s.resources;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import se.laz.casual.test.tdk8s.store.ResourceNotFoundException;

import java.util.Objects;

import static se.laz.casual.test.tdk8s.resources.ContainerFinder.findIndexOfContainerWithName;

/**
 * Update images on different resources.
 */
public final class ContainerImageUpdater
{

    private ContainerImageUpdater()
    {
    }

    /**
     * Update the image for first container in the pod.
     *
     * @param pod to update the container image.
     * @param image the new image.
     * @return updated pod.
     */
    public static Pod setImage( Pod pod, String image )
    {
        Objects.requireNonNull( pod, "Pod is null." );
        Objects.requireNonNull( image, "Image is null." );

        return setImageOfContainerAtIndex( pod, image, 0 );
    }

    /**
     * Update the image for the container with the name provided in the pod.
     *
     * @param pod to update the container image.
     * @param image the new image.
     * @param container the container name to update.
     * @return updated pod.
     * @throws ResourceNotFoundException if container is not found.
     */
    public static Pod setImage( Pod pod, String image, String container )
    {
        Objects.requireNonNull( pod, "Pod is null." );
        Objects.requireNonNull( image, "Image is null." );
        Objects.requireNonNull( container, "container is null." );

        int index = findIndexOfContainerWithName( pod, container );
        if( index == -1 )
        {
            throw new ResourceNotFoundException( "Unable to find container with name: " + container );
        }
        return setImageOfContainerAtIndex( pod, image, index );
    }

    /**
     * Update the image for first container in the deployment.
     *
     * @param deployment to update the container image.
     * @param image the new image.
     * @return updated deployment.
     */
    public static Deployment setImage( Deployment deployment, String image )
    {
        Objects.requireNonNull( deployment, "Deployment is null." );
        Objects.requireNonNull( image, "Image is null." );

        return setImageOfContainerAtIndex( deployment, image, 0 );

    }

    /**
     * Update the image for the container with the name provided in the deployment.
     *
     * @param deployment to update the container image.
     * @param image the new image.
     * @param container the container name to update.
     * @return updated deployment.
     * @throws ResourceNotFoundException if container is not found.
     */
    public static Deployment setImage( Deployment deployment, String image, String container )
    {
        Objects.requireNonNull( deployment, "Deployment is null." );
        Objects.requireNonNull( image, "Image is null." );
        Objects.requireNonNull( container, "container is null." );

        int index = findIndexOfContainerWithName( deployment, container );
        if( index == -1 )
        {
            throw new ResourceNotFoundException( "Unable to find container with name: " + container );
        }
        return setImageOfContainerAtIndex( deployment, image, index );
    }

    private static Pod setImageOfContainerAtIndex( Pod pod, String image, int containerIndex )
    {
        return pod.edit()
                .editSpec()
                    .editContainer( containerIndex )
                        .withImage( image )
                    .endContainer()
                .endSpec()
                .build();
    }

    private static Deployment setImageOfContainerAtIndex( Deployment deployment, String image, int containerIndex )
    {
        return deployment.edit()
                .editSpec()
                    .editTemplate()
                        .editSpec()
                            .editContainer( containerIndex )
                                .withImage( image )
                            .endContainer()
                        .endSpec()
                    .endTemplate()
                .endSpec()
                .build();
    }
}
