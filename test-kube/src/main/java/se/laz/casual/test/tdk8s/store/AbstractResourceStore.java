/*
 * Copyright (c) 2025, The casual project. All rights reserved.
 *
 * This software is licensed under the MIT license, https://opensource.org/licenses/MIT
 */

package se.laz.casual.test.tdk8s.store;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public abstract class AbstractResourceStore<T> implements ResourceStore<T>
{
    public static final String NAME_IS_NULL = "Name is null.";
    protected final Map<String, T> resources;

    protected AbstractResourceStore()
    {
        resources = new HashMap<>();
    }

    @Override
    public T get( String name )
    {
        Objects.requireNonNull( name, NAME_IS_NULL );

        if( !resources.containsKey( name ) )
        {
            throw new ResourceNotFoundException( "Resource not found: " + name );
        }
        return resources.get( name );
    }

    public Map<String, T> getAll( )
    {
        return new HashMap<>( resources );
    }

    @Override
    public boolean contains( String name )
    {
        Objects.requireNonNull( name, NAME_IS_NULL );

        return resources.containsKey( name );
    }

    @Override
    public void put( String name, T value )
    {
        Objects.requireNonNull( name, NAME_IS_NULL );
        Objects.requireNonNull( value, "Value is null." );

        resources.put( name, value );
    }

    @Override
    public void putAll( Map<String, T> all )
    {
        Objects.requireNonNull( all, "All is null." );

        resources.putAll( all );
    }

    @Override
    public T remove( String name )
    {
        Objects.requireNonNull( name, NAME_IS_NULL );

        T resource = resources.remove( name );
        if( resource == null )
        {
            throw new ResourceNotFoundException( "Resource not found: " + name );
        }
        return resource;
    }
}
