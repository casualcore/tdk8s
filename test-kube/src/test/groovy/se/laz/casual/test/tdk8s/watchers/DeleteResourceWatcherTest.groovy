/*
 * Copyright (c) 2025, The casual project. All rights reserved.
 *
 * This software is licensed under the MIT license, https://opensource.org/licenses/MIT
 */

package se.laz.casual.test.tdk8s.watchers

import io.fabric8.kubernetes.api.model.Pod
import io.fabric8.kubernetes.client.dsl.PodResource
import spock.lang.Specification

class DeleteResourceWatcherTest extends Specification
{

    def "Create with null watcher throws NullPointerException."()
    {
        when:
        new DeleteResourceWatcher<Pod>( null, Collections.emptyList(  ) )

        then:
        thrown NullPointerException
    }

    def "Create with null watcher throws NullPointerException."()
    {
        given:
        PodResource p = null

        when:
        new DeleteResourceWatcher<Pod>( new DeleteWatcher<Pod>(  ), p as PodResource )

        then:
        thrown NullPointerException
    }

    def "Create with null watcher throws NullPointerException."()
    {
        given:
        List<PodResource> p = null

        when:
        new DeleteResourceWatcher<Pod>( new DeleteWatcher<Pod>(  ), p as List )

        then:
        thrown NullPointerException
    }

}
