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

package lu.kremi151.jenkins.wolagent;

import org.jvnet.localizer.ResourceBundleHolder;

public class Messages {

    private final static ResourceBundleHolder holder = ResourceBundleHolder.get(Messages.class);

    public static String WOLLauncher_AgentDescription() {
        return holder.format("WOLAgent.AgentDescription");
    }

    public static String WOLLauncher_ExpectedPositiveInteger() {
        return holder.format("WOLAgent.ExpectedPositiveInteger");
    }

}
