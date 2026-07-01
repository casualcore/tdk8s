/*
 * Copyright (c) 2025, The casual project. All rights reserved.
 *
 * This software is licensed under the MIT license, https://opensource.org/licenses/MIT
 */

package se.laz.casual.test.tdk8s.watchers;

import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.WatcherException;
import se.laz.casual.test.tdk8s.TestKubeException;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static java.lang.System.Logger.Level.DEBUG;

public class DeleteWatcher<T> implements Watcher<T>
{
    private static final System.Logger logger = System.getLogger( DeleteWatcher.class.getName() );

    private final CountDownLatch deleteLatch;

    public DeleteWatcher()
    {
        this( 1 );
    }

    public DeleteWatcher( int count )
    {
        deleteLatch = new CountDownLatch( count );
    }

    @Override
    public void eventReceived( Action action, T resource )
    {
        if( action == Action.DELETED )
        {
            deleteLatch.countDown();
            logger.log( DEBUG, () -> deleteLatch.getCount() + " delete(s) remaining, after delete observed for: " + resource.toString() );
        }
    }

    @Override
    public void onClose( WatcherException cause )
    {
        throw new TestKubeException( "Watch closed exceptional.", cause );
    }

    public void waitUntilDeleted()
    {
        try
        {
            logger.log( DEBUG, () -> "Waiting for " + deleteLatch.getCount() + " deletions." );
            deleteLatch.await();
        }
        catch( InterruptedException e )
        {
            Thread.currentThread().interrupt();
            throw new TestKubeException( e );
        }
    }

    public boolean waitUntilDeleted( long timeout, TimeUnit unit )
    {
        try
        {
            logger.log( DEBUG, () -> "Waiting for " + deleteLatch.getCount() + " deletions, with timeout." );
            return deleteLatch.await( timeout, unit );
        }
        catch( InterruptedException e )
        {
            Thread.currentThread().interrupt();
            throw new TestKubeException( e );
        }
    }
}
