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

package lu.kremi151.jenkins.wolagent.util;

import hudson.slaves.ComputerLauncher;
import lu.kremi151.jenkins.wolagent.launcher.WOLLauncher;
import org.apache.commons.lang.StringUtils;

import javax.annotation.Nullable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.logging.Logger;

public class HostHelper {

    private static final Logger LOGGER = java.util.logging.Logger.getLogger(HostHelper.class.getName());

    @Nullable
    private static Class<? extends ComputerLauncher> sshLauncherClass = null;

    private static boolean initialized = false;

    private static synchronized void ensureInitialized() {
        if (initialized) {
            return;
        }
        sshLauncherClass = tryLoadClass("hudson.plugins.sshslaves.SSHLauncher");
        initialized = true;
    }

    @SuppressWarnings("unchecked")
    @Nullable
    private static <T> Class<T> tryLoadClass(String name) {
        try {
            return (Class<T>) Class.forName(name);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    @Nullable
    public static String tryInferHost(@Nullable ComputerLauncher launcher) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        if (launcher == null) {
            return null;
        }
        ensureInitialized();
        if (launcher instanceof WOLLauncher) {
            launcher = WOLLauncher.unpackLauncher(launcher);
        }
        if (sshLauncherClass != null && sshLauncherClass.isAssignableFrom(launcher.getClass())) {
            return tryInferSSHLauncherHost(launcher);
        }
        return null;
    }

    @Nullable
    private static String tryInferSSHLauncherHost(ComputerLauncher launcher) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        ensureInitialized();
        if (sshLauncherClass == null) {
            return null;
        }
        Method hostGetter = sshLauncherClass.getDeclaredMethod("getHost");
        hostGetter.setAccessible(true);
        return (String) hostGetter.invoke(launcher);
    }

    public static boolean isIpAddress(@Nullable String ipAddr) {
        if (StringUtils.isBlank(ipAddr)) {
            return false;
        }
        return ipAddr.matches("^(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])$");
    }

    @Nullable
    public static String tryGuessBroadcastIp(@Nullable String host) throws UnknownHostException {
        if (StringUtils.isBlank(host)) {
            return null;
        }
        if (!isIpAddress(host)) {
            InetAddress address = InetAddress.getByName(host);
            host = address.getHostAddress();
        }
        if (!isIpAddress(host)) {
            return null;
        }
        String[] parts = host.split("\\.");
        return parts[0] + "." + parts[1] + "." + parts[2] + ".255";
    }

}
