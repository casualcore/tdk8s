/*
 * Copyright (c) 2026, The casual project. All rights reserved.
 *
 * This software is licensed under the MIT license, https://opensource.org/licenses/MIT
 */

package se.laz.casual.test.tdk8s.resources;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import se.laz.casual.test.tdk8s.store.ResourceNotFoundException;

import java.util.function.Supplier;

public abstract class VolumeMounter2
{

    protected String configMap;
    protected String persistentVolumeClaim;
    protected String volumeName;
    protected String mountPath;
    protected String subPath;
    protected String containerName;
    protected int containerIndex = 0;

    public VolumeMounter2( )
    {
    }

    public VolumeMounter2 configMap( String name )
    {
        this.configMap = name;
        return this;
    }

    public VolumeMounter2 persistentVolumeClaim( String name )
    {
        this.persistentVolumeClaim = name;
        return this;
    }

    public VolumeMounter2 name( String name )
    {
        this.volumeName = name;
        return this;
    }

    public VolumeMounter2 mountPath( String mountPath )
    {
        this.mountPath = mountPath;
        return this;
    }

    public VolumeMounter2 subPath( String subPath )
    {
        this.subPath = subPath;
        return this;
    }

    public VolumeMounter2 containerName( String name )
    {
        this.containerName = name;
        return this;
    }

    public Pod mount( Pod resource )
    {
        prepareMount( ()-> ContainerFinder.findIndexOfContainerWithName( resource, this.containerName ) );

        return resource.edit()
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

    public Deployment mount( Deployment resource )
    {
        prepareMount( ()-> ContainerFinder.findIndexOfContainerWithName( resource, this.containerName ) );

        return resource.edit()
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

    private void prepareMount( Supplier<Integer> indexSupplier )
    {
        if( configMap != null && persistentVolumeClaim != null )
        {
            throw new IllegalStateException( "Volume cannot be both a config map and a pvc.");
        }
        if( this.containerName != null )
        {
            int index = indexSupplier.get();
            if( index == -1 )
            {
                throw new ResourceNotFoundException( "Unable to find container with name: " + this.containerName );
            }
            this.containerIndex = index;
        }
        if( this.volumeName == null )
        {
            this.volumeName = "tdk8s-vol-01";
        }
    }

}
