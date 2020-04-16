/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend.generators

import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.backend.Fir2IrComponents
import org.jetbrains.kotlin.fir.backend.FirMetadataSource
import org.jetbrains.kotlin.fir.backend.declareThisReceiverParameter
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.builder.buildSimpleFunction
import org.jetbrains.kotlin.fir.declarations.builder.buildValueParameter
import org.jetbrains.kotlin.fir.declarations.impl.FirDeclarationStatusImpl
import org.jetbrains.kotlin.fir.symbols.CallableId
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.fir.types.impl.FirImplicitBooleanTypeRef
import org.jetbrains.kotlin.fir.types.impl.FirImplicitIntTypeRef
import org.jetbrains.kotlin.fir.types.impl.FirImplicitNullableAnyTypeRef
import org.jetbrains.kotlin.fir.types.impl.FirImplicitStringTypeRef
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.IrGeneratorContextBase
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrValueParameterImpl
import org.jetbrains.kotlin.ir.descriptors.WrappedSimpleFunctionDescriptor
import org.jetbrains.kotlin.ir.descriptors.WrappedValueParameterDescriptor
import org.jetbrains.kotlin.ir.expressions.IrMemberAccessExpression
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.DataClassMembersGenerator
import org.jetbrains.kotlin.ir.util.classId
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.name.Name

class DataClassMembersGenerator(val components: Fir2IrComponents) {

    // TODO: generateInlineClassMembers

    fun generateDataClassMembers(irClass: IrClass, firClass: FirClass<*>): List<Name> =
        MyDataClassMethodsGenerator(irClass, firClass, IrDeclarationOrigin.GENERATED_DATA_CLASS_MEMBER).generate()

    private inner class MyDataClassMethodsGenerator(
        val irClass: IrClass,
        val firClass: FirClass<*>,
        val origin: IrDeclarationOrigin
    ) {
        val properties = irClass.declarations.filterIsInstance<IrProperty>().map { it.descriptor }

        private val irDataClassMembersGenerator = object : DataClassMembersGenerator(
            IrGeneratorContextBase(components.irBuiltIns),
            components.symbolTable,
            irClass,
            origin
        ) {
            override fun declareSimpleFunction(startOffset: Int, endOffset: Int, functionDescriptor: FunctionDescriptor): IrFunction {
                throw IllegalStateException("Not expect to see function declaration.")
            }

            override fun generateSyntheticFunctionParameterDeclarations(irFunction: IrFunction) {
                // TODO
            }

            override fun commitSubstituted(irMemberAccessExpression: IrMemberAccessExpression, descriptor: CallableDescriptor) {
                // TODO
            }
        }

        fun generateDispatchReceiverParameter(irFunction: IrFunction, valueParameterDescriptor: WrappedValueParameterDescriptor) =
            irFunction.declareThisReceiverParameter(
                components.symbolTable,
                irClass.defaultType,
                origin,
                UNDEFINED_OFFSET,
                UNDEFINED_OFFSET
            ).apply {
                valueParameterDescriptor.bind(this)
            }

        fun generate(): List<Name> {
            if (properties.isEmpty()) {
                return emptyList()
            }

            // TODO: consolidate componentN() and copy(...) too?

            // TODO: generate equals, hashCode, and toString only if needed
            val equalsFunction = createSyntheticIrFunction(
                Name.identifier("equals"),
                components.irBuiltIns.booleanType,
            ) { createSyntheticIrParameter(it, Name.identifier("other"), components.irBuiltIns.anyNType) }
            irDataClassMembersGenerator.generateEqualsMethod(equalsFunction, properties)

            val hashCodeFunction = createSyntheticIrFunction(
                Name.identifier("hashCode"),
                components.irBuiltIns.intType,
            )
            irDataClassMembersGenerator.generateHashCodeMethod(hashCodeFunction, properties)

            val toStringFunction = createSyntheticIrFunction(
                Name.identifier("toString"),
                components.irBuiltIns.stringType,
            )
            irDataClassMembersGenerator.generateToStringMethod(toStringFunction, properties)

            return listOf(equalsFunction.name, hashCodeFunction.name, toStringFunction.name)
        }

        private fun createSyntheticIrFunction(
            name: Name,
            returnType: IrType,
            valueParameterBuilder: (IrFunction) -> IrValueParameter? = { null }
        ): IrFunction {
            val functionDescriptor = WrappedSimpleFunctionDescriptor()
            val thisReceiverDescriptor = WrappedValueParameterDescriptor()
            return components.symbolTable.declareSimpleFunction(UNDEFINED_OFFSET, UNDEFINED_OFFSET, origin, functionDescriptor) { symbol ->
                IrFunctionImpl(
                    UNDEFINED_OFFSET,
                    UNDEFINED_OFFSET,
                    origin,
                    symbol,
                    name,
                    Visibilities.PUBLIC,
                    Modality.OPEN,
                    returnType,
                    isInline = false,
                    isExternal = false,
                    isTailrec = false,
                    isSuspend = false,
                    isExpect = false,
                    isFakeOverride = false,
                    isOperator = false
                ).apply {
                    val irValueParameter = valueParameterBuilder(this)?.let {
                        this.valueParameters = listOf(it)
                        it
                    }
                    metadata = FirMetadataSource.Function(
                        buildSimpleFunction {
                            this.name = name
                            val classId = firClass.symbol.classId
                            this.symbol = FirNamedFunctionSymbol(CallableId(classId.packageFqName, classId.relativeClassName, name))
                            this.status = FirDeclarationStatusImpl(Visibilities.PUBLIC, Modality.FINAL)
                            this.session = components.session
                            this.returnTypeRef = when (returnType) {
                                components.irBuiltIns.booleanType -> FirImplicitBooleanTypeRef(null)
                                components.irBuiltIns.intType -> FirImplicitIntTypeRef(null)
                                components.irBuiltIns.stringType -> FirImplicitStringTypeRef(null)
                                else -> throw AssertionError("Should not be here")
                            }
                            if (irValueParameter != null) {
                                this.valueParameters.add(
                                    buildValueParameter {
                                        this.name = irValueParameter.name
                                        this.session = components.session
                                        this.returnTypeRef = FirImplicitNullableAnyTypeRef(null)
                                        this.symbol = FirVariableSymbol(irValueParameter.name)
                                        isCrossinline = false
                                        isNoinline = false
                                        isVararg = false
                                    }
                                )
                            }
                        },
                        descriptor
                    )

                }
            }.apply {
                parent = irClass
                functionDescriptor.bind(this)
                dispatchReceiverParameter = generateDispatchReceiverParameter(this, thisReceiverDescriptor)
            }
        }

        private fun createSyntheticIrParameter(irFunction: IrFunction, name: Name, type: IrType): IrValueParameter {
            val descriptor = WrappedValueParameterDescriptor()
            return components.symbolTable.declareValueParameter(
                UNDEFINED_OFFSET,
                UNDEFINED_OFFSET,
                origin,
                descriptor,
                type
            ) { symbol ->
                IrValueParameterImpl(
                    UNDEFINED_OFFSET,
                    UNDEFINED_OFFSET,
                    origin,
                    symbol,
                    name,
                    0,
                    type,
                    null,
                    isCrossinline = false,
                    isNoinline = false
                )
            }.apply {
                parent = irFunction
                descriptor.bind(this)
            }
        }
    }
}