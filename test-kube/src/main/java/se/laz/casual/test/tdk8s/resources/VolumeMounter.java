/*
 * Copyright (c) 2026, The casual project. All rights reserved.
 *
 * This software is licensed under the MIT license, https://opensource.org/licenses/MIT
 */

package se.laz.casual.test.tdk8s.resources;

import se.laz.casual.test.tdk8s.store.ResourceNotFoundException;

import java.util.function.Supplier;

public abstract class VolumeMounter<T>
{

    protected T resource;
    protected String configMap;
    protected String persistentVolumeClaim;
    protected String volumeName;
    protected String mountPath;
    protected String subPath;
    protected String containerName;
    protected int containerIndex = 0;

    public VolumeMounter( T resource )
    {
        this.resource = resource;
    }

    public VolumeMounter<T> configMap( String name )
    {
        this.configMap = name;
        return this;
    }

    public VolumeMounter<T> persistentVolumeClaim( String name )
    {
        this.persistentVolumeClaim = name;
        return this;
    }

    public VolumeMounter<T> name( String name )
    {
        this.volumeName = name;
        return this;
    }

    public VolumeMounter<T> mountPath( String mountPath )
    {
        this.mountPath = mountPath;
        return this;
    }

    public VolumeMounter<T> subPath( String subPath )
    {
        this.subPath = subPath;
        return this;
    }

    public VolumeMounter<T> containerName( String name )
    {
        this.containerName = name;
        return this;
    }

    protected abstract Supplier<Integer> resolveContainerIndex( );

    protected abstract T editResourceVolumes( );

    public T mount( )
    {
        if( configMap != null && persistentVolumeClaim != null )
        {
            throw new IllegalStateException( "Volume cannot be both a config map and a pvc.");
        }
        if( this.containerName != null )
        {
            int index = resolveContainerIndex().get();
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

        return editResourceVolumes();
    }

}
