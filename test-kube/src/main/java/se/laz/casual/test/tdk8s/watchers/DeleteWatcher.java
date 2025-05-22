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
import java.util.logging.Logger;

public class DeleteWatcher<T> implements Watcher<T>
{
    Logger log = Logger.getLogger(DeleteWatcher.class.getName());

    private final CountDownLatch deleteLatch;

    public DeleteWatcher( )
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
            log.finest( ()-> "Deleted." );
            deleteLatch.countDown();
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
            return deleteLatch.await( timeout, unit );
        }
        catch( InterruptedException e )
        {
            Thread.currentThread().interrupt();
            throw new TestKubeException( e );
        }
    }
}
