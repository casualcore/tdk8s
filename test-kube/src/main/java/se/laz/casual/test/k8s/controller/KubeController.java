/*
 * Copyright (c) 2025, The casual project. All rights reserved.
 *
 * This software is licensed under the MIT license, https://opensource.org/licenses/MIT
 */

package se.laz.casual.test.k8s.controller;

import io.fabric8.kubernetes.client.KubernetesClient;
import se.laz.casual.test.k8s.connection.KubeConnection;
import se.laz.casual.test.k8s.exec.ExecResult;
import se.laz.casual.test.k8s.store.ResourcesStore;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

public class KubeController
{
    private final ProvisioningController provisioningController;
    private final ConnectionController connectionController;
    private final ExecController execController;
    private final LogController logController;
    private final FileTransferController fileTransferController;

    public KubeController( Builder builder )
    {
        this.provisioningController = builder.provisioningController;
        this.connectionController = builder.connectionController;
        this.execController = builder.execController;
        this.logController = builder.logController;
        this.fileTransferController = builder.fileTransferController;
    }

    // Provisioning Controller

    public void init()
    {
        provisioningController.init();
    }

    public void initAsync()
    {
        provisioningController.initAsync();
    }

    public void waitUntilReady()
    {
        provisioningController.waitUntilReady();
    }

    public void destroy()
    {
        provisioningController.destroy();
    }

    public void destroyAsync()
    {
        provisioningController.destroyAsync();
    }

    public void waitUntilDestroyed()
    {
        provisioningController.waitUntilDestroyed();
    }

    // ConnectionController

    public KubeConnection getConnection( String resource, int targetPort )
    {
        return connectionController.getConnection( resource, targetPort );
    }

    public KubeConnection getPortForwardConnection( String resource, int port )
    {
        return connectionController.getPortForwardConnection( resource, port );
    }

    // Execution Controller

    public ExecResult executeCommand( String pod, String... command )
    {
        return this.execController.executeCommand( pod, command );
    }

    public CompletableFuture<ExecResult> executeCommandAsync( String pod, String... command )
    {
        return this.execController.executeCommandAsync( pod, command );
    }

    // Log Controller

    public String getLog( String pod )
    {
        return this.logController.getLog( pod );
    }

    public String getLogTail( String pod, int lines )
    {
        return this.logController.getLogTail( pod, lines );
    }

    public String getLogSince( String pod, String sinceTime )
    {
        return this.logController.getLogSince( pod, sinceTime );
    }

    // File Transfer Controller

    public boolean download( String pod, String source, Path destination )
    {
        return this.fileTransferController.download( pod, source, destination );
    }

    public boolean upload( String pod, String source, Path destination )
    {
        return this.fileTransferController.upload( pod, source, destination );
    }

    public static Builder newBuilder()
    {
        return new Builder();
    }

    public static final class Builder
    {
        private KubernetesClient client;
        private ResourcesStore store;
        private String label;

        private ResourceLookupController resourceLookupController;
        private NetworkController networkController;
        private RuntimeController runtimeController;
        private ProvisioningController provisioningController;
        private ConnectionController connectionController;
        private ExecController execController;
        private LogController logController;
        private FileTransferController fileTransferController;

        private Builder()
        {
        }

        public Builder client( KubernetesClient client )
        {
            this.client = client;
            return this;
        }

        public Builder resourcesStore( ResourcesStore store )
        {
            this.store = store;
            return this;
        }

        public Builder label( String label )
        {
            this.label = label;
            return this;
        }

        Builder provisioningController( ProvisioningController provisioningController )
        {
            this.provisioningController = provisioningController;
            return this;
        }

        Builder connectionController( ConnectionController connectionController )
        {
            this.connectionController = connectionController;
            return this;
        }

        Builder execController( ExecController execController )
        {
            this.execController = execController;
            return this;
        }

        Builder logController( LogController logController )
        {
            this.logController = logController;
            return this;
        }

        Builder fileTransferController( FileTransferController fileTransferController )
        {
            this.fileTransferController = fileTransferController;
            return this;
        }

        public KubeController build()
        {
            initControllers();

            return new KubeController( this );
        }

        private void initControllers()
        {
            initRuntimeController();
            initNetworkController();
            initResourceLookupController();

            initProvisioningController();
            initConnectionController();
            initExecController();
            initLogController();
            initFileTransferController();
        }

        private void initRuntimeController()
        {
            this.runtimeController = new RuntimeController();
        }

        private void initNetworkController()
        {
            this.networkController = new NetworkController();
        }

        private void initResourceLookupController()
        {
            this.resourceLookupController = new ResourceLookupController( client, store );
        }

        private void initProvisioningController()
        {
            if( this.provisioningController == null )
            {
                this.provisioningController = new ProvisioningController( client, store, label );
            }
        }

        private void initConnectionController()
        {
            if( this.connectionController == null )
            {
                this.connectionController = new ConnectionController( resourceLookupController, networkController, runtimeController );
            }
        }

        private void initExecController()
        {
            if( this.execController == null )
            {
                this.execController = new ExecController( resourceLookupController );
            }
        }

        private void initLogController()
        {
            if( this.logController == null )
            {
                this.logController = new LogController( resourceLookupController );
            }
        }

        private void initFileTransferController()
        {
            if( this.fileTransferController == null )
            {
                this.fileTransferController = new FileTransferController( resourceLookupController );
            }
        }
    }
}
