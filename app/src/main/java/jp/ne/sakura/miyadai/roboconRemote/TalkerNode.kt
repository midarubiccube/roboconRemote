package com.example.ros2_android_test_app

import android.util.Log
import geometry_msgs.msg.Vector3
import org.ros2.rcljava.node.BaseComposableNode
import org.ros2.rcljava.publisher.Publisher
import org.ros2.rcljava.timer.WallTimer
import java.util.concurrent.TimeUnit

class TalkerNode(name: String, private val topic: String) : BaseComposableNode(name) {
    var publisher: Publisher<geometry_msgs.msg.Twist> = node.createPublisher(
        geometry_msgs.msg.Twist::class.java, this.topic
    )
    fun publish(msg : geometry_msgs.msg.Twist) {
        publisher.publish(msg)
    }
    companion object {
        private val logtag: String = TalkerNode::class.java.name
    }
}