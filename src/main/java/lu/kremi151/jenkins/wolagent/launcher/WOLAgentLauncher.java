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

public class WOLAgentLauncher extends SSHLauncher {

    private String macAddress;

    public WOLAgentLauncher(
            @NonNull String host,
            int port,
            String credentialsId,
            String macAddress,
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
    }

    @DataBoundConstructor
    public WOLAgentLauncher(@NonNull String host, int port, String credentialsId, String macAddress) {
        super(host, port, credentialsId);
        this.macAddress = macAddress;
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
                        if (address.isReachable(2000)) {
                            break;
                        }
                        Thread.sleep(2000L);
                    } catch (IOException | InterruptedException e) {}
                }
            });
            future.get(60L, TimeUnit.SECONDS);
        } catch (ExecutionException | TimeoutException e) {
            throw new RuntimeException(e);
        } finally {
            if (!executorService.isTerminated()) {
                executorService.shutdown();
            }
        }

        super.launch(computer, listener);
    }

    @DataBoundSetter
    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }

    public String getMacAddress() {
        return this.macAddress;
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

    }

}
