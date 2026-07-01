/*
 * Copyright (c) 2025, The casual project. All rights reserved.
 *
 * This software is licensed under the MIT license, https://opensource.org/licenses/MIT
 */

package se.laz.casual.test.tdk8s.controller.logging;

import se.laz.casual.test.tdk8s.store.ResourceNotFoundException;

public interface LogController
{
    /**
     * Retrieves the full log for the resource.
     * <p>
     * The resource name can be either the alias for the managed resource
     * or the actual underlying name of the resource inside the cluster.
     * </p>
     * <p>
     * Note: If the resource has multiple pods associated the first is used.
     * </p>
     *
     * @param resource from which to retrieve the log.
     * @return the log.
     * @throws ResourceNotFoundException if the resource was not found.
     */
    String getLog( String resource );

    /**
     * Retrieve the number of lines from the tail of the log for the resource.
     * <p>
     * The resource name can be either the alias for the managed resource
     * or the actual underlying name of the resource inside the cluster.
     * </p>
     * <p>
     * Note: If the resource has multiple pods associated the first is used.
     * </p>
     *
     * @param resource from which to retrieve the log.
     * @param lines    number of lines to retrieve from the tail of the log.
     * @return tail lines of the log.
     * @throws ResourceNotFoundException if the resource was not found.
     */
    String getLogTail( String resource, int lines );

    /**
     * Retrieves the log lines since the provided time (RFC3339) for the resource.
     * <p>
     * The resource name can be either the alias for the managed resource
     * or the actual underlying name of the resource inside the cluster.
     * </p>
     * <p>
     * Note: If the resource has multiple pods associated the first is used.
     * </p>
     * <p>
     * Example: Get lines from log after provided date.
     * <pre>
     * {@code
     *      ZoneDateTime afterInit = ZonedDateTime.now();
     *      String sinceTime = afterInit.format( DateTimeFormatter.ISO_OFFSET_DATE_TIME );
     *      String log = instance.getController().getLogSince( podName, sinceTime );
     *      }
     * </pre>
     * </p>
     *
     * @param resource  from which to retrieve the log.
     * @param sinceTime timestamp as string.
     * @return filtered log.
     * @throws ResourceNotFoundException if the resource was not found.
     */
    String getLogSince( String resource, String sinceTime );
}
