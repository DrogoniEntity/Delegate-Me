import java.io.PrintStream;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import fr.drogonistudio.delegateme.Delegator;

public class InvocationCounter<Type> extends Delegator<Type>
{
    private Map<Method, Integer> counter;
    
    public InvocationCounter(Type delegated)
    {
	super(delegated);
	this.counter = new ConcurrentHashMap<>();
    }

    @Override
    public Object invoke(Type proxy, Method method, Object[] args) throws Throwable
    {
	this.counter.put(method, this.counter.getOrDefault(method, 0) + 1);
	Throwable failed = null;
	Object invocationResult = null;
	
	try
	{
	    invocationResult = this.delegate(proxy, method, args);
	    this.updateProxyFieldsValue(proxy, EqualsCompareStrategy.BY_REFERENCE);
	}
	catch (Throwable t)
	{
	    failed = t;
	}
	
	if (failed != null)
	    throw failed;
	
	return invocationResult;
    }
    
    public void printCounter(PrintStream out)
    {
	System.out.println("##################");
	for (Method key : this.counter.keySet())
	    System.out.printf("- %s => %d\n", key.toGenericString(), this.counter.getOrDefault(key, 0));
	System.out.println("##################");
    }
}
