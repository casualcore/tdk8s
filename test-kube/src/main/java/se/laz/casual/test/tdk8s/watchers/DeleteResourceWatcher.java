/*
 * Copyright (c) 2025, The casual project. All rights reserved.
 *
 * This software is licensed under the MIT license, https://opensource.org/licenses/MIT
 */

package se.laz.casual.test.tdk8s.watchers;

import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.dsl.Resource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class DeleteResourceWatcher<T>
{
    private final DeleteWatcher<T> watcher;
    private final List<Watch> watches = new ArrayList<>();

    public DeleteResourceWatcher( DeleteWatcher<T> watcher, Resource<T> resource )
    {
        this( watcher, toList( resource ) );
    }

    private static <T> List<Resource<T>> toList( Resource<T> resource )
    {
        Objects.requireNonNull( resource, "Resource is null." );
        return Collections.singletonList( resource );
    }

    public DeleteResourceWatcher( DeleteWatcher<T> watcher, List<? extends Resource<T>> resources )
    {
        Objects.requireNonNull( watcher, "Watcher is null." );
        Objects.requireNonNull( resources, "Resources is null." );
        this.watcher = watcher;
        for( Resource<T> r : resources )
        {
            Watch watch = r.watch( watcher );
            watches.add( watch );
        }
    }

    public void waitUntilDeleted()
    {
        watcher.waitUntilDeleted();

        for( Watch w : watches )
        {
            w.close();
        }
    }
}
