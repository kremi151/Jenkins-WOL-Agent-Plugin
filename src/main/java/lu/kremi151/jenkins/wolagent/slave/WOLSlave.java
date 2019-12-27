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

    private transient final WOLLauncher wolLauncher;

    @DataBoundConstructor
    public WOLSlave(
            @Nonnull String name,
            String remoteFS,
            ComputerLauncher launcher,
            String macAddress,
            int pingInterval,
            int connectionTimeout,
            boolean autoSuspend,
            boolean suspendAsSuperuser,
            boolean ignoreSessionsOnSuspend
    ) throws Descriptor.FormException, IOException {
        super(name, remoteFS, launcher);
        this.wolLauncher = new WOLLauncher(
                launcher,
                macAddress,
                pingInterval,
                connectionTimeout,
                autoSuspend,
                suspendAsSuperuser,
                ignoreSessionsOnSuspend
        );
    }

    public ComputerLauncher getDelegateLauncher() {
        return wolLauncher.getLauncher();
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
        this.wolLauncher.setMacAddress(macAddress);
    }

    public String getMacAddress() {
        return this.wolLauncher.getMacAddress();
    }

    @DataBoundSetter
    public void setAutoSuspend(boolean autoSuspend) {
        this.wolLauncher.setAutoSuspend(autoSuspend);
    }

    public boolean isAutoSuspend() {
        return this.wolLauncher.isAutoSuspend();
    }

    @DataBoundSetter
    public void setSuspendAsSuperuser(boolean suspendAsSuperuser) {
        this.wolLauncher.setSuspendAsSuperuser(suspendAsSuperuser);
    }

    public boolean isSuspendAsSuperuser() {
        return wolLauncher.isSuspendAsSuperuser();
    }

    @DataBoundSetter
    public void setIgnoreSessionsOnSuspend(boolean ignoreSessionsOnSuspend) {
        this.wolLauncher.setIgnoreSessionsOnSuspend(ignoreSessionsOnSuspend);
    }

    public boolean isIgnoreSessionsOnSuspend() {
        return wolLauncher.isIgnoreSessionsOnSuspend();
    }

    @DataBoundSetter
    public void setPingInterval(int pingInterval) {
        this.wolLauncher.setPingInterval(pingInterval);
    }

    public int getPingInterval() {
        return wolLauncher.getPingInterval();
    }

    @DataBoundSetter
    public void setConnectionTimeout(int connectionTimeout) {
        this.wolLauncher.setConnectionTimeout(connectionTimeout);
    }

    public int getConnectionTimeout() {
        return wolLauncher.getConnectionTimeout();
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
                    : FormValidation.error(Messages.WOLSlave_InvalidMACAddress());
        }

        @Nullable
        private String validatePositiveIntegerInput(String str) {
            if (StringUtils.isBlank(str) || !str.matches("^[0-9]+$")) {
                return Messages.WOLSlave_InputMustBeInteger();
            }
            try {
                int number = Integer.parseInt(str);
                if (number <= 0) {
                    return Messages.WOLSlave_InputNumberMustBeStrictlyPositive();
                }
            } catch (NumberFormatException e) {
                return Messages.WOLSlave_InputMustBeInteger();
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
                    .filter(descriptor -> !WOLLauncher.DescriptorImpl.class.isAssignableFrom(descriptor.getClass()))
                    .collect(Collectors.toList());
        }

    }
}
