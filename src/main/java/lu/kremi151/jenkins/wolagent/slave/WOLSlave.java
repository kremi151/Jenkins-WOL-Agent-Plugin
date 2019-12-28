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
import hudson.model.Computer;
import hudson.model.Slave;
import hudson.slaves.ComputerLauncher;
import hudson.slaves.JNLPLauncher;
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
import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class WOLSlave extends Slave implements Serializable {

    private static final Logger LOGGER = java.util.logging.Logger.getLogger(WOLSlave.class.getName());

    private String macAddress;
    private String broadcastIP;

    private ComputerLauncher launcher;

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
            String broadcastIP,
            int pingInterval,
            int connectionTimeout,
            boolean autoSuspend,
            boolean suspendAsSuperuser,
            boolean ignoreSessionsOnSuspend
    ) throws Descriptor.FormException, IOException {
        super(name, remoteFS, null);
        this.macAddress = macAddress;
        this.broadcastIP = broadcastIP;
        this.pingInterval = pingInterval;
        this.connectionTimeout = connectionTimeout;
        this.autoSuspend = autoSuspend;
        this.suspendAsSuperuser = suspendAsSuperuser;
        this.ignoreSessionsOnSuspend = ignoreSessionsOnSuspend;

        launcher = WOLLauncher.unpackLauncher(launcher);
        LOGGER.log(Level.INFO, "Construct delegate launcher of type " + (launcher == null ? "null" : launcher.getClass()));
        this.launcher = ensureNotNullWithDefault(launcher);
    }

    @Override
    public ComputerLauncher getLauncher() {
        LOGGER.log(Level.INFO, "Get launcher");
        ComputerLauncher launcher = WOLLauncher.unpackLauncher(this.launcher);
        return ensureNotNullWithDefault(launcher);
    }

    @Override
    @DataBoundSetter
    public void setLauncher(ComputerLauncher launcher) {
        launcher = WOLLauncher.unpackLauncher(launcher);
        LOGGER.log(Level.INFO, "Set launcher of type " + (launcher == null ? "null" : launcher.getClass()));
        this.launcher = ensureNotNullWithDefault(launcher);
    }

    @DataBoundSetter
    public void setMacAddress(String macAddress) {
        LOGGER.log(Level.INFO, "Set mac address to {0}", macAddress);
        this.macAddress = macAddress;
    }

    public String getMacAddress() {
        return macAddress;
    }

    @DataBoundSetter
    public void setAutoSuspend(boolean autoSuspend) {
        LOGGER.log(Level.INFO, "Set auto suspend to {0}", autoSuspend);
        this.autoSuspend = autoSuspend;
    }

    public boolean isAutoSuspend() {
        return autoSuspend;
    }

    @DataBoundSetter
    public void setSuspendAsSuperuser(boolean suspendAsSuperuser) {
        LOGGER.log(Level.INFO, "Set suspend as superuser to {0}", suspendAsSuperuser);
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
        LOGGER.log(Level.INFO, "Set ping interval to {0}", pingInterval);
        this.pingInterval = pingInterval;
    }

    public int getPingInterval() {
        return pingInterval;
    }

    @DataBoundSetter
    public void setConnectionTimeout(int connectionTimeout) {
        LOGGER.log(Level.INFO, "Set connection timeout to {0}", connectionTimeout);
        this.connectionTimeout = connectionTimeout;
    }

    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    public String getBroadcastIP() {
        return broadcastIP;
    }

    @DataBoundSetter
    public void setBroadcastIP(String broadcastIP) {
        this.broadcastIP = broadcastIP;
    }

    static ComputerLauncher ensureNotNullWithDefault(@Nullable ComputerLauncher launcher) {
        if (launcher != null) {
            return launcher;
        }
        // This is the most simply launcher to configure, so we use it as a fallback
        return new JNLPLauncher(false);
    }

    @Override
    public Computer createComputer() {
        LOGGER.log(Level.INFO, "Create WOLSlave computer with name {0} and type {1}", new Object[]{name, launcher});
        return new WOLSlaveComputer(this);
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

        public FormValidation doCheckBroadcastIP(@QueryParameter String broadcastIP) {
            if (StringUtils.isBlank(broadcastIP) || !broadcastIP.matches("^(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])$")) {
                return FormValidation.error(Messages.WOLSlave_InvalidIPAddress());
            }
            return FormValidation.ok();
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

    }
}
