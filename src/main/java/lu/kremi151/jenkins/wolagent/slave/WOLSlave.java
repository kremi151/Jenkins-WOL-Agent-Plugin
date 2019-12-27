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

package lu.kremi151.jenkins.wolagent.slave;

import hudson.Extension;
import hudson.Functions;
import hudson.model.Slave;
import hudson.slaves.ComputerLauncher;
import hudson.util.FormValidation;
import lu.kremi151.jenkins.wolagent.Messages;
import lu.kremi151.jenkins.wolagent.launcher.WOLLauncher;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class WOLSlave extends Slave {

    private final WOLLauncher wolLauncher;

    private String macAddress;
    private boolean autoSuspend;
    private boolean suspendAsSuperuser;
    private boolean ignoreSessionsOnSuspend;
    private int pingInterval = 2000;
    private int connectionTimeout = 60000;

    @DataBoundConstructor
    public WOLSlave(
            @Nonnull String name,
            String remoteFS,
            ComputerLauncher launcher,
            String macAddress,
            boolean autoSuspend,
            boolean suspendAsSuperuser,
            boolean ignoreSessionsOnSuspend
    ) throws Descriptor.FormException, IOException {
        super(name, remoteFS, launcher);
        this.wolLauncher = new WOLLauncher(launcher, this);
        this.macAddress = macAddress;
        this.autoSuspend = autoSuspend;
        this.suspendAsSuperuser = suspendAsSuperuser;
        this.ignoreSessionsOnSuspend = ignoreSessionsOnSuspend;
    }

    @Override
    public ComputerLauncher getLauncher() {
        return wolLauncher;
    }

    @Override
    public void setLauncher(ComputerLauncher launcher) {
        super.setLauncher(launcher);
        wolLauncher.setLauncher(launcher);
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
    @Symbol("wolSlave")
    public static class Descriptor extends SlaveDescriptor {

        @Override
        public String getDisplayName() {
            return Messages.WOLSlave_SlaveDescription();
        }

        @Override
        public boolean isInstantiable() {
            return true;
        }

        public FormValidation doCheckMacAddress(@QueryParameter String macAddress) {
            return (StringUtils.isNotBlank(macAddress) && macAddress.matches("^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$"))
                    ? FormValidation.ok()
                    : FormValidation.error(hudson.plugins.sshslaves.Messages.SSHLauncher_HostNotSpecified());
        }

        @Nullable
        private String validatePositiveIntegerInput(String str) {
            if (StringUtils.isBlank(str) || !str.matches("^[0-9]+$")) {
                return Messages.WOLAgent_InputMustBeInteger();
            }
            try {
                int number = Integer.parseInt(str);
                if (number <= 0) {
                    return Messages.WOLAgent_InputNumberMustBeStrictlyPositive();
                }
            } catch (NumberFormatException e) {
                return Messages.WOLAgent_InputMustBeInteger();
            }
            return null;
        }

        public FormValidation doCheckPingInterval(@QueryParameter String pingInterval) {
            String errorMessage = validatePositiveIntegerInput(pingInterval);
            return errorMessage == null
                    ? FormValidation.ok()
                    : FormValidation.error(errorMessage);
        }

        public FormValidation doCheckConnectionTimeout(@QueryParameter String connectionTimeout) {
            String errorMessage = validatePositiveIntegerInput(connectionTimeout);
            return errorMessage == null
                    ? FormValidation.ok()
                    : FormValidation.error(errorMessage);
        }

        public List<hudson.model.Descriptor<ComputerLauncher>> getComputerLauncherDescriptors() {
            return Functions.getComputerLauncherDescriptors()
                    .stream()
                    .filter(descriptor -> !WOLLauncher.DESCRIPTOR.getClass().isAssignableFrom(descriptor.getClass()))
                    .collect(Collectors.toList());
        }

    }
}
