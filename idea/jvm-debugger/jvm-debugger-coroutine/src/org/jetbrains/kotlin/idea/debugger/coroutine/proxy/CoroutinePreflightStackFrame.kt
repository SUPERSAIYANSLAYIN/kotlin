/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.coroutine.proxy

import com.intellij.debugger.engine.DebugProcessImpl
import com.intellij.debugger.engine.DebuggerManagerThreadImpl
import com.intellij.debugger.engine.JVMStackFrameInfoProvider
import com.intellij.debugger.engine.SuspendManagerUtil
import com.intellij.debugger.impl.DebuggerContextImpl
import com.intellij.debugger.jdi.StackFrameProxyImpl
import com.intellij.debugger.ui.impl.watch.StackFrameDescriptorImpl
import com.intellij.xdebugger.frame.XCompositeNode
import com.intellij.xdebugger.frame.XValueChildrenList
import org.jetbrains.kotlin.idea.debugger.coroutine.data.CoroutineInfoData
import org.jetbrains.kotlin.idea.debugger.coroutine.util.isInUnitTest
import org.jetbrains.kotlin.idea.debugger.stackFrame.KotlinStackFrame

/**
 * Coroutine exit frame represented by a stack frames
 * invokeSuspend():-1
 * resumeWith()
 *
 */

class CoroutinePreflightStackFrame(
    val coroutineInfoData: CoroutineInfoData,
    val stackFrameDescriptorImpl: StackFrameDescriptorImpl,
    val threadPreCoroutineFrames: List<StackFrameProxyImpl>,
) : KotlinStackFrame(stackFrameDescriptorImpl), JVMStackFrameInfoProvider {

    override fun computeChildren(node: XCompositeNode) {
        val childrenList = XValueChildrenList()
        val firstRestoredCoroutineStackFrameItem = coroutineInfoData.stackTrace.firstOrNull() ?: return
        firstRestoredCoroutineStackFrameItem.spilledVariables.forEach {
            childrenList.add(it)
        } // firstRestoredCoroutineStackFrameItem should be skipped later on
        node.addChildren(childrenList, false)
        super.computeChildren(node)
        if (isInUnitTest()) {
            val children = XValueChildrenList()
            buildVariablesThreadAction(getFrameDebuggerContext1(), children, node)
            node.addChildren(children, true)
        }
    }

    override fun isInLibraryContent() =
        false

    override fun isSynthetic() =
        false

    fun getFrameDebuggerContext1(): DebuggerContextImpl? {
        DebuggerManagerThreadImpl.assertIsManagerThread()
        val debugProcess = descriptor.debugProcess as DebugProcessImpl
        var context = debugProcess.debuggerContext
        if (context.frameProxy !== stackFrameProxy) {
            val threadSuspendContext = SuspendManagerUtil.findContextByThread(debugProcess.suspendManager, stackFrameProxy.threadProxy())
            context = DebuggerContextImpl.createDebuggerContext(
                debugProcess.session,
                threadSuspendContext,
                stackFrameProxy.threadProxy(),
                stackFrameProxy
            )
            context.setPositionCache(descriptor.sourcePosition)
            context.initCaches()
        }
        return context
    }

}