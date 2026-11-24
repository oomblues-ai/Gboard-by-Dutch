package patches.gboard.features.sharedmodel

import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patcher.util.smali.ExternalLabel
import patches.gboard.Constants.COMPATIBILITY_GBOARD
import patches.gboard.shared.*
import patches.gboard.shared.accesspoint.gboardAccessPointContributions1803Patch
import patches.gboard.features.manualincognito.GboardManualIncognitoTargets
import patches.gboard.shared.runtimeabi.*

internal val gboardSharedModelPatch = bytecodePatch(
    description = "Add the optional shared local model backend through Gboard 18.8's complete native writing interface.",
) {
    compatibleWith(COMPATIBILITY_GBOARD)
    dependsOn(gboardPatchesExtensionCarrierPatch,gboardAccessPointContributions1803Patch)
    execute {
        GboardSharedWritingTargets.preflight(this)
        installSharedEditorBridge(this)
        GboardMethodTarget("Lpdq;","fk",emptyList(),"V").resolve(this).addInstructions(0,RuntimeCallEmitter.invoke(RuntimeCallId.SHARED_EDITOR_INVALIDATE,""))
        val nativeKeyboard="Lcom/google/android/apps/inputmethod/libs/jarvis/JarvisKeyboard;"
        GboardMethodTarget(nativeKeyboard,"ep",listOf("Lcom/google/android/libraries/inputmethod/widgets/SoftKeyboardView;","Lcom/google/android/libraries/inputmethod/metadata/KeyboardViewDef;"),"V").resolve(this).addInstructions(0,RuntimeCallEmitter.invoke(RuntimeCallId.SHARED_KEYBOARD_SURFACE_BIND,"p0 .. p1"))
        GboardMethodTarget(nativeKeyboard,"f",emptyList(),"V").resolve(this).addInstructions(0,RuntimeCallEmitter.invoke(RuntimeCallId.SHARED_KEYBOARD_SURFACE_CLEAR,""))
        applySharedRequestDispatch(this)
        findMutableMethodOrThrow(GboardSharedWritingTargets.readiness).applySharedFactory(this,RuntimeCallId.SHARED_WRITING_READINESS, "")
        findMutableMethodOrThrow(GboardSharedWritingTargets.v2Factory).applySharedFactory(this,RuntimeCallId.SHARED_WRITING_CREATE_V2,"p0 .. p1")
        // Preserve adtl.d: pdq.al casts its result to the original adtk wrapper.
        // Shared dispatch replaces the underlying request handler, never this native wrapper.
        findMutableMethodOrThrow(GboardMethodTarget("Lafae;","onUpdateSelection",List(6){"I"},"V")).addInstructions(0,RuntimeCallEmitter.invoke(RuntimeCallId.SHARED_WRITING_SELECTION_UPDATED,"p0 .. p6"))
        findMutableMethodOrThrow(GboardManualIncognitoTargets.onStartInput).addInstructions(0,RuntimeCallEmitter.invoke(RuntimeCallId.SHARED_WRITING_INPUT_START,"p0 .. p1"))
        findMutableMethodOrThrow(GboardManualIncognitoTargets.onWindowHidden).applyVoidExitLifecycleDelegate(RuntimeCallId.SHARED_WRITING_INPUT_HIDDEN,"p0")
    }
}

internal object GboardSharedWritingTargets {
    val v2Factory=GboardMethodTarget("Lpzy;","a",listOf("Landroid/content/Context;","Lagrh;"),"Ladvg;")
    val readiness=GboardMethodTarget("Ladtl;","a",emptyList(),"Ladsd;")
    val nativeWrapperFactory=GboardMethodTarget("Ladtl;","d",listOf("Lagrh;"),"Ladvh;")
    fun preflight(context: BytecodePatchContext) {
        v2Factory.resolve(context);nativeWrapperFactory.resolve(context);readiness.resolve(context)
        // Enter through the same native request as the original Writing Tools access point.
        GboardMethodTarget("Lpve;","f",listOf("Ladbd;","Laicj;","Z","Laeaj;","Ljava/util/function/Consumer;"),"V").resolve(context)
        GboardMethodTarget("Laicj;","d",listOf("Lavgf;"),"Laicj;").resolve(context)
        GboardMethodTarget("Lafae;","bT",listOf("Ladan;"),"V").resolve(context)
        GboardFieldTarget("Ladbd;","d","Ladbd;").resolve(context)
        GboardMethodTarget("Ladan;","e",listOf("I","Ljava/lang/Object;"),"Ladan;").resolve(context)
        GboardFieldTarget("Lpva;","a","Lagoo;").resolve(context)
        check(context.mutableClass("Ladvg;").interfaces.toSet()==setOf("Ljava/lang/AutoCloseable;","Lzwg;")) { "Writing handler parent interface drift" }
        listOf(
            GboardMethodTarget("Lavvx;","b",listOf("Ljava/lang/Runnable;","Ljava/util/concurrent/Executor;"),"V"),
            GboardMethodTarget("Lzwg;","j",emptyList(),"Ljava/lang/String;"),
            GboardMethodTarget("Ladvh;","s",listOf("Landroid/content/Context;"),"V"),
            GboardMethodTarget("Laeak;","b",emptyList(),"Laeai;"),
            GboardMethodTarget("Laeai;","a",emptyList(),"Laeak;"),
            GboardMethodTarget("Laeai;","i",listOf("Ljava/lang/String;"),"V"),
            GboardMethodTarget("Laeai;","d",listOf("I"),"V"),
            GboardMethodTarget("Laeai;","g",listOf("D"),"V"),
            GboardMethodTarget("Laeai;","h",listOf("Ljava/lang/String;"),"V"),
            GboardMethodTarget("Laeai;","f",listOf("Ljava/lang/String;"),"V"),
            GboardMethodTarget("Laeai;","b",listOf("Laeaf;"),"V"),
            GboardMethodTarget("Laeai;","c",listOf("Laeal;"),"V"),
            GboardMethodTarget("Laeai;","k",listOf("Lj$/util/Optional;"),"V"),
            GboardMethodTarget("Laeal;","values",emptyList(),"[Laeal;"),
            GboardMethodTarget("Laeaj;","values",emptyList(),"[Laeaj;"),
            GboardMethodTarget("Lauaz;","q",listOf("Ljava/lang/Object;"),"Lauaz;"),
            GboardMethodTarget("Ladvj;","c",emptyList(),"Ljava/lang/String;"),
            GboardMethodTarget("Ladvj;","d",emptyList(),"I"),
        ).forEach{it.resolve(context)}
        listOf(GboardFieldTarget("Laeaf;","a","Laeaf;"),GboardFieldTarget("Ladsd;","a","Ladsd;"),GboardFieldTarget("Ladsd;","b","Ladsd;")).forEach{it.resolve(context)}
        check(context.mutableClass("Laeal;").superclass=="Ljava/lang/Enum;"){"Native error enum representation drift"}
        check(context.mutableClass("Ladvh;").interfaces.toSet()==setOf("Ladvg;")){"Prepared writing handler parent drift"}
        val native=(context.mutableClass("Ladvg;").methods+context.mutableClass("Ladvh;").methods).filter{it.name!="<clinit>"}.map{it.name to (it.parameterTypes.map(CharSequence::toString) to it.returnType)}.toSet()
        val adapter=context.mutableClass("LMorpheSharedWritingHandler188;").methods.map{it.name to (it.parameterTypes.map(CharSequence::toString) to it.returnType)}.toSet()
        check(adapter.containsAll(native)){"Shared handler omits a native method"}
        check("Lavvx;" in context.mutableClass("LMorpheSharedFuture188;").interfaces)
    }
}

internal fun MutableMethod.applySharedFactory(context:BytecodePatchContext,call: RuntimeCallId,registers: String) {
    val original=implementation?.instructions?.firstOrNull()?:error("Factory has no instructions")
    val parameterWords=parameterTypes.size + if (accessFlags and 8 == 0)1 else 0
    val abi=RuntimeAbiCatalog.abi(call)
    if(implementation!!.registerCount==parameterWords){
        val publicName=name
        val stockName="morpheSharedStock_"+publicName
        val owner=definingClass
        val parameters=parameterTypes.map(CharSequence::toString)
        check(context.mutableClass(owner).methods.none{it.name==stockName}){"Stock factory wrapper already exists"}
        val flags=accessFlags
        name=stockName
        val args=if(parameterWords==0)"" else "p0 .. p${parameterWords-1}"
        val invokeKind=if(flags and 8 != 0)"invoke-static" else if(flags and 2 != 0)"invoke-direct" else "invoke-virtual"
        val stockInvoke=if(parameterWords==0)"$invokeKind {}, $owner->$stockName(${parameters.joinToString("")})$returnType"
            else "$invokeKind/range {$args}, $owner->$stockName(${parameters.joinToString("")})$returnType"
        context.addHelperMethodIfMissing(owner,publicName,parameters,returnType,flags,parameterWords+1,"""
            ${RuntimeCallEmitter.invoke(call,registers)}
            move-result-object v0
            if-nez v0, :done
            $stockInvoke
            move-result-object v0
            :done
            return-object v0
        """)
        return
    }
    check(implementation!!.registerCount>parameterWords){"Factory register layout is invalid"}
    check(implementation!!.instructions.none{it.isMethodReference(abi.reference)}){"Shared factory already modified"}
    addInstructionsWithLabels(0,"""
        ${RuntimeCallEmitter.invoke(call,registers)}
        move-result-object v0
        if-eqz v0, :original
        return-object v0
    """.trimIndent(),ExternalLabel("original",original))
}

/** Preserve every register and result type while routing cached native interface calls through a typed bridge. */
internal fun applySharedRequestDispatch(context: BytecodePatchContext) {
    val calls=mapOf(
        "b" to RuntimeCallId.SHARED_WRITING_DISPATCH_B,
        "c" to RuntimeCallId.SHARED_WRITING_DISPATCH_C,
        "d" to RuntimeCallId.SHARED_WRITING_DISPATCH_D,
        "e" to RuntimeCallId.SHARED_WRITING_DISPATCH_E,
        "f" to RuntimeCallId.SHARED_WRITING_DISPATCH_F,
        "g" to RuntimeCallId.SHARED_WRITING_DISPATCH_G,
        "r" to RuntimeCallId.SHARED_WRITING_DISPATCH_CANCEL,
    )
    val all=mutableListOf<com.android.tools.smali.dexlib2.iface.ClassDef>()
    context.classDefForEach{all.add(it)}
    var changed=0
    for(owner in all.toList()) {
        if(owner.type.startsWith("LMorpheShared") || owner.type.startsWith("Ldev/jason/") || owner.type.startsWith("Lapp/johninnl/modelhost/"))continue
        val methods=owner.methods.filter{m->m.implementation?.instructions?.any{(it as? ReferenceInstruction)?.reference.let{ref->ref is MethodReference && ref.definingClass=="Ladvg;" && ref.name in calls}}==true}
        for(original in methods){
            val method=context.mutableClass(owner.type).methods.single{it.name==original.name && it.parameterTypes==original.parameterTypes && it.returnType==original.returnType}
            val instructions=method.implementation!!.instructions.toList()
            instructions.forEachIndexed{index,instruction->
                val reference=(instruction as? ReferenceInstruction)?.reference as? MethodReference?:return@forEachIndexed
                if(reference.definingClass!="Ladvg;")return@forEachIndexed
                val call=calls[reference.name]?:return@forEachIndexed
                val registers=when(instruction){
                    is RegisterRangeInstruction -> "v${instruction.startRegister} .. v${instruction.startRegister+instruction.registerCount-1}"
                    is FiveRegisterInstruction -> listOf(instruction.registerC,instruction.registerD,instruction.registerE,instruction.registerF,instruction.registerG).take(instruction.registerCount).joinToString(", "){"v$it"}
                    else -> error("Unexpected native writing invoke encoding")
                }
                val abi=RuntimeAbiCatalog.abi(call)
                check(abi.parameters==listOf("Ladvg;")+reference.parameterTypes.map(CharSequence::toString) && abi.returnType==reference.returnType){"Shared dispatch descriptor drift"}
                method.replaceInstruction(index,RuntimeCallEmitter.invoke(call,registers));changed++
            }
        }
    }
    check(changed>0){"No native writing calls were routed"}
}

internal fun installSharedEditorBridge(context:BytecodePatchContext){
    GboardMethodTarget("Lpdq;","x",listOf("Z"),"Laesl;").resolve(context)
    GboardMethodTarget("Lpdq;","U",listOf("Ljava/lang/CharSequence;","Ljava/lang/String;","I","Laeaj;"),"V").resolve(context)
    GboardFieldTarget("Lpdq;","y","Laesl;").resolve(context)
    context.addHelperMethodIfMissing("Lpdq;","morpheSharedPrepare",emptyList(),"Ljava/lang/String;",1,3,"""
        const/4 v0, 0x1
        invoke-virtual {p0, v0}, Lpdq;->x(Z)Laesl;
        move-result-object v0
        iput-object v0, p0, Lpdq;->y:Laesl;
        invoke-virtual {v0}, Laesl;->toString()Ljava/lang/String;
        move-result-object v0
        return-object v0
    """)
    context.addHelperMethodIfMissing("Lpdq;","morpheSharedApply",listOf("Ljava/lang/String;","Ljava/lang/String;"),"V",1,7,"""
        const-string v0, "shared-local"
        const/4 v1, 0x0
        ${RuntimeCallEmitter.invoke(RuntimeCallId.SHARED_EDITOR_OPERATION,"p2")}
        move-result-object v2
        invoke-virtual {p0, p1, v0, v1, v2}, Lpdq;->U(Ljava/lang/CharSequence;Ljava/lang/String;ILaeaj;)V
        return-void
    """)
    val activation=GboardMethodTarget("Lpdq;","n",listOf("Laeuj;","Landroid/view/inputmethod/EditorInfo;","Z","Ljava/util/Map;","Ladbd;"),"Z").resolve(context)
    val returns=activation.returnInstructionIndices().filter{activation.implementation!!.instructions[it].isOpcode("RETURN")}
    check(returns.isNotEmpty())
    returns.asReversed().forEach{activation.addInstructions(it,RuntimeCallEmitter.invoke(RuntimeCallId.SHARED_EDITOR_REMEMBER,"p0"))}
}
