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
import hudson.model.Computer;
import hudson.model.Slave;
import hudson.slaves.ComputerLauncher;
import hudson.slaves.JNLPLauncher;
import hudson.slaves.RetentionStrategy;
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
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public final class WOLSlave extends Slave {

    private static final Logger LOGGER = java.util.logging.Logger.getLogger(WOLSlave.class.getName());

    private transient WOLLauncher wolLauncher;

    private String macAddress;

    private int pingInterval;
    private int connectionTimeout;

    private boolean autoSuspend;
    private boolean suspendAsSuperuser;
    private boolean ignoreSessionsOnSuspend;

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
        super(name, remoteFS, ensureNotNullWithDefault(launcher));
        this.macAddress = macAddress;
        this.pingInterval = pingInterval;
        this.connectionTimeout = connectionTimeout;
        this.autoSuspend = autoSuspend;
        this.suspendAsSuperuser = suspendAsSuperuser;
        this.ignoreSessionsOnSuspend = ignoreSessionsOnSuspend;

        // Unpack WOLLauncher until we reach the base delegate launcher
        while (launcher != null && launcher.getClass() == WOLLauncher.class) {
            launcher = ((WOLLauncher) launcher).getLauncher();
        }
        LOGGER.log(Level.INFO, "Construct delegate launcher of type " + (launcher == null ? "null" : launcher.getClass()));
        this.getWolLauncher(launcher).setLauncher(launcher);
    }

    private WOLLauncher getWolLauncher(@Nullable ComputerLauncher launcher) {
        if (this.wolLauncher == null) {
            LOGGER.log(Level.INFO, "Init delegate launcher of type " + (launcher == null ? "null" : launcher.getClass()));
            this.wolLauncher = new WOLLauncher(
                    ensureNotNullWithDefault(launcher),
                    macAddress,
                    pingInterval,
                    connectionTimeout,
                    autoSuspend,
                    suspendAsSuperuser,
                    ignoreSessionsOnSuspend
            );
        }
        return this.wolLauncher;
    }

    @Override
    public Computer createComputer() {
        LOGGER.log(Level.INFO, "Create WOLSlave computer with name {0} and type {1}", new Object[]{name, getDelegateLauncher()});
        return super.createComputer();
    }

    public ComputerLauncher getDelegateLauncher() {
        return getWolLauncher(null).getLauncher();
    }

    @Override
    public ComputerLauncher getLauncher() {
        return getWolLauncher(null);
    }

    @Override
    @DataBoundSetter
    public void setLauncher(ComputerLauncher launcher) {
        super.setLauncher(launcher);
        // Unpack WOLLauncher until we reach the base delegate launcher
        while (launcher != null && launcher.getClass() == WOLLauncher.class) {
            launcher = ((WOLLauncher) launcher).getLauncher();
        }
        LOGGER.log(Level.INFO, "Set delegate launcher of type " + (launcher == null ? "null" : launcher.getClass()));
        getWolLauncher(launcher).setLauncher(ensureNotNullWithDefault(launcher));
    }

    @DataBoundSetter
    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
        this.getWolLauncher(null).setMacAddress(macAddress);
    }

    public String getMacAddress() {
        return macAddress;
    }

    @DataBoundSetter
    public void setAutoSuspend(boolean autoSuspend) {
        this.autoSuspend = autoSuspend;
        this.getWolLauncher(null).setAutoSuspend(autoSuspend);
    }

    public boolean isAutoSuspend() {
        return autoSuspend;
    }

    @DataBoundSetter
    public void setSuspendAsSuperuser(boolean suspendAsSuperuser) {
        this.suspendAsSuperuser = suspendAsSuperuser;
        this.getWolLauncher(null).setSuspendAsSuperuser(suspendAsSuperuser);
    }

    public boolean isSuspendAsSuperuser() {
        return suspendAsSuperuser;
    }

    @DataBoundSetter
    public void setIgnoreSessionsOnSuspend(boolean ignoreSessionsOnSuspend) {
        this.ignoreSessionsOnSuspend = ignoreSessionsOnSuspend;
        this.getWolLauncher(null).setIgnoreSessionsOnSuspend(ignoreSessionsOnSuspend);
    }

    public boolean isIgnoreSessionsOnSuspend() {
        return ignoreSessionsOnSuspend;
    }

    @DataBoundSetter
    public void setPingInterval(int pingInterval) {
        this.pingInterval = pingInterval;
        this.getWolLauncher(null).setPingInterval(pingInterval);
    }

    public int getPingInterval() {
        return pingInterval;
    }

    @DataBoundSetter
    public void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
        this.getWolLauncher(null).setConnectionTimeout(connectionTimeout);
    }

    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    private static ComputerLauncher ensureNotNullWithDefault(@Nullable ComputerLauncher launcher) {
        if (launcher != null) {
            return launcher;
        }
        // This is the most simply launcher to configure, so we use it as a fallback
        return new JNLPLauncher(false);
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

        public List<hudson.model.Descriptor<RetentionStrategy<?>>> getRetentionStrategyDescriptors() {
            return Functions.getRetentionStrategyDescriptors();
        }

    }
}
