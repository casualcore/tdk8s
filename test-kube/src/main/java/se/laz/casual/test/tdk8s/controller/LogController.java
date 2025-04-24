/*
 * Copyright (c) 2025, The casual project. All rights reserved.
 *
 * This software is licensed under the MIT license, https://opensource.org/licenses/MIT
 */

package se.laz.casual.test.tdk8s.controller;

public interface LogController
{
    /**
     * Retrieves the full log for the pod named.
     * </br>
     * The name can be either the alias for the managed resource or
     * the actual underlying name of the resource inside the cluster.
     *
     * @param pod from which to retrieve the log.
     * @return the log.
     */
    String getLog( String pod );

    /**
     * Retrieve the number of lines from the tail of the log for pod named.
     * </br>
     * The name can be either the alias for the managed resource or
     * the actual underlying name of the resource inside the cluster.
     *
     * @param pod from which to retrieve the log.
     * @param lines number of lines to retrieve from the tail of the log.
     * @return tail lines of the log.
     */
    String getLogTail( String pod, int lines );

    /**
     * Retrieves the log lines since the provided time (RFC3339) for the pod named.
     * </br>
     * The name can be either the alias for the managed resource or
     * the actual underlying name of the resource inside the cluster.
     * <br/>
     * Example: Get lines from log after provided date.
     * <pre>
     * {@code
     * ZoneDateTime afterInit = ZonedDateTime.now();
     * String sinceTime = afterInit.format( DateTimeFormatter.ISO_OFFSET_DATE_TIME );
     * String log = instance.getController().getLogSince( podName, sinceTime );
     * }
     * </pre>
     *
     * @param pod from which to retrieve the log.
     * @param sinceTime timestamp as string.
     * @return filtered log.
     */
    String getLogSince( String pod, String sinceTime );
}
