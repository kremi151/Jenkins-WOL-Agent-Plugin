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

import javax.annotation.Nullable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.logging.Logger;

public class HostHelper {

    private static final Logger LOGGER = java.util.logging.Logger.getLogger(HostHelper.class.getName());

    @Nullable
    private static final Class<? extends ComputerLauncher> sshLauncherClass;

    static {
        sshLauncherClass = tryLoadClass("hudson.plugins.sshslaves.SSHLauncher");
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
        if (sshLauncherClass != null && sshLauncherClass.isAssignableFrom(launcher.getClass())) {
            return tryInferSSHLauncherHost(launcher);
        }
        return null;
    }

    @Nullable
    private static String tryInferSSHLauncherHost(ComputerLauncher launcher) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        if (sshLauncherClass == null) {
            return null;
        }
        Method hostGetter = sshLauncherClass.getDeclaredMethod("getHost");
        hostGetter.setAccessible(true);
        return (String) hostGetter.invoke(launcher);
    }

}
