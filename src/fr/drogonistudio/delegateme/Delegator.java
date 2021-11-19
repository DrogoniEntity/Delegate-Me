package fr.drogonistudio.delegateme;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * A proxy class used to delegate something.
 * 
 * <p>
 * A delegator allow user to warp a object and to enchance object's methods to
 * perform some special taks depends on method being invoked.
 * </p>
 * 
 * <p>
 * Any intercepted method are called in
 * {@link #invoke(Object, Method, Object[])} and delegator can perform
 * additionnal tasks during method invocation.
 * {@link #delegate(Method, Object[])} will perform original method task during
 * an invocation (delegator must use this method in its
 * {@link #invoke(Object, Method, Object[])} implementation.
 * </p>
 * 
 * <p>
 * A delegator act like an {@link java.lang.reflect.InvocationHandler invocation
 * handler} but store object to delegate and know delegated type and purpose
 * some useful methods to handle on proxy objects.
 * </p>
 * 
 * @author DrogoniEntity
 * @param <Delegated>
 *            Object's type to delegate
 * @see DelegatorFactory {@code DelegatorFactory} to warp delegated method.
 */
public abstract class Delegator<Delegated>
{
    
    /**
     * Object to delegate.
     * 
     * @see #getDelegated()
     */
    protected final Delegated delegated;
    
    /**
     * Setup delegation by storing object to delegate
     * 
     * @param delegated
     *            object to delegate
     */
    public Delegator(Delegated delegated)
    {
	if (delegated == null)
	    throw new NullPointerException("delegator must delegate something");
	
	this.delegated = delegated;
    }
    
    /**
     * Handle {@code method} invocation.
     * 
     * <p>
     * From this method, any tasks to perform during method invocation are done
     * here. Delegator can perform different task depend on {@code method}.
     * </p>
     * 
     * <p>
     * To perform default behavior, {@link #delegate(Method, Object[])} will invoke
     * {@code method} on {@link #delegated} with parameters {@code args}. You can
     * call default behavior at any time and perform some special task depend on
     * invocation return.
     * </p>
     * 
     * @param method
     *            invoked method
     * @param args
     *            passed parameters
     * @return invocation return value
     * @throws Throwable
     *             if something went wrong during invocation process
     * @see #delegate(Method, Object[])
     * @see java.lang.reflect.InvocationHandler#invoke(Object, Method, Object[])
     */
    public abstract Object invoke(Delegated proxy, Method method, Object args[]) throws Throwable;
    
    /**
     * Invoke default behavior of {@code method} on {@link #delegated}.
     * 
     * <p>
     * This method will simply done base job of {@code method} on
     * {@link #delegated}. It will return method invocation's result or {@code null}
     * if method return {@code void} type.
     * </p>
     * 
     * <p>
     * To limit accessibility issue, a special method is used to invoke
     * {@code method}. This method exist only in {@code proxy}'s class.
     * </p>
     * 
     * @param method
     *            invoked method
     * @param args
     *            passed parameters
     * @return invocation result
     * @throws IllegalArgumentException
     *             if {@code proxy} is not proxied object
     * @throws Throwable
     *             if something went wrong during invocation process
     * @see java.lang.reflect.Method#invoke(Object, Object...)
     */
    protected final Object delegate(Delegated proxy, Method method, Object args[])
	    throws IllegalArgumentException, Throwable
    {
	try
	{
	    Method invoker = proxy.getClass().getMethod(DelegatorFactory.METHOD_RUNNER_NAME, Method.class,
		    Object[].class);
	    return invoker.invoke(proxy, method, args);
	} catch (NoSuchMethodException methodNotFound)
	{
	    throw new IllegalArgumentException("object is not proxied object");
	}
    }
    
    /**
     * Updating all fields value to {@code proxy}.
     * 
     * <p>
     * This method will explore all declared fields from delegated's class down to
     * {@link java.lang.Object} class. It will compare fields content between two
     * objects with selected strategy. It will copy only changed field
     * since last update.
     * </p>
     * 
     * @param proxy
     *            proxy object
     * @param strategy
     *            compare strategy
     * @throws NullPointerException
     *             if {@code strategy} or {@code proxy} are null
     * @see #updateDelegatedFieldsValue(Object, EqualsCompareStrategy) Updating
     *      delegated object fields' value instead
     */
    public final void updateProxyFieldsValue(Delegated proxy, EqualsCompareStrategy strategy)
	    throws NullPointerException
    {
	this.copyFields(this.delegated, proxy, strategy);
    }
    
    /**
     * Updating all fields value to delegated object from {@ode proxy}.
     * 
     * <p>
     * This method will explore all declared fields from delegated's class down to
     * {@link java.lang.Object} class. It will compare fields content between two
     * objects with selected strategy. It will copy only changed field
     * since last update.
     * </p>
     * 
     * @param proxy
     *            proxy object
     * @param strategy
     *            compare strategy
     * @throws NullPointerException
     *             if {@code strategy} or {@code proxy} are null
     * @see #updateProxyFieldsValue(Object, EqualsCompareStrategy) Updating
     *      {@code proxy} fields' value instead
     */
    public final void updateDelegatedFieldsValue(Delegated proxy, EqualsCompareStrategy strategy)
	    throws NullPointerException
    {
	this.copyFields(proxy, this.delegated, strategy);
    }
    
    /**
     * Utility method to copy all fields value from {@code src} to {@code dest}.
     * 
     * <p>
     * This method will explore all declared fields from {@link #delegated}'s class
     * down to {@link java.lang.Object} class. It will compare fields content
     * between two objects with selected strategy. It will copy only changed field
     * since last update.
     * </p>
     * 
     * @param src
     *            object source for copy
     * @param dest
     *            object destination for copy
     * @param srcClass
     *            first class to explore
     * @param strategy
     *            compare strategy
     * @throws NullPointerException
     *             if {@code strategy}, {@code src} or {@code dest} are null
     */
    private void copyFields(Delegated src, Delegated dest, EqualsCompareStrategy strategy) throws NullPointerException
    {
	if (strategy == null)
	    throw new NullPointerException("excepted an equals strategy but got null");
	if (src == null || dest == null)
	    throw new NullPointerException("objects to copy may not be null");
	
	Class<?> currentClass = this.delegated.getClass();
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
		    Object srcValue = f[i].get(src);
		    Object destValue = f[i].get(dest);
		    
		    // Copy only if values are different
		    if (!strategy.equals(srcValue, destValue))
		    {
			f[i].set(dest, srcValue);
		    }
		    
		    f[i].setAccessible(accessible);
		} catch (Exception ex)
		{
		    ex.printStackTrace();
		}
	    }
	    
	    currentClass = superClass;
	    if (superClass != null)
		superClass = superClass.getSuperclass();
	}
    }
    
    /**
     * Getting delegated instance.
     * 
     * @return delegated instance
     */
    public final Delegated getDelegated()
    {
	return this.delegated;
    }
    
    /**
     * Compare strategy
     * 
     * <p>
     * Compare strategy will allow user to select how to check if two fields are
     * equals or not.
     * </p>
     * 
     * <p>
     * This enumeration contains two strategy :
     * </p>
     * <ul>
     * <li>Checking on reference (simple done {@code o1 == o2}</li>
     * <li>Checking by invoking {@link java.lang.Object#equals}</li>
     * </ul>
     * 
     * @author DrogoniEntity
     */
    public static enum EqualsCompareStrategy
    {
	
	/**
	 * Compare two objects by their reference.
	 */
	BY_REFERENCE()
	{
	    
	    @Override
	    public boolean equals(Object o1, Object o2)
	    {
		// Simply compare references...
		return o1 == o2;
	    }
	},
	
	/**
	 * Compare two object by invoking {@link java.lang.Object#equals(Object)}.
	 * 
	 * <p>
	 * To avoid error with null pointer, some checks are done before invoking
	 * {@link java.lang.Object#equals(Object) equals} method, these objects are
	 * compared to know if their are {@code null} or not. It both are {@code null},
	 * then it will return {@code true}. Otherwise, it will return {@code false} if
	 * one of these 2 objects is null and the other not.
	 * </p>
	 */
	BY_EQUALS_METHOD()
	{
	    
	    @Override
	    public boolean equals(Object o1, Object o2)
	    {
		// If both are null, their are equals
		if (o1 == null && o2 == null)
		    return true;
		// If both are non-null, invoke 'equals'
		else if (o1 != null && o2 != null)
		    return o1.equals(o2);
		// At this stage, one of these 2 object is null and other isn't
		else
		    return false;
	    }
	};
	
	/**
	 * Checking if {@code o1} is equals to {@code o2}.
	 * 
	 * <p>
	 * Depends on selected strategy, this method will check differently.
	 * </p>
	 * 
	 * @param o1
	 *            first to compare
	 * @param o2
	 *            second to compare
	 * @return {@code true} if {@code o1} is equals to {@code o2}.
	 */
	public boolean equals(Object o1, Object o2)
	{
	    return false;
	}
    }
}
