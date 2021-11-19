import java.lang.reflect.Method;

import fr.drogonistudio.delegateme.Delegator;

public class InvocationTracer<Type> extends Delegator<Type>
{
    public InvocationTracer(Type delegated)
    {
	super(delegated);
    }
    
    @Override
    public Object invoke(Type proxy, Method method, Object[] args) throws Throwable
    {
	System.out.printf("#> Invoke \"%s\" on \"%s\" with [", method.toGenericString(), this.delegated.toString());
	for (int i = 0; i < args.length; i++)
	{
	    System.out.print(args[i].toString());
	    if (i < (args.length - 1))
		System.out.print(", ");
	}
	System.out.println("]");
	
	// Working...
	Object returnValue = this.delegate(proxy, method, args);
	this.updateProxyFieldsValue(proxy, EqualsCompareStrategy.BY_REFERENCE);
	
	if (returnValue != null)
	    System.out.println("#> Return \"" + returnValue.toString() + "\"");
	return returnValue;
    }
}
