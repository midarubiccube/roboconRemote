package com.example.ros2_android_test_app /* Copyright 2017 Esteve Fernandez <esteve@apache.org>
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

import android.os.Bundle
import android.os.Handler
import androidx.activity.ComponentActivity
import org.ros2.rcljava.RCLJava
import org.ros2.rcljava.executors.Executor
import org.ros2.rcljava.executors.SingleThreadedExecutor
import java.util.Timer
import java.util.TimerTask

open class ROSActivity : ComponentActivity() {
    lateinit var executor: Executor
    lateinit var timer: Timer
    lateinit var handler: Handler

    private val SPINNER_PERIOD_MS : Long = 200
    private val SPINNER_DELAY : Long  = 0
    protected val ROS_DOMAIN_ID = 123

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.handler = Handler(mainLooper)
        RCLJava.rclJavaInit(ROS_DOMAIN_ID)
        this.executor = this.createExecutor()
    }

    override fun onResume() {
        super.onResume()
        timer = Timer()
        timer.schedule(
            object : TimerTask() {
                override fun run() {
                    val runnable = Runnable { executor.spinSome() }
                    handler.post(runnable)
            }
        }, SPINNER_DELAY, SPINNER_PERIOD_MS)
    }

    override fun onPause() {
        super.onPause()
        timer.cancel()
    }

    protected fun createExecutor(): Executor {
        return SingleThreadedExecutor()
    }
}