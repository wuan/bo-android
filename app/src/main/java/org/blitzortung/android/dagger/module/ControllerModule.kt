/*

   Copyright 2015 Andreas Würl

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

*/

package org.blitzortung.android.dagger.module

import dagger.Binds
import dagger.Module
import org.blitzortung.android.app.controller.NotificationHandler
import org.blitzortung.android.protocol.AlertNotifier

@Module
abstract class ControllerModule {
    @Binds
    abstract fun bindAlertNotifier(impl: NotificationHandler): AlertNotifier
}
