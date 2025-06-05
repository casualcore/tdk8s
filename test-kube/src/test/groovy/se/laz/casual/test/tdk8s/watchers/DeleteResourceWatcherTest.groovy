/*
 * Copyright (c) 2025, The casual project. All rights reserved.
 *
 * This software is licensed under the MIT license, https://opensource.org/licenses/MIT
 */

package se.laz.casual.test.tdk8s.watchers

import io.fabric8.kubernetes.api.model.Pod
import io.fabric8.kubernetes.client.Watch
import io.fabric8.kubernetes.client.Watcher
import io.fabric8.kubernetes.client.dsl.PodResource
import spock.lang.Specification

import java.util.concurrent.TimeUnit

class DeleteResourceWatcherTest extends Specification
{

    def "Create with null resource throws NullPointerException."()
    {
        given:
        PodResource p = null

        when:
        new DeleteResourceWatcher<Pod>( p as PodResource )

        then:
        thrown NullPointerException
    }

    def "Create with null list throws NullPointerException."()
    {
        given:
        List<PodResource> p = null

        when:
        new DeleteResourceWatcher<Pod>( p as List )

        then:
        thrown NullPointerException
    }

    def "Create with null list and desired count throws NullPointerException."()
    {
        when:
        new DeleteResourceWatcher<Pod>( null, 1 )

        then:
        thrown NullPointerException
    }

    def "Delete wait until ready."()
    {
        given:
        Pod pod = Mock()
        PodResource resource = Mock()
        mockWatchDeletePodResource( resource, pod )

        when:
        DeleteResourceWatcher<Pod> instance = new DeleteResourceWatcher<>( resource )
        instance.waitUntilDeleted( )

        then:
        noExceptionThrown()
    }

    def "Delete wait until ready with timeout."()
    {
        given:
        Pod pod = Mock()
        PodResource resource = Mock()
        mockWatchDeletePodResource( resource, pod )

        when:
        DeleteResourceWatcher<Pod> instance = new DeleteResourceWatcher<>( resource )
        instance.waitUntilDeleted( 1, TimeUnit.SECONDS )

        then:
        noExceptionThrown()
    }

    PodResource mockWatchDeletePodResource( PodResource resource, Pod pod )
    {
        Watch watch = Mock()
        1* resource.watch( _ ) >> { Watcher watcher ->
            watcher.eventReceived( Watcher.Action.DELETED, pod )
            return watch
        }
        1* watch.close()
        return resource
    }

}
