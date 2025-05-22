/*
 * Copyright (c) 2025, The casual project. All rights reserved.
 *
 * This software is licensed under the MIT license, https://opensource.org/licenses/MIT
 */

package se.laz.casual.test.tdk8s.watcher

import io.fabric8.kubernetes.api.model.Pod
import io.fabric8.kubernetes.client.Watcher
import io.fabric8.kubernetes.client.WatcherException
import se.laz.casual.test.tdk8s.TestKubeException
import se.laz.casual.test.tdk8s.watchers.DeleteWatcher
import spock.lang.Specification

import java.util.concurrent.CompletableFuture
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutionException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

class DeleteWatcherTest extends Specification
{
    DeleteWatcher<Pod> instance

    def setup()
    {
        instance = new DeleteWatcher<>()
    }

    def "Wait for delete."()
    {
        given:
        CountDownLatch complete = new CountDownLatch(1 )

        when:
        CompletableFuture<Void> future = CompletableFuture.supplyAsync( ()->{ instance.waitUntilDeleted(); complete.countDown(  ) } )
        instance.eventReceived( Watcher.Action.ADDED, Mock( Pod ) )
        complete.await( 1, TimeUnit.MILLISECONDS )

        then:
        !future.isDone(  )

        when:
        instance.eventReceived( Watcher.Action.DELETED, Mock( Pod ) )
        complete.await()

        then:
        future.isDone()
    }

    def "Wait for delete multiple."()
    {
        given:
        int count = 2
        instance = new DeleteWatcher<>( count )
        CountDownLatch complete = new CountDownLatch( 1 )

        when:
        CompletableFuture<Void> future = CompletableFuture.supplyAsync( ()->{ instance.waitUntilDeleted(); complete.countDown(  ) } )
        for( int i=0; i<count; i++ )
        {
            instance.eventReceived( Watcher.Action.DELETED, Mock( Pod ) )
        }
        complete.await()

        then:
        future.isDone()
    }

    def "Wait for delete with timeout, false."()
    {
        given:
        CountDownLatch complete = new CountDownLatch(1 )

        when:
        CompletableFuture<Boolean> future = CompletableFuture.supplyAsync( ()->{
            Boolean result = instance.waitUntilDeleted(1, TimeUnit.MILLISECONDS)
            complete.countDown(  )
            return result
        } )

        complete.await()

        then:
        !future.get()
    }

    def "Wait for delete with timeout, true."()
    {
        given:
        CountDownLatch started = new CountDownLatch( 1 )
        CountDownLatch start = new CountDownLatch( 1 )
        CountDownLatch complete = new CountDownLatch(1 )

        when:
        CompletableFuture<Boolean> task = CompletableFuture.supplyAsync(   ()-> {
            started.countDown(  )
            start.await()
            Boolean result = instance.waitUntilDeleted(1, TimeUnit.MILLISECONDS)
            complete.countDown(  )
            return result
        })

        and:
        started.await()
        instance.eventReceived( Watcher.Action.DELETED, Mock( Pod ) )
        start.countDown(  )
        complete.await()

        Boolean result = task.get( 1, TimeUnit.SECONDS)

        then:
        result
    }

    def "Watch closed exceptionally, throws TestKubeException."()
    {
        given:
        WatcherException t = new WatcherException( "Fake" )

        when:
        instance.onClose( t )

        then:
        TestKubeException actual = thrown()
        actual.getCause(  ) == t
    }

    def "Wait for delete, interrupted."()
    {
        given:
        ExecutorService executor = Executors.newSingleThreadExecutor()
        CountDownLatch start = new CountDownLatch( 1 )

        when:
        Future<?> task = executor.submit( ()-> {
            start.countDown(  )
            if( timeout == null )
            {
                instance.waitUntilDeleted()
            }
            else
            {
                instance.waitUntilDeleted( timeout, unit )
            }
        })
        start.await()
        executor.shutdownNow(  )
        assert executor.awaitTermination( 1, TimeUnit.SECONDS )
        unwrapFuture( task )

        then:
        thrown TestKubeException

        where:
        timeout | unit
        null    | null
        5       | TimeUnit.SECONDS
    }


    <T>T unwrapFuture( Future<T> future )
    {
        try
        {
            return future.get()
        }
        catch( ExecutionException e )
        {
            throw e.getCause(  )
        }
    }

}
