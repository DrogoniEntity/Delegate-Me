package fr.drogonistudio.delegateme;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.objenesis.ObjenesisStd;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.TypeCache;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.dynamic.loading.ClassInjector;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.Implementation.Context;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.This;
import net.bytebuddy.implementation.bytecode.ByteCodeAppender;
import net.bytebuddy.jar.asm.MethodVisitor;
import net.bytebuddy.jar.asm.Opcodes;
import net.bytebuddy.matcher.ElementMatchers;

public final class DelegatorFactory
{
    
    /**
     * Extra field's name used to retrieve delegator.
     */
    public static final String EXTRA_FIELD_NAME = "_DELEGATEME_delegator";
    
    /**
     * Extra method's name used to invoke methods with less accessibility issues.
     */
    public static final String METHOD_RUNNER_NAME = "_DELEGATEME_runInvoke";
    
    /**
     * Cache where all proxy classes are stored.
     */
    private static final TypeCache<Class<?>> CLASSES_CACHE = new TypeCache<>(TypeCache.Sort.SOFT);
    
    /**
     * Objenesis instance used to instantiate object without calling any constructor
     * (needed to create wrapped objects).
     */
    private static final ObjenesisStd OBJENESIS = new ObjenesisStd();
    
    /**
     * Create a wrapped object with {@code delegator} as delegator.
     * 
     * <p>
     * A wrapped object will be a special object where any method invocations are
     * intercepted by {@code delegator}. We assume that the delegator will correctly
     * handle these invocation and use the right object to warp.
     * </p>
     * 
     * <p>
     * To allow this task, a new class will be generated (only if this class haven't
     * be created previously). This class will have as superclass {@code objectType}
     * and will be loaded with {@code objectType}'s class loader.
     * </p>
     * 
     * <p>
     * About generated class, this class will intercept all method. It will invoke
     * {@link Delegator#invoke(Method, Object[])} and use {@code delegator} as
     * invoker. To be able to retrieve {@code delegator}, a special field is added
     * named as {@link #EXTRA_FIELD_NAME} and it's accessible at any time. This
     * field will be set to {@code delegator}.
     * </p>
     * 
     * <p>
     * Once the proxy class is retrieved/generated, a new instance of
     * {@code objectType} is instantiate but no of these constructor are called.
     * From this, you should only use methods since fields couldn't be captured. You
     * can always get delegated object with {@link #unwarp(Object)}.
     * </p>
     * 
     * @param <Instance>
     *            object's type to warp
     * 	   
     * @param objectType
     *            object's type to warp
     * @param delegator
     *            delegator to use to handle any methods
     * @return warped object
     * @see #getDelegator(Object)
     */
    @SuppressWarnings("unchecked")
    public static <Instance> Instance warp(Class<Instance> objectType, Delegator<Instance> delegator)
    {
	// Getting class to use or create new one if not already created
	Class<?> proxyClass = CLASSES_CACHE.findOrInsert(objectType.getClassLoader(), objectType,
		() -> createProxyClass(objectType));
	Object warpped = OBJENESIS.getInstantiatorOf(proxyClass).newInstance();
	
	copyFields(delegator.getDelegated(), warpped);
	try
	{
	    proxyClass.getField(EXTRA_FIELD_NAME).set(warpped, delegator);
	} catch (ReflectiveOperationException ex)
	{
	    // Should not happen
	    ex.printStackTrace();
	}
	
	return (Instance) warpped;
    }
    
    /**
     * Retrieve delegator of {@code proxy}.
     * 
     * <p>
     * It will return value stored into {@link #EXTRA_FIELD_NAME} field. If this
     * field doesn't exist, an {@link java.lang.IllegalArgumentException} is thrown
     * because {@code proxy} isn't a poxied object.
     * </p>
     * 
     * @param <Instance>
     *            wrapped object's type
     * @param proxy
     *            wrapped object
     * @return delegator used to warp with {@link #warp(Class, Delegator)}
     * @throws IllegalArgumentException
     *             if {@code proxy} isn't a proxied object
     * @see #warp(Class, Delegator)
     */
    @SuppressWarnings("unchecked")
    public static <Instance> Delegator<Instance> getDelegator(Instance proxy) throws IllegalArgumentException
    {
	try
	{
	    Field delegatorField = proxy.getClass().getField(EXTRA_FIELD_NAME);
	    return (Delegator<Instance>) delegatorField.get(proxy);
	} catch (NoSuchFieldException ex)
	{
	    throw new IllegalArgumentException(proxy + " isn't a proxied object");
	} catch (Exception otherExceptions)
	{
	    // Should not happen
	    otherExceptions.printStackTrace();
	    return null;
	}
    }
    
    /**
     * Generate a new proxy class.
     * 
     * <p>
     * A proxy class will intercept any invocated method and delegate it to an
     * delegator. This delegator will be stored into future object.
     * </p>
     * 
     * <p>
     * The generated class will have a new field named {@link #EXTRA_FIELD_NAME}
     * with a public visibility (to make the access easier). This field will store
     * the used delegator.
     * </p>
     * 
     * @param <Type>
     *            delegated type
     * @param type
     *            delegated type
     * @return a proxy class which intercept any methods.
     */
    private static <Type> Class<?> createProxyClass(Class<Type> type)
    {
	ClassLoadingStrategy<ClassLoader> strategy = null;
	
	// Checking at possibility to generate new class before doing anything
	if (ClassInjector.UsingLookup.isAvailable())
	{
	    try
	    {
		Class<?> MethodHandles = Class.forName("java.lang.invoke.MethodHandles");
		Class<?> MethodHandles$Lookup = Class.forName("java.lang.invoke.MethodHandles$Lookup");
		
		Method $privateLookupIn = MethodHandles.getMethod("privateLookupIn", Class.class, MethodHandles$Lookup);
		Method $lookup = MethodHandles.getMethod("lookup");
		
		Object lookup = $lookup.invoke(null);
		Object lookupIn = $privateLookupIn.invoke(null, type, lookup);
		
		strategy = ClassLoadingStrategy.UsingLookup.of(lookupIn);
	    } catch (ReflectiveOperationException ex)
	    {
		// Should not happen
		ex.printStackTrace();
	    }
	} else if (ClassInjector.UsingReflection.isAvailable())
	{
	    strategy = ClassLoadingStrategy.Default.INJECTION;
	} else
	{
	    throw new IllegalStateException("not able to load any generated class");
	}
	
	// @formatter:off
	// Okay, let's creating class...
	return new ByteBuddy()
		// Setting class header
		.subclass(type)
		.name(type.getName().concat("$DelegateMeProxy"))
		
		// Add extra-field to remember current delegator (public access to avoid illegal access)
		// Warning: this field is modifiable by anybody. Asume nobody will change it
		.defineField(EXTRA_FIELD_NAME, Delegator.class, Visibility.PUBLIC)
		
		// Intercept all public methods
		.method(ElementMatchers.isPublic())
		.intercept(MethodDelegation.to(DelegatedMethodIntercepter.class))
		
		// Add utility method to bypass access to invoke method
		// Signature: public final Object _DELEGATEME_runInvoke(Method, Object[]) throws Throwable
		.defineMethod(METHOD_RUNNER_NAME, Object.class, Modifier.PUBLIC | Modifier.FINAL)
		.withParameters(Method.class, Object[].class)
		.throwing(Throwable.class)
		.intercept(new Implementation.Simple(new MethodRunnerCode()))
		
		// And generating
		.make()
		.load(type.getClassLoader(), strategy).getLoaded();
	// @formatter:on
    }
    
    /**
     * Copy all field value from {@code src} to {@code dest}.
     * 
     * <p>
     * This method will explore all declared fields from {@code src}'s class down to
     * {@link java.lang.Object} class. During exploration, all fields from
     * {@code src} are set to be accessible (if accessibility change failed, copy
     * isn't done) and got value is set to {@code dest}.
     * </p>
     * 
     * <p>
     * Any errors are printed on {@link java.lang.System#err} (but it should not
     * happen).
     * </p>
     * 
     * @param src
     *            source for copy
     * @param dest
     *            destination for copy
     */
    private static void copyFields(Object src, Object dest)
    {
	Class<?> currentClass = src.getClass();
	Class<?> superClass = currentClass.getSuperclass();
	
	while (currentClass != superClass)
	{
	    Field f[] = currentClass.getDeclaredFields();
	    for (int i = 0; i < f.length; i++)
	    {
		try
		{
		    // Need to use this method to keep compatibility with Java 8
		    @SuppressWarnings("deprecation")
		    boolean accessible = f[i].isAccessible();
		    f[i].setAccessible(true);
		    f[i].set(dest, f[i].get(src));
		    f[i].setAccessible(accessible);
		} catch (ReflectiveOperationException | SecurityException ex)
		{
		    // Okay, we're not allowed to do this...
		    ex.printStackTrace();
		}
	    }
	    
	    currentClass = superClass;
	    if (superClass != null)
		superClass = superClass.getSuperclass();
	}
    }
    
    /**
     * A method intercepter.
     * 
     * <p>
     * This intercepter will retrieve stored delegator and invoke
     * {@link Delegator#invoke(Method, Object[])}.
     * </p>
     * 
     * @author DrogoniEntity
     */
    public static class DelegatedMethodIntercepter
    {
	@RuntimeType
	public static Object intercept(@This Object proxy, @Origin Method method, @AllArguments Object args[])
		throws Throwable
	{
	    @SuppressWarnings("unchecked")
	    Delegator<Object> delegator = (Delegator<Object>) proxy.getClass().getField(EXTRA_FIELD_NAME).get(proxy);
	    return delegator.invoke(proxy, method, args);
	}
    }
    
    /**
     * Writing method invoker's code.
     * 
     * <p>
     * This method allow to bypass method visibility to be sure we can execute-it
     * without accessibility issue (like "couldn't invoke method from private class
     * in out-of-scope method).
     * </p>
     * 
     * @author DrogoniEntity
     */
    public static class MethodRunnerCode implements ByteCodeAppender
    {
	@Override
	public ByteCodeAppender.Size apply(MethodVisitor methodVisitor, Context implementationContext,
		MethodDescription instrumentedMethod)
	{
	    String delegatorOwner = Delegator.class.getName().replace('.', '/');
	    String delegatorDescriptor = "L" + delegatorOwner + ";";
	    
	    /* @formatter:off
	     * -----------------------------------
	     * // Java Code :
	     * 
	     * Object delegated = this.delegator.getDelegated(); return
	     * method.invoke(delegated, args);
	     * 
	     * ----------------------------------- 
	     * /* Bytecode (Eclipse format) : // Stack: 3, Locals: 4
	     * 
	     * aload_0 [this]
	     * getfield fr.drogonistudio.delegateme.Temp.delegator : fr.drogonistudio.delegateme.Delegator [23]
	     * invokevirtual fr.drogonistudio.delegateme.Delegator.getDelegated() : java.lang.Object [25]
	     * astore_3 [delegated]
	     * aload_1 [method]
	     * aload_3 [delegated]
	     * aload_2 [args]
	     * invokevirtual java.lang.reflect.Method.invoke(java.lang.Object, java.lang.Object[]) : java.lang.Object [31]
	     * areturn
	     * -----------------------------------
	     * @formatter:on*/
	    
	    methodVisitor.visitCode();
	    methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
	    methodVisitor.visitFieldInsn(Opcodes.GETFIELD, implementationContext.getInstrumentedType().getName(),
		    EXTRA_FIELD_NAME, delegatorDescriptor);
	    methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, delegatorOwner, "getDelegated", "()Ljava/lang/Object;",
		    false);
	    methodVisitor.visitVarInsn(Opcodes.ASTORE, 3);
	    methodVisitor.visitVarInsn(Opcodes.ALOAD, 1);
	    methodVisitor.visitVarInsn(Opcodes.ALOAD, 3);
	    methodVisitor.visitVarInsn(Opcodes.ALOAD, 2);
	    methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/reflect/Method", "invoke",
		    "(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;", false);
	    methodVisitor.visitInsn(Opcodes.ARETURN);
	    methodVisitor.visitEnd();
	    
	    return new ByteCodeAppender.Size(3, 4);
	}
	
    }
}
