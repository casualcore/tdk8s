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
import java.util.concurrent.TimeUnit;

/**
 * Manages the watches for a resource that's deletion is being watched / monitored.
 * <p>
 *     Both the {@link Watch} network channels to the resource(s) and the actual
 *     {@link DeleteWatcher} used to count down the desired number of deletions.
 * </p>
 * <p>
 *     There is always a single DeleteWatcher and 1 or more resources to Watch.
 * </p>
 * <p>
 *     For example, deleting a single Pod, you will have a DeleteWatcher with the count down
 *     of 1 and a single Pod resource Watch to maintain whilst waiting for Deletion to occur.
 * </p>
 * <p>
 *     When deleting a Deployment, it will be similar to the Pod deletion scenario above for
 *     the Deployment resource.
 *     <br/>
 *     But you can also create an additional DeleteResourceWatcher with providing multiple
 *     Pod resources for the Pods associated with the Deployment, such that you can also wait
 *     for their deletion.
 *     <br/>
 *     In this scenario, a single DeleteWatcher is created with a count down set to the size of the
 *     Pod resource count for the a Pod Watch instance for each and every pod.
 * </p>
 * <p>
 *     When monitoring a Scale down operation, you will have a single DeleteResourceWatcher for all
 *     Deployment Pods.
 *     <br/>
 *     Though the provided desiredDeletionCount sets DeleteWatcher count down for the number of pods
 *     that need to be removed from the total number of Pods that are being watched.
 *     <br/>
 *     Thereby, ensuring that the {@link #waitUntilDeleted()} returns once the desired number of pods
 *     has been deleted, not all of them.
 * </p>
 * <p>
 *     All DeleteResourceWatch objects need to be created prior to calling any actual delete operations
 *     to ensure they work as expected.
 * </p>
 *
 * @param <T> type of resource being watched for deletion.
 */
public class DeleteResourceWatcher<T>
{
    private final DeleteWatcher<T> watcher;
    private final List<Watch> watches = new ArrayList<>();

    /**
     * Watch for the deletion of a single resource.
     *
     * @param resource to watch.
     */
    public DeleteResourceWatcher( Resource<T> resource )
    {
        this( toList( resource ) );
    }

    /**
     * Watch for the deletion of all resources provided.
     *
     * @param resources all resources to watch.
     */
    public DeleteResourceWatcher( List<? extends Resource<T>> resources )
    {
        this( check(resources), resources.size() );
    }

    /**
     * Watch for the deletion of a number of the resources provided.
     *
     * @param resources all resources to watch.
     * @param desiredDeletionCount total number of deletions desired to monitor.
     */
    public DeleteResourceWatcher( List<? extends Resource<T>> resources, int desiredDeletionCount )
    {
        check( resources );
        this.watcher = new DeleteWatcher<>( desiredDeletionCount );
        for( Resource<T> r : resources )
        {
            Watch watch = r.watch( watcher );
            watches.add( watch );
        }
    }

    private static <T> List<? extends Resource<T>> toList( Resource<T> resource )
    {
        Objects.requireNonNull( resource, "Resource is null." );
        return Collections.singletonList( resource );
    }

    private static <T> List<? extends Resource<T>> check( List<? extends Resource<T>> resources )
    {
        return Objects.requireNonNull( resources, "Resources is null." );
    }

    /**
     * Wait until the desired deletion count is reached.
     */
    public void waitUntilDeleted()
    {
        watcher.waitUntilDeleted();

        for( Watch w : watches )
        {
            w.close();
        }
    }

    /**
     * Wait until the desired deletion count is reached or timeout expires.
     *
     * @param time to wait.
     * @param unit of the timeout.
     */
    public void waitUntilDeleted( long time, TimeUnit unit )
    {
        try
        {
            watcher.waitUntilDeleted( time, unit );
        }
        finally
        {
            for( Watch w : watches )
            {
                w.close();
            }
        }
    }
}
