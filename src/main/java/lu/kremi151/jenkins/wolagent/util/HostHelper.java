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

/**
 * Utility class for interacting with the SSH Slaves Plugin
 * (see https://github.com/jenkinsci/ssh-slaves-plugin) and for WOL specific helper methods.
 */
public final class HostHelper {

    @Nullable
    private static Class<? extends ComputerLauncher> sshLauncherClass = null;

    private static boolean initialized = false;

    /**
     * Internal dummy constructor for this utility class.
     * This documentation mainly exists to shut checkstyle.
     */
    private HostHelper() {
    }

    /**
     * Initializes {@link HostHelper}, if not already done.
     * This method will try to access the SSH Slaves plugin if loaded.
     */
    private static synchronized void ensureInitialized() {
        if (initialized) {
            return;
        }
        sshLauncherClass = tryLoadClass("hudson.plugins.sshslaves.SSHLauncher");
        initialized = true;
    }

    /**
     * Tries to load a Java class via reflection.
     * On failure, it will gracefully return {@code null}.
     * @param name The fully qualified class name.
     * @param <T> The base type of the class to load.
     * @return The class if loaded, or {@code null} otherwise.
     */
    @SuppressWarnings("unchecked")
    @Nullable
    private static <T> Class<T> tryLoadClass(final String name) {
        try {
            return (Class<T>) Class.forName(name);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    /**
     * Tries to read the hostname of the given {@link ComputerLauncher}, if it comes from the
     * SSH Slaves plugin.
     * This method supports launchers wrapped in a {@link WOLLauncher}.
     * @param launcher The {@link ComputerLauncher} to extract the information from.
     * @return If the given {@link ComputerLauncher} comes from the SSH Slaves plugin, the
     *         hostname.
     *         If not, {@code null}.
     * @throws NoSuchMethodException     In case of a reflection error.
     * @throws IllegalAccessException    In case of a reflection error.
     * @throws InvocationTargetException In case of a reflection error.
     */
    @Nullable
    public static String tryInferHost(@Nullable final ComputerLauncher launcher)
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        ComputerLauncher unpacked = launcher;
        if (unpacked == null) {
            return null;
        }
        ensureInitialized();
        if (unpacked instanceof WOLLauncher) {
            unpacked = WOLLauncher.unpackLauncher(unpacked);
            if (unpacked == null) {
                return null;
            }
        }
        if (sshLauncherClass != null && sshLauncherClass.isAssignableFrom(unpacked.getClass())) {
            return tryInferSSHLauncherHost(unpacked);
        }
        return null;
    }

    /**
     * Tries to read the hostname of the given {@link ComputerLauncher}, if it comes from the
     * SSH Slaves plugin.
     * @param launcher The {@link ComputerLauncher} to extract the information from.
     * @return If the given {@link ComputerLauncher} comes from the SSH Slaves plugin, the
     *         hostname.
     *         If not, {@code null}.
     * @throws NoSuchMethodException     In case of a reflection error.
     * @throws InvocationTargetException In case of a reflection error.
     * @throws IllegalAccessException    In case of a reflection error.
     */
    @Nullable
    private static String tryInferSSHLauncherHost(final ComputerLauncher launcher)
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        ensureInitialized();
        if (sshLauncherClass == null) {
            return null;
        }
        Method hostGetter = sshLauncherClass.getDeclaredMethod("getHost");
        hostGetter.setAccessible(true);
        return (String) hostGetter.invoke(launcher);
    }

    /**
     * Checks whether the given string is a valid IP address.
     * @param ipAddr The string to check.
     * @return {@code true} if valid, {@code false} if invalid.
     */
    public static boolean isIpAddress(@Nullable final String ipAddr) {
        if (StringUtils.isBlank(ipAddr)) {
            return false;
        }
        return ipAddr.matches("^(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.){3}"
                        + "([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])$");
    }

    /**
     * Tries to guess the broadcast IP address based on the given hostname.
     * @param host The hostname.
     * @return The broadcast IP address if it could be guessed, {@code null} otherwise.
     * @throws UnknownHostException If no IP address for the hostname could be found.
     */
    @Nullable
    public static String tryGuessBroadcastIp(@Nullable final String host)
            throws UnknownHostException {
        String inHost = host;
        if (StringUtils.isBlank(inHost)) {
            return null;
        }
        if (!isIpAddress(inHost)) {
            InetAddress address = InetAddress.getByName(inHost);
            inHost = address.getHostAddress();
        }
        if (!isIpAddress(inHost)) {
            return null;
        }
        String[] parts = inHost.split("\\.");
        return parts[0] + "." + parts[1] + "." + parts[2] + ".255";
    }

}
