/*
 * Copyright (c) 2025, The casual project. All rights reserved.
 *
 * This software is licensed under the MIT license, https://opensource.org/licenses/MIT
 */

package se.laz.casual.test.tdk8s.controller;

import io.fabric8.kubernetes.client.KubernetesClient;
import se.laz.casual.test.tdk8s.TestKube;
import se.laz.casual.test.tdk8s.connection.KubeConnection;
import se.laz.casual.test.tdk8s.controller.connection.ConnectionController;
import se.laz.casual.test.tdk8s.controller.connection.ConnectionControllerImpl;
import se.laz.casual.test.tdk8s.controller.connection.NetworkController;
import se.laz.casual.test.tdk8s.controller.connection.NetworkControllerImpl;
import se.laz.casual.test.tdk8s.controller.exec.ExecController;
import se.laz.casual.test.tdk8s.controller.exec.ExecControllerImpl;
import se.laz.casual.test.tdk8s.controller.logging.LogController;
import se.laz.casual.test.tdk8s.controller.logging.LogControllerImpl;
import se.laz.casual.test.tdk8s.controller.provisioning.ProvisioningController;
import se.laz.casual.test.tdk8s.controller.provisioning.ProvisioningControllerImpl;
import se.laz.casual.test.tdk8s.controller.provisioning.ProvisioningProbeController;
import se.laz.casual.test.tdk8s.controller.provisioning.ProvisioningProbeControllerImpl;
import se.laz.casual.test.tdk8s.controller.provisioning.ResourceLookupController;
import se.laz.casual.test.tdk8s.controller.provisioning.ResourceLookupControllerImpl;
import se.laz.casual.test.tdk8s.controller.runtime.RuntimeController;
import se.laz.casual.test.tdk8s.controller.runtime.RuntimeControllerImpl;
import se.laz.casual.test.tdk8s.controller.transfer.FileTransferController;
import se.laz.casual.test.tdk8s.controller.transfer.FileTransferControllerImpl;
import se.laz.casual.test.tdk8s.exec.ExecResult;
import se.laz.casual.test.tdk8s.store.ResourcesStore;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

/**
 * Controller facade, delegating to more specialised controllers to perform the actual operations.
 */
public class KubeController implements ProvisioningController, ConnectionController, ExecController, LogController, FileTransferController
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

    @Override
    public void init()
    {
        provisioningController.init();
    }

    @Override
    public void initAsync()
    {
        provisioningController.initAsync();
    }

    @Override
    public void waitUntilReady()
    {
        provisioningController.waitUntilReady();
    }

    @Override
    public void destroy()
    {
        provisioningController.destroy();
    }

    @Override
    public void destroyAsync()
    {
        provisioningController.destroyAsync();
    }

    @Override
    public void waitUntilDestroyed()
    {
        provisioningController.waitUntilDestroyed();
    }

    @Override
    public void scale( String resource, int replicas )
    {
        provisioningController.scale( resource, replicas );
    }

    @Override
    public CompletableFuture<Void> scaleAsync( String resource, int replicas )
    {
        return provisioningController.scaleAsync( resource, replicas );
    }

    // ConnectionController

    @Override
    public KubeConnection getConnection( String service, int targetPort )
    {
        return connectionController.getConnection( service, targetPort );
    }

    @Override
    public KubeConnection getPortForwardConnection( String resource, int targetPort )
    {
        return connectionController.getPortForwardConnection( resource, targetPort );
    }

    // Execution Controller

    @Override
    public ExecResult executeCommand( String resource, String... command )
    {
        return this.execController.executeCommand( resource, command );
    }

    @Override
    public CompletableFuture<ExecResult> executeCommandAsync( String resource, String... command )
    {
        return this.execController.executeCommandAsync( resource, command );
    }

    // Log Controller

    @Override
    public String getLog( String resource )
    {
        return this.logController.getLog( resource );
    }

    @Override
    public String getLogTail( String resource, int lines )
    {
        return this.logController.getLogTail( resource, lines );
    }

    @Override
    public String getLogSince( String resource, String sinceTime )
    {
        return this.logController.getLogSince( resource, sinceTime );
    }

    // File Transfer Controller

    @Override
    public boolean download( String resource, String source, Path destination )
    {
        return this.fileTransferController.download( resource, source, destination );
    }

    @Override
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
        private TestKube testKube;
        private KubernetesClient client;
        private ResourcesStore store;
        private String label;

        private ResourceLookupController resourceLookupController;
        private NetworkController networkController;
        private RuntimeController runtimeController;
        private ProvisioningProbeController provisioningProbeController;
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

        public Builder testKube( TestKube testKube )
        {
            this.testKube = testKube;
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
            initProvisioningProbeController();

            initProvisioningController();
            initConnectionController();
            initExecController();
            initLogController();
            initFileTransferController();
        }

        private void initRuntimeController()
        {
            this.runtimeController = new RuntimeControllerImpl();
        }

        private void initNetworkController()
        {
            this.networkController = new NetworkControllerImpl();
        }

        private void initResourceLookupController()
        {
            this.resourceLookupController = new ResourceLookupControllerImpl( client, store );
        }

        private void initProvisioningProbeController()
        {
            this.provisioningProbeController = new ProvisioningProbeControllerImpl( testKube );
        }

        private void initProvisioningController()
        {
            if( this.provisioningController == null )
            {
                this.provisioningController = new ProvisioningControllerImpl( provisioningProbeController, client, store, resourceLookupController, label );
            }
        }

        private void initConnectionController()
        {
            if( this.connectionController == null )
            {
                this.connectionController = new ConnectionControllerImpl( resourceLookupController, networkController, runtimeController );
            }
        }

        private void initExecController()
        {
            if( this.execController == null )
            {
                this.execController = new ExecControllerImpl( resourceLookupController );
            }
        }

        private void initLogController()
        {
            if( this.logController == null )
            {
                this.logController = new LogControllerImpl( resourceLookupController );
            }
        }

        private void initFileTransferController()
        {
            if( this.fileTransferController == null )
            {
                this.fileTransferController = new FileTransferControllerImpl( resourceLookupController );
            }
        }
    }
}
