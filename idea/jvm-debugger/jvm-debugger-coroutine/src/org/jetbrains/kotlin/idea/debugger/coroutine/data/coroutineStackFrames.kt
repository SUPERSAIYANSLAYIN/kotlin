/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.idea.debugger.coroutine.data

import com.intellij.debugger.engine.DebugProcessImpl
import com.intellij.debugger.jdi.StackFrameProxyImpl
import com.intellij.debugger.memory.utils.StackFrameItem
import com.intellij.ui.ColoredTextContainer
import com.intellij.ui.SimpleTextAttributes
import com.intellij.xdebugger.frame.XCompositeNode
import org.jetbrains.kotlin.idea.debugger.coroutine.KotlinDebuggerCoroutinesBundle
import org.jetbrains.kotlin.idea.debugger.coroutine.proxy.SkipCoroutineStackFrameProxyImpl
import org.jetbrains.kotlin.idea.debugger.coroutine.util.coroutineDebuggerTraceEnabled
import org.jetbrains.kotlin.idea.debugger.invokeInManagerThread


class CreationCoroutineStackFrame(debugProcess: DebugProcessImpl, item: StackFrameItem) : CoroutineStackFrame(debugProcess, item) {
    override fun getCaptionAboveOf() = KotlinDebuggerCoroutinesBundle.message("coroutine.dump.creation.trace")

    override fun hasSeparatorAbove(): Boolean =
        true
}

/**
 * Acts as a joint frame, take variables from restored frame and information from the real 'exit' frame.
 */
class PreCoroutineStackFrame(val frame: StackFrameProxyImpl, val debugProcess: DebugProcessImpl, item: StackFrameItem) :
    CoroutineStackFrame(debugProcess, item) {
    override fun computeChildren(node: XCompositeNode) {
        debugProcess.invokeInManagerThread {
            val skipCoroutineFrame = SkipCoroutineStackFrameProxyImpl(frame)
            debugProcess.positionManager.createStackFrame(skipCoroutineFrame, debugProcess, frame.location())
                ?.computeChildren(node) // hack but works
        }
        super.computeChildren(node)
    }
}

open class CoroutineStackFrame(debugProcess: DebugProcessImpl, val item: StackFrameItem) :
    StackFrameItem.CapturedStackFrame(debugProcess, item) {
    override fun customizePresentation(component: ColoredTextContainer) {
        if (coroutineDebuggerTraceEnabled())
            component.append("${item.javaClass.simpleName} / ${this.javaClass.simpleName} ", SimpleTextAttributes.GRAYED_ATTRIBUTES)
        super.customizePresentation(component)
    }

    override fun getCaptionAboveOf() = "CoroutineExit"

    override fun hasSeparatorAbove(): Boolean =
        false
}
