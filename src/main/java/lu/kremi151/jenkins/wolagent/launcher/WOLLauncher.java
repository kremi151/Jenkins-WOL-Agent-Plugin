/*
 * Copyright 2019 Michel Kremer (kremi151)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package lu.kremi151.jenkins.wolagent.launcher;

import hudson.Extension;
import hudson.model.Descriptor;
import hudson.model.TaskListener;
import hudson.remoting.Channel;
import hudson.slaves.ComputerLauncher;
import hudson.slaves.DelegatingComputerLauncher;
import hudson.slaves.SlaveComputer;
import jline.internal.Nullable;
import lu.kremi151.jenkins.wolagent.remoting.callables.Suspend;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import java.io.IOException;
import java.net.InetAddress;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class WOLLauncher extends DelegatingComputerLauncher {

    private static final Logger LOGGER = java.util.logging.Logger.getLogger(WOLLauncher.class.getName());

    private transient String macAddress;

    private transient int pingInterval;
    private transient int connectionTimeout;

    private transient boolean autoSuspend;
    private transient boolean suspendAsSuperuser;
    private transient boolean ignoreSessionsOnSuspend;

    @DataBoundConstructor
    public WOLLauncher(ComputerLauncher launcher) {
        super(launcher);
    }

    public WOLLauncher(
            ComputerLauncher launcher,
            String macAddress,
            int pingInterval,
            int connectionTimeout,
            boolean autoSuspend,
            boolean suspendAsSuperuser,
            boolean ignoreSessionsOnSuspend
    ) {
        this(launcher);
        this.macAddress = macAddress;
        this.pingInterval = pingInterval;
        this.connectionTimeout = connectionTimeout;
        this.autoSuspend = autoSuspend;
        this.suspendAsSuperuser = suspendAsSuperuser;
        this.ignoreSessionsOnSuspend = ignoreSessionsOnSuspend;
    }

    @DataBoundSetter
    public void setLauncher(ComputerLauncher launcher) {
        this.launcher = launcher;
    }

    private void ping(@Nullable String host) throws InterruptedException, IOException {
        if (host == null) {
            // No host specified, so we apply a cooldown of 5 seconds
            Thread.sleep(5000L);
            return;
        }
        final InetAddress address = InetAddress.getByName(host);

        final ExecutorService executorService = Executors.newSingleThreadExecutor();
        try {
            final Future future = executorService.submit(() -> {
                while (true) {
                    try {
                        if (address.isReachable(pingInterval)) {
                            break;
                        }
                        Thread.sleep((long) pingInterval);
                    } catch (IOException | InterruptedException e) {}
                }
            });
            future.get(connectionTimeout, TimeUnit.MILLISECONDS);
        } catch (ExecutionException | TimeoutException e) {
            throw new RuntimeException(e);
        } finally {
            if (!executorService.isTerminated()) {
                executorService.shutdown();
            }
        }
    }

    @Override
    public void launch(SlaveComputer computer, TaskListener listener) throws IOException, InterruptedException {
        Process process = Runtime.getRuntime().exec("etherwake " + macAddress);
        process.waitFor(5L, TimeUnit.SECONDS);

        //TODO: Find a way to ping the host
        ping(null);

        super.launch(computer, listener);
    }

    private void trySuspend(SlaveComputer computer, TaskListener listener) throws IOException, InterruptedException {
        if (!autoSuspend) {
            return;
        }
        final Channel channel = computer.getChannel();
        if (channel == null) {
            listener.error("Cannot send suspend command, channel is null");
            return;
        }
        channel.call(new Suspend(suspendAsSuperuser, ignoreSessionsOnSuspend));
    }

    @Override
    public void beforeDisconnect(SlaveComputer computer, TaskListener listener) {
        try {
            trySuspend(computer, listener);
        } catch (IOException | InterruptedException e) {
            LOGGER.log(Level.WARNING, "An exception occurred while requesting suspending on remote", e);
        }
        super.beforeDisconnect(computer, listener);
    }

    public boolean isAutoSuspend() {
        return autoSuspend;
    }

    public void setAutoSuspend(boolean autoSuspend) {
        this.autoSuspend = autoSuspend;
    }

    public boolean isSuspendAsSuperuser() {
        return suspendAsSuperuser;
    }

    public void setSuspendAsSuperuser(boolean suspendAsSuperuser) {
        this.suspendAsSuperuser = suspendAsSuperuser;
    }

    public boolean isIgnoreSessionsOnSuspend() {
        return ignoreSessionsOnSuspend;
    }

    public void setIgnoreSessionsOnSuspend(boolean ignoreSessionsOnSuspend) {
        this.ignoreSessionsOnSuspend = ignoreSessionsOnSuspend;
    }

    public int getPingInterval() {
        return pingInterval;
    }

    public void setPingInterval(int pingInterval) {
        this.pingInterval = pingInterval;
    }

    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    public void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<ComputerLauncher> {

        @Override
        public String getDisplayName() {
            return null;
        }

        public boolean isApplicable(Class<?> targetType) {
            return false;
        }

        public boolean isApplicable() {
            return false;
        }

    }
}
