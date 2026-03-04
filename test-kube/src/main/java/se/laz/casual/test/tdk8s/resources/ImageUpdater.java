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
import java.util.function.IntSupplier;

import static se.laz.casual.test.tdk8s.resources.ContainerFinder.findIndexOfContainerWithName;

/**
 * Update container images on k8s resources.
 */
public final class ImageUpdater
{

    private ImageUpdater()
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
        validate( pod, image );

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
        validate( pod, image );

        int index = findValidContainer( container, ()-> findIndexOfContainerWithName( pod, container ) );

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
        validate( deployment, image );

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
        validate( deployment, image );

        int index = findValidContainer( container, ()-> findIndexOfContainerWithName( deployment, container ) );

        return setImageOfContainerAtIndex( deployment, image, index );
    }

    private static void validate( Object resource, String image )
    {
        Objects.requireNonNull( resource, "Resource is null." );
        Objects.requireNonNull( image, "Image is null." );
    }

    private static int findValidContainer( String container, IntSupplier containerFinder )
    {
        Objects.requireNonNull( container, "container is null." );

        int index = containerFinder.getAsInt();
        if( index == -1 )
        {
            throw new ResourceNotFoundException( "Unable to find container with name: " + container );
        }
        return index;
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
