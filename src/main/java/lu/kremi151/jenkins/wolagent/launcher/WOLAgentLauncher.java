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

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.model.TaskListener;
import hudson.plugins.sshslaves.Messages;
import hudson.plugins.sshslaves.SSHLauncher;
import hudson.plugins.sshslaves.verifiers.SshHostKeyVerificationStrategy;
import hudson.slaves.SlaveComputer;
import hudson.util.FormValidation;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class WOLAgentLauncher extends SSHLauncher {

    private static final Logger LOGGER = java.util.logging.Logger.getLogger(WOLAgentLauncher.class.getName());

    private String macAddress;
    private boolean autoSuspend;
    private boolean suspendAsSuperuser;
    private boolean ignoreSessionsOnSuspend;
    private int pingInterval = 2000;
    private int connectionTimeout = 60000;

    public WOLAgentLauncher(
            @NonNull String host,
            int port,
            String credentialsId,
            String macAddress,
            boolean autoSuspend,
            boolean suspendAsSuperuser,
            boolean ignoreSessionsOnSuspend,
            String jvmOptions,
            String javaPath,
            String prefixStartSlaveCmd,
            String suffixStartSlaveCmd,
            Integer launchTimeoutSeconds,
            Integer maxNumRetries,
            Integer retryWaitTime,
            SshHostKeyVerificationStrategy sshHostKeyVerificationStrategy
    ) {
        super(host, port, credentialsId, jvmOptions, javaPath, prefixStartSlaveCmd, suffixStartSlaveCmd, launchTimeoutSeconds, maxNumRetries, retryWaitTime, sshHostKeyVerificationStrategy);
        this.macAddress = macAddress;
        this.autoSuspend = autoSuspend;
        this.suspendAsSuperuser = suspendAsSuperuser;
        this.ignoreSessionsOnSuspend = ignoreSessionsOnSuspend;
    }

    @DataBoundConstructor
    public WOLAgentLauncher(
            @NonNull String host,
            int port,
            String credentialsId,
            String macAddress,
            boolean autoSuspend,
            boolean suspendAsSuperuser,
            boolean ignoreSessionsOnSuspend
    ) {
        super(host, port, credentialsId);
        this.macAddress = macAddress;
        this.autoSuspend = autoSuspend;
        this.suspendAsSuperuser = suspendAsSuperuser;
        this.ignoreSessionsOnSuspend = ignoreSessionsOnSuspend;
    }

    @Override
    public void launch(SlaveComputer computer, TaskListener listener) throws InterruptedException {
        Process process;
        try {
            process = Runtime.getRuntime().exec("etherwake " + this.macAddress);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        process.waitFor(5L, TimeUnit.SECONDS);

        final InetAddress address;
        try {
            address = InetAddress.getByName(this.getHost());
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }

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

        super.launch(computer, listener);
    }

    private void trySuspend(TaskListener listener) throws IOException, InterruptedException {
        if (!this.autoSuspend) {
            return;
        }
        String suspendCommand = "systemctl suspend";
        if (this.suspendAsSuperuser) {
            suspendCommand = "sudo " + suspendCommand;
        }
        if (this.ignoreSessionsOnSuspend) {
            suspendCommand = suspendCommand + " -i";
        }
        final int result = this.getConnection().exec(suspendCommand, listener.getLogger());
        if (result != 0) {
            LOGGER.log(Level.WARNING, "Could not suspend remote, error code {0}", result);
        } else {
            LOGGER.log(Level.INFO, "Remote was requested to suspend");
        }
    }

    @Override
    public void beforeDisconnect(SlaveComputer computer, TaskListener listener) {
        try {
            trySuspend(listener);
        } catch (IOException | InterruptedException e) {
            LOGGER.log(Level.WARNING, "An exception occurred while requesting suspending on remote", e);
        }
        super.beforeDisconnect(computer, listener);
    }

    @DataBoundSetter
    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }

    public String getMacAddress() {
        return this.macAddress;
    }

    @DataBoundSetter
    public void setAutoSuspend(boolean autoSuspend) {
        this.autoSuspend = autoSuspend;
    }

    public boolean isAutoSuspend() {
        return this.autoSuspend;
    }

    @DataBoundSetter
    public void setSuspendAsSuperuser(boolean suspendAsSuperuser) {
        this.suspendAsSuperuser = suspendAsSuperuser;
    }

    public boolean isSuspendAsSuperuser() {
        return suspendAsSuperuser;
    }

    @DataBoundSetter
    public void setIgnoreSessionsOnSuspend(boolean ignoreSessionsOnSuspend) {
        this.ignoreSessionsOnSuspend = ignoreSessionsOnSuspend;
    }

    public boolean isIgnoreSessionsOnSuspend() {
        return ignoreSessionsOnSuspend;
    }

    @DataBoundSetter
    public void setPingInterval(int pingInterval) {
        this.pingInterval = pingInterval;
    }

    public int getPingInterval() {
        return pingInterval;
    }

    @DataBoundSetter
    public void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    @Extension
    @Symbol({"wol", "wOLLauncher"})
    public static class DescriptorImpl extends SSHLauncher.DescriptorImpl {

        @Override
        public String getDisplayName() {
            return "Send commands over SSH, but wake it up over LAN first";
        }

        public FormValidation doCheckMacAddress(@QueryParameter String macAddress) {
            return (StringUtils.isNotBlank(macAddress) && macAddress.matches("^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$"))
                    ? FormValidation.ok()
                    : FormValidation.error(Messages.SSHLauncher_HostNotSpecified());
        }

        public FormValidation doCheckPingInterval(@QueryParameter String pingInterval) {
            return (StringUtils.isNotBlank(pingInterval) && pingInterval.matches("^[0-9]+$"))
                    ? FormValidation.ok()
                    : FormValidation.error("Expected a non-decimal number");
        }

        public FormValidation doCheckConnectionTimeout(@QueryParameter String connectionTimeout) {
            return (StringUtils.isNotBlank(connectionTimeout) && connectionTimeout.matches("^[0-9]+$"))
                    ? FormValidation.ok()
                    : FormValidation.error("Expected a non-decimal number");
        }

    }

}
